# 💳 PayFlow

> **Cloud Native Fintech Platform** — Billetera digital con transferencias P2P construida con microservicios Java/Spring Boot, React y desplegada en AWS.

![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?style=flat-square&logo=mongodb&logoColor=white)
![Podman](https://img.shields.io/badge/Podman-Rootless-892CA0?style=flat-square&logo=podman&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-Free_Tier-FF9900?style=flat-square&logo=amazonaws&logoColor=white)

---

## 📌 Tabla de Contenidos

- [¿Qué es PayFlow?](#-qué-es-payflow)
- [Arquitectura](#-arquitectura)
- [Stack Tecnológico](#-stack-tecnológico)
- [Microservicios](#-microservicios)
- [Decisiones Técnicas](#-decisiones-técnicas)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Requisitos Previos](#-requisitos-previos)
- [Setup Local](#-setup-local)
- [Variables de Entorno](#-variables-de-entorno)
- [API Reference](#-api-reference)
- [Infraestructura AWS](#-infraestructura-aws)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Testing](#-testing)
- [Roadmap](#-roadmap)

---

## 🎯 ¿Qué es PayFlow?

PayFlow es una plataforma fintech **cloud native** que permite a usuarios:

- 📲 Registrarse y autenticarse con JWT seguro
- 💰 Gestionar una billetera digital con saldo en tiempo real
- 🔄 Realizar transferencias P2P con garantías ACID
- 📊 Consultar historial completo de movimientos
- 🔔 Recibir notificaciones asíncronas por email

Está construida con una **arquitectura de microservicios** donde cada servicio tiene una responsabilidad única, su propia base de datos y se comunica a través de HTTP y mensajería asíncrona (AWS SQS).

---

## 🏗 Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        React SPA (S3)                       │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTPS
┌────────────────────────────▼────────────────────────────────┐
│              API Gateway  :8080                             │
│         (Spring Cloud Gateway + JWT Filter)                 │
└──┬─────────────┬──────────────┬──────────────┬─────────────┘
   │             │              │              │
   ▼             ▼              ▼              ▼
Auth :8081  Wallet :8082  Transfer :8083  Transaction :8084
   │             │              │              │
   ▼             ▼              ▼              ▼
MySQL(RDS)   MySQL(RDS)    MySQL(RDS)      MongoDB Atlas
                                │
                                │ SQS Event
                                ▼
                      Notification :8085
                                │
                                ▼
                            AWS SNS
                           (Email/Push)
```

### Flujo de una Transferencia

```
Usuario → Gateway → Transfer Service
                         │
                         ├─→ Wallet Service (débito cuenta origen)   ─┐
                         │                                             │ MySQL ACID
                         ├─→ Wallet Service (crédito cuenta destino) ─┘
                         │
                         ├─→ Transaction Service (log en MongoDB)
                         │
                         └─→ SQS → Notification Service → SNS (email)
```

---

## 🛠 Stack Tecnológico

| Capa | Tecnología | Justificación |
|---|---|---|
| **Backend** | Java 21 + Spring Boot 3.5 | LTS con Virtual Threads para alto throughput I/O-bound |
| **Frontend** | React 18 + Vite + TailwindCSS | SPA ligera, build estático deployable en S3 |
| **DB Transaccional** | MySQL 8 (AWS RDS) | Transacciones ACID para wallets y transferencias |
| **DB Historial** | MongoDB 7 (Atlas M0) | Append-only, lectura intensiva, esquema flexible |
| **Gateway** | Spring Cloud Gateway | Enrutamiento, JWT validation, rate limiting centralizado |
| **Mensajería** | AWS SQS + SNS | Desacoplamiento async de notificaciones |
| **Contenedores** | Podman (rootless) | Seguridad mejorada vs Docker, compatible OCI |
| **CI/CD** | GitHub Actions + ECR | Build → Test → Push → Deploy automatizado |
| **Infra** | AWS (EC2, RDS, S3, ECR) | Free Tier / bajo costo (~$1–5/mes) |
| **Observabilidad** | Spring Actuator + CloudWatch | Health checks y logs centralizados |

---

## 🧩 Microservicios

### `auth-service` — Puerto 8081
Gestiona el ciclo de vida de autenticación de usuarios.

| Endpoint | Método | Auth | Descripción |
|---|---|---|---|
| `/api/auth/register` | POST | ❌ | Registro de nuevo usuario |
| `/api/auth/login` | POST | ❌ | Login, retorna access + refresh token |
| `/api/auth/refresh` | POST | ❌ | Renueva el access token |
| `/api/auth/me` | GET | ✅ JWT | Datos del usuario autenticado |

**Stack interno:** Spring Security · jjwt 0.12.6 · BCrypt · MySQL

---

### `wallet-service` — Puerto 8082
Gestiona cuentas y saldos de usuarios.

| Endpoint | Método | Auth | Descripción |
|---|---|---|---|
| `/api/wallets/me` | GET | ✅ JWT | Ver/crear mi cuenta wallet |
| `/api/wallets/me/balance` | GET | ✅ JWT | Consultar saldo disponible |
| `/api/wallets/topup` | POST | ✅ JWT | Agregar saldo a la cuenta |

**Stack interno:** Spring Data JPA · MySQL · Pessimistic Lock · BigDecimal

---

### `transfer-service` — Puerto 8083
Orquesta transferencias P2P con garantías ACID.

| Endpoint | Método | Auth | Descripción |
|---|---|---|---|
| `/api/transfers` | POST | ✅ JWT | Realizar transferencia P2P |
| `/api/transfers/{id}` | GET | ✅ JWT | Detalle de una transferencia |

**Stack interno:** @Transactional · Feign Client · SQS Producer

---

### `transaction-service` — Puerto 8084
Historial y auditoría de movimientos financieros.

| Endpoint | Método | Auth | Descripción |
|---|---|---|---|
| `/api/transactions` | GET | ✅ JWT | Historial paginado del usuario |
| `/api/transactions/{id}` | GET | ✅ JWT | Detalle de un movimiento |

**Stack interno:** Spring Data MongoDB · Paginación · Filtros por fecha/tipo

---

### `notification-service` — Puerto 8085
Consume eventos de SQS y envía notificaciones.

| Trigger | Canal | Descripción |
|---|---|---|
| SQS `transfer.completed` | Email (SNS) | Notifica origen y destino de la transferencia |

**Stack interno:** AWS SDK SQS Consumer · Spring SNS · Retry automático

---

### `api-gateway` — Puerto 8080
Punto de entrada único. JWT validation, routing y rate limiting.

**Stack interno:** Spring Cloud Gateway · ReactiveWeb · JWT Filter

---

## 🧠 Decisiones Técnicas

### ¿Por qué dos bases de datos?

**MySQL** para wallets y transferencias porque las operaciones requieren **transacciones ACID**: si un transfer debita la cuenta A pero falla antes de acreditar la B, el rollback garantiza consistencia del dinero.

**MongoDB** para el historial de transacciones porque es un log **append-only**, con lectura intensiva, sin necesidad de joins, y el esquema puede variar según el tipo de movimiento (top-up, transfer, reverso).

### ¿Por qué Podman sobre Docker?

Podman corre **rootless por defecto** — los contenedores no tienen acceso root al host. Mejor postura de seguridad para servicios financieros. Compatible con OCI, misma sintaxis que Docker.

### ¿Por qué Virtual Threads (Java 21)?

Los microservicios son I/O-bound (MySQL, MongoDB, SQS). Virtual Threads permiten manejar miles de requests concurrentes sin cambiar el modelo de programación imperativo, sin callbacks ni reactive streams.

### ¿Por qué SQS y no Kafka?

Para el volumen de este proyecto (demo/MVP) SQS tiene cobertura suficiente, está en el Free Tier de AWS, y no requiere infraestructura adicional. Kafka sería la evolución natural en producción con mayor throughput.

### ¿Por qué Spring Cloud Gateway y no NGINX?

Gateway permite implementar la validación JWT en código Java, con acceso a los mismos beans de Spring Security. Más fácil de mantener en un equipo Java que configurar NGINX + Lua scripts.

---

## 📁 Estructura del Proyecto

```
payflow/
├── pom.xml                          # Parent POM multi-módulo
├── podman-compose.yml               # Infraestructura local (MySQL + MongoDB)
├── .env.example                     # Plantilla de variables de entorno
├── .github/
│   └── workflows/
│       └── deploy.yml               # CI/CD Pipeline
│
├── api-gateway/                     # Spring Cloud Gateway :8080
│   └── src/main/
│       ├── java/com/payflow/gateway/
│       └── resources/application.yml
│
├── auth-service/                    # Autenticación JWT :8081
│   └── src/main/
│       ├── java/com/payflow/auth/
│       │   ├── config/SecurityConfig.java
│       │   ├── controller/AuthController.java
│       │   ├── dto/
│       │   ├── entity/User.java
│       │   ├── exception/
│       │   ├── repository/UserRepository.java
│       │   ├── security/
│       │   │   ├── JwtService.java
│       │   │   ├── JwtAuthFilter.java
│       │   │   └── UserDetailsServiceImpl.java
│       │   └── service/AuthService.java
│       └── resources/application.yml
│
├── wallet-service/                  # Billetera digital :8082
│   └── src/main/
│       ├── java/com/payflow/wallet/
│       │   ├── config/SecurityConfig.java
│       │   ├── controller/WalletController.java
│       │   ├── dto/
│       │   ├── entity/Account.java
│       │   ├── exception/
│       │   ├── repository/AccountRepository.java
│       │   ├── security/
│       │   └── service/WalletService.java
│       └── resources/application.yml
│
├── transfer-service/                # Transferencias P2P :8083
├── transaction-service/             # Historial MongoDB :8084
├── notification-service/            # Notificaciones SQS/SNS :8085
│
└── frontend/                        # React 18 + Vite + TailwindCSS
    ├── src/
    │   ├── components/
    │   ├── pages/
    │   ├── hooks/
    │   └── services/
    └── package.json
```

---

## ✅ Requisitos Previos

| Herramienta | Versión | Verificar |
|---|---|---|
| Java | 21 LTS | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 20 LTS | `node -v` |
| Podman | 4.x+ | `podman -v` |
| podman-compose | 1.x | `podman-compose --version` |
| Git | 2.x+ | `git --version` |
| AWS CLI | 2.x | `aws --version` |

---

## 🚀 Setup Local

### 1. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/payflow.git
cd payflow
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
# Editar .env con tus valores
```

### 3. Levantar las bases de datos con Podman

```bash
podman-compose up -d mysql mongodb

# Verificar que están corriendo:
podman ps
```

### 4. Compilar todos los módulos

```bash
mvn clean install -DskipTests
```

### 5. Arrancar los servicios (cada uno en una terminal)

```bash
# Terminal 1 — Auth Service
cd auth-service && mvn spring-boot:run

# Terminal 2 — Wallet Service
cd wallet-service && mvn spring-boot:run

# Terminal 3 — Transfer Service (próximamente)
cd transfer-service && mvn spring-boot:run

# Terminal 4 — API Gateway (próximamente)
cd api-gateway && mvn spring-boot:run
```

### 6. Verificar que todo está up

```bash
curl http://localhost:8081/actuator/health   # Auth
curl http://localhost:8082/actuator/health   # Wallet
```

### 7. Prueba rápida E2E

```bash
# Register
curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ana García","email":"ana@payflow.com","password":"secret123"}' \
  | python3 -m json.tool

# Login y guardar token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@payflow.com","password":"secret123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Top-up
curl -s -X POST http://localhost:8082/api/wallets/topup \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000.00, "description": "Depósito inicial"}' \
  | python3 -m json.tool
```

---

## 🔐 Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto basándote en `.env.example`:

```env
# MySQL
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DB=payflow_db
MYSQL_USER=payflow_user
MYSQL_PASS=payflow_pass

# MongoDB
MONGO_HOST=localhost
MONGO_PORT=27017
MONGO_DB=payflow_transactions
MONGO_USER=payflow_user
MONGO_PASS=payflow_pass

# JWT — mínimo 64 caracteres para HS512
JWT_SECRET=payflow-super-secret-key-that-is-at-least-64-characters-long-for-hs512
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

# AWS (para Semana 3)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=tu-access-key
AWS_SECRET_ACCESS_KEY=tu-secret-key
SQS_TRANSFER_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/XXXX/payflow-transfers
SNS_NOTIFICATIONS_TOPIC_ARN=arn:aws:sns:us-east-1:XXXX:payflow-notifications
```

> ⚠️ **Nunca subas `.env` a GitHub.** Está en `.gitignore`.

---

## 📡 API Reference

La colección completa de Postman/Bruno está en `/docs/PayFlow.postman_collection.json`.

### Autenticación

Todos los endpoints protegidos requieren el header:
```
Authorization: Bearer <access_token>
```

El `access_token` expira en **15 minutos**. Usa `/api/auth/refresh` con el `refreshToken` (válido 7 días) para obtener uno nuevo sin re-login.

### Códigos de respuesta

| Código | Significado |
|---|---|
| `200` | OK |
| `201` | Recurso creado |
| `400` | Validación fallida |
| `401` | Token inválido o expirado |
| `403` | Sin permisos / cuenta suspendida |
| `404` | Recurso no encontrado |
| `409` | Conflicto (ej: email ya registrado) |
| `422` | Error de negocio (ej: saldo insuficiente) |
| `500` | Error interno |

### Formato de error (RFC 7807)

```json
{
  "timestamp": "2026-03-05T15:00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente"
}
```

---

## ☁️ Infraestructura AWS

| Servicio | Uso | Costo estimado |
|---|---|---|
| EC2 t2.micro | API Gateway + 5 microservicios (Podman) | Free Tier 12 meses |
| RDS MySQL t3.micro | wallets, transfers, users | Free Tier 12 meses |
| MongoDB Atlas M0 | transaction logs | Gratis permanente |
| ECR | Registry de imágenes Podman | ~$0.50/mes |
| SQS | Cola de notificaciones async | Free Tier 1M req/mes |
| SNS | Envío de emails | Free Tier 1M ntf/mes |
| S3 | Frontend React (build estático) | Free Tier 5GB |
| CloudWatch | Logs y alertas | Free Tier |
| **TOTAL** | | **~$1–5/mes** |

### Configurar AWS CLI

```bash
aws configure
# AWS Access Key ID: (IAM → Users → Security credentials)
# Secret Access Key: (solo visible al crear)
# Default region: us-east-1
# Output format: json

# Verificar
aws sts get-caller-identity
```

---

## ⚙️ CI/CD Pipeline

El pipeline en `.github/workflows/deploy.yml` se ejecuta en cada push a `main`:

```
Push to main
    │
    ▼
1. Checkout código
    │
    ▼
2. Setup Java 21 + Maven cache
    │
    ▼
3. mvn clean install (con tests)
    │
    ▼
4. Build imágenes Podman
    │
    ▼
5. Push a Amazon ECR
    │
    ▼
6. SSH a EC2 → pull imágenes → podman-compose up
    │
    ▼
7. Health check de todos los servicios
```

### Secrets requeridos en GitHub

| Secret | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM user con permisos ECR + EC2 |
| `AWS_SECRET_ACCESS_KEY` | Secret del IAM user |
| `EC2_HOST` | IP pública de la instancia |
| `EC2_SSH_KEY` | Private key PEM de EC2 |
| `JWT_SECRET` | El mismo secret que en producción |

---

## 🧪 Testing

```bash
# Todos los tests
mvn test

# Solo un módulo
cd auth-service && mvn test

# Con reporte de cobertura
mvn test jacoco:report
# Reporte en: target/site/jacoco/index.html
```

### Cobertura objetivo

| Servicio | Objetivo | Prioridad |
|---|---|---|
| `transfer-service` | ≥ 70% | 🔴 Crítico — maneja dinero |
| `auth-service` | ≥ 60% | 🟠 Alta — maneja credenciales |
| `wallet-service` | ≥ 50% | 🟡 Media |
| Resto | ≥ 40% | 🟢 Normal |

---

## 🗓 Roadmap

### ✅ Semana 1 — Fundamentos (27 Feb – 5 Mar)
- [x] Setup mono-repo Maven multi-módulo
- [x] Podman Compose con MySQL y MongoDB
- [x] Auth Service: register, login, refresh, JWT
- [x] Wallet Service: account, balance, top-up
- [ ] API Gateway: routing + JWT filter
- [ ] Frontend React: Login + Dashboard skeleton

### ⏳ Semana 2 — Core Fintech (6 – 13 Mar)
- [ ] Transfer Service: transferencias ACID
- [ ] Transaction Service: historial en MongoDB
- [ ] Notification Service: SQS + SNS
- [ ] Frontend: transferencias + historial

### ⏳ Semana 3 — AWS + CI/CD (14 – 21 Mar)
- [ ] EC2 + RDS + MongoDB Atlas + ECR
- [ ] GitHub Actions pipeline completo
- [ ] Frontend en S3 con HTTPS
- [ ] CloudWatch logs y alertas

### ⏳ Semana 4 — Pulido + Presentación (22 – 27 Mar)
- [ ] HTTPS + rate limiting + validaciones
- [ ] Dashboard con Chart.js
- [ ] README final + colección Postman
- [ ] Slide deck + script demo

---

## 🤝 Contribuir

Este es un proyecto de portafolio personal. Si encuentras un bug o tienes una sugerencia, abre un Issue.

---

## 📄 Licencia

MIT © 2025 PayFlow Project

---

<p align="center">
  Construido con ☕ Java · ⚛️ React · ☁️ AWS · 🐳 Podman
</p>