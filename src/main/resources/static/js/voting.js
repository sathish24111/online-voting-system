// Voting Controller

let activeElection = null;
let candidatesList = [];
// Key: Position name, Value: Selected candidate ID
let selections = {};

async function loadVotingPage() {
    const session = await requireSession('STUDENT');
    if (!session) return;

    showLoading(true);
    try {
        const response = await fetch('/api/voting/active-election');
        if (response.status === 404) {
            window.location.href = '/pages/student-dashboard.html';
            return;
        }

        const data = await response.json();
        activeElection = data.election;
        candidatesList = data.candidates;

        if (data.hasVoted) {
            window.location.href = '/pages/already-voted.html';
            return;
        }

        // Set Title
        document.getElementById('election-header-title').innerText = activeElection.title;

        // Render positions & candidate cards
        renderVotingBallot();

    } catch (e) {
        showToast('Error loading ballot. Please refresh.', 'error');
    } finally {
        showLoading(false);
    }
}

function renderVotingBallot() {
    const container = document.getElementById('ballot-container');
    container.innerHTML = '';

    // Group candidates by position
    const grouped = {};
    candidatesList.forEach(c => {
        if (!grouped[c.position]) {
            grouped[c.position] = [];
        }
        grouped[c.position].push(c);
    });

    if (Object.keys(grouped).length === 0) {
        container.innerHTML = '<div class="glass-card text-center"><p>No candidates registered for this election yet.</p></div>';
        return;
    }

    // Sort positions for consistent display
    const sortedPositions = Object.keys(grouped).sort();

    sortedPositions.forEach(position => {
        const posSection = document.createElement('div');
        posSection.className = 'glass-card mb-4';
        posSection.style.marginBottom = '2rem';
        
        posSection.innerHTML = `
            <h3 class="mb-3" style="border-bottom: 2px solid var(--border-color); padding-bottom: 0.5rem; text-align: left;">
                <i class="fas fa-user-tag text-gradient"></i> ${position}
            </h3>
            <p class="mb-4" style="text-align: left;">Select one candidate for the post of ${position}:</p>
            <div class="card-grid" id="position-${position.replace(/\s+/g, '-')}"></div>
        `;

        container.appendChild(posSection);
        
        const grid = posSection.querySelector(`#position-${position.replace(/\s+/g, '-')}`);
        const positionCandidates = grouped[position];

        positionCandidates.forEach(cand => {
            const card = document.createElement('div');
            card.className = 'glass-card candidate-card';
            card.id = `cand-card-${cand.id}`;
            
            card.innerHTML = `
                <div class="candidate-photo-container">
                    <img class="candidate-photo" src="${cand.photo || '/images/default-candidate.png'}" alt="${cand.name}">
                </div>
                <h4>${cand.name}</h4>
                <p style="font-size: 0.85rem; font-weight: 500; color: var(--primary-light); margin-bottom: 0.5rem;">${cand.department} - ${cand.year}</p>
                <div style="font-size: 0.9rem; text-align: left; margin: 1rem 0; width: 100%;">
                    <strong>Achievements:</strong>
                    <p style="font-size: 0.85rem; margin-bottom: 0.5rem;">${cand.manifesto ? cand.manifesto.split('\n')[0] : 'No record.'}</p>
                </div>
                <button class="btn btn-secondary w-100 select-candidate-btn" onclick="selectCandidate('${position}', ${cand.id})">
                    <i class="far fa-circle"></i> Select Candidate
                </button>
            `;

            grid.appendChild(card);
        });
    });

    // Add submit button wrapper
    const submitWrapper = document.createElement('div');
    submitWrapper.className = 'text-center mt-5';
    submitWrapper.innerHTML = `
        <button class="btn btn-primary btn-lg" style="padding: 1rem 3rem; font-size: 1.1rem;" onclick="openConfirmationModal()">
            <i class="fas fa-paper-plane"></i> Submit Ballot
        </button>
    `;
    container.appendChild(submitWrapper);
}

function selectCandidate(position, candidateId) {
    // 1. Get all candidate cards in this position grid
    const grouped = candidatesList.filter(c => c.position === position);
    
    grouped.forEach(c => {
        const card = document.getElementById(`cand-card-${c.id}`);
        if (card) {
            card.style.border = '1px solid var(--border-color)';
            card.style.boxShadow = 'var(--shadow-glass)';
            const btn = card.querySelector('.select-candidate-btn');
            btn.className = 'btn btn-secondary w-100 select-candidate-btn';
            btn.innerHTML = '<i class="far fa-circle"></i> Select Candidate';
        }
    });

    // 2. Select the chosen candidate
    selections[position] = candidateId;
    
    const card = document.getElementById(`cand-card-${candidateId}`);
    if (card) {
        card.style.border = '2px solid var(--primary-color)';
        card.style.boxShadow = '0 0 15px rgba(37, 99, 235, 0.2)';
        const btn = card.querySelector('.select-candidate-btn');
        btn.className = 'btn btn-primary w-100 select-candidate-btn';
        btn.innerHTML = '<i class="fas fa-check-circle"></i> Selected';
    }
}

function openConfirmationModal() {
    // Check if voter has selected at least one candidate
    const selectedIds = Object.values(selections);
    if (selectedIds.length === 0) {
        showToast('Please select at least one candidate before submitting.', 'warning');
        return;
    }

    // Render review items in the modal
    const list = document.getElementById('review-selections-list');
    list.innerHTML = '';

    for (const [pos, candId] of Object.entries(selections)) {
        const cand = candidatesList.find(c => c.id === candId);
        if (cand) {
            const li = document.createElement('li');
            li.style.display = 'flex';
            li.style.justifyContent = 'space-between';
            li.style.padding = '0.5rem 0';
            li.style.borderBottom = '1px dashed var(--border-color)';
            li.innerHTML = `
                <span style="font-weight:600; color:var(--text-secondary);">${pos}</span>
                <span style="font-weight:700; color:var(--primary-color);">${cand.name}</span>
            `;
            list.appendChild(li);
        }
    }

    document.getElementById('confirm-modal').style.display = 'flex';
}

function closeConfirmationModal() {
    document.getElementById('confirm-modal').style.display = 'none';
}

async function submitBallot() {
    closeConfirmationModal();
    const candidateIds = Object.values(selections);

    showLoading(true);
    try {
        const response = await fetch('/api/voting/cast', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({
                electionId: activeElection.id,
                candidateIds: candidateIds
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Redirect to index.html with a query param to display a beautiful toast banner
            window.location.href = '/index.html?voted=true';
        } else {
            showToast(data.error || 'Failed to submit ballot.', 'error');
        }
    } catch (e) {
        showToast('Submission error. Server unreachable.', 'error');
    } finally {
        showLoading(false);
    }
}
