USE voting_db;

-- 1. Clear all operational data (order matters to satisfy foreign key constraints)
DELETE FROM otp_verification;
DELETE FROM election_participation;
DELETE FROM votes;
DELETE FROM candidates;
DELETE FROM students;
DELETE FROM election;
DELETE FROM audit_logs;

-- 2. Clear administrators and re-insert the default admin account (admin / admin123)
DELETE FROM admins;
INSERT INTO admins (id, username, password, role) 
VALUES (1, 'admin', '$2a$10$s9pam.map8BLV3H0troks.gzbqDpvqgGzRCFppFFGopNBIBmH4Z2.', 'ROLE_ADMIN');
