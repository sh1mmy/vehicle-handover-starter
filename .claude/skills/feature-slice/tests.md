# Required tests

Every case below is required. Names follow the vehicle tests exactly: substitute `<Entity>` and `<Unique>`.

Every `@Pattern` and `@Email` field is covered by a format test: either broken in `invalidBody()` (with its message asserted) or in its own `...HasInvalidFormat` test.

Skip a case only when its feature is absent (e.g. no unique field means no conflict cases), and list each skipped case in the summary.

## `<Entity>ControllerTest`
Setup:
- `@WebMvcTest(<Entity>Controller.class)`, `@Autowired MockMvc`, `@MockitoBean <Entity>Service`.
- A `VALID_BODY` text block with every field.
- `invalidBody()` that breaks exactly 3 fields.
- `response()` building one `<Entity>Response` with id 1.

| Test | Asserts |
|---|---|
| `findAllReturns<Entities>` | 200, `$.length()`, `$[0].id`, one `$[0].<field>` per type (an enum as its string, a date as `yyyy-MM-dd`) |
| `findByIdReturns<Entity>` | 200, one field |
| `findByIdReturnsNotFoundProblemWhen<Entity>DoesNotExist` | 404, `APPLICATION_PROBLEM_JSON`, `$.status`, `$.detail` = `"<Entity> with id 99 not found"` |
| `createReturnsCreated<Entity>` | 201, `Location` = `http://localhost/api/<entities>/1`, `$.id` |
| `createReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid` | 400, `$.detail` = `Validation failed`, `$.errors.length()` = 3, every `field` and `message` in sorted order, `verifyNoInteractions(service)` |
| `createReturnsEnglishValidationMessagesWhenAcceptLanguageIsPolish` | `Accept-Language: pl`, one blank field, message `must not be blank` |
| `createReturnsBadRequestProblemWithFieldErrorWhen<Enum>IsUnknown` (per enum) | 400, message `must be one of: A, B` in enum order |
| `createReturnsBadRequestProblemWithFieldErrorWhenValueHasWrongType` | a number field sent as a string, message `has an invalid value` |
| `createReturnsBadRequestProblemWithFieldErrorWhen<Field>HasInvalidFormat` (one per `@Pattern` / `@Email` field not broken in `invalidBody()`) | one invalid value replaced in `VALID_BODY`, `$.errors.length()` = 1, the field and its exact message, `verifyNoInteractions(service)` |
| `createReturnsBadRequestProblemWhenBodyIsNotJson` | `field` null, `request body must be valid JSON` |
| `createReturnsConflictProblemWhen<Unique>AlreadyExists` | 409, the exact conflict message |
| `createReturnsConflictProblemWhenDatabaseConstraintIsViolated` | service throws `DataIntegrityViolationException("uq_<table>_<column>")`, `detail` = `Request conflicts with existing data` |
| `updateReturnsUpdated<Entity>` | 200 |
| `updateReturnsNotFoundProblemWhen<Entity>DoesNotExist` | 404 |
| `updateReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid` | 400, fields in order, `verifyNoInteractions` |
| `updateReturnsConflictProblemWhen<Unique>AlreadyExists` | 409 |
| `deleteReturnsNoContent` | 204, `verify(service).delete(1L)` |
| `deleteReturnsNotFoundProblemWhen<Entity>DoesNotExist` | `doThrow(...)`, 404 |

Rules:
- Validation order is sorted by field name. Compute it from `invalidBody()`, do not guess.
- A date rule relative to today additionally needs:
  - `@Import(ClockConfig.class)` plus a `@TestConfiguration` with a `@Bean @Primary Clock` returning `Clock.fixed(...)`;
  - one test for 201 on the fixed day and one for 400 on the day after.

## `<Entity>ServiceTest`
Setup:
- `@ExtendWith(MockitoExtension.class)`, `@Mock <Entity>Repository`, `@InjectMocks <Entity>Service`.
- Helpers `<entity>(<unique>)` and `request(<unique>)`.
- Constants `<UNIQUE>` and `OTHER_<UNIQUE>`.

| Test | Asserts |
|---|---|
| `findAllReturnsMapped<Entities>` | stubs `findAll(Sort.by("id"))`, `extracting(...).containsExactly(...)` |
| `findByIdThrowsNotFoundWhen<Entity>DoesNotExist` | `hasMessage("<Entity> with id 99 not found")` |
| `createSaves<Entity>` | `save` answers its argument, one assert per field |
| `createThrowsConflictWhen<Unique>AlreadyExists` | exact message, `verify(repository, never()).save(any())` |
| `updateChangesExisting<Entity>` | the entity is mutated, `verify(repository).save(entity)` |
| `updateThrowsConflictWhen<Unique>BelongsToAnother<Entity>` | `existsBy<Unique>AndIdNot(OTHER, 1L)` true, `never().save` |
| `updateThrowsNotFoundWhen<Entity>DoesNotExist` | exact message |
| `deleteRemovesExisting<Entity>` | `verify(repository).delete(entity)` |
| `deleteThrowsNotFoundWhen<Entity>DoesNotExist` | exact message |

## `<Entity>RepositoryTest`
Setup:
- `@DataJpaTest`, `@Autowired <Entity>Repository`, `TestEntityManager`.
- Unique values that do not exist in any seed migration (check the seed files).

| Test | Asserts |
|---|---|
| `existsBy<Unique>AndIdNotFinds<Unique>OfAnother<Entity>` | save two, `flush`, true for the other id, false for its own id, false for an unknown value |
| `saveStores<Entity>ThatCanBeReadBack` | save with every field set, `flush`, `clear`, `findById`, `isNotSameAs`, one assert per field; a `BigDecimal` uses `isEqualByComparingTo` |

## `<file>-form.spec.ts`
Setup:
- Copy the `vehicle-form.spec.ts` setup: TestBed with `provideHttpClient()`, `provideHttpClientTesting()`, `provideRouter([])`.
- `afterEach(() => http.verify())`.
- Helpers `fill(type, index, value)`, `select(value)`, `fillValid<Entity>()`, `submit()`, `errors()`.
- A checkbox helper sets `checked` and dispatches `change`.

| Test (`it(...)`) | Asserts |
|---|---|
| `shows required errors and sends nothing when the form is empty` | `http.expectNone('/api/<folder>')`, every required message in field order |
| `leaves format and range rules to the backend` | invalid format values are sent; the backend 400 `errors` are shown |
| `creates the <label> and navigates to its details` | the POST body `toEqual` the full request (optional fields `null`), navigate to `['/<folder>', 7]` |
| `loads the <label> in edit mode and updates it` | `setInput('id', '3')`, GET, change one field, the PUT body `toEqual`, navigate |
| `shows a dedicated message when the <label> to edit does not exist` | 404 -> `'<Label> not found'` |
| `shows the ProblemDetail detail when loading the <label> fails` | 503 -> detail |
| `shows object-level errors returned by the backend above the form` | `field: null` |
| `shows field errors returned by the backend as ProblemDetail` | one field error |
| `shows the ProblemDetail detail when the backend reports a conflict` | 409 detail (only with a unique field) |

## Nested additions
Controller test:
- Every URL includes the parent: `/api/<parents>/1/<entities>`.
- Service methods are stubbed with the parent id first.
- `Location` = `http://localhost/api/<parents>/1/<entities>/1`.
- Add `findAllReturnsNotFoundProblemWhen<Parent>DoesNotExist` (404, `"<Parent> with id 99 not found"`).

Service test:
- `@Mock <Parent>Repository`.
- For list, get, create, update and delete:
  - `...ThrowsNotFoundWhen<Parent>DoesNotExist` (`existsById` false, `never().save`);
  - `...ThrowsNotFoundWhen<Entity>BelongsToAnother<Parent>` (`findByIdAnd<Parent>Id` empty).

Repository test:
- Save a parent through `<Parent>Repository` first. Do not rely on seeded parents.
- Add `findAllBy<Parent>IdReturnsOnlyChildrenOf<Parent>` (sorted by id) and `findByIdAnd<Parent>IdIgnoresChildOfAnother<Parent>`.

Form spec:
- `setInput('<parent>Id', '1')` before every test.
- URLs include the parent.

### `<Parent>DeleteWith<Entities>IntegrationTest` (required)
```java
@SpringBootTest
@AutoConfigureMockMvc
class <Parent>DeleteWith<Entities>IntegrationTest {
    // @Autowired MockMvc, <Parent>Repository, <Entity>Repository
    // Not @Transactional: the delete must really flush and commit for the FK to fire.

    @AfterEach
    void cleanUp() {
        // delete the children, then the parent created by the test
    }

    @Test
    void deleteReturnsConflictProblemWhen<Parent>Has<Entities>() throws Exception {
        // save a parent with values unique to this test, save one child for it
        // DELETE /api/<parents>/{id} -> 409, APPLICATION_PROBLEM_JSON,
        // $.detail "Request conflicts with existing data"; the parent still exists
    }
}
```
If it returns 500, apply the `flush()` change from [backend.md](backend.md) "Nested", and do not change the test.
