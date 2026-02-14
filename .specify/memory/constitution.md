<!--
Sync Impact Report:
- Version: 1.0.0 (Verified)
- Templates requiring updates: ✅ plan-template.md, ✅ spec-template.md, ✅ tasks-template.md
- Follow-up: None
-->
# iSeries-Otel-Bridge Constitution

## Core Principles

### I. Reliability & Data Integrity
The bridge must never lose valid log data due to transient failures. It MUST implement robust error handling (e.g., skip bad lines with logging, retry export failures) and ensure at-least-once delivery semantics where possible. Persistent buffering (e.g., file tailing with position tracking) is preferred over in-memory only solutions.

### II. Observability First
The bridge is an observability tool and must itself be observable. It MUST expose internal metrics (log throughput, parsing error rates, export latency) and its own internal logs must be structured and distinct from the processed user logs.

### III. Configuration Over Code
Log patterns (Grok), file paths, and OTLP endpoints MUST be configurable via environment variables or external configuration files (Spring properties). Recompilation should not be required for standard operational changes like updating a log format or changing the collector endpoint.

### IV. Cloud-Native Deployment
The application MUST be packaged as a stateless container (Docker) and support orchestration (Kubernetes). It MUST handle SIGTERM for graceful shutdowns to flush buffers and ensure file handles are closed properly. Deployment manifests (Helm/Kustomize/YAML) are a required deliverable.

### V. OpenTelemetry Standards
All exported data MUST strictly adhere to OpenTelemetry semantic conventions. Trace context injection (extracting trace_id/span_id from logs) and structured logging (mapping log attributes to resource/log attributes) are first-class citizens and MUST be implemented where log formats allow.

## Technology Standards

- **Language:** Java 17+ (LTS)
- **Framework:** Spring Boot 3.x & Spring Batch
- **Build System:** Maven
- **Testing:** JUnit 5, Mockito, & Spring Boot Test (Coverage > 80% target)
- **Containerization:** Docker (Multi-stage builds required)
- **CI/CD:** GitHub Actions (implied)

## Development Workflow

- **Testing:** All changes MUST be verified with `mvn test`. Integration tests in `src/test/java/com/iseries/otel/bridge/` are mandatory for new parsing logic.
- **Documentation:** `README.md` and `DEPLOY_EKS.md` MUST be kept in sync with code changes.
- **Commits:** Follow conventional commit messages (feat:, fix:, docs:, chore:).
- **Code Style:** Follow Google Java Style (enforced via Checkstyle/Spotless if added).

## Governance

This constitution defines the architectural constraints and non-negotiable rules for the iSeries-Otel-Bridge project. Amendments require a Pull Request with explicit justification and approval from core maintainers. Runtime guidance for agents can be found in `AGENTS.md`.

**Version**: 1.0.0 | **Ratified**: 2026-02-15 | **Last Amended**: 2026-02-15
