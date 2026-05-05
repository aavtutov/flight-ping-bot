# ✈️ About FlightPing

**FlightPing bot** is a personal flight assistant that delivers real-time updates directly to your telegram chat. 

## 🎯 Why FlightPing?

Was built to solve common travel frustrations:

*   **No "Heavy" Apps**: No need to download bulky applications for one simple feature.
*   **Privacy First**: No registration, no passwords, and no personal data required. Your privacy stays with you.
*   **Zero Spam**: The bot only talks to you when there is an update about *your* flight.
*   **One Mission**: Once your flight lands and you're off the plane, the interaction ends. No lingering background processes or annoying notifications.
*   **Instant Start**: From starting the bot to tracking your flight takes less than 10 seconds.

### 🔍 What it tracks
*   **Live Status:** On-time, delayed, boarding, check-in and many others.
*   **Schedule:** Actual vs. scheduled times and aircraft model.
*   **Airport Details:** Terminal, gate, and even **check-in desks**.

### 🎯 Smart Features and Logic
*   **Instant Updates (Webhooks):** The bot doesn't "poll" the API. It listens for active signals (Webhooks) from AeroDataBox and notifies you the second something changes.
*   **Codeshare Support:** Automatically recognizes and tracks codeshare flights.
*   **Smart Aliases:** Handles various input formats (e.g., "KL123", "kl 123", "KLM 123") by normalizing flight codes before searching.
*   **API Optimization:** To save API credits and speed up responses, the bot first checks aliases and the local database for existing, up-to-date flight data before making external requests.
*   **Resilience:** Built-in retry logic for `429 Too Many Requests` ensures the bot stays stable even under API rate limits.

## 🛠 Tech Stack

- **Java 17** / **Spring Boot 3**
- **API and Integration**: Telegram Bot API (Webhooks), AeroDataBox API (Webhooks)
- **Database**: PostgreSQL 15
- **Caching**: Redis 7
- **Mapping**: MapStruct
- **DevOps**: Docker Compose

## 🚀 Quick Start

1. **Clone:**

```bash
git clone https://github.com/aavtutov/flight-ping-bot
cd flight-ping-bot
```

2. **Setup:**

```bash
cp .env.example .env
nano .env
# Edit .env with your credintials, Telegram Token and API Keys
```

3. **Run:**

```bash
docker-compose up --build -d
```
 
   
## 🏗 Infrastructure

The app is fully containerized, so it runs anywhere with Docker.

*   **One-command setup**: Use Docker Compose to launch the entire stack (App, DB, Redis) instantly.
*   **Smart startup**: Built-in health checks to ensure the Java app waits for PostgreSQL and Redis to be fully ready before starting.
*   **Persistent data**: Docker volumes to keep your flight subscriptions and user data safe, even if containers restart.
*   **Secure config**: No hardcoded secrets. All API keys and credentials stay safe in environment variables.

### 🔑 Environment credentials

**Important:** Never share your `.env` file or commit it to version control. The `.gitignore` file included in this project is configured to protect your credentials.
