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


## Test your endpoints
Open these in your browser:
http://localhost:8080/api/test
http://localhost:8080/api/db-test