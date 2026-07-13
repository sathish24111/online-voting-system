# Department Online Voting System

A secure, responsive, and anonymous online voting application for college department elections, built with Java Spring Boot, Spring Security, JPA, and a Glassmorphism frontend using HTML5, CSS3, Vanilla JS, and Chart.js.

## Directory Structure
```
online-voting-system/
│
├── database/
│   ├── schema.sql
│   └── data.sql
│
├── docs/
│   └── README.md
│
├── pom.xml
│
└── src/
    └── main/
        ├── java/com/college/voting/ (Backend packages: config, controller, dto, entity, etc.)
        └── resources/
            ├── application.properties
            └── static/               (Frontend: index.html, css, js, pages, images)
```

## Features & Security
1. **True Ballot Secrecy**: The voter's identity is completely detached from the choices recorded. Voter participation is logged in a separate participation table to prevent double voting.
2. **6-Digit Gmail OTP**: Protects login screens. Dispatches codes using Gmail SMTP (with a fallback logging mechanism that prints codes to the server console for easy local testing).
3. **Session Protections**: Concurrency limits (1 session max per student), automatic logouts on vote casts, and 15-minute session timeouts.
4. **Rate Limiting**: Throttles brute-force attempts on login screens (blocks IP/account for 15 minutes after 5 errors) and limits OTP resends (max 3 resends per session, with a 30s cooldown).
5. **Voter CSV & Excel Import**: Admins can import student directories in bulk. Duplicate register numbers/emails are automatically skipped, and details are output in a validation summary.
6. **Live charts & Exports**: Aggregates vote tallies (hidden until election is ended) and supports printing/saving results as PDF, Excel, and CSV files.

---

## Installation & Setup

### 1. Database Configuration
1. Start your local MySQL server.
2. Create the database and import schema/seed files using a MySQL client:
   ```sql
   CREATE DATABASE voting_db;
   USE voting_db;
   ```
   Run the statements in `database/schema.sql` to create tables, followed by `database/data.sql` to import mock candidates and admin credentials.
3. Update database credentials in `src/main/resources/application.properties` if they differ from the defaults (username `root` and empty password).

### 2. Gmail SMTP Setup (Optional)
To enable real email dispatch:
1. Turn on 2-Factor Authentication on your Google Account.
2. Go to Google Account Settings -> Security -> App Passwords. Generate an App Password for "Mail".
3. Update the SMTP settings in `src/main/resources/application.properties`:
   ```properties
   spring.mail.username=your-gmail@gmail.com
   spring.mail.password=your-16-character-app-password
   ```
*Note: If these properties remain as placeholders, the application will print the generated OTP directly to the Java console log, allowing immediate testing.*

---

## Running the Application
From the project root directory, compile and launch using Maven:

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

---

## Testing Workflows

### 1. Admin Login & Configurations
1. Open `http://localhost:8080` in your browser.
2. Navigate to the **Admin Portal** and log in with:
   - **Username**: `admin`
   - **Password**: `admin123`
3. Check the **Dashboard** metrics.
4. Go to **Manage Students** to add individual students, reset passwords, or upload spreadsheets (using the template button to download sample sheets).
5. Go to **Manage Candidates** to register candidates and upload manifesto texts.
6. Check **Election Settings** to verify the scheduled union election (pre-set to `RUNNING` status by `data.sql`).

### 2. Student Login & Ballot Casting
1. Open a new Incognito browser window (to avoid cookie conflict with the admin session).
2. Go to the **Student Portal** and log in with a pre-configured student:
   - **Register Number**: `2026CSE001`
   - **Password**: `Password123!`
3. Check your **Java Server Console** to retrieve the generated 6-digit OTP (e.g. `123456`).
4. Type the OTP on the verification screen.
5. Check your details and election timers on the **Student Dashboard**, then proceed to the **Voting Page**.
6. Select one candidate for each position, click **Submit Ballot**, review selections in the warning modal, and confirm.
7. The system will save your votes anonymously, mark you as voted in `election_participation`, and log you out.

### 3. Result Inspection & Exports
1. Return to the **Admin Portal** (or log back in).
2. To make results visible, go to **Election Settings** and click **End** on the active election.
3. Navigate to **Results Dashboard** to view winner cards, turnout rates, and Chart.js bar graphs.
4. Trigger Excel, CSV, or PDF exports by clicking the download buttons.
