// Student Dashboard Controller

let currentStudent = null;

async function loadStudentDashboard() {
    const session = await requireSession('STUDENT');
    if (!session) return;

    currentStudent = session.student;
    
    // Fill Profile Info
    document.getElementById('student-name').innerText = currentStudent.name;
    document.getElementById('student-reg').innerText = currentStudent.registerNo;
    document.getElementById('student-dept').innerText = currentStudent.department;
    document.getElementById('student-year').innerText = currentStudent.year;
    document.getElementById('student-email').innerText = currentStudent.email;

    const photoEl = document.getElementById('student-photo');
    if (photoEl && currentStudent.photo) {
        photoEl.src = currentStudent.photo;
    }

    // Load active election to check turnout status and countdowns
    await fetchActiveElectionInfo();
}

async function fetchActiveElectionInfo() {
    try {
        const response = await fetch('/api/voting/active-election');
        
        // Handle when there is no active election
        if (response.status === 404) {
            document.getElementById('election-title').innerText = "No active election currently running.";
            document.getElementById('election-status').innerText = "Inactive";
            document.getElementById('voting-status').innerText = "N/A";
            document.getElementById('countdown-container').style.display = 'none';
            
            const voteBtn = document.getElementById('go-vote-btn');
            if (voteBtn) voteBtn.style.display = 'none';
            return;
        }

        const data = await response.json();
        const election = data.election;
        const hasVoted = data.hasVoted;

        document.getElementById('election-title').innerText = election.title;
        document.getElementById('election-status').innerText = election.status;
        
        const statusBadge = document.getElementById('voting-status');
        if (hasVoted) {
            statusBadge.innerText = "Already Voted";
            statusBadge.className = "btn btn-secondary";
            statusBadge.style.cursor = "default";
            
            const voteBtn = document.getElementById('go-vote-btn');
            if (voteBtn) {
                voteBtn.innerText = "Already Voted";
                voteBtn.className = "btn btn-secondary";
                voteBtn.href = "#";
                voteBtn.onclick = (e) => {
                    e.preventDefault();
                    showToast("You have already recorded your vote for this election.", "info");
                };
            }
        } else {
            statusBadge.innerText = "Pending Vote";
            statusBadge.className = "btn btn-danger";
        }

        // Initialize Countdown Timer
        initElectionTimer(election.endTime);

    } catch (e) {
        showToast('Failed to load active election info.', 'error');
    }
}

let timerInterval = null;
function initElectionTimer(endTimeString) {
    const timerDays = document.getElementById('timer-days');
    const timerHours = document.getElementById('timer-hours');
    const timerMinutes = document.getElementById('timer-minutes');
    const timerSeconds = document.getElementById('timer-seconds');
    const timerContainer = document.getElementById('countdown-container');

    if (!timerDays) return;

    const endTime = new Date(endTimeString).getTime();

    if (timerInterval) clearInterval(timerInterval);

    timerInterval = setInterval(() => {
        const now = new Date().getTime();
        const distance = endTime - now;

        if (distance < 0) {
            clearInterval(timerInterval);
            timerContainer.innerHTML = "<h4>Election Ended</h4>";
            // Refresh dashboard
            setTimeout(() => { window.location.reload(); }, 2000);
            return;
        }

        const days = Math.floor(distance / (1000 * 60 * 60 * 24));
        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((distance % (1000 * 60)) / 1000);

        timerDays.innerText = String(days).padStart(2, '0');
        timerHours.innerText = String(hours).padStart(2, '0');
        timerMinutes.innerText = String(minutes).padStart(2, '0');
        timerSeconds.innerText = String(seconds).padStart(2, '0');
    }, 1000);
}

// Student Self Password Reset
async function changeStudentPassword(event) {
    event.preventDefault();
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!newPassword || !confirmPassword) {
        showToast('Please fill in both password fields.', 'error');
        return;
    }

    if (newPassword !== confirmPassword) {
        showToast('Passwords do not match.', 'error');
        return;
    }

    showLoading(true);
    try {
        const response = await fetch('/api/student/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ password: newPassword })
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Password updated successfully.', 'success');
            document.getElementById('changePasswordForm').reset();
        } else {
            showToast(data.error || 'Password update failed.', 'error');
        }
    } catch (e) {
        showToast('Request failed. Connection error.', 'error');
    } finally {
        showLoading(false);
    }
}
