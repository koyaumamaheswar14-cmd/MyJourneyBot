
# MyJourney

MyJourney is a Telegram-based personal learning and knowledge management bot built with Java and Spring Boot. It helps users organize, store, search, retrieve, and export their learning information directly through Telegram.

## Features

- Create and manage learning categories
- Add and manage learning information
- Search stored learning information
- Generate PDF reports
- Filter information by category and time period
- Interactive Telegram reply keyboards
- State-based conversational workflows
- Firebase integration
- Export learning information as PDF
- Real-time interaction through Telegram Bot API and long polling

## Tech Stack

- Java
- Spring Boot
- Telegram Bot API
- Firebase
- REST APIs
- Maven
- PDF Generation
- Git & GitHub

## Architecture

```text
                    ┌─────────────────┐
                    │      User       │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    Telegram     │
                    │      App        │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ Telegram Bot API│
                    └────────┬────────┘
                             │
                             ▼
              ┌──────────────────────────┐
              │       Spring Boot        │
              │         Backend          │
              └────────────┬─────────────┘
                           │
       ┌───────────────────┼───────────────────┐
       │                   │                   │
       ▼                   ▼                   ▼
┌──────────────┐   ┌───────────────┐   ┌──────────────┐
│ UserService  │   │CategoryService│   │Information   │
│              │   │               │   │Service       │
└──────────────┘   └───────────────┘   └──────┬───────┘
                                               │
                                               ▼
                                      ┌────────────────┐
                                      │    Firebase    │
                                      └────────────────┘
                                               │
                                               ▼
                                      ┌────────────────┐
                                      │   PdfService   │
                                      └────────────────┘
````

## Application Flow

### Add Learning Information

```text
User
 ↓
Telegram Bot
 ↓
Select Category
 ↓
Enter Learning Information
 ↓
InformationService
 ↓
Firebase
```

### Search Information

```text
User
 ↓
Search
 ↓
Enter Search Query
 ↓
InformationService
 ↓
Firebase
 ↓
Matching Results
 ↓
Telegram Bot
 ↓
User
```

### Generate PDF

```text
User
 ↓
Generate PDF
 ↓
Select Category / Time Period
 ↓
Fetch Matching Information
 ↓
PdfService
 ↓
Generate PDF
 ↓
Send PDF through Telegram
```

## Project Structure

```text
demo/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.mybotty.demo/
│   │   │       ├── CategoryService.java
│   │   │       ├── DemoApplication.java
│   │   │       ├── FirebaseConfig.java
│   │   │       ├── InformationService.java
│   │   │       ├── MyJourneyBot.java
│   │   │       ├── PdfService.java
│   │   │       └── UserService.java
│   │   │
│   │   └── resources/
│   │       ├── static/
│   │       ├── templates/
│   │       └── application.yaml
│   │
│   └── test/
│
├── .gitattributes
├── .gitignore
├── HELP.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Core Components

### MyJourneyBot

Handles Telegram messages, commands, reply keyboards, and state-based conversational workflows.

### UserService

Handles user-related operations and user data management.

### CategoryService

Manages learning categories and category-related operations.

### InformationService

Handles storing, retrieving, searching, and managing learning information.

### PdfService

Generates PDF documents based on stored learning information and selected filters.

### FirebaseConfig

Configures Firebase and initializes Firebase Admin SDK integration.

### DemoApplication

Main Spring Boot application entry point.

## Environment Variables

Configure sensitive values using environment variables:

```env
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=

FIREBASE_PROJECT_ID=
FIREBASE_CLIENT_EMAIL=
FIREBASE_PRIVATE_KEY=

WEATHER_API_KEY=
```

> Important: Never commit Telegram bot tokens, Firebase private keys, service-account JSON files, or other secrets to GitHub.

## Run Locally

### Clone the Repository

```bash
git clone <your-repository-url>
cd demo
```

### Configure Environment Variables

Set the required environment variables in your local environment.

### Build

Windows:

```cmd
mvnw.cmd clean install
```

Linux/macOS:

```bash
./mvnw clean install
```

### Run

Windows:

```cmd
mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Once the application starts, open Telegram and interact with the configured MyJourney bot.

## Deployment

MyJourney can be deployed on cloud platforms such as Render or other Java-compatible hosting platforms.

Before deployment:

1. Configure all required environment variables.
2. Do not upload Firebase service-account credentials to GitHub.
3. Configure the production Firebase project.
4. Configure the Telegram bot token.
5. Build and deploy the Spring Boot application.

## Future Enhancements

### 4-Digit PIN-Based Access Protection

* Allow users to set a 4-digit PIN.
* Allow users to update their PIN.
* Require PIN verification before viewing protected information.
* Require PIN verification before generating PDF reports.
* Store PINs securely using hashing such as BCrypt.

### Improved User Management

* Enhanced user profiles.
* User-specific learning statistics.
* Personalized learning dashboards.

### Learning Analytics

* Track learning progress.
* Category-wise learning statistics.
* Daily, weekly, and monthly learning summaries.

### Learning Reminders

* Schedule learning reminders.
* Daily and weekly study notifications.

### Tags and Advanced Organization

* Add tags to learning information.
* Filter information using multiple tags and categories.

### Advanced Search

* Search by category, date, keywords, and tags.
* Improve search and filtering capabilities.

### Cloud and Backup Improvements

* Automated data backup.
* Improved data recovery and synchronization.

### Additional Client Support

* Explore a web or mobile interface for accessing the same learning data.

### AI-Powered Learning Assistance

* Summarize stored learning information.
* Generate revision notes.
* Provide personalized learning suggestions.

## Project Objective

MyJourney aims to provide a simple personal learning management system through Telegram without requiring a separate web or mobile application.

The project combines Java, Spring Boot, Telegram Bot API, Firebase, state-based conversation management, CRUD operations, search, and PDF generation into a single learning and knowledge management platform.

## Author

**Uma Maheswar Koya**

B.Tech – Information Technology
Vishnu Institute of Technology
2024–2028

