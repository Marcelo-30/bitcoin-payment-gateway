import { useEffect, useState } from 'react';
import { fetchHealth } from './api/health';
import { apiClient } from './api/client';
import { PaymentForm } from './components/PaymentForm';
import './App.css';

type HealthState =
  | { kind: 'loading' }
  | { kind: 'ok'; status: string; service: string }
  | { kind: 'error'; message: string };

function App() {
  const [health, setHealth] = useState<HealthState>({ kind: 'loading' });

  useEffect(() => {
    let active = true;
    fetchHealth()
      .then((data) => {
        if (active) setHealth({ kind: 'ok', status: data.status, service: data.service });
      })
      .catch((error: unknown) => {
        if (active) {
          setHealth({
            kind: 'error',
            message: error instanceof Error ? error.message : 'Unknown error',
          });
        }
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="app">
      <header className="app__header">
        <h1>Bitcoin Payment Gateway</h1>
        <p className="app__subtitle">Merchant dashboard</p>
      </header>

      <main className="app__main">
        <section className="card">
          <h2>Backend status</h2>
          <BackendStatus health={health} />
          <p className="card__meta">API base URL: {apiClient.baseUrl}</p>
        </section>

        <section className="card">
          <h2>Create payment</h2>
          <PaymentForm />
        </section>
      </main>
    </div>
  );
}

function BackendStatus({ health }: { health: HealthState }) {
  switch (health.kind) {
    case 'loading':
      return <p className="status status--loading">Checking backend…</p>;
    case 'ok':
      return (
        <p className="status status--ok">
          {health.service} is <strong>{health.status}</strong>
        </p>
      );
    case 'error':
      return <p className="status status--error">Cannot reach backend: {health.message}</p>;
  }
}

export default App;
