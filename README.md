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

## Project Status

The project is currently under development.

## Security

This project will use Bitcoin test networks during development.

Real funds should not be used, and private keys, passwords, tokens, or credentials must never be committed to the repository.

## Authors

Marcelo Medina

Leonardo Balmes 

Enrique Zavala

