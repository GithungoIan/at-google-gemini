# Contributing to VoiceAI

Thank you for your interest in contributing to VoiceAI! This document provides guidelines and instructions for contributing.

## Getting Started

1. **Fork the repository**
2. **Clone your fork**:
   ```bash
   git clone https://github.com/your-username/at-google-gemini-scala.git
   cd at-google-gemini-scala
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
- Follow Scala best practices
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

## Code Style Guidelines

### Scala Style

- Use 2 spaces for indentation
- Maximum line length: 100 characters
- Use meaningful variable names
- Add comments for complex logic
- Follow functional programming principles

Example:
```scala
// Good
def processMessage(text: String): Future[String] = {
  val sanitized = sanitizeInput(text)
  geminiService.generateResponse(sanitized)
}

// Avoid
def pm(t: String): Future[String] = {
  geminiService.generateResponse(t)
}
```

### Testing

- Write unit tests for all new functionality
- Use descriptive test names
- Test edge cases and error conditions

Example:
```scala
"SMSUtils" should {
  "split long messages correctly" in {
    val longText = "a" * 200
    val parts = SMSUtils.splitMessage(longText, 160)

    parts.length shouldBe 2
    parts.foreach(_.length should be <= 160)
  }
}
```

## Project Structure

```
src/
├── main/
│   ├── scala/
│   │   └── com/githungo/voiceai/
│   │       ├── actors/      # Actor implementations
│   │       ├── api/         # HTTP routes
│   │       ├── domains/     # Domain models
│   │       ├── services/    # External service clients
│   │       ├── utils/       # Utility functions
│   │       ├── metrics/     # Prometheus metrics
│   │       └── logging/     # Structured logging
│   └── resources/
│       ├── application.conf # Configuration
│       └── logback.xml      # Logging config
└── test/
    └── scala/               # Test files
```

## Pull Request Guidelines

### Before Submitting

- [ ] All tests pass
- [ ] Code is formatted with `scalafmt`
- [ ] Documentation is updated
- [ ] Commit messages are clear
- [ ] No merge conflicts

### PR Description

Include:
1. **What** - What changes does this PR introduce?
2. **Why** - Why are these changes needed?
3. **How** - How were the changes implemented?
4. **Testing** - How was this tested?

Example:
```markdown
## What
Adds SMS message splitting functionality to handle messages longer than 160 characters.

## Why
Users were unable to receive long AI responses via SMS. This ensures all responses are delivered properly.

## How
- Created `SMSUtils.splitMessage()` to split text by words
- Updated `SMSConversationActor` to use the utility
- Added numbering for multi-part messages (1/3, 2/3, etc.)

## Testing
- Added unit tests for various message lengths
- Tested with messages up to 500 characters
- Verified multi-part numbering works correctly
```

## Reporting Issues

### Bug Reports

Include:
- Clear description of the bug
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, Java version, etc.)
- Relevant logs or error messages

### Feature Requests

Include:
- Clear description of the feature
- Use case and benefits
- Proposed implementation (if any)
- Examples or mockups

## Community Guidelines

- Be respectful and inclusive
- Help others learn and grow
- Provide constructive feedback
- Give credit where it's due
- Follow the [Code of Conduct](CODE_OF_CONDUCT.md)

## Questions?

- Open an issue for discussion
- Join our community chat
- Check the [documentation](README.md)

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

Thank you for contributing to VoiceAI! 🚀
