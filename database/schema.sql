CREATE DATABASE IF NOT EXISTS voting_db;
USE voting_db;

-- 1. Admins Table
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_ADMIN'
);

-- 2. Students Table
CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    register_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    year VARCHAR(10) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    photo VARCHAR(255) DEFAULT 'default-student.png',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    dob VARCHAR(50) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Election Table
CREATE TABLE IF NOT EXISTS election (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL
);

-- 4. Candidates Table
CREATE TABLE IF NOT EXISTS candidates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    election_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    register_no VARCHAR(50) NOT NULL,
    department VARCHAR(100) NOT NULL,
    year VARCHAR(10) NOT NULL,
    position VARCHAR(100) NOT NULL,
    manifesto TEXT,
    photo VARCHAR(255) DEFAULT 'default-candidate.png',
    FOREIGN KEY (election_id) REFERENCES election(id) ON DELETE CASCADE
);

-- 5. Votes Table (Fully Anonymous)
CREATE TABLE IF NOT EXISTS votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    election_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    position VARCHAR(100) NOT NULL,
    vote_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (election_id) REFERENCES election(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

-- 6. Election Participation
CREATE TABLE IF NOT EXISTS election_participation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_student_election (student_id, election_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (election_id) REFERENCES election(id) ON DELETE CASCADE
);

-- 7. OTP Verification Table
CREATE TABLE IF NOT EXISTS otp_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    otp VARCHAR(6) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_time TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    attempts INT DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

-- 8. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user VARCHAR(100) NOT NULL,
    action VARCHAR(255) NOT NULL,
    time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);
