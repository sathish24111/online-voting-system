let allRecords = [];
let activeElectionId = null;
let refreshInterval = null;
const sectionStates = {
    '1st-year': true,
    '2nd-year': true,
    '3rd-year': true
};

async function initVoteRecordsPage() {
    try {
        const statusResponse = await fetch('/api/auth/status');
        const session = await statusResponse.json();

        if (!session.loggedIn || session.role !== 'ADMIN') {
            window.location.href = '/index.html';
            return;
        }

        await loadElectionsDropdown();

        document.getElementById('search-input').addEventListener('input', renderVoteRecords);
        document.getElementById('sort-select').addEventListener('change', renderVoteRecords);

        startAutoRefresh();
    } catch (err) {
        console.error("Error initializing vote records page:", err);
    }
}

async function loadElectionsDropdown() {
    try {
        const response = await fetch('/api/admin/elections');
        if (!response.ok) throw new Error("Failed to load elections.");

        const elections = await response.json();
        const dropdown = document.getElementById('election-select');
        dropdown.innerHTML = '';

        if (elections.length === 0) {
            dropdown.innerHTML = '<option value="">No elections available</option>';
            return;
        }

        elections.forEach(e => {
            const opt = document.createElement('option');
            opt.value = e.id;
            opt.innerText = e.title + (e.status === 'ENDED' ? ' (Ended)' : ' (Active)');
            dropdown.appendChild(opt);
        });

        activeElectionId = dropdown.value;
        dropdown.addEventListener('change', (e) => {
            activeElectionId = e.target.value;
            fetchVoteRecords();
        });

        await fetchVoteRecords();
    } catch (err) {
        showToast('Error loading elections: ' + err.message, 'error');
    }
}

async function fetchVoteRecords() {
    if (!activeElectionId) return;

    try {
        const response = await fetch(`/api/admin/votes/records?electionId=${activeElectionId}`);
        if (!response.ok) throw new Error("Failed to fetch vote records.");

        allRecords = await response.json();
        renderVoteRecords();
    } catch (err) {
        console.error("Error retrieving vote records:", err);
    }
}

function renderVoteRecords() {
    const searchVal = document.getElementById('search-input').value.trim().toLowerCase();
    const sortVal = document.getElementById('sort-select').value;

    let filtered = allRecords.filter(r => {
        return r.studentName.toLowerCase().includes(searchVal) ||
               r.registerNo.toLowerCase().includes(searchVal) ||
               r.department.toLowerCase().includes(searchVal) ||
               r.candidatesVoted.toLowerCase().includes(searchVal);
    });

    const [field, direction] = sortVal.split('-');
    filtered.sort((a, b) => {
        let valA = a[field];
        let valB = b[field];

        if (field === 'votedAt') {
            valA = new Date(valA);
            valB = new Date(valB);
        } else {
            valA = valA ? valA.toString().toLowerCase() : '';
            valB = valB ? valB.toString().toLowerCase() : '';
        }

        if (valA < valB) return direction === 'asc' ? -1 : 1;
        if (valA > valB) return direction === 'asc' ? 1 : -1;
        return 0;
    });

    const stats = {
        total: allRecords.length,
        '1st Year': 0,
        '2nd Year': 0,
        '3rd Year': 0
    };

    allRecords.forEach(r => {
        if (stats[r.year] !== undefined) {
            stats[r.year]++;
        }
    });

    document.getElementById('stat-total-voters').innerText = stats.total;
    document.getElementById('stat-1st-year-voters').innerText = stats['1st Year'];
    document.getElementById('stat-2nd-year-voters').innerText = stats['2nd Year'];
    document.getElementById('stat-3rd-year-voters').innerText = stats['3rd Year'];

    const filteredStats = {
        '1st-year': 0,
        '2nd-year': 0,
        '3rd-year': 0
    };

    filtered.forEach(r => {
        if (r.year === '1st Year') filteredStats['1st-year']++;
        else if (r.year === '2nd Year') filteredStats['2nd-year']++;
        else if (r.year === '3rd Year') filteredStats['3rd-year']++;
    });

    document.getElementById('count-1st-year').innerText = filteredStats['1st-year'];
    document.getElementById('count-2nd-year').innerText = filteredStats['2nd-year'];
    document.getElementById('count-3rd-year').innerText = filteredStats['3rd-year'];

    const groups = {
        '1st-year': [],
        '2nd-year': [],
        '3rd-year': []
    };

    filtered.forEach(r => {
        if (r.year === '1st Year') groups['1st-year'].push(r);
        else if (r.year === '2nd Year') groups['2nd-year'].push(r);
        else if (r.year === '3rd Year') groups['3rd-year'].push(r);
    });

    Object.keys(groups).forEach(key => {
        const tbody = document.getElementById(`table-body-${key}`);
        tbody.innerHTML = '';

        if (groups[key].length === 0) {
            const yearName = key === '1st-year' ? '1st Year' : (key === '2nd-year' ? '2nd Year' : '3rd Year');
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-muted" style="padding: 2.5rem 1rem;">
                        No votes have been cast by ${yearName} students yet.
                    </td>
                </tr>
            `;
            return;
        }

        groups[key].forEach((r, idx) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="font-weight:700;">${idx + 1}</td>
                <td>${r.studentName}</td>
                <td>${r.registerNo}</td>
                <td><span class="badge-dept">${r.department}</span></td>
                <td style="font-size:0.9rem; color:var(--text-color); max-width:350px;">${r.candidatesVoted || 'Abstained'}</td>
                <td style="font-size:0.9rem; color:var(--text-muted);">${formatDateTime(r.votedAt)}</td>
            `;
            tbody.appendChild(tr);
        });
    });
}

function toggleSection(year) {
    const content = document.getElementById(`content-${year}`);
    const arrow = document.getElementById(`arrow-${year}`);
    
    if (sectionStates[year]) {
        content.style.maxHeight = '0px';
        content.style.marginTop = '0px';
        arrow.style.transform = 'rotate(-90deg)';
        sectionStates[year] = false;
    } else {
        content.style.maxHeight = '2000px';
        content.style.marginTop = '1.5rem';
        arrow.style.transform = 'rotate(0deg)';
        sectionStates[year] = true;
    }
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleString();
}

function startAutoRefresh() {
    if (refreshInterval) clearInterval(refreshInterval);
    refreshInterval = setInterval(fetchVoteRecords, 5000);
}

async function exportVoteRecords() {
    if (!activeElectionId) return;

    showToast('Preparing vote audit records Excel sheet...', 'info');
    try {
        const response = await fetch(`/api/admin/votes/records/export/excel?electionId=${activeElectionId}`);
        if (!response.ok) throw new Error("Failed to generate report.");

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = `detailed_vote_records_election_${activeElectionId}.xlsx`;
        
        document.body.appendChild(a);
        a.click();
        
        window.URL.revokeObjectURL(url);
        a.remove();
        showToast('Detailed vote records downloaded successfully.', 'success');
    } catch (err) {
        showToast('Error exporting records: ' + err.message, 'error');
    }
}

window.addEventListener('beforeunload', () => {
    if (refreshInterval) clearInterval(refreshInterval);
});
