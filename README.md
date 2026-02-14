# iSeries OpenTelemetry Bridge

A production-ready Spring Batch application designed to bridge legacy iSeries (AS/400) application logs to modern OpenTelemetry (OTel) observability platforms.

![Java 17](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3.2-green?style=flat-square)
![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-1.34-blue?style=flat-square)
![Build Status](https://github.com/wilsonwscui-coder/iseries-otel-bridge-doc/actions/workflows/maven.yml/badge.svg)

## 🚀 Overview

Many legacy systems (like IBM iSeries) output logs to local files in proprietary or unstructured formats. This bridge acts as a modern shipper that:

1.  **Tails** active log files in real-time.
2.  **Parses** complex log lines using **Grok** patterns.
3.  **Injects** Trace IDs and Span IDs from log content into OTel context.
4.  **Exports** structured logs to any OTLP-compliant backend (Jaeger, Grafana Tempo, Splunk, etc.).

## ✨ Key Features

*   **Reliability**: Persistent file tailing (handles rotation/restarts) and Spring Batch chunk processing.
*   **Multiline Support**: Automatically stitches Java stack traces or multi-line records into single OTel LogRecords.
*   **Trace Context Injection**: Extracts `trace_id` and `span_id` from logs to enable distributed tracing correlation.
*   **Metrics Extraction**: Can parse `key=value` metrics from logs and export them as structured attributes.
*   **Cloud Native**: Dockerized, stateless (state via file system), and Kubernetes-ready.

## 🛠️ Tech Stack

*   **Language**: Java 17 (Eclipse Temurin)
*   **Framework**: Spring Boot 3.2, Spring Batch, Spring Integration
*   **Observability**: OpenTelemetry Java SDK
*   **Parsing**: Java Grok (Elasticsearch-compatible patterns)
*   **Build**: Maven

## 🏃 Quick Start

### Prerequisites
*   Java 17+
*   Maven 3.8+
*   Docker (optional, for OTel Collector)

### Build
```bash
mvn clean package
```

### Run Locally
```bash
# 1. Start OTel Collector (Optional)
docker-compose up -d

# 2. Run the application
java -jar target/iseries-otel-bridge-1.0-SNAPSHOT.jar \
  --input.file.path.tail=/path/to/your/legacy.log \
  --otel.exporter.otlp.endpoint=http://localhost:4317
```

### Code Formatting
This project uses **Spotless** to enforce Google Java Style.
```bash
mvn spotless:apply
```

## ⚙️ Configuration

Configuration is handled via `application.properties` or Environment Variables (recommended for K8s).

| Env Variable | Default | Description |
|--------------|---------|-------------|
| `INPUT_FILE_PATH_TAIL` | *(empty)* | Absolute path to the log file to tail. |
| `PARSER_GROK_PATTERN` | `%{GREEDYDATA:message}` | Grok pattern to parse log lines. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | OTLP gRPC endpoint. |
| `OTEL_SERVICE_NAME` | `iseries-log-bridge` | Service name in OTel backend. |

## 📦 Deployment

### Docker
```bash
docker build -t iseries-otel-bridge .
docker run -v /var/log/app:/logs -e INPUT_FILE_PATH_TAIL=/logs/app.log iseries-otel-bridge
```

### Kubernetes (EKS)
See [DEPLOY_EKS.md](DEPLOY_EKS.md) for detailed instructions on deploying to AWS EKS.

## 🧪 Testing

The project includes a comprehensive test suite covering:
*   **Integration Tests**: End-to-end batch flow (`BatchIntegrationTest`).
*   **Parsing Logic**: verifying Grok patterns (`GrokItemProcessorTest`).
*   **Performance**: 1-hour load simulation (`LargeScaleSimulationTest`).

```bash
mvn test
```

## 🤝 Governance

This project adheres to strict architectural principles defined in the [Project Constitution](.specify/memory/constitution.md).
Key tenets: **Reliability**, **Observability First**, and **Configuration Over Code**.

---
*Maintained by the OpenCode Team*
