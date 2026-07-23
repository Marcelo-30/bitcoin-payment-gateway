# Bitcoin Payment Gateway — Frontend

React + TypeScript single-page app (built with Vite) for the Bitcoin Payment Gateway.

## Requirements

- Node.js 20+ and npm

## Setup

```bash
cd frontend
npm install
cp .env.example .env   # then adjust VITE_API_BASE_URL if needed
```

### Environment variables

| Variable            | Description                     | Default                 |
| ------------------- | ------------------------------- | ----------------------- |
| `VITE_API_BASE_URL` | Base URL of the backend REST API | `http://localhost:8080` |

If `.env` is missing, the API client falls back to `http://localhost:8080`.

## Scripts

| Command           | Description                                  |
| ----------------- | -------------------------------------------- |
| `npm run dev`     | Start the dev server with hot reload         |
| `npm run build`   | Type-check and build the production bundle   |
| `npm run preview` | Serve the production build locally           |
| `npm run lint`    | Run the linter                               |

## Running

Make sure the backend is running (see the root `README`), then:

```bash
npm run dev
```

The app is served at http://localhost:5173 and shows the backend health status,
which it reads from `GET /api/health` through the reusable API client in
`src/api/`.
