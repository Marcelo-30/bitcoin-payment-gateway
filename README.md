# Bitcoin Payment Gateway

A platform for creating payment orders, generating Bitcoin invoices, and monitoring transactions on a Bitcoin test network.

## Frontend

The React + TypeScript frontend lives in [`frontend/`](frontend/README.md). See that
README for setup and how to run it.

## API Documentation

The REST API is documented with OpenAPI (springdoc). Once the backend is running, the interactive Swagger UI is available at:

* Swagger UI: http://localhost:8080/swagger-ui.html
* OpenAPI spec (JSON): http://localhost:8080/v3/api-docs

Every endpoint returns a standard error body on failure, with the fields `timestamp`, `status`, `error`, `message` and `path`.

## Project Goal

The goal of this project is to build a payment platform that allows merchants to generate Bitcoin payment requests and monitor their status without using real funds during development.

## Planned Features

* User registration and authentication
* Merchant account management
* Payment order creation
* Bitcoin invoice generation
* QR code generation
* Transaction monitoring
* Automatic payment confirmation
* Payment history
* Webhook notifications
* Administrative dashboard

## Planned Technologies

### Backend

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Bitcoin Core

### Frontend

* React
* TypeScript

### Infrastructure

* Docker
* Docker Compose
* GitHub Actions


## Backend Environment Variables

The backend supports the following environment variables:

| Variable | Default value | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://127.0.0.1:5433/bitcoin_payment_gateway` | PostgreSQL connection URL. |
| `DB_USERNAME` | `postgres` | PostgreSQL username. |
| `DB_PASSWORD` | `postgres` | PostgreSQL password. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated list of frontend origins allowed to access the backend API. |

For local development, the backend allows the Vite frontend by default:

```text
http://localhost:5173
````

To configure a different allowed origin in PowerShell:

```powershell
$env:CORS_ALLOWED_ORIGINS="https://frontend.example.com"
```

To allow multiple origins, separate them with commas:

```powershell
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,https://frontend.example.com"
```

After setting the environment variable, start the backend in the same terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

## Project Status

The project is currently under development.

## Security

This project will use Bitcoin test networks during development.

Real funds should not be used, and private keys, passwords, tokens, or credentials must never be committed to the repository.

## Authors

Marcelo Medina

Leonardo Balmes 

Enrique Zavala

