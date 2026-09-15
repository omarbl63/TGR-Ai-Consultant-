# AdminAI — Employee App (Expo)

The employee-facing mobile app. Employees talk to the **AI agent** in natural language; the agent detects the request type, asks for missing details, creates and submits the request, and shows the AI's recommendation. Employees also track their requests and view the AI analysis.

Built with **React Native · Expo (SDK 57) · TypeScript · NativeWind · React Navigation**. UI in French.

## Screens
- **Connexion** — login (JWT stored in AsyncStorage).
- **Assistant** — ChatGPT-style conversation with the AI agent (suggested prompts, typing indicator, in-chat request cards).
- **Mes demandes** — list of the employee's requests with status/recommendation.
- **Détail de la demande** — structured data, AI analysis (recommendation, risk, missing documents, anomalies, citations), and the timeline.

## Prerequisites
- Node.js 20+
- The backend running (see the root `README.md` → `docker compose up -d`).

## Configure the API URL
The app reads `expo.extra.apiBase` from `app.json` (default `http://localhost:8080/api/v1`).

- **iOS simulator / web:** `localhost` works as-is.
- **Android emulator:** use `http://10.0.2.2:8080/api/v1`.
- **Physical device (Expo Go):** use your machine's LAN IP, e.g. `http://192.168.1.20:8080/api/v1` (phone and computer on the same network). The backend CORS allows `http://localhost:*` origins.

## Run
```bash
cd mobile
npm install
npm start           # then press: i (iOS), a (Android), or w (web)
# or directly:
npm run web         # opens the app in a browser (http://localhost:8081)
```

Sign in with the seeded employee account: **employe@adminai.ma** / `Password123!`.

## Try it
In the Assistant tab, type something like:
> « Je dois aller à Rabat lundi pour une réunion, retour mardi. Je prends le train. »

The agent extracts the details, creates & submits an **Ordre de mission**, runs the RAG-grounded compliance analysis, and replies with its recommendation and a link to the request.

> Note: the mobile app runs on the host (not in Docker) — Expo is designed to run directly via the CLI.
