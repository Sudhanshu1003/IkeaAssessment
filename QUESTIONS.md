# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
OpenAPI YAML (Code Generation) Approach:

PROS:
- API-First Development: Forces clear API design before implementation
- Automatic Code Generation: Reduces boilerplate code and human error
- Consistency: Ensures uniform API structure across endpoints
- Documentation: Auto-generated interactive documentation (Swagger UI)
- Client SDK Generation: Can generate client libraries in multiple languages
- Contract Testing: Enables consumer-driven contract testing
- Version Management: Easier to track API changes and maintain backward compatibility

CONS:
- Learning Curve: Requires understanding OpenAPI specification
- Generation Constraints: Limited by code generator capabilities
- Maintenance Overhead: Need to keep YAML and generated code in sync
- Debugging Complexity: Harder to debug generated code issues
- Customization Limits: Complex business logic may require manual overrides
- Build Time: Adds generation step to build process

Hand-Coded Endpoints Approach:

PROS:
- Full Control: Complete flexibility in implementation
- Simpler Debugging: Direct access to code for troubleshooting
- No Generation Dependencies: No need for additional tools/setup
- Business Logic Integration: Easier to implement complex domain logic
- Performance Optimization: Can fine-tune implementation details
- Faster Iteration: Quick changes without regeneration steps

CONS:
- Inconsistency Risk: Different developers may implement patterns differently
- Documentation Burden: Manual documentation maintenance required
- Boilerplate Code: More repetitive code to write and maintain
- Error-Prone: Higher chance of human errors in endpoint definitions
- Contract Drift: API may diverge from intended specification
- Testing Overhead: Need to write more comprehensive tests

My Choice: OpenAPI YAML with Hybrid Approach

I would choose the OpenAPI YAML approach for the following reasons:

1. **API Design Discipline**: Forces thoughtful API design before implementation
2. **Documentation Benefits**: Auto-generated, always-up-to-date API documentation
3. **Consistency**: Ensures uniform patterns across all endpoints
4. **Future-Proofing**: Easier to generate client SDKs and support multiple languages
5. **Team Collaboration**: Clear contract between frontend and backend teams

However, I would use a hybrid approach where:
- Core CRUD operations use generated code
- Complex business logic endpoints are hand-coded but follow OpenAPI contracts
- Use OpenAPI validators to ensure hand-coded endpoints comply with specifications
- Leverage code generation as a starting point, then customize as needed
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Testing Priority Pyramid (Time/Resource Constraints):

1. **CRITICAL PATH INTEGRATION TESTS** (Highest Priority)
   - Focus on end-to-end user journeys
   - Test warehouse CRUD operations through REST API
   - Test business logic validations (capacity limits, location validation)
   - Test transaction boundaries and error handling
   - Reason: These tests catch real integration issues that unit tests miss

2. **UNIT TESTS FOR DOMAIN LOGIC** (High Priority)
   - Test use cases: CreateWarehouseUseCase, ReplaceWarehouseUseCase, ArchiveWarehouseUseCase
   - Test business rules and validations
   - Test edge cases and error conditions
   - Use @InjectMock for dependencies to isolate logic
   - Reason: Fast feedback on business logic changes

3. **REPOSITORY LAYER TESTS** (Medium Priority)
   - Test database operations with real H2 database
   - Test search and filtering functionality
   - Test transaction management
   - Reason: Ensures data access layer works correctly

4. **PARAMETERIZED TESTS** (Medium Priority)
   - Test multiple input combinations for validation rules
   - Test boundary conditions (min/max values, null/empty inputs)
   - Test different sorting and filtering scenarios
   - Reason: Efficiently covers many test cases

5. **CONCURRENCY TESTS** (Lower Priority)
   - Test concurrent operations on same data
   - Test optimistic locking scenarios
   - Reason: Important but time-consuming; focus on critical paths first

Coverage Strategy (JaCoCo ≥80%):

1. **Critical Path Coverage First**
   - Identify and instrument core business flows
   - Ensure ≥90% coverage on use cases and repository methods
   - Focus on warehouse domain logic as primary business value

2. **Coverage Gates in CI/CD**
   - Set minimum coverage thresholds (80% overall, 90% for critical classes)
   - Fail builds if coverage drops below thresholds
   - Use coverage reports to identify untested code paths

3. **Coverage Monitoring Over Time**
   - Track coverage trends in each PR/commit
   - Add coverage requirements for new features
   - Refactor low-coverage code or add missing tests

4. **Smart Coverage Targets**
   - Higher coverage for business logic (≥90%)
   - Moderate coverage for infrastructure code (≥70%)
   - Accept lower coverage for generated code (≥50%)

5. **Coverage Quality Over Quantity**
   - Focus on meaningful tests, not just coverage numbers
   - Use mutation testing to ensure test quality
   - Review coverage reports for untested edge cases

Implementation Strategy:

1. **Start with Integration Tests**
   - Write tests for warehouse API endpoints
   - Include positive, negative, and edge cases
   - Use @QuarkusTest with real database

2. **Add Unit Tests for Complex Logic**
   - Test validation rules in use cases
   - Test error handling and edge cases
   - Use parameterized tests for multiple scenarios

3. **Maintain Coverage**
   - Run coverage reports in CI pipeline
   - Review coverage changes in code reviews
   - Add tests for new features immediately

4. **Balance Speed and Coverage**
   - Use fast unit tests for quick feedback
   - Run integration tests less frequently but thoroughly
   - Use test categorization (unit, integration, slow)

This approach ensures critical business functionality is well-tested while maintaining reasonable development velocity and meeting coverage requirements.
```
