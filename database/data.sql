USE voting_db;

-- Clear tables (avoid constraint errors by doing it in correct order)
DELETE FROM otp_verification;
DELETE FROM election_participation;
DELETE FROM votes;
DELETE FROM candidates;
DELETE FROM students;
DELETE FROM admins;
DELETE FROM election;
DELETE FROM audit_logs;

-- 1. Insert Admin (password: admin123)
-- BCrypt hash for 'admin123': $2a$10$s9pam.map8BLV3H0troks.gzbqDpvqgGzRCFppFFGopNBIBmH4Z2.
INSERT INTO admins (id, username, password, role) 
VALUES (1, 'admin', '$2a$10$s9pam.map8BLV3H0troks.gzbqDpvqgGzRCFppFFGopNBIBmH4Z2.', 'ROLE_ADMIN');

-- 2. Insert Sample Students (password: Password123!)
-- BCrypt hash for 'Password123!': $2a$10$RxBOfZlFzfxdsOyIYsI7g.WE12rmcP1kwNwmFhGaC40J2aWlZz1G.
INSERT INTO students (id, register_no, name, department, year, email, password, photo, status, dob)
VALUES 
(1, '2026CSE001', 'Alice Vance', 'Computer Science', 'III Year', 'alice@gmail.com', '$2a$10$RxBOfZlFzfxdsOyIYsI7g.WE12rmcP1kwNwmFhGaC40J2aWlZz1G.', 'default-student.png', 'ENABLED', '15-08-2005'),
(2, '2026CSE002', 'Bob Smith', 'Computer Science', 'III Year', 'bob@gmail.com', '$2a$10$RxBOfZlFzfxdsOyIYsI7g.WE12rmcP1kwNwmFhGaC40J2aWlZz1G.', 'default-student.png', 'ENABLED', '10-09-2005'),
(3, '2026CSE003', 'Charlie Brown', 'Computer Science', 'II Year', 'charlie@gmail.com', '$2a$10$RxBOfZlFzfxdsOyIYsI7g.WE12rmcP1kwNwmFhGaC40J2aWlZz1G.', 'default-student.png', 'ENABLED', '05-12-2006'),
(4, '2026ECE001', 'Diana Prince', 'Electronics & Comm', 'IV Year', 'diana@gmail.com', '$2a$10$RxBOfZlFzfxdsOyIYsI7g.WE12rmcP1kwNwmFhGaC40J2aWlZz1G.', 'default-student.png', 'ENABLED', '22-04-2004'),
(5, '2026MECH01', 'Evan Wright', 'Mechanical Eng', 'I Year', 'evan@gmail.com', '$2a$10$RxBOfZlFzfxdsOyIYsI7g.WE12rmcP1kwNwmFhGaC40J2aWlZz1G.', 'default-student.png', 'DISABLED', '30-11-2007');

-- 3. Insert Sample Election (Status: RUNNING, active from now onwards)
-- Dates are set around current mock date
INSERT INTO election (id, title, status, start_time, end_time)
VALUES (1, 'College Union Election 2026', 'RUNNING', '2026-07-10 09:00:00', '2026-07-11 18:00:00');

-- 4. Insert Sample Candidates for Election 1
INSERT INTO candidates (id, election_id, name, register_no, department, year, position, manifesto, photo)
VALUES
-- Position: Secretary
(1, 1, 'John Doe', '2026CSE100', 'Computer Science', 'IV Year', 'Secretary', 'Empowering student voices and organizing tech workshops.', 'candidate-john.png'),
(2, 1, 'Jane Miller', '2026ECE102', 'Electronics & Comm', 'IV Year', 'Secretary', 'Bridging the gap between students and administration.', 'candidate-jane.png'),

-- Position: Joint Secretary
(3, 1, 'Sam Wilson', '2026MECH201', 'Mechanical Eng', 'III Year', 'Joint Secretary', 'Enhancing co-curricular activities and campus facilities.', 'candidate-sam.png'),
(4, 1, 'Lucy Heart', '2026CSE203', 'Computer Science', 'III Year', 'Joint Secretary', 'Promoting cultural exchange programs and events.', 'candidate-lucy.png'),

-- Position: Treasurer
(5, 1, 'David Miller', '2026CSE305', 'Computer Science', 'III Year', 'Treasurer', 'Ensuring transparency in student body funds and budgets.', 'candidate-david.png'),
(6, 1, 'Mary Watson', '2026ECE306', 'Electronics & Comm', 'III Year', 'Treasurer', 'Funding creative club initiatives and sports programs.', 'candidate-mary.png'),

-- Position: Sports Secretary
(7, 1, 'Vikram Singh', '2026MECH401', 'Mechanical Eng', 'IV Year', 'Sports Secretary', 'Upgrading sports equipment and hosting inter-department tournament.', 'candidate-vikram.png'),
(8, 1, 'Emma Stone', '2026CSE405', 'Computer Science', 'IV Year', 'Sports Secretary', 'Incentivizing athletic accomplishments and fitness programs.', 'candidate-emma.png');
