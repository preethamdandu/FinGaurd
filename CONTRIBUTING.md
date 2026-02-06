# Contributing to FinGaurd

Thank you for your interest in contributing to FinGaurd! This document provides guidelines and instructions for contributing to the project.

## 📋 Table of Contents

- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)

## 🚀 Getting Started

1. **Fork the repository**
```bash
# Fork on GitHub, then clone your fork
git clone git@github.com:YOUR_USERNAME/FinGaurd.git
cd FinGaurd
```

2. **Set up development environment**
```bash
# Follow instructions in SETUP.md
```

3. **Create a feature branch**
```bash
git checkout -b feature/your-feature-name
# or
git checkout -b bugfix/issue-number-description
```

## 🔄 Development Workflow

### Branch Naming Convention

- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Urgent production fixes
- `refactor/` - Code refactoring
- `docs/` - Documentation updates
- `test/` - Test additions or updates

**Examples:**
- `feature/user-authentication`
- `bugfix/123-fix-transaction-error`
- `docs/api-documentation`

### Working on Your Branch

1. **Keep your branch updated**
```bash
git checkout main
git pull origin main
git checkout your-branch
git merge main
```

2. **Make your changes**
- Write clean, readable code
- Follow project coding standards
- Add/update tests as needed
- Update documentation if required

3. **Test your changes**
```bash
# Java service
cd java-service
mvn test

# Python service
cd python-fraud-service
pytest
```

## 📝 Coding Standards

### Java (Spring Boot)

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Keep methods small and focused (< 50 lines)
- Add JavaDoc comments for public methods
- Use Lombok to reduce boilerplate

**Example:**
```java
/**
 * Creates a new transaction for the authenticated user
 *
 * @param request Transaction creation request
 * @param user Currently authenticated user
 * @return Created transaction details
 */
@PostMapping
public ResponseEntity<TransactionResponse> createTransaction(
    @Valid @RequestBody TransactionRequest request,
    @AuthenticationPrincipal UserDetails user
) {
    // Implementation
}
```

### Python (FastAPI)

- Follow [PEP 8](https://pep8.org/) style guide
- Use [Black](https://github.com/psf/black) for code formatting
- Use type hints for all function parameters and returns
- Add docstrings to all functions and classes
- Maximum line length: 100 characters

**Example:**
```python
async def analyze_transaction(
    request: TransactionAnalysisRequest
) -> TransactionAnalysisResponse:
    """
    Analyze a transaction for potential fraud.
    
    Args:
        request: Transaction data to analyze
        
    Returns:
        Fraud analysis result with risk score
        
    Raises:
        HTTPException: If analysis fails
    """
    # Implementation
```

### Code Formatting

**Java:**
```bash
# Format with IDE or Maven plugin
mvn formatter:format
```

**Python:**
```bash
# Format with Black
black app/

# Sort imports
isort app/

# Lint
flake8 app/
```

## 💬 Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples

```bash
feat(auth): add JWT token refresh mechanism

Implement token refresh endpoint to allow users to obtain new
access tokens without re-authenticating.

Closes #42
```

```bash
fix(transactions): correct balance calculation

Fixed bug where balance was not updated correctly after
deleting a transaction.

Fixes #87
```

```bash
docs(readme): update setup instructions for M1 Macs

Added specific instructions for Apple Silicon users regarding
Docker configuration.
```

## 🔀 Pull Request Process

1. **Update your branch**
```bash
git checkout main
git pull origin main
git checkout your-branch
git rebase main
```

2. **Push your changes**
```bash
git push origin your-branch
```

3. **Create Pull Request**
- Go to GitHub repository
- Click "New Pull Request"
- Select your branch
- Fill out PR template

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe testing performed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] All tests passing
- [ ] No new warnings
```

4. **Code Review**
- Address review comments
- Push additional commits if needed
- Maintain discussion in PR comments

5. **Merge**
- Squash and merge (preferred)
- Delete branch after merge

## 🧪 Testing

### Unit Tests

**Java:**
```java
@Test
void shouldCreateTransactionSuccessfully() {
    // Given
    TransactionRequest request = createValidRequest();
    
    // When
    TransactionResponse response = service.createTransaction(request);
    
    // Then
    assertNotNull(response.getId());
    assertEquals(request.getAmount(), response.getAmount());
}
```

**Python:**
```python
@pytest.mark.asyncio
async def test_analyze_transaction_success():
    # Given
    request = TransactionAnalysisRequest(
        transaction_id=uuid4(),
        amount=100.0,
        # ... other fields
    )
    
    # When
    response = await analyze_transaction(request)
    
    # Then
    assert response.risk_score >= 0.0
    assert response.risk_score <= 1.0
```

### Integration Tests

Test API endpoints end-to-end:

```bash
# Java
mvn verify

# Python
pytest tests/integration/
```

## 🐛 Reporting Bugs

### Before Reporting

1. Check existing issues
2. Verify it's reproducible
3. Collect relevant information

### Bug Report Template

```markdown
**Description**
Clear description of the bug

**To Reproduce**
Steps to reproduce:
1. Go to '...'
2. Click on '...'
3. See error

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Environment**
- OS: [e.g., macOS 13.0]
- Java Version: [e.g., 17]
- Python Version: [e.g., 3.11]

**Additional Context**
Screenshots, logs, etc.
```

## 💡 Feature Requests

### Feature Request Template

```markdown
**Problem**
What problem does this solve?

**Proposed Solution**
How should it work?

**Alternatives Considered**
Other approaches considered

**Additional Context**
Mockups, examples, etc.
```

## 📞 Getting Help

- **Documentation**: Check `docs/` directory
- **Issues**: Search existing issues
- **Discussions**: GitHub Discussions
- **Email**: [Your contact email]

## 📜 License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

Thank you for contributing to FinGaurd! 🎉

