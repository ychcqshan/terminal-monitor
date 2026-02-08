# AGENTS.md

This file contains guidelines for agentic coding agents working on the Terminal Security Monitoring System.

## Project Overview

This is a lightweight terminal security monitoring solution with three main components:
1. **Agent** (Python 3.8+): Collects system information (processes, ports) and reports to backend
2. **Backend** (Java SpringBoot 3.2): API server for data processing and MySQL storage
3. **Web** (Vue 3 + Element Plus): Management interface for monitoring agents

## Build/Lint/Test Commands

### Python Agent
```bash
# Install dependencies
pip install -r requirements.txt

# Run tests
python -m pytest tests/ -v

# Run single test
python -m pytest tests/test_collectors.py::test_process_collect -v

# Lint code
flake8 agent/ --max-line-length=88
black agent/ --check
mypy agent/

# Run agent
python main.py --server-url http://localhost:8080
python main.py config.json
```

### Java Backend
```bash
# Build project
./mvnw clean compile

# Run tests
./mvnw test

# Run single test
./mvnw test -Dtest=AgentControllerTest

# Package application
./mvnw clean package -DskipTests

# Run backend
java -jar target/terminal-monitor-backend-1.0.0.jar

# Run with Spring Boot
./mvnw spring-boot:run
```

### Vue3 Frontend
```bash
# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Run lint
npm run lint
npm run lint:fix
```

### Docker Deployment
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## Code Style Guidelines

### Python (Agent)
- **Imports**: Use `isort`, stdlib first, then third-party, then local imports
- **Formatting**: Use `black` with line length 88, `flake8` for linting
- **Type Hints**: Required for all functions, use `mypy` for type checking
- **Naming**:
  - Functions/variables: `snake_case`
  - Classes: `PascalCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Error Handling**: Use specific exceptions, avoid bare except, implement logging
- **Documentation**: Use docstrings for all public functions and classes

### Java (Backend)
- **Formatting**: Manual getter/setter (no Lombok, Java 25 compatibility)
- **Naming**:
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Annotations**: Use `@RestController`, `@Service`, `@Repository` appropriately
- **Error Handling**: Use `@ControllerAdvice` for global exception handling
- **Database**: Use `saveAndFlush()` for immediate persistence (not `save()`)
- **Documentation**: Use JavaDoc for public APIs

### Vue 3 (Frontend)
- **Component Structure**: Use Composition API with `<script setup>` syntax
- **Naming**:
  - Components: `PascalCase`
  - Files: `PascalCase.vue`
  - Variables/functions: `camelCase`
- **State Management**: Use Pinia stores
- **API Calls**: Use Axios with error handling and loading states
- **Styling**: Use scoped CSS with Element Plus theme variables

## Development Workflow

1. **Before coding**:
   - Read existing code patterns in the component/module
   - Check for similar implementations
   - Understand the data flow and API contracts

2. **During development**:
   - Follow existing code style and patterns
   - Write tests alongside code
   - Use meaningful variable and function names
   - Add proper error handling and logging

3. **Before committing**:
   - Run linting commands for the relevant module
   - Ensure tests pass
   - Build the project to check for errors

## API Design

- **RESTful conventions** for all endpoints
- **Field naming**: Use camelCase in JSON (`agentId`, not `agent_id`)
- **Response format**: Consistent structure with `success`, `message`, `data` fields
- **Error handling**: Return appropriate HTTP status codes (200, 400, 404, 500)
- **CORS**: Enable `@CrossOrigin(origins = "*")` for development

## Database Guidelines

- **Agent**: SQLite for local caching (`storage/db.py`)
- **Backend**: MySQL with JPA/Hibernate
- **Naming**: Use `snake_case` for table and column names
- **Entities**: Manual getter/setter methods (no Lombok)
- **Transactions**: Use `@Transactional` appropriately
- **Persistence**: Use `saveAndFlush()` for immediate write

## Testing Guidelines

### Unit Tests
- Test all public methods and edge cases
- Mock external dependencies (database, HTTP calls)
- Aim for high code coverage (>80%)

### Integration Tests
- Test API endpoints with real database
- Test Agent-to-Backend communication

### API Testing
```bash
# Use test-api.sh for API testing
./test-api.sh

# Manual testing
curl -X POST http://localhost:8080/api/agents/register
curl -X POST http://localhost:8080/api/agents/{id}/heartbeat
curl -X POST http://localhost:8080/api/agents/{id}/data
curl http://localhost:8080/api/agents
```

## Security Guidelines

- Never log sensitive information (passwords, tokens, keys)
- Validate all input data on both client and server sides
- Use HTTPS for production API communications
- Follow OWASP security guidelines

## Git Workflow

- Use descriptive commit messages
- Create feature branches for new development
- Keep commits small and focused
- Include tests in the same commit when possible

## Common Issues

1. **Java 25 + Lombok**: Use manual getter/setter instead of Lombok annotations
2. **Database persistence**: Use `saveAndFlush()` instead of `save()` for immediate writes
3. **JSON field naming**: Backend expects camelCase (`agentId`), Python uses snake_case (`agent_id`)
4. **CORS errors**: Ensure `@CrossOrigin(origins = "*")` is configured
