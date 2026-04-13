# 6. DevOps and Development Process

This chapter describes the DevOps practices, workflows, and automation strategy adopted in SmartGym Monitor.
The tools and processes implemented in the repository aim to ensure a structured development process, high code quality, and efficient release management.

## 6.1 Version Control Strategy

The project follows a **Git Flow** branching model to ensure a structured and controlled development process.

The main branches are:

- `main`: contains only stable and production-ready code.
- `develop`: integration branch where completed features are merged before release.
- `feature/<feature-name>`: used for the development of individual features.

Each new functionality is developed in a dedicated `feature` branch created from `develop`.
Once completed, the feature is merged back into `develop` through a Pull Request.

The `main` branch only receives code that has been validated and is ready for release.

This approach ensures:

- Isolation of new features.
- Controlled integration.
- Clear separation between stable and in-progress code.

## 6.2 Branch Protection Rules

To preserve repository integrity and prevent accidental changes to production code, strict protection rules are applied to the `main` branch using **GitHub rulesets**.

The following restrictions are enforced:

- Direct pushes to `main` are prohibited.
- Code cannot be added or deleted directly on `main`.
- Every Pull Request targeting `main` requires at least one review before merging.
- All required CI checks must pass before merge approval.

This guarantees that production code is always reviewed, tested, and validated.

## 6.3 Conventional Commits and Branch Naming

The project adopts the **Conventional Commits specification** to standardize commit messages and improve traceability.
More information available [here](https://www.conventionalcommits.org/en/v1.0.0/).

Commit messages follow a structured format:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

<p align="center"><em>Listing 6.1: Conventional Commits message format</em></p>

Examples:

- `feat(auth): add login endpoint`
- `fix(api): resolve session validation bug`
- `docs(readme): update setup instructions`

Branch naming also follows a consistent convention, the **Conventional Branch specification**.
More information available [here](https://conventional-branch.github.io/).

Branch names are structured as follows:

```
<type>/<description>
```

<p align="center"><em>Listing 6.2: Branch naming format</em></p>

Examples:

- `feature/<feature-name>`
- `feature/docs`

### 6.3.1 Automated Enforcement

To enforce these conventions, **Git hooks** were implemented using [Husky](https://typicode.github.io/husky/).

These hooks run locally, upon commit and push actions:

- _commit-msg_ validates commit messages before acceptance, using [commitlint](https://commitlint.js.org/) with the Conventional Commits configuration.
- _pre-commit_ formats staged code to keep style consistency, using:
  - [Prettier](https://prettier.io/) for docs/web files
  - [Spotless](https://github.com/diffplug/spotless) for Java
  - [Ruff formatter](https://docs.astral.sh/ruff/formatter/) for Python
  - [gofumpt](https://github.com/mvdan/gofumpt) for Go
- _pre-push_ runs tests before push.

This prevents inconsistent commit history and improves maintainability.

## 6.4 GitHub Actions Workflows

**GitHub Actions** orchestrates the automation pipeline of the project through four workflows: CI validation, commit validation, documentation deployment, and release automation. The following subsections describe each workflow and its role in the overall quality gate.

### 6.4.1 Continuous Integration Workflow (`build-and-test.yml`)

The CI workflow runs on Pull Requests targeting `main` and can also be launched manually (`workflow_dispatch`).

It thoroughly tests the backend by running JUnit, integration, and end-to-end tests, and test the frontend in parallel.

The workflow also uploads execution artifacts to support debugging:

- JUnit XML reports.
- e2e HTML report.
- Docker Compose logs.

This structure ensures that every change is validated against the existing test suite, preventing regressions and maintaining code quality before merging into `main`.

### 6.4.2 Commit Validation Workflow (`validate-commits.yml`)

The commit validation workflow runs on Pull Requests to `main`.

This workflow guarantees that release automation receives a clean and machine-readable commit history, which is required for semantic versioning decisions.

### 6.4.3 Documentation Deployment Workflow (`deploy-report-site.yml`)

The documentation deployment workflow runs when:

- a push reaches `main`, and
- at least one file under `docs/**` changed.

It's used to automatically deploy the latest version of the documentation site, based on [VitePress](https://vitepress.dev/), to GitHub Pages, ensuring that the public report is always up to date with the latest approved changes in the `main` branch.

### 6.4.4 Automated Release Workflow (`release.yml`)

The release workflow is triggered automatically on every push to `main`.
It executes `semantic-release`, which analyzes commit messages to determine the next version number, generate the changelog, and publish the release.
The adopted release model follows the [SemVer](https://semver.org/), using [semantic-release](https://semantic-release.gitbook.io/semantic-release).

In this model, for service-scoped release streams, each service release is expected to produce:

- A Git tag with the version number (for example `2.0.0`).
- A GitHub Release with an auto-generated changelog, published on the repository Releases page.

In the current repository workflow, release detection is based on SemVer-like tags (for example `v2.0.0`), and release notes are reflected in `CHANGELOG.md` for traceability directly in the codebase.

## 6.5 Automated Testing Strategy

Automated tests are integrated into the CI process with multiple layers:

- Unit and integration tests for backend services (JUnit).
- End-to-end tests in the backend `e2e` module (Cucumber-based).
- Frontend tests with `pytest`.

This layered strategy reduces regression risk and improves confidence in cross-service behavior.

## 6.6 Dependency Management with Renovate

The repository uses [Renovate](https://docs.renovatebot.com/) to keep dependencies up to date.
Renovate periodically scans the repository and automatically opens pull requests whenever newer dependency versions are available, across package ecosystems used in the repo.

Each update is reviewed and validated through the CI pipeline before being merged.

## 6.7 Observability with Prometheus and Grafana

To monitor runtime behavior and detect regressions early, the project includes an observability stack integrated in `docker-compose.yml`.

The telemetry flow is:

- Spring Boot services expose metrics via Actuator at `/actuator/prometheus`.
- Prometheus scrapes those endpoints according to `prometheus.yml` (5s scrape interval).
- Grafana reads Prometheus as default datasource and renders the pre-provisioned dashboards.

### 6.7.1 Prometheus Targets

`prometheus.yml` registers a dedicated job for each backend microservice:

- `gateway`
- `service-discovery`
- `auth-service`
- `analytics-service`
- `area-service`
- `machine-service`
- `tracking-service`
- `embedded-service`

All jobs scrape `/actuator/prometheus` and run inside the Docker network (`smartgym-net`) using internal service hostnames.

### 6.7.2 Grafana Provisioning and Dashboard

Grafana is preconfigured at startup through:

- Datasource file: `grafana/provisioning/datasources/prometheus.yml`
  - Datasource name: `Prometheus`
  - URL: `http://prometheus:9090`
  - `isDefault: true`
- Dashboard provider file: `grafana/provisioning/dashboards/dashboards.yml`
  - Loads dashboards from `/var/lib/grafana/dashboards`

The default dashboard is `grafana/dashboards/smartgym-overview.json` with:

- UID: `smartgym-overview`
- Title: `SmartGym Monitor`
- Key panels for throughput, p95 response time, JVM memory/threads, `up` status, 5xx error rate, and system CPU usage.

## 6.8 DevOps Benefits

The adopted DevOps strategy provides:

- Structured collaboration.
- High code quality.
- Automated validation.
- Safe and controlled releases.
- Continuous documentation updates.
- Automated dependency management.

Overall, the combination of Git Flow, GitHub Actions automation, commit standardization, and dependency monitoring significantly increases the robustness and maintainability of SmartGym Monitor.
