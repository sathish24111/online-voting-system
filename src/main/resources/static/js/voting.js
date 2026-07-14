// Voting Controller

let activeElection = null;
let candidatesList = [];
// Key: Position name, Value: Selected candidate ID
let selections = {};
let positionsList = [];
let currentStepIndex = 0;

const POSITION_ORDER = ["Secretary", "Joint Secretary", "Treasurer", "Chairman", "Vice Chairman"];

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

        // Group candidates by position
        const grouped = {};
        candidatesList.forEach(c => {
            if (!grouped[c.position]) {
                grouped[c.position] = [];
            }
            grouped[c.position].push(c);
        });

        // Establish positions list
        positionsList = Object.keys(grouped).sort((a, b) => {
            const idxA = POSITION_ORDER.indexOf(a);
            const idxB = POSITION_ORDER.indexOf(b);
            if (idxA !== -1 && idxB !== -1) return idxA - idxB;
            if (idxA !== -1) return -1;
            if (idxB !== -1) return 1;
            return a.localeCompare(b);
        });

        if (positionsList.length === 0) {
            const container = document.getElementById('ballot-container');
            container.innerHTML = '<div class="glass-card text-center"><p>No candidates registered for this election yet.</p></div>';
            return;
        }

        // Show progress wrapper
        document.getElementById('progress-wrapper').style.display = 'block';

        // Render current step
        renderStep();

    } catch (e) {
        showToast('Error loading ballot. Please refresh.', 'error');
    } finally {
        showLoading(false);
    }
}

function renderStep() {
    const container = document.getElementById('ballot-container');
    container.innerHTML = '';

    // If we are on the Review step
    if (currentStepIndex === positionsList.length) {
        renderReviewPage(container);
        return;
    }

    // Normal position selection step
    const currentPosition = positionsList[currentStepIndex];
    const grouped = {};
    candidatesList.forEach(c => {
        if (!grouped[c.position]) grouped[c.position] = [];
        grouped[c.position].push(c);
    });
    const positionCandidates = grouped[currentPosition] || [];

    // Update Progress Indicator
    document.getElementById('progress-wrapper').style.display = 'block';
    document.getElementById('step-indicator').innerText = `Step ${currentStepIndex + 1} of ${positionsList.length}`;
    const progressPercent = ((currentStepIndex + 1) / positionsList.length) * 100;
    document.getElementById('progress-bar-fill').style.width = `${progressPercent}%`;

    // Render current position details
    const stepSection = document.createElement('div');
    stepSection.className = 'glass-card mb-4';
    stepSection.style.marginBottom = '2rem';
    stepSection.innerHTML = `
        <h2 class="mb-3" style="border-bottom: 2px solid var(--border-color); padding-bottom: 0.75rem; text-align: left;">
            <i class="fas fa-user-tag text-gradient"></i> ${currentPosition}
        </h2>
        <p class="mb-4" style="text-align: left; color: var(--text-secondary);">Select one candidate for the post of ${currentPosition}:</p>
        <div class="card-grid" id="candidates-step-grid"></div>
    `;
    container.appendChild(stepSection);

    const grid = stepSection.querySelector('#candidates-step-grid');
    positionCandidates.forEach(cand => {
        const card = document.createElement('div');
        card.className = 'glass-card candidate-card';
        card.id = `cand-card-${cand.id}`;

        // Highlight card if already selected
        const isSelected = selections[currentPosition] === cand.id;
        const cardBorder = isSelected ? '2px solid var(--primary-color)' : '1px solid var(--border-color)';
        const cardShadow = isSelected ? '0 0 15px rgba(37, 99, 235, 0.2)' : 'var(--shadow-glass)';
        const btnClass = isSelected ? 'btn btn-primary w-100 select-candidate-btn' : 'btn btn-secondary w-100 select-candidate-btn';
        const btnIcon = isSelected ? '<i class="fas fa-check-circle"></i> Selected' : '<i class="far fa-circle"></i> Select Candidate';

        card.style.border = cardBorder;
        card.style.boxShadow = cardShadow;

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
            <button class="${btnClass}" onclick="selectStepCandidate('${currentPosition}', ${cand.id})">
                ${btnIcon}
            </button>
        `;
        grid.appendChild(card);
    });

    // Navigation buttons section
    const navSection = document.createElement('div');
    navSection.className = 'wizard-buttons';
    navSection.style.display = 'flex';
    navSection.style.justifyContent = 'space-between';
    navSection.style.marginTop = '2rem';

    const isFirstStep = currentStepIndex === 0;
    const isLastPosition = currentStepIndex === positionsList.length - 1;

    const prevDisabledAttr = isFirstStep ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : '';
    const nextBtnText = isLastPosition ? 'Review Votes <i class="fas fa-clipboard-list"></i>' : 'Next <i class="fas fa-chevron-right"></i>';
    const hasSelection = selections[currentPosition] !== undefined;
    const nextDisabledAttr = !hasSelection ? 'disabled' : '';

    navSection.innerHTML = `
        <button id="prev-btn" class="btn btn-secondary" style="padding: 0.85rem 2.5rem;" onclick="prevStep()" ${prevDisabledAttr}>
            <i class="fas fa-chevron-left"></i> Previous
        </button>
        <button id="next-btn" class="btn btn-primary" style="padding: 0.85rem 2.5rem;" onclick="nextStep()" ${nextDisabledAttr}>
            ${nextBtnText}
        </button>
    `;
    container.appendChild(navSection);
}

function selectStepCandidate(position, candidateId) {
    selections[position] = candidateId;

    // Render updates to cards locally to maintain responsiveness without full layout redraw
    const grouped = candidatesList.filter(c => c.position === position);
    grouped.forEach(c => {
        const card = document.getElementById(`cand-card-${c.id}`);
        if (card) {
            const isChosen = c.id === candidateId;
            card.style.border = isChosen ? '2px solid var(--primary-color)' : '1px solid var(--border-color)';
            card.style.boxShadow = isChosen ? '0 0 15px rgba(37, 99, 235, 0.2)' : 'var(--shadow-glass)';
            const btn = card.querySelector('.select-candidate-btn');
            btn.className = isChosen ? 'btn btn-primary w-100 select-candidate-btn' : 'btn btn-secondary w-100 select-candidate-btn';
            btn.innerHTML = isChosen ? '<i class="fas fa-check-circle"></i> Selected' : '<i class="far fa-circle"></i> Select Candidate';
        }
    });

    // Enable next button
    const nextBtn = document.getElementById('next-btn');
    if (nextBtn) {
        nextBtn.removeAttribute('disabled');
    }
}

function nextStep() {
    if (currentStepIndex < positionsList.length) {
        const currentPosition = positionsList[currentStepIndex];
        if (selections[currentPosition] === undefined) {
            showToast('Please select a candidate before proceeding.', 'warning');
            return;
        }
        currentStepIndex++;
        renderStep();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function prevStep() {
    if (currentStepIndex > 0) {
        currentStepIndex--;
        renderStep();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function jumpToStep(stepIndex) {
    if (stepIndex >= 0 && stepIndex < positionsList.length) {
        currentStepIndex = stepIndex;
        renderStep();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function renderReviewPage(container) {
    // Hide progress bar for the final verification state
    document.getElementById('progress-wrapper').style.display = 'none';

    const reviewSection = document.createElement('div');
    reviewSection.className = 'glass-card mb-4';
    reviewSection.style.marginBottom = '2rem';
    reviewSection.style.padding = '2.5rem 2rem';
    reviewSection.style.textAlign = 'left';

    let tableRows = '';
    positionsList.forEach((pos, idx) => {
        const candId = selections[pos];
        const cand = candidatesList.find(c => c.id === candId);
        const candName = cand ? cand.name : '<span style="color: var(--danger-color); font-weight:600;">Not Selected</span>';
        
        tableRows += `
            <tr style="border-bottom: 1px solid var(--border-color);">
                <td style="padding: 1rem 0.5rem; font-weight: 700; color: var(--text-primary);">${pos}</td>
                <td style="padding: 1rem 0.5rem; color: var(--primary-color); font-weight: 600;">${candName}</td>
                <td style="padding: 1rem 0.5rem; text-align: right;">
                    <button class="review-change-btn" onclick="jumpToStep(${idx})">
                        <i class="fas fa-edit"></i> Change
                    </button>
                </td>
            </tr>
        `;
    });

    reviewSection.innerHTML = `
        <h2 class="mb-3" style="border-bottom: 2px solid var(--border-color); padding-bottom: 0.75rem;"><i class="fas fa-clipboard-check text-gradient"></i> Review Your Selections</h2>
        <p class="mb-4" style="color: var(--text-secondary);">Verify your selections below. You can go back or modify individual positions. Once submitted, your vote is final and cannot be modified.</p>
        
        <div class="table-container" style="overflow-x: auto; margin-bottom: 2rem;">
            <table class="review-table" style="width:100%; border-collapse:collapse;">
                <thead>
                    <tr style="border-bottom: 2px solid var(--border-color);">
                        <th style="padding: 0.75rem 0.5rem; text-align:left;">Position</th>
                        <th style="padding: 0.75rem 0.5rem; text-align:left;">Candidate Chosen</th>
                        <th style="padding: 0.75rem 0.5rem; text-align:right;">Modify</th>
                    </tr>
                </thead>
                <tbody>
                    ${tableRows}
                </tbody>
            </table>
        </div>

        <div style="background: rgba(239, 68, 68, 0.08); padding: 1rem; border-radius: var(--radius-md); margin-bottom: 2rem; color: var(--danger-color); font-weight: 600; font-size: 0.9rem; text-align: center; border: 1px solid rgba(239, 68, 68, 0.15);">
            <i class="fas fa-info-circle"></i> Once submitted, your vote is recorded anonymously and cannot be updated.
        </div>

        <div class="wizard-buttons" style="display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap;">
            <button class="btn btn-secondary" style="padding: 0.85rem 2.5rem;" onclick="prevStep()">
                <i class="fas fa-chevron-left"></i> Previous
            </button>
            <button class="btn btn-primary" style="padding: 0.85rem 2.5rem; background: var(--success-color); border-color: var(--success-color);" onclick="submitBallot()">
                <i class="fas fa-check-circle"></i> Confirm & Submit
            </button>
        </div>
    `;
    container.appendChild(reviewSection);
}

async function submitBallot() {
    const candidateIds = Object.values(selections);

    // Validate that all positions have selections before sending
    if (candidateIds.length < positionsList.length) {
        showToast('Please make sure you have selected a candidate for all positions.', 'warning');
        return;
    }

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
