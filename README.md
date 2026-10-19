# OpenStudy

**AI-Assisted Quiz, Assignment Assessment and Performance Analytics Platform**

## 1. About the Project

OpenStudy is a college integrated project designed to help teachers create quizzes and assignments, assess student performance, and analyze learning outcomes.

The platform will have two main dashboards:

- **Student Dashboard:** Attempt MCQ quizzes, submit assignment answers, view marks, and track personal performance.
- **Teacher Dashboard:** Create quizzes and assignments, review AI-generated questions, review suggested assignment grades, provide feedback, and view class analytics.

## 2. Technology Stack

- **Backend:** Java, Spring Boot, JDBC
- **Frontend:** HTML, CSS, JavaScript, and the configured Java view technology
- **Database:** MySQL
- **Statistical Analysis:** R
- **AI Integration:** An AI API for question generation and suggested subjective-answer evaluation
- **Development Tools:** Maven, Git, GitHub, and VS Code
- **Cloud Database:** Aiven MySQL (planned/configured)

## 3. Repository Structure

- `backend/` — Spring Boot application and automated tests
- `database/` — Database schema, migrations, and sample data
- `R/` — R scripts and statistical analysis
- `docs/` — Project design and team documentation

## 4. Prerequisites

Install the following before running the project:

- Git
- A compatible JDK matching the version configured in `backend/pom.xml`
- Maven, or use the included Maven Wrapper
- MySQL access through a configured database
- R and required R packages for statistical analysis

## 5. Getting Started

### Step 1: Clone the repository

```bash
git clone https://github.com/Zia-Kalra/OpenStudy.git
cd OpenStudy
```

### Step 2: Configure database credentials

Create the required environment variables:

- `DB_URL` — JDBC URL for your configured MySQL database
- `DB_USERNAME` — Application database username
- `DB_PASSWORD` — Application database password

Ask the project maintainer for the approved database setup instructions. Never commit actual credentials to GitHub.

### Step 3: Prepare the database

Follow the instructions in `database/` to create the required schema and load optional dummy data. Use the agreed migration procedure so every team member has a consistent database structure.

### Step 4: Run the backend

From the repository root, use the Maven Wrapper.

Windows:

```powershell
.\backend\mvnw.cmd -f backend\pom.xml test
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

If the database environment variables are not configured, the application may fail to start. Configure them before running the application.

Teacher Dashboard (Member 5) can be started without MySQL using the `mock` profile:

```powershell
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=mock"
```

Open http://localhost:8080/teacher/dashboard

### Student account registration and password recovery

Student sign-up uses the existing `users` table. MySQL/Aiven generates `user_id` when the new row is inserted; student sign-up cannot create teacher accounts. Passwords are stored as BCrypt hashes. The app does not run schema or migration scripts automatically. The approved database setup must already have created the `users` table before using the database-backed profile.

Do not use the `mock` profile for registration or password recovery. Configure the database and mail provider using environment variables, then start without `-Dspring-boot.run.profiles=mock`:

- `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` — database connection
- `DB_TRUSTSTORE_URL` and `DB_TRUSTSTORE_PASSWORD` — when required by the Aiven TLS configuration
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, and `SMTP_PASSWORD` — outbound email for password-reset links
- `PASSWORD_RESET_SECRET` — a private random value of at least 32 characters used to sign 30-minute reset links
- `APP_PUBLIC_BASE_URL` — externally reachable app URL used in reset emails; use `http://localhost:8080` only for local testing

Never commit database, mail, or reset-signing credentials. Password reset returns a generic response whether or not an account exists. Reset links are invalidated after the password changes.

Integration notes: `docs/member5-integration.md`

## 6. Database Guidelines

- Use the agreed database schema and migrations.
- Use dummy student and assessment data during development.
- Do not store plaintext passwords.
- Do not commit database passwords, API keys, or private certificates.
- Avoid destructive database changes without coordinating with the team.

## 7. Team Development Workflow

1. Pull the latest changes from `main`.
2. Create a separate feature branch for your assigned work.
3. Implement and test your changes.
4. Commit only the relevant files.
5. Open a pull request for review before merging into `main`.

Do not commit directly to `main`.

Coordinate database schema changes and shared Java interfaces with the team before implementing dependent features.

## 8. Project Status

This repository is being prepared for collaborative development. Features, database migrations, analytics scripts, and integrations should be documented as they are implemented and tested.

## 9. License

See the repository's `LICENSE` file for licensing terms.

<<<<<<< HEAD
## Assignment feature

The assignment workspace is served at `/assignments/`. Apply the SQL migrations in order, including `database/migrations/V4__assignment_attempts_and_attachments.sql`, before using it. V4 adds attempt timing and attachment metadata to the existing V2/V3 assignment model.

The feature expects the application's authenticated session to expose either a `User` object under `user`, or `userId` and `role` attributes. It enforces the teacher/student role and assignment ownership/enrollment in the assignment endpoints. The repository currently has no sign-in implementation, so a session must be established by the authentication feature.

Set `GEMINI_API_KEY` in the server environment to enable AI question suggestions and answer evaluation. `GEMINI_MODEL` is optional and defaults to `gemini-2.5-flash`. The browser never receives the key. Student uploads are stored under `uploads/assignments` (configurable with the `openstudy.upload-dir` JVM system property); retain this directory with the application deployment and serve downloads through an authenticated endpoint before exposing attachments for retrieval.


=======
>>>>>>> main
## Test your endpoints

Open these in your browser:
http://localhost:8080/api/test
http://localhost:8080/api/db-test
