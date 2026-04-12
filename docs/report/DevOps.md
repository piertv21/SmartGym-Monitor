# 6. DevOps and Development Process

This section describes the DevOps practices, workflows, and automation strategies adopted during the development of the SmartGym Monitor system.

## 6.1 Version Control Strategy

The project follows a **Git Flow** branching model to ensure a structured and controlled development process.

The main branches are:

- `main`: contains only stable and production-ready code.
- `develop`: integration branch where completed features are merged before release.
- `feature/<feature-name>`: used for the development of individual features.

Each new functionality is developed in a dedicated `feature` branch created from `develop`.

Each `feature` branch is in turn broken down into sub-branches `feature/<feature-name>-<sub-name>` that make up the feature in its entirety.

Once completed, the feature is merged back into `develop` through a Pull Request.

The `main` branch only receives code that has been validated and is ready for release.

This approach ensures:

- Isolation of new features
- Controlled integration
- Clear separation between stable and in-progress code

## 6.2 Branch Protection Rules

To preserve repository integrity and prevent accidental changes to production code, strict protection rules are applied to the `main` branch,
using GitHub rulesets.

The following restrictions are enforced:

- Direct pushes to `main` are prohibited.
- Code cannot be added or deleted directly on `main`.
- Every Pull Request targeting `main` requires at least one review before merging.
- All required CI checks must pass before merge approval (e.g., commit message validation).

This guarantees that production code is always reviewed, tested, and validated.

## 6.3 Conventional Commits and Branch Naming

The project adopts the **Conventional Commits specification** to standardize commit messages and improve traceability.
More info on Conventional Commits can be found [here](https://www.conventionalcommits.org/en/v1.0.0/).

Commit messages follow a structured format:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Examples:

- `feat(auth): add login endpoint`
- `fix(api): resolve session validation bug`
- `docs(readme): update setup instructions`

Branch naming also follows a consistent convention, the **Conventional Branch specification**.
More info on Conventional Branch Naming can be found [here](https://conventional-branch.github.io/).

Branch names are structured as follows:

```
<type>/<description>
```

Examples:

- `feature/<feature-name>`
- `fix/<bug-name>`
- `feature/docs`

### Automated Enforcement

To enforce these conventions:

- **Husky** is used to configure Git hooks locally.
  - A _commit-msg_ hook validates commit messages before they are accepted.
  - A _pre-commit_ hook formats code to ensure code style consistency.
  - A _pre-push_ hook runs tests to catch issues before code is pushed.
- A **GitHub Action** runs on each Pull Request to verify that all commits comply with the Conventional Commit standard.

This prevents inconsistent commit history and improves maintainability.

## 6.4 Continuous Integration (CI)

The project uses GitHub Actions to validate pull requests and keep the main branch stable.
The main CI workflow is `build-and-test.yml`, which performs the following checks:

- frontend tests with Python 3.12 and Poetry;
- backend microservice tests and integration validation with Gradle and Docker Compose;
- e2e test execution for the full backend stack;
- artifact collection for JUnit reports and Docker logs.

This workflow ensures that functional changes are verified before merging and that regressions are detected early.

## 6.5 Continuous Deployment and Documentation Publishing

The documentation site is built with VitePress and deployed through GitHub Pages.
The `deploy-report-site.yml` workflow is triggered when files under `docs/**` change on `main`.

Its pipeline is straightforward:

- checkout the repository;
- install Node.js dependencies in `docs/`;
- build the static documentation site;
- publish the generated artifact to GitHub Pages.

This keeps the project documentation aligned with the latest code and report updates.

## 6.6 Automated Release Process

The repository uses Semantic Release for automated versioning and release generation.
The release process is defined in `release.yml` together with `release.config.js`.

When a change reaches `main`, the workflow:

- checks out the repository with release permissions;
- installs Node.js dependencies;
- runs `semantic-release`;
- creates a new Git tag and release when the commit history contains a release-worthy change.

Release automation is supported by Conventional Commits and commit validation, so version increments remain predictable and traceable.

## 6.7 Automated Testing

Automated tests are integrated into both the backend and frontend development flow.
The repository includes:

- JUnit tests for the Java services;
- Cucumber-based e2e tests for the full backend stack;
- pytest tests for the Flask frontend.

The combination of unit, integration, and e2e tests provides fast feedback on individual components while also validating the complete system behavior.

## 6.8 Dependency Management with Renovate

The repository integrates **Renovate Bot** to automate dependency updates.

Renovate:

- monitors project dependencies;
- automatically creates pull requests when updates are available;
- can auto-merge safe minor and patch updates;
- keeps the dependency tree up to date with minimal manual effort.

Each update is reviewed and validated through the CI pipeline before being merged.

## 6.9 DevOps Benefits

The adopted DevOps strategy provides:

- structured collaboration;
- high code quality;
- automated validation;
- safe and controlled releases;
- continuous documentation updates;
- automated dependency management.

Overall, the combination of Git Flow, CI/CD automation, commit standardization, and dependency monitoring significantly increases the robustness and maintainability of the SmartGym Monitor system.
