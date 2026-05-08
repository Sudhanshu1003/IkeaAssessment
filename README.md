# Java Hackathon Assignment - 6 Hour Edition

This is a **hackathon-style code assignment** designed to be completed in approximately **6 hours**. It covers API design, persistence, testing patterns, and transaction management in a real-world Quarkus application.

## Quick Start

```bash
# 1. Start the application in dev mode
./mvnw quarkus:dev

# 2. Access Swagger UI
open http://localhost:8080/q/swagger-ui

# 3. Run tests
./mvnw test

# 4. Compile and package
./mvnw package
```

## Before You Begin

Read [BRIEFING.md](BRIEFING.md) for domain context, then [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md) for your tasks.

---

## CI/CD Pipeline

This project includes a **comprehensive CI/CD pipeline** with automated build, test, security, and deployment capabilities.

### 🚀 **Main Pipeline Stages:**
1. **Build & Test** - Maven build with JaCoCo coverage (80% threshold)
2. **Code Quality** - SpotBugs, Checkstyle, SonarCloud analysis
3. **Security Scanning** - Trivy, OWASP Dependency Check, CodeQL
4. **Docker Build** - Container image creation and push to registry
5. **Integration Tests** - Database integration testing
6. **Performance Tests** - K6 load testing
7. **Deployment** - Automated deployment to staging/production

### 📋 **Available Workflows:**
- **`main.yml`** - Complete CI/CD pipeline with all stages
- **`build-test.yml`** - Simplified build and test workflow
- **`security.yml`** - Dedicated security scanning
- **`quality-gates.yml`** - Code quality and coverage checks
- **`release.yml`** - Automated release management
- **`rollback.yml`** - Manual rollback to previous versions
- **`health-check.yml`** - Automated health monitoring
- **`cleanup.yml`** - Artifact cleanup automation

### 📊 **Quality Gates:**
- **Code Coverage**: Minimum 80% threshold (JaCoCo)
- **Static Analysis**: SpotBugs and Checkstyle validation
- **Security**: Vulnerability scanning and dependency checks
- **Performance**: Response time and throughput validation

### 🐳 **Deployment Options:**
- **Docker Compose**: Local development environment
- **Kubernetes**: Production-ready manifests
- **GitHub Actions**: Automated CI/CD workflows

### 🔧 **Quick Commands:**
```bash
# Run full pipeline locally
docker-compose up --build

# Run tests with JaCoCo coverage (80% threshold)
./mvnw clean test jacoco:report

# View JaCoCo coverage report
open target/site/jacoco/index.html

# Build Docker image
docker build -f src/main/docker/Dockerfile.jvm -t warehouse-app .

# Deploy to Kubernetes
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/staging/
```

### 📈 **Coverage Tracking:**
- **Tool**: JaCoCo Maven Plugin
- **Threshold**: 80% minimum coverage
- **Reports**: HTML, XML, CSV formats
- **Artifacts**: Uploaded as GitHub Actions artifacts
- **Integration**: Codecov integration for visualization

### 🔄 **Pipeline Triggers:**
- **Push to main/develop** → Full CI/CD pipeline
- **Pull requests** → Build, test, and quality checks
- **Tags (v*)** → Release creation and deployment
- **Manual** → Rollback and health checks
- **Scheduled** → Health monitoring and cleanup

### 🌍 **Environments:**
- **Staging** (`develop` branch) → Auto-deployment with health checks
- **Production** (`main` branch + releases) → Manual approval required
- **Local** → Docker Compose with full stack

### 🔐 **Security Features:**
- **Container scanning** with Trivy
- **Dependency checks** with OWASP
- **Code analysis** with CodeQL
- **SBOM generation** for compliance
- **Secret management** with GitHub Secrets

### 📈 **Monitoring:**
- **Prometheus**: Metrics collection
- **Grafana**: Visualization dashboards
- **Health Checks**: Application health monitoring
- **Performance Testing**: K6 load testing

---

## Architecture

This codebase follows **Hexagonal Architecture** (Ports & Adapters) with:

- Domain use cases isolated from REST and database concerns
- CDI events for post-commit integration calls
- OpenAPI-generated REST layer for the Warehouse API
- Hand-coded REST endpoints for Stores and Products

---

## Technologies

- **Java 17+**
- **Quarkus 3.13.3**
- **PostgreSQL** (via Docker or Quarkus Dev Services)
- **JUnit 5** + **Testcontainers** + **Mockito**
- **OpenAPI** (code generation for Warehouse API)

---

## Running the Code

```bash
# Compile and run tests
./mvnw clean test

# Run specific test class
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest

# Start development mode
./mvnw quarkus:dev

# Access Swagger UI
open http://localhost:8080/q/swagger-ui
```

### (Optional) Run in JVM mode

First compile:

```bash
./mvnw package
```

Start a PostgreSQL instance:

```bash
docker run -it --rm=true --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

Then run:

```bash
java -jar ./target/quarkus-app/quarkus-run.jar
```

Navigate to <http://localhost:8080/index.html>

---

**Good luck and have fun!** This is about demonstrating your understanding of production-grade patterns, not just writing code under pressure.
