// Admin Dashboard Controller

let activeElectionIdForAdmin = 1; // Default
let currentStudentPage = 0;
let currentStudentSearch = '';

async function loadAdminDashboard() {
    const session = await requireSession('ADMIN');
    if (!session) return;

    // Load active election metrics
    await fetchAdminMetrics();
    await fetchRecentAudits();
}

async function fetchAdminMetrics() {
    try {
        const response = await fetch('/api/admin/elections');
        const elections = await response.json();
        
        const grid = document.getElementById('admin-elections-list');
        if (!grid) return;

        if (elections.length === 0) {
            grid.innerHTML = '<div class="glass-card text-center"><p>No elections found. Create one in Election Settings.</p></div>';
            return;
        }

        // Set metrics based on the first election
        const active = elections.find(e => e.status === 'RUNNING') || elections[0];
        activeElectionIdForAdmin = active.id;

        const res = await fetch(`/api/results/${active.id}`);
        if (res.ok) {
            const data = await res.json();
            document.getElementById('stat-total-students').innerText = data.totalStudents;
            document.getElementById('stat-votes-cast').innerText = data.votesCast;
            
            const remaining = data.totalStudents - data.votesCast;
            document.getElementById('stat-votes-pending').innerText = remaining >= 0 ? remaining : 0;
            
            document.getElementById('stat-election-status').innerText = active.status;
        }

    } catch (e) {
        showToast('Error loading dashboard metrics.', 'error');
    }
}

async function fetchRecentAudits() {
    const auditContainer = document.getElementById('recent-activity-container');
    if (!auditContainer) return;

    try {
        const response = await fetch('/api/admin/audit-logs?page=0&size=5');
        const data = await response.json();
        
        auditContainer.innerHTML = '';
        if (data.content.length === 0) {
            auditContainer.innerHTML = '<li>No recent activity logged.</li>';
            return;
        }

        data.content.forEach(log => {
            const li = document.createElement('li');
            li.style.display = 'flex';
            li.style.justifyContent = 'space-between';
            li.style.padding = '0.75rem 0';
            li.style.borderBottom = '1px solid var(--border-color)';
            
            const time = new Date(log.time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            
            li.innerHTML = `
                <div style="text-align: left;">
                    <strong>${log.action}</strong>
                    <p style="font-size:0.8rem; margin:0;">User: ${log.user} | IP: ${log.ipAddress}</p>
                </div>
                <span style="font-size:0.8rem; color:var(--text-muted);">${time}</span>
            `;
            auditContainer.appendChild(li);
        });
    } catch (e) {
        // Silent error
    }
}

// Student Management Functions
async function loadStudentsList() {
    const session = await requireSession('ADMIN');
    if (!session) return;

    showLoading(true);
    try {
        const response = await fetch(`/api/admin/students?search=${currentStudentSearch}&page=${currentStudentPage}&size=10&sortBy=id&direction=desc`);
        const data = await response.json();
        
        const tbody = document.getElementById('students-table-body');
        if (!tbody) return;

        tbody.innerHTML = '';
        if (data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center">No students found.</td></tr>';
            return;
        }

        data.content.forEach(s => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${s.registerNo}</td>
                <td>${s.name}</td>
                <td>${s.department}</td>
                <td>${s.year}</td>
                <td>${s.email}</td>
                <td>
                    <span class="btn ${s.status === 'ENABLED' ? 'btn-success' : 'btn-secondary'}" style="padding:0.25rem 0.5rem; font-size:0.75rem; cursor:pointer;" onclick="toggleStudentStatus(${s.id})">
                        ${s.status}
                    </span>
                </td>
                <td>
                    <div style="display:flex; gap:0.5rem;">
                        <button class="btn btn-secondary" style="padding:0.25rem 0.5rem;" title="Edit Student" onclick="openEditStudentModal(${s.id}, '${s.name}', '${s.registerNo}', '${s.department}', '${s.year}', '${s.email}')"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-secondary" style="padding:0.25rem 0.5rem;" title="Reset Password" onclick="openResetPasswordModal(${s.id}, '${s.name}')"><i class="fas fa-key"></i></button>
                        <button class="btn btn-secondary" style="padding:0.25rem 0.5rem; background-color: #d97706; color: white;" title="Reset OTP Limits" onclick="resetStudentOtpLimit(${s.id}, '${s.name}')"><i class="fas fa-redo-alt"></i></button>
                        <button class="btn btn-danger" style="padding:0.25rem 0.5rem;" title="Delete Student" onclick="deleteStudent(${s.id})"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });

        // Set Pagination Text
        document.getElementById('page-info').innerText = `Page ${data.number + 1} of ${data.totalPages}`;
        
        const prevBtn = document.getElementById('prev-page-btn');
        const nextBtn = document.getElementById('next-page-btn');
        if (prevBtn) prevBtn.disabled = data.first;
        if (nextBtn) nextBtn.disabled = data.last;

    } catch (e) {
        showToast('Error retrieving student directory.', 'error');
    } finally {
        showLoading(false);
    }
}

function searchStudents() {
    currentStudentSearch = document.getElementById('student-search-input').value.trim();
    currentStudentPage = 0;
    loadStudentsList();
}

function changeStudentPage(direction) {
    currentStudentPage += direction;
    loadStudentsList();
}

async function saveStudent(event) {
    event.preventDefault();
    const id = document.getElementById('student-id').value;
    const name = document.getElementById('student-name-input').value.trim();
    const registerNo = document.getElementById('student-reg-input').value.trim();
    const department = document.getElementById('student-dept-input').value.trim();
    const year = document.getElementById('student-year-input').value.trim();
    const email = document.getElementById('student-email-input').value.trim();
    const password = document.getElementById('student-pass-input').value;

    const payload = { name, registerNo, department, year, email };
    if (!id) {
        payload.password = password;
    }

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/admin/students/${id}` : '/api/admin/students';

    showLoading(true);
    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (response.ok) {
            showToast(`Student ${id ? 'updated' : 'added'} successfully.`, 'success');
            closeStudentModal();
            loadStudentsList();
        } else {
            showToast(data.error || 'Failed to save student record.', 'error');
        }
    } catch (e) {
        showToast('Error saving student record.', 'error');
    } finally {
        showLoading(false);
    }
}

async function toggleStudentStatus(id) {
    showLoading(true);
    try {
        const response = await fetch(`/api/admin/students/${id}/toggle`, {
            method: 'POST',
            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
        });
        if (response.ok) {
            showToast('Student status updated.', 'success');
            loadStudentsList();
        }
    } catch (e) {
        // Silent error
    } finally {
        showLoading(false);
    }
}

async function deleteStudent(id) {
    if (!confirm('Are you sure you want to delete this student permanently?')) return;
    showLoading(true);
    try {
        const response = await fetch(`/api/admin/students/${id}`, {
            method: 'DELETE',
            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
        });
        if (response.ok) {
            showToast('Student deleted.', 'success');
            loadStudentsList();
        }
    } catch (e) {
        // Silent error
    } finally {
        showLoading(false);
    }
}

async function performStudentPasswordReset(event) {
    event.preventDefault();
    const id = document.getElementById('reset-pass-student-id').value;
    const password = document.getElementById('reset-pass-input').value;

    showLoading(true);
    try {
        const response = await fetch(`/api/admin/students/${id}/reset-password`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ password: password })
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Password reset successfully.', 'success');
            closeResetPasswordModal();
        } else {
            showToast(data.error || 'Failed to reset password.', 'error');
        }
    } catch (e) {
        showToast('Error resetting password.', 'error');
    } finally {
        showLoading(false);
    }
}

async function handleStudentImport(event) {
    event.preventDefault();
    const fileInput = document.getElementById('import-file');
    if (fileInput.files.length === 0) {
        showToast('Please select an Excel or CSV file first.', 'warning');
        return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    showLoading(true);
    try {
        const response = await fetch('/api/admin/students/import', {
            method: 'POST',
            headers: { 'X-XSRF-TOKEN': getCsrfToken() },
            body: formData
        });

        const data = await response.json();

        if (response.ok) {
            // Render Import Summary Modal details
            document.getElementById('summary-total').innerText = data.totalRows;
            document.getElementById('summary-success').innerText = data.successCount;
            document.getElementById('summary-duplicates').innerText = data.duplicateCount;
            document.getElementById('summary-errors').innerText = data.errorCount;

            const logsList = document.getElementById('summary-logs');
            logsList.innerHTML = '';
            
            if (data.logs.length === 0) {
                logsList.innerHTML = '<li>All rows imported successfully.</li>';
            } else {
                data.logs.forEach(log => {
                    const li = document.createElement('li');
                    li.innerText = log;
                    logsList.appendChild(li);
                });
            }

            closeImportModal();
            document.getElementById('import-summary-modal').style.display = 'flex';
            loadStudentsList();
        } else {
            showToast(data.error || 'Error occurred during import file parsing.', 'error');
        }
    } catch (e) {
        showToast('Import request failed. Server error.', 'error');
    } finally {
        showLoading(false);
    }
}

// Candidate Management Functions
async function loadCandidatesAdmin() {
    const session = await requireSession('ADMIN');
    if (!session) return;

    // Load active elections dropdown first
    await loadElectionsDropdownForCandidates();
}

async function loadElectionsDropdownForCandidates() {
    try {
        const response = await fetch('/api/admin/elections');
        const elections = await response.json();
        
        const dropdown = document.getElementById('candidate-election-select');
        if (!dropdown) return;

        dropdown.innerHTML = '';
        if (elections.length === 0) {
            dropdown.innerHTML = '<option value="">No elections available</option>';
            return;
        }

        elections.forEach(e => {
            const opt = document.createElement('option');
            opt.value = e.id;
            opt.innerText = e.title;
            dropdown.appendChild(opt);
        });

        dropdown.addEventListener('change', () => fetchCandidatesAdmin(dropdown.value));
        fetchCandidatesAdmin(dropdown.value);

    } catch (e) {
        showToast('Error loading elections select.', 'error');
    }
}

async function fetchCandidatesAdmin(electionId) {
    if (!electionId) return;
    
    showLoading(true);
    try {
        const response = await fetch(`/api/admin/candidates?electionId=${electionId}`);
        const list = await response.json();
        
        const tbody = document.getElementById('candidates-table-body');
        if (!tbody) return;

        tbody.innerHTML = '';
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center">No candidates registered for this election.</td></tr>';
            return;
        }

        list.forEach(c => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <img src="${c.photo || '/images/default-candidate.png'}" style="width:40px; height:40px; object-fit:cover; border-radius:50%; border:2px solid var(--primary-color);">
                </td>
                <td>${c.name}</td>
                <td>${c.registerNo}</td>
                <td>${c.department}</td>
                <td>${c.year}</td>
                <td>${c.position}</td>
                <td>
                    <div style="display:flex; gap:0.5rem;">
                        <button class="btn btn-secondary" style="padding:0.25rem 0.5rem;" onclick="openEditCandidateModal(${c.id}, '${c.name}', '${c.registerNo}', '${c.department}', '${c.year}', '${c.position}', '${c.manifesto.replace(/'/g, "\\'")}')"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger" style="padding:0.25rem 0.5rem;" onclick="deleteCandidate(${c.id})"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showToast('Error retrieving candidates list.', 'error');
    } finally {
        showLoading(false);
    }
}

async function saveCandidate(event) {
    event.preventDefault();
    const id = document.getElementById('candidate-id').value;
    const electionId = document.getElementById('candidate-election-select').value;
    const name = document.getElementById('candidate-name-input').value.trim();
    const registerNo = document.getElementById('candidate-reg-input').value.trim();
    const department = document.getElementById('candidate-dept-input').value.trim();
    const year = document.getElementById('candidate-year-input').value.trim();
    const position = document.getElementById('candidate-pos-input').value.trim();
    const manifesto = document.getElementById('candidate-manifesto-input').value.trim();
    const photo = document.getElementById('candidate-photo-input').files[0];

    const formData = new FormData();
    formData.append('electionId', electionId);
    formData.append('name', name);
    formData.append('registerNo', registerNo);
    formData.append('department', department);
    formData.append('year', year);
    formData.append('position', position);
    formData.append('manifesto', manifesto);
    
    if (photo) {
        formData.append('photo', photo);
    }

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/admin/candidates/${id}` : '/api/admin/candidates';

    showLoading(true);
    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'X-XSRF-TOKEN': getCsrfToken() },
            body: formData
        });

        const data = await response.json();

        if (response.ok) {
            showToast(`Candidate ${id ? 'updated' : 'added'} successfully.`, 'success');
            closeCandidateModal();
            fetchCandidatesAdmin(electionId);
        } else {
            showToast(data.error || 'Failed to save candidate profile.', 'error');
        }
    } catch (e) {
        showToast('Error saving candidate profile.', 'error');
    } finally {
        showLoading(false);
    }
}

async function deleteCandidate(id) {
    if (!confirm('Are you sure you want to remove this candidate?')) return;
    const electionId = document.getElementById('candidate-election-select').value;

    showLoading(true);
    try {
        const response = await fetch(`/api/admin/candidates/${id}`, {
            method: 'DELETE',
            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
        });
        if (response.ok) {
            showToast('Candidate removed.', 'success');
            fetchCandidatesAdmin(electionId);
        }
    } catch (e) {
        // Silent error
    } finally {
        showLoading(false);
    }
}

// Election Settings Functions
async function loadElectionSettings() {
    const session = await requireSession('ADMIN');
    if (!session) return;

    showLoading(true);
    try {
        const response = await fetch('/api/admin/elections');
        const list = await response.json();
        
        const tbody = document.getElementById('elections-table-body');
        if (!tbody) return;

        tbody.innerHTML = '';
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No elections found. Create one.</td></tr>';
            return;
        }

        list.forEach(e => {
            const tr = document.createElement('tr');
            
            const start = new Date(e.startTime).toLocaleString();
            const end = new Date(e.endTime).toLocaleString();

            let actionButtons = '';
            if (e.status === 'NOT_STARTED') {
                actionButtons = `<button class="btn btn-success" style="padding:0.25rem 0.5rem; font-size:0.75rem;" onclick="changeElectionStatus(${e.id}, 'RUNNING')"><i class="fas fa-play"></i> Start</button>`;
            } else if (e.status === 'RUNNING') {
                actionButtons = `
                    <button class="btn btn-secondary" style="padding:0.25rem 0.5rem; font-size:0.75rem;" onclick="changeElectionStatus(${e.id}, 'PAUSED')"><i class="fas fa-pause"></i> Pause</button>
                    <button class="btn btn-danger" style="padding:0.25rem 0.5rem; font-size:0.75rem;" onclick="changeElectionStatus(${e.id}, 'ENDED')"><i class="fas fa-stop"></i> End</button>
                `;
            } else if (e.status === 'PAUSED') {
                actionButtons = `
                    <button class="btn btn-success" style="padding:0.25rem 0.5rem; font-size:0.75rem;" onclick="changeElectionStatus(${e.id}, 'RUNNING')"><i class="fas fa-play"></i> Resume</button>
                    <button class="btn btn-danger" style="padding:0.25rem 0.5rem; font-size:0.75rem;" onclick="changeElectionStatus(${e.id}, 'ENDED')"><i class="fas fa-stop"></i> End</button>
                `;
            } else {
                actionButtons = `<span style="font-weight:600; color:var(--text-muted);">Published</span>`;
            }

            tr.innerHTML = `
                <td>${e.title}</td>
                <td>
                    <span style="font-weight:700; color:${e.status==='RUNNING'?'var(--success-color)':(e.status==='PAUSED'?'var(--warning-color)':'var(--text-muted)')}">${e.status}</span>
                </td>
                <td>${start}</td>
                <td>${end}</td>
                <td>
                    <div style="display:flex; gap:0.25rem;">
                        ${actionButtons}
                    </div>
                </td>
                <td>
                    <div style="display:flex; gap:0.5rem;">
                        <button class="btn btn-secondary" style="padding:0.25rem 0.5rem;" onclick="openEditElectionModal(${e.id}, '${e.title}', '${e.startTime}', '${e.endTime}')"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger" style="padding:0.25rem 0.5rem;" onclick="deleteElection(${e.id})"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });

    } catch (e) {
        showToast('Error loading elections list.', 'error');
    } finally {
        showLoading(false);
    }
}

async function saveElection(event) {
    event.preventDefault();
    const id = document.getElementById('election-id').value;
    const title = document.getElementById('election-title-input').value.trim();
    const startTime = document.getElementById('election-start-input').value;
    const endTime = document.getElementById('election-end-input').value;

    const payload = { title, startTime, endTime };
    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/admin/elections/${id}` : '/api/admin/elections';

    showLoading(true);
    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (response.ok) {
            showToast(`Election ${id ? 'updated' : 'created'} successfully.`, 'success');
            closeElectionModal();
            loadElectionSettings();
        } else {
            showToast(data.error || 'Failed to save election.', 'error');
        }
    } catch (e) {
        showToast('Error saving election.', 'error');
    } finally {
        showLoading(false);
    }
}

async function changeElectionStatus(id, targetStatus) {
    showLoading(true);
    try {
        const response = await fetch(`/api/admin/elections/${id}/status`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ status: targetStatus })
        });
        if (response.ok) {
            showToast(`Election status updated to ${targetStatus}.`, 'success');
            loadElectionSettings();
        } else {
            const data = await response.json();
            showToast(data.error || 'Status change failed.', 'error');
        }
    } catch (e) {
        showToast('Request failed. Connection lost.', 'error');
    } finally {
        showLoading(false);
    }
}

async function deleteElection(id) {
    if (!confirm('Are you sure you want to delete this election? All candidate and vote records linked to it will be lost.')) return;
    showLoading(true);
    try {
        const response = await fetch(`/api/admin/elections/${id}`, {
            method: 'DELETE',
            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
        });
        if (response.ok) {
            showToast('Election deleted.', 'success');
            loadElectionSettings();
        }
    } catch (e) {
        // Silent error
    } finally {
        showLoading(false);
    }
}

// Modal open/closes
function openAddStudentModal() {
    document.getElementById('student-modal-title').innerText = "Add New Student";
    document.getElementById('student-id').value = "";
    document.getElementById('student-form').reset();
    document.getElementById('student-pass-group').style.display = 'block';
    document.getElementById('student-pass-input').required = true;
    document.getElementById('student-modal').style.display = 'flex';
}

function openEditStudentModal(id, name, regNo, dept, year, email) {
    document.getElementById('student-modal-title').innerText = "Edit Student Profile";
    document.getElementById('student-id').value = id;
    document.getElementById('student-name-input').value = name;
    document.getElementById('student-reg-input').value = regNo;
    document.getElementById('student-dept-input').value = dept;
    document.getElementById('student-year-input').value = year;
    document.getElementById('student-email-input').value = email;
    document.getElementById('student-pass-group').style.display = 'none';
    document.getElementById('student-pass-input').required = false;
    document.getElementById('student-modal').style.display = 'flex';
}

function closeStudentModal() {
    document.getElementById('student-modal').style.display = 'none';
}

function openResetPasswordModal(id, name) {
    document.getElementById('reset-pass-student-id').value = id;
    document.getElementById('reset-pass-student-name').innerText = name;
    document.getElementById('reset-pass-form').reset();
    document.getElementById('reset-password-modal').style.display = 'flex';
}

function closeResetPasswordModal() {
    document.getElementById('reset-password-modal').style.display = 'none';
}

function openImportModal() {
    document.getElementById('import-form').reset();
    document.getElementById('import-modal').style.display = 'flex';
}

function closeImportModal() {
    document.getElementById('import-modal').style.display = 'none';
}

function closeImportSummaryModal() {
    document.getElementById('import-summary-modal').style.display = 'none';
}

function openAddCandidateModal() {
    document.getElementById('candidate-modal-title').innerText = "Register Candidate";
    document.getElementById('candidate-id').value = "";
    document.getElementById('candidate-form').reset();
    document.getElementById('candidate-modal').style.display = 'flex';
}

function openEditCandidateModal(id, name, regNo, dept, year, pos, manifesto) {
    document.getElementById('candidate-modal-title').innerText = "Edit Candidate Profile";
    document.getElementById('candidate-id').value = id;
    document.getElementById('candidate-name-input').value = name;
    document.getElementById('candidate-reg-input').value = regNo;
    document.getElementById('candidate-dept-input').value = dept;
    document.getElementById('candidate-year-input').value = year;
    document.getElementById('candidate-pos-input').value = pos;
    document.getElementById('candidate-manifesto-input').value = manifesto;
    document.getElementById('candidate-modal').style.display = 'flex';
}

function closeCandidateModal() {
    document.getElementById('candidate-modal').style.display = 'none';
}

function openAddElectionModal() {
    document.getElementById('election-modal-title').innerText = "Create New Election";
    document.getElementById('election-id').value = "";
    document.getElementById('election-form').reset();
    document.getElementById('election-modal').style.display = 'flex';
}

function openEditElectionModal(id, title, startTime, endTime) {
    document.getElementById('election-modal-title').innerText = "Edit Election Times";
    document.getElementById('election-id').value = id;
    document.getElementById('election-title-input').value = title;
    
    // Format LocalDateTime string (YYYY-MM-DDTHH:MM) for HTML datetime-local input
    document.getElementById('election-start-input').value = startTime.substring(0, 16);
    document.getElementById('election-end-input').value = endTime.substring(0, 16);
    document.getElementById('election-modal').style.display = 'flex';
}

function closeElectionModal() {
    document.getElementById('election-modal').style.display = 'none';
}

async function clearAllStudents() {
    if (!confirm("⚠️ WARNING: This will permanently delete ALL students, all OTP verification records, and ALL votes cast in all elections! This action cannot be undone.\n\nAre you sure you want to proceed?")) {
        return;
    }
    showLoading(true);
    try {
        const response = await fetch('/api/admin/students/all', {
            method: 'DELETE',
            headers: { 'X-XSRF-TOKEN': getCsrfToken() }
        });
        if (response.ok) {
            showToast('All student and voting records cleared successfully.', 'success');
            loadStudentsList();
        } else {
            const data = await response.json();
            showToast(data.error || 'Failed to clear student directory.', 'error');
        }
    } catch (e) {
        showToast('Error clearing student directory.', 'error');
    } finally {
        showLoading(false);
    }
}

async function resetStudentOtpLimit(id, name) {
    if (!confirm(`Are you sure you want to reset OTP resend limit for student ${name}?`)) {
        return;
    }
    showLoading(true);
    try {
        const response = await fetch(`/api/admin/students/${id}/reset-otp-limit`, {
            method: 'POST',
            headers: {
                'X-XSRF-TOKEN': getCsrfToken()
            }
        });
        const data = await response.json();
        if (response.ok) {
            showToast(`OTP limits reset successfully for ${name}.`, 'success');
        } else {
            showToast(data.error || 'Failed to reset OTP limits.', 'error');
        }
    } catch (e) {
        showToast('Error resetting OTP limits.', 'error');
    } finally {
        showLoading(false);
    }
}
