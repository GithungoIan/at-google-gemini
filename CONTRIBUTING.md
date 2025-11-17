# Contributing to AT-Google-Gemini

Thank you for your interest in contributing to AT-Google-Gemini! This document provides guidelines and instructions for contributing.

## Getting Started

1. **Fork the repository**
2. **Clone your fork**:
   ```bash
   git clone https://github.com/your-username/at-google-gemini.git
   cd at-google-gemini
   ```
3. **Set up your development environment**:
   ```bash
   ./scripts/setup-local.sh
   ```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

Use prefixes:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation updates
- `refactor/` - Code refactoring
- `test/` - Test additions or changes

### 2. Make Your Changes

- Write clean, readable code
- Follow  best practices
- Add tests for new functionality
- Update documentation as needed

### 3. Run Tests

```bash
./scripts/run-tests.sh
```

Ensure all tests pass before submitting.

### 4. Format Your Code

```bash
sbt scalafmt
```

### 5. Commit Your Changes

Use clear, descriptive commit messages:

```bash
git commit -m "feat: Add SMS message splitting functionality"
git commit -m "fix: Handle empty Gemini responses"
git commit -m "docs: Update deployment instructions"
```

Follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation
- `style:` - Code style changes
- `refactor:` - Code refactoring
- `test:` - Test updates
- `chore:` - Maintenance tasks

### 6. Push and Create a Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.
