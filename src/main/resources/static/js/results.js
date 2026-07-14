// Results Controller

let resultsData = null;
let charts = {};

async function loadResultsPage() {
    // Determine user role and load appropriate session
    const statusResponse = await fetch('/api/auth/status');
    const session = await statusResponse.json();

    if (!session.loggedIn) {
        window.location.href = '/index.html';
        return;
    }

    const isAdmin = session.role === 'ADMIN';
    
    // Fill Sidebar menu links or header appropriately
    const backBtn = document.getElementById('back-btn');
    if (backBtn) {
        backBtn.href = isAdmin ? '/pages/admin-dashboard.html' : '/pages/student-dashboard.html';
    }

    // Load elections list for selection dropdown
    await loadElectionsDropdown(isAdmin);
}

async function loadElectionsDropdown(isAdmin) {
    try {
        const url = isAdmin ? '/api/admin/elections' : '/api/voting/active-election';
        const response = await fetch(url);
        
        let elections = [];
        if (response.ok) {
            if (isAdmin) {
                elections = await response.json();
            } else {
                const active = await response.json();
                elections = [active.election];
            }
        }

        const dropdown = document.getElementById('election-select');
        if (!dropdown) return;

        dropdown.innerHTML = '';
        if (elections.length === 0) {
            dropdown.innerHTML = '<option value="">No elections available</option>';
            document.getElementById('results-view-container').innerHTML = `
                <div class="glass-card text-center">
                    <p>No results are available at this moment.</p>
                </div>
            `;
            return;
        }

        elections.forEach(e => {
            const opt = document.createElement('option');
            opt.value = e.id;
            opt.innerText = e.title + (e.status === 'ENDED' ? ' (Ended)' : ' (Active)');
            dropdown.appendChild(opt);
        });

        // Load results for the first election
        dropdown.addEventListener('change', () => fetchResults(dropdown.value, isAdmin));
        fetchResults(dropdown.value, isAdmin);

    } catch (e) {
        showToast('Failed to load elections list.', 'error');
    }
}

async function fetchResults(electionId, isAdmin) {
    if (!electionId) return;
    
    showLoading(true);
    try {
        const response = await fetch(`/api/results/${electionId}`);
        const container = document.getElementById('results-view-container');

        if (!response.ok) {
            const err = await response.json();
            container.innerHTML = `
                <div class="glass-card text-center">
                    <i class="fas fa-eye-slash text-gradient" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                    <h3>Results Unavailable</h3>
                    <p class="mt-2">${err.error || 'Results are hidden until the election ends.'}</p>
                </div>
            `;
            // Hide export buttons
            document.getElementById('export-actions-card').style.display = 'none';
            return;
        }

        resultsData = await response.json();
        
        // Show export buttons
        document.getElementById('export-actions-card').style.display = 'flex';
        
        renderResultsDashboard();

    } catch (e) {
        showToast('Error retrieving election results.', 'error');
    } finally {
        showLoading(false);
    }
}

function renderResultsDashboard() {
    // 1. Fill turnout cards
    document.getElementById('stat-total-voters').innerText = resultsData.totalStudents;
    document.getElementById('stat-votes-cast').innerText = resultsData.votesCast;
    document.getElementById('stat-turnout-rate').innerText = resultsData.participationRate + '%';
    
    const remaining = resultsData.totalStudents - resultsData.votesCast;
    document.getElementById('stat-votes-remaining').innerText = remaining >= 0 ? remaining : 0;

    // 2. Render winner cards and charts
    const winnerGrid = document.getElementById('winners-container');
    const chartsContainer = document.getElementById('charts-container');
    
    winnerGrid.innerHTML = '';
    chartsContainer.innerHTML = '';

    // Destroy existing Chart.js instances
    Object.values(charts).forEach(c => c.destroy());
    charts = {};

    const positions = resultsData.resultsByPosition;

    if (Object.keys(positions).length === 0) {
        winnerGrid.innerHTML = '<p class="w-100 text-center">No votes recorded yet.</p>';
        return;
    }

    for (const [position, candidates] of Object.entries(positions)) {
        // Find all Winners or Ties
        const winners = candidates.filter(c => c.label === 'WINNER' || c.label === 'TIE');
        
        winners.forEach(winner => {
            const isTie = winner.label === 'TIE';
            const badgeText = isTie ? '<i class="fas fa-handshake"></i> Tie' : '<i class="fas fa-crown text-warning"></i> Winner';
            const badgeColor = isTie ? 'var(--warning-color)' : 'var(--success-color)';
            const borderColor = isTie ? 'var(--warning-color)' : 'var(--success-color)';
            
            const winnerCard = document.createElement('div');
            winnerCard.className = 'glass-card candidate-card';
            winnerCard.style.padding = '1.5rem';
            winnerCard.innerHTML = `
                <span class="candidate-badge" style="background:${badgeColor}; color:white;">${badgeText}</span>
                <div class="candidate-photo-container" style="width:90px; height:90px; border-color:${borderColor};">
                    <img class="candidate-photo" src="${winner.photo || '/images/default-candidate.png'}" alt="${winner.name}">
                </div>
                <h4 style="font-size:1.1rem; margin-bottom:0.25rem;">${winner.name}</h4>
                <p style="font-size:0.8rem; font-weight:600; text-transform:uppercase; color:var(--text-muted);">${position}</p>
                <p style="font-size:0.85rem; color:${borderColor}; font-weight:700; margin-top:0.5rem;">
                    ${winner.voteCount} Votes (${winner.percentage}%)
                </p>
            `;
            winnerGrid.appendChild(winnerCard);
        });

        // Render Chart Container
        const chartWrapper = document.createElement('div');
        chartWrapper.className = 'glass-card';
        chartWrapper.style.padding = '2rem';
        chartWrapper.innerHTML = `
            <h3 class="mb-4" style="text-align: left; font-size:1.2rem;">
                <i class="fas fa-chart-bar text-gradient"></i> ${position} Vote Tally
            </h3>
            <div style="position: relative; height: 260px; width: 100%;">
                <canvas id="chart-${position.replace(/\s+/g, '-')}"></canvas>
            </div>
        `;
        chartsContainer.appendChild(chartWrapper);

        // Draw Chart.js Bar Chart
        const canvas = chartWrapper.querySelector('canvas');
        const labels = candidates.map(c => c.name);
        const dataValues = candidates.map(c => c.voteCount);
        const colors = candidates.map((c, i) => i === 0 ? '#10b981' : (i === 1 ? '#3b82f6' : '#64748b'));

        const ctx = canvas.getContext('2d');
        charts[position] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Votes Received',
                    data: dataValues,
                    backgroundColor: colors,
                    borderRadius: 6,
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1, color: '#94a3b8' },
                        grid: { color: 'rgba(148, 163, 184, 0.1)' }
                    },
                    x: {
                        ticks: { color: '#94a3b8' },
                        grid: { display: false }
                    }
                }
            }
        });
    }
}

// Download Exporters
async function triggerExport(format) {
    const dropdown = document.getElementById('election-select');
    const electionId = dropdown.value;
    if (!electionId) return;

    showToast(`Preparing results report as ${format.toUpperCase()}...`, 'info');
    try {
        const response = await fetch(`/api/results/${electionId}/export/${format}`);
        if (!response.ok) throw new Error("Failed to generate download.");

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        
        let extension = format;
        if (format === 'excel') extension = 'xlsx';
        a.download = `results_election_${electionId}.${extension}`;
        
        document.body.appendChild(a);
        a.click();
        
        window.URL.revokeObjectURL(url);
        a.remove();
        showToast(`Results report downloaded successfully.`, 'success');
    } catch (e) {
        showToast('Failed to download results: ' + e.message, 'error');
    }
}

function printResults() {
    window.print();
}
