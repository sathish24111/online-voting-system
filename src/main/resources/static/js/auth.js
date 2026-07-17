// Authentication Controller JS

async function studentLogin(event) {
    event.preventDefault();
    const registerNo = document.getElementById('registerNo').value.trim();
    const password = document.getElementById('password').value;

    if (!registerNo || !password) {
        showToast('Please fill in all fields.', 'error');
        return;
    }

    showLoading(true);
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ username: registerNo, password: password })
        });

        const data = await response.json();
        
        if (response.ok) {
            showToast(data.message || 'Login successful. OTP sent.', 'success');
            // Redirect to OTP verification page
            setTimeout(() => {
                window.location.href = '/pages/otp-verification.html';
            }, 1000);
        } else {
            showToast(data.message || 'Invalid register number or password.', 'error');
        }
    } catch (e) {
        showToast('Login failed. Server unreachable.', 'error');
    } finally {
        showLoading(false);
    }
}

async function adminLogin(event) {
    event.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username || !password) {
        showToast('Please fill in all fields.', 'error');
        return;
    }

    showLoading(true);
    try {
        const response = await fetch('/api/auth/admin-login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ username: username, password: password })
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Welcome, Administrator!', 'success');
            setTimeout(() => {
                window.location.href = '/pages/admin-dashboard.html';
            }, 1000);
        } else {
            showToast(data.message || 'Invalid credentials.', 'error');
        }
    } catch (e) {
        showToast('Login failed. Connection lost.', 'error');
    } finally {
        showLoading(false);
    }
}

async function verifyOtp(event) {
    event.preventDefault();
    const code = document.getElementById('otpCode').value.trim();

    if (code.length !== 6 || isNaN(code)) {
        showToast('Please enter a valid 6-digit numeric OTP.', 'error');
        return;
    }

    const form = event.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    showLoading(true);
    try {
        const response = await fetch('/api/auth/otp/verify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ otp: code })
        });

        const data = await response.json();

        if (response.ok) {
            showToast('OTP Verified! Redirecting...', 'success');
            setTimeout(() => {
                window.location.href = '/pages/student-dashboard.html';
            }, 1000);
        } else {
            showToast(data.error || 'OTP verification failed.', 'error');
        }
    } catch (e) {
        showToast('Verification failed. Server unreachable.', 'error');
    } finally {
        showLoading(false);
        if (submitBtn) submitBtn.disabled = false;
    }
}

let resendTimer = null;
function startOtpTimer(seconds = 60) {
    const resendBtn = document.getElementById('resend-btn');
    if (!resendBtn) return;
    
    resendBtn.disabled = true;
    let remaining = seconds;
    
    resendBtn.innerHTML = `Resend in ${remaining}s`;

    if (resendTimer) clearInterval(resendTimer);

    resendTimer = setInterval(() => {
        remaining--;
        resendBtn.innerHTML = `Resend in ${remaining}s`;
        if (remaining <= 0) {
            clearInterval(resendTimer);
            resendBtn.disabled = false;
            resendBtn.innerHTML = 'Resend OTP';
        }
    }, 1000);
}

async function resendOtp() {
    showLoading(true);
    try {
        const response = await fetch('/api/auth/otp/resend', {
            method: 'POST',
            headers: {
                'X-XSRF-TOKEN': getCsrfToken()
            }
        });

        const data = await response.json();

        if (response.ok) {
            showToast('A new OTP has been dispatched to your email.', 'success');
            startOtpTimer(60);
        } else {
            showToast(data.message || 'Resend request failed.', 'error');
        }
    } catch (e) {
        showToast('Failed to resend OTP. Server unreachable.', 'error');
    } finally {
        showLoading(false);
    }
}

// Redirects based on user login state
async function requireSession(role = 'STUDENT') {
    try {
        const response = await fetch('/api/auth/status');
        const data = await response.json();

        if (!data.loggedIn) {
            window.location.href = role === 'ADMIN' ? '/pages/admin-login.html' : '/pages/student-login.html';
            return null;
        }

        if (data.role !== role) {
            window.location.href = data.role === 'ADMIN' ? '/pages/admin-dashboard.html' : '/pages/student-dashboard.html';
            return null;
        }

        if (role === 'STUDENT' && !data.otpVerified) {
            window.location.href = '/pages/otp-verification.html';
            return null;
        }

        return data;
    } catch (e) {
        showToast('Connection lost. Please refresh.', 'error');
        return null;
    }
}
