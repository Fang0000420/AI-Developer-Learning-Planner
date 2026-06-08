# Frontend

Next.js frontend application for AI Developer Learning Planner.

## Stack

- Node.js `>=20.9.0`
- Next.js 16
- React 19
- TypeScript
- Tailwind CSS 4
- ESLint
- Prettier

## Server Startup

Run frontend commands on the server project path:

```bash
cd /home/AI-Developer-Learning-Planner/frontend
npm install
npm run dev
```

The development server listens on `http://localhost:3000` by default.

## Scripts

```bash
npm run dev
npm run build
npm run start
npm run lint
npm run typecheck
npm run format
npm run format:check
```

## Environment

Frontend API variables are defined from the repository root `.env.example`.

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Day 04 task 1 only initializes the frontend project skeleton. Backend health
integration is planned for a later Day 04 task.
