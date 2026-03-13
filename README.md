# Step

[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/exense/step-distribution)](https://github.com/exense/step-distribution/releases)

**Step** is an open-source automation platform that unifies software automation across the entire DevOps lifecycle — from functional and load testing to RPA and monitoring — on a single, centralized orchestration layer.

## What is Step?

Step focuses on the **reuse of automation artifacts** across teams, tools, and automation use cases. Instead of maintaining separate codebases for tests, load scenarios, and monitoring probes, Step lets you write automation once and run it in any context:

![Illustration of Step's reusability](https://step.dev/knowledgebase/images/step-reusability.svg)

Step integrates with existing automation tools and DevOps platforms rather than replacing them:

![Illustration of Step's integrations](https://step.dev/knowledgebase/images/step-integration.svg)

For a full introduction see the [Step knowledgebase](https://step.dev/knowledgebase).

## Getting Started

The quickest path is to [get a free SaaS instance](https://portal.stepcloud.ch) and follow one of the **[getting started tutorials](https://step.dev/knowledgebase/all-resources?tab=tutorials-tab)**.

To run Step on your own infrastructure, follow the [quick setup guide](https://step.dev/knowledgebase/setup/installation/binaries/quick-setup) or download the latest release from [step-distribution/releases](https://github.com/exense/step-distribution/releases).

## Architecture

Step is a distributed platform built around a **controller – agent** model:

```
┌──────────────┐   REST / WebSocket   ┌─────────────────────┐
│   Browser /  │ ───────────────────► │     Controller      │
│   Step CLI   │                      │  (orchestration,    │
└──────────────┘                      │   plans, reports)   │
                                      └──────────┬──────────┘
                                                 │  step-grid protocol
                                      ┌──────────▼──────────┐
                                      │   Agent Pool        │
                                      │  (keyword execution)│
                                      └─────────────────────┘
```

- The **Controller** stores and executes automation plans, schedules runs, manages resources, and serves the web UI.
- **Agents** are lightweight worker processes that execute Keywords on remote machines and report results back in real time.
- **Keywords** are the atomic units of automation — Java methods annotated with `@Keyword` that the engine discovers, routes, and calls remotely.

See [Architecture overview](https://step.dev/knowledgebase/concepts/architecture) in the knowledgebase for a deeper dive.

## Key Concepts

| Concept | Description                                                                                                                   | Learn more |
|---------|-------------------------------------------------------------------------------------------------------------------------------|------------|
| **Keyword** | An executable unit of automation implemented in Java, .NET, JavaScript, or via a tool plugin (Cypress, K6, JMeter, SoapUI, …) | [Keywords](https://step.dev/knowledgebase/userdocs/keywords/) |
| **Plan** | A test or automation scenario composed of Keywords and control-flow artefacts                                                 | [Plans](https://step.dev/knowledgebase/userdocs/plans/) |
| **Automation Package** | An executable and deployable unit (Archive + descriptor) that bundles Step entities (Keywords, Plans,...)                     | [Automation Packages](https://step.dev/knowledgebase/devops/automation-packages-overview) |
| **Agent / Token** | A worker process exposing a pool of execution slots (tokens) to the controller                                                | [Agents](https://step.dev/knowledgebase/setup/installation/binaries/agentinstall/) |
| **Scheduler** | Triggers plan executions on a cron schedule                                                             | [Scheduler](https://step.dev/knowledgebase/userdocs/executions/#schedule) |

## Modules

This repository contains the Step backend, organized as a Maven multi-module build:

| Module | Description |
|--------|-------------|
| `step-commons` | Shared utilities used across the platform |
| `step-constants` | Platform-wide constants and enumerations |
| `step-core-model` | Core domain model: plans, artefacts, functions, resources |
| `step-core` | Execution engine: artefact lifecycle, expression evaluation, parameter resolution, security |
| `step-plans` | Plan parsing, YAML support, and control-flow artefacts (loops, conditionals, sequences) |
| `step-automation-packages` | Automation package management with YAML, JUnit, and JUnit5 support |
| `step-functions` | Keyword execution layer: handlers, routing, and package management |
| `step-functions-plugins` | Plugin extensions for the function execution layer |
| `step-agent` | Agent process that executes Keywords on worker nodes |
| `step-controller` | Central orchestration server: REST API, scheduling, reporting, multi-tenancy |
| `step-controller-plugins` | Controller plugin infrastructure |
| `step-repositories` | Repository abstraction for automation artifact storage and retrieval |
| `step-json-schema` | JSON schema support for Step entity validation |
| `step-livereporting` | Real-time execution monitoring and streaming |
| `step-maven-plugin` | Maven plugin for CI/CD integration |
| `step-cli` | Command-line interface for triggering and managing executions |
| `step-libs-maven-client` | Maven client for dynamic dependency resolution at runtime |
| `step-ide` | IDE integration utilities |

## Step Ecosystem

This repository is the core backend of a broader set of open-source repositories:

| Repository | Description |
|------------|-------------|
| [step-api](https://github.com/exense/step-api) | The Java API for writing Keywords (`@Keyword`, `AbstractKeyword`) — the only dependency needed to build keyword libraries |
| [step-grid](https://github.com/exense/step-grid) | The distributed execution grid: agent lifecycle, token management, and controller–agent communication |
| [step-framework](https://github.com/exense/step-framework) | Reusable infrastructure components: persistence, REST server, collections, and time series |
| [step-frontend](https://github.com/exense/step-frontend) | The Angular-based web UI |
| [step-distribution](https://github.com/exense/step-distribution/releases) | Release bundles and Docker images |

## Developer Setup

### Git hooks

After cloning, configure the included git hooks (requires git ≥ 2.9):

```bash
git config core.hooksPath git-hooks
```

### Build

```bash
git clone https://github.com/exense/step.git
cd step
mvn clean install
```

To skip tests:

```bash
mvn clean install -DskipTests
```

### Requirements

- Java 11 or later
- Maven 3.6 or later
- MongoDB or PostgreSQL (for persistence — see [installation guide](https://step.dev/knowledgebase/setup/installation/binaries/quick-setup))

## Important Links

| Resource | URL |
|----------|-----|
| Documentation | [step.dev/knowledgebase](https://step.dev/knowledgebase) |
| Official website | [step.dev](https://step.dev) |
| SaaS portal | [portal.stepcloud.ch](https://portal.stepcloud.ch) |
| Releases | [step-distribution/releases](https://github.com/exense/step-distribution/releases) |

## Contributing

Contributions are welcome. Please open an issue to discuss a bug or feature request before submitting a pull request. All submissions are expected to include appropriate test coverage.

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).
