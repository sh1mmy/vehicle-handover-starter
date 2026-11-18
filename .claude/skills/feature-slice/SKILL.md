---
name: feature-slice
description: Generates a complete feature slice for a new entity in this repository - Flyway migration, Spring Boot CRUD API, Angular list, detail and form screens, and tests - mirroring the vehicle module 1:1. Use when adding a new entity, a CRUD module or a feature slice, including a child resource nested under an existing parent resource.
argument-hint: "Entity description: name, fields with types and rules, unique fields, optional parent. Example: Supplier: name text required, email unique, rating 1-5 optional"
---

# Feature slice

Entity description: $ARGUMENTS

This skill runs without a human. Never ask questions. When the description is incomplete, take the
simplest assumption consistent with the vehicle module, write it down, and list it in the final summary.

## 1. Read the reference and mirror it 1:1
Read all of these before writing anything:
- `CLAUDE.md`
- `CLAUDE.local.md`, only if it exists (it is local and usually absent)
- `backend/src/main/java/com/example/dealership/vehicle/`, every file
- `backend/src/main/java/com/example/dealership/error/GlobalExceptionHandler.java`
- `backend/src/main/resources/db/migration/`, every file; note the highest `V<n>`
- `backend/src/test/java/com/example/dealership/vehicle/`, every file
- `frontend/src/app/vehicles/`, every file
- `frontend/src/app/shared/`, `frontend/src/app/app.routes.ts`, `frontend/src/app/app.html`

Copy structure, naming, annotation order, member order, test names and test helpers.
Differ only where the entity differs. Do not "improve" the pattern.

## 2. Build the entity spec
Derive names (example `ServiceVisit`):

| Name | Rule | Example |
|---|---|---|
| Java package | lowercase singular, no separators | `servicevisit` |
| Class prefix | PascalCase singular | `ServiceVisit` |
| Table | snake_case singular | `service_visit` |
| URL | `/api/` + kebab-case plural | `/api/service-visits` |
| Frontend folder | kebab-case plural | `frontend/src/app/service-visits/` |
| Frontend files | kebab-case singular prefix | `service-visit-list.ts` |

Then write a field table with these columns: name (camelCase), type, required, constraints, unique, UI label.

Defaults when the description is silent:
- Field type is `String`, and String length is 100.
- Every field is required, unless the description says optional.
- A field is unique only when the description says so.
- Enum labels are the value in sentence case (`IN_PROGRESS` -> `In progress`).
- Operations are full CRUD: list, get, create, update, delete.
- Operation subsets: when the description lists operations (e.g. `operations: list, create`), generate only those.
  An omitted operation has no endpoint, service method, API method, UI and tests:

  | Omitted | Remove |
  |---|---|
  | list | `findAll` in controller and service; `list()` in the API; the list component and the `''` route; the nav link; list tests |
  | get | `GET /{id}` and `findById`; `get()` in the API; the detail component and the `':id'` route; links to the detail; get tests |
  | create | `POST`, `create` and `<Entity>Mapper.toEntity`; `create()` in the API; the `'new'` route and the Add link; create tests (controller, service, form spec) |
  | update | `PUT /{id}`, `update`, `updateEntity` and `existsBy<Unique>AndIdNot`; `update()` in the API; the `':id/edit'` route and Edit links; update tests, and in the form spec the edit-mode tests |
  | delete | `DELETE /{id}` and `delete`; `delete()` in the API; the Delete button and `deleteError`; delete tests |

  - Without both create and update there is no form, no form spec and no `<Entity>Request`.
  - Without update, the form has no `id` input and no `isEdit`.
  - A route that remains keeps its place in the order `''`, `'new'`, `':id/edit'`, `':id'`.
  - With no operation list in the description, the default is full CRUD.
- There is no parent (a top-level resource).
- Seed data:
  - a top-level entity gets one `V<n>__<table>_seed.sql` with 3 rows, emails on `@example.test`;
  - a nested entity gets no seed.
- Detail heading is the first String field. The list shows every field, and the first one links to the detail.
- Plural is the regular English plural (`category` -> `categories`).

## 3. Type mapping

| Spec | Request record + validation | Entity / SQL | TypeScript | Form model / control |
|---|---|---|---|---|
| text | `@NotBlank @Size(max = N) String` | `@Column(nullable = false, length = N)` / `VARCHAR(N) NOT NULL` | `string` | `string`, `''` / `input type="text"` + `required()` |
| text with format (email, pattern) | add `@Email` or `@Pattern(regexp = ..., message = "must be ...")` | as text | `string` | as text, no client format check |
| Integer / Long | `@NotNull Integer` / `Long` (+ `@Min`/`@Max` if a range is given) | `INTEGER` / `BIGINT` | `number` | `number \| null` / `input type="number"` |
| BigDecimal | `@NotNull @Digits(integer = 10, fraction = 2) BigDecimal` (+ `@DecimalMin` if a minimum is given) | `@Column(nullable = false, precision = 12, scale = 2)` / `NUMERIC(12, 2) NOT NULL` | `number` | `number \| null` / `input type="number" step="0.01"` |
| boolean | primitive `boolean` (missing or `null` -> `false`) | `boolean` / `BOOLEAN DEFAULT FALSE NOT NULL` | `boolean` | `boolean`, `false` / `input type="checkbox"`, no `required()` |
| LocalDate | `@NotNull LocalDate` (+ `@PastOrPresent` / `@FutureOrPresent` if stated) | `DATE NOT NULL` | `string` (`yyyy-MM-dd`) | `string`, `''` / `input type="date"` |
| enum | `@NotNull <Enum>` | `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)` / `VARCHAR(20) NOT NULL` | union type + `<ENUM>_LABELS` + `<ENUM>S` | `<Enum> \| ''` / `select` with `<option value="">Select ...</option>` |
| optional (any type) | drop `@NotNull` / `@NotBlank`, keep the rest | nullable column, no `NOT NULL` | `T \| null` | empty value -> `null` in `toRequest`, no `required()` |
| unique | service `existsBy<Field>` / `existsBy<Field>AndIdNot` | `CONSTRAINT uq_<table>_<column> UNIQUE (<column>)` | - | - |

Rules:
- Date rules relative to today use the app `Clock` through `ClockProvider` (see [backend.md](backend.md), "Clock").
- Conflict message: `"<Entity> with <field> <value> already exists"`.
  - Write `<field>` as in the UI label: acronyms in capitals (`VIN`, `NIP`), every other word in lowercase.
  - Examples: `Vehicle with VIN X already exists`, `Customer with email x already exists`.

## 4. Files to generate
Backend, in `backend/src/main/java/com/example/dealership/<package>/` unless noted:
1. `backend/src/main/resources/db/migration/V<n+1>__<table>.sql`, plus `V<n+2>__<table>_seed.sql` for a top-level entity
2. `<Entity>.java`, and one `<Enum>.java` per enum
3. `<Entity>Repository.java`, `<Entity>Service.java`
4. `<Entity>Request.java`, `<Entity>Response.java`, `<Entity>Mapper.java`
5. `<Entity>Controller.java`

Frontend, in `frontend/src/app/<folder>/`:
6. `<entity>-api.ts`
7. `<entity>-list.ts` / `.html`, `<entity>-detail.ts` / `.html`, `<entity>-form.ts` / `.html`
8. `<folder>.routes.ts`, plus:
   - a lazy route in `app.routes.ts`;
   - a nav link in `app.html` (top-level only).

Tests:
9. In `backend/src/test/java/com/example/dealership/<package>/`:
   - `<Entity>ControllerTest`, `<Entity>ServiceTest`, `<Entity>RepositoryTest`;
   - nested only: `<Parent>DeleteWith<Entities>IntegrationTest`.
10. `<entity>-form.spec.ts`

Allowed changes outside the new files, and nothing else:
- `app.routes.ts` and `app.html`
- the parent detail template (nested only)
- the parent service, only for the `flush()` from step 5
- `backend/src/main/java/com/example/dealership/config/ClockConfig.java`, when a date rule needs it and it does not exist yet

## 5. Nested resource (only when the description names a parent)
The URL is `/api/<parents>/{<parent>Id}/<entities>`. Skeletons are in the "Nested" sections of
[backend.md](backend.md), [frontend.md](frontend.md) and [tests.md](tests.md).

- **Foreign key:**
  - SQL column: `<parent_table>_id BIGINT NOT NULL`, the parent table name in snake_case plus `_id` (`service_visit_id`, not `servicevisit_id`).
  - SQL constraint: `CONSTRAINT fk_<table>_<parent_table> FOREIGN KEY (<parent_table>_id) REFERENCES <parent_table> (id)`.
  - The entity has a plain `Long <parent>Id` (camelCase, `serviceVisitId`) with `@Column(name = "<parent_table>_id")`, not `@ManyToOne`.
- **DTOs:** the response includes `<parent>Id`, the request does not.
- **Not found:**
  - Every service method checks the parent first. A missing parent -> `NotFoundException("<Parent> with id X not found")`.
  - The child is loaded with `findByIdAnd<Parent>Id`. A child of another parent -> `NotFoundException("<Entity> with id X not found")`.
- **Queries:**
  - The list uses `findAllBy<Parent>Id(parentId, Sort.by("id"))`.
  - Uniqueness is scoped to the parent: `existsBy<Parent>IdAnd<Field>` / `existsBy<Parent>IdAnd<Field>AndIdNot`.
- **Deleting the parent:**
  - No `ON DELETE CASCADE`. Deleting a parent that has children must return 409 through the existing `DataIntegrityViolationException` handler, not 500.
  - The FK is reported at flush or commit, so the required integration test covers it (see [tests.md](tests.md)).
  - If that test gets 500, add `<parent>Repository.flush()` after `delete` in the parent service.

## 6. Write the code
Order: migration -> backend -> backend tests -> frontend -> form spec.
- Use the skeletons in [backend.md](backend.md) and [frontend.md](frontend.md).
- Use the case list in [tests.md](tests.md). Every case listed there is required.

## 7. Validation loop
1. If `CLAUDE.local.md` exists, set `JAVA_HOME` as it says. Otherwise use the environment as it is.
2. Run `./test.sh` from the repository root.
3. If red, read the first failure, fix the code and run again.
   - Fix a test only when it contradicts the pattern.
   - Never weaken an assertion to get to green.
4. Repeat. After 5 red rounds, stop and report the failing output instead of continuing.
5. Run `npx prettier --write` on the generated frontend files (in `frontend/`), then `./test.sh` once more.

Continue to step 8 only on green.

## 8. Compliance checklist
Check each item against the files and tick it in the summary:
- [ ] The migration number is the next free `V<n>`, and existing migrations are untouched.
- [ ] Every entity column exists in SQL with the same nullability and length (`ddl-auto=validate`).
- [ ] Status codes: 201 + `Location`, 200, 204, 404, 400, 409.
- [ ] Exception messages use the exact templates from `CLAUDE.md` and step 3.
- [ ] The list uses `Sort.by("id")`, and the service test stubs that exact call.
- [ ] Each unique field has all three layers: SQL constraint, `existsBy...` checks, global handler.
- [ ] Request validation is only in annotations; there is no logic in the controller.
- [ ] No Lombok, MapStruct, new dependencies, `ResponseStatusException` or custom error JSON.
- [ ] Frontend:
  - Signal Forms and `httpResource` reads;
  - no `.component` files, no `Component` suffix;
  - no component CSS, no new CSS classes.
- [ ] Routes are in the order `''`, `'new'`, `':id/edit'`, `':id'`, and the lazy route and nav link are added.
- [ ] Operation subset only: no endpoint, service method, API method, route, link, button or test remains for an omitted operation (`grep` for the omitted verbs).
- [ ] Client validation is `required()` only, and server errors go through `problemDetailToFormErrors`.
- [ ] Repository tests use values that cannot collide with seed data.
- [ ] Nested only:
  - parent 404 and foreign-child 404 are tested in the service and controller tests;
  - parent delete with children returns 409 in the integration test.
- [ ] `./test.sh` is green, and existing tests are unchanged.
- [ ] `git status --short` lists only files from step 4 (new files plus the allowed changes).

## 9. Summary (final message)
- Files created and modified (paths), copied from `git status --short`
- Assumptions taken in step 2, one line each
- The compliance checklist with ticks
- The `./test.sh` summary block, pasted verbatim

## Pitfalls
Mistakes an agent makes in this repo without this skill. Update this list after each test run.
- **Angular CLI defaults:** `*.component.ts`, `XxxComponent`, component CSS, reactive forms. This repo uses none of them.
- **Client-side format validators** (`maxlength`, `email`, `pattern`): format rules belong to the backend, and the spec asserts the backend messages.
- **Wrong create/delete responses:** `ResponseEntity.ok` on create, a missing `Location` header, or 200 on delete.
- **Update without `existsBy<Field>AndIdNot`:** the entity conflicts with itself.
- **`findAll()` without `Sort.by("id")`.**
- **Repository test values that collide with seed data:** `@DataJpaTest` runs the migrations.
- **Validation messages that name the field or start with a capital** (`Email must not be blank`). The pattern is `must not be blank`.
- **Nested backend:**
  - `@ManyToOne` breaks lazy loading with `open-in-view=false`.
  - `findById` without the parent id exposes another parent's child.
- **Nested route order:** the route in `app.routes.ts` is registered after the parent's lazy route, so the parent's `:id` swallows it.
- **Nested integration test marked `@Transactional`:** the delete is never flushed, so the FK error never happens and the test passes falsely.
- **Form spec without `afterEach(() => http.verify())`,** or without `http.expectNone` on an empty submit.
- **Single backslash in `@Pattern`:** `regexp` is a Java string literal, so every regex backslash is doubled in the source: write `"\\+?[0-9]+"` and `"\\d{10}"`. A single `\+` does not compile.
- **Pattern made only of optional characters:** `[0-9 -]+` also accepts `"---"` or `"   "`. A pattern must start and end with a required character class, e.g. `"\\+?[0-9]([0-9 -]*[0-9])?"` in Java source; put any length limit in a lookahead (`(?=.{9,20}$)`).
- **`@Pattern` / `@Email` field without a format test:** every such field needs a controller test with an invalid value and its exact message (see [tests.md](tests.md)).
