# Dealership: agent context

Spring Boot 3.5 / Java 21 backend (`backend/`), Angular 22 frontend (`frontend/`).
Requirements and assumptions: `INTENT.md`. The API contract in `BRIEF.md` (paths, enum values, response shapes) is fixed.

## Reference module: copy it, do not reinvent it
- Backend: `backend/src/main/java/com/example/dealership/vehicle/` and the shared `.../error/`.
- Frontend: `frontend/src/app/vehicles/` and `frontend/src/app/shared/`.
- Tests: `backend/src/test/java/com/example/dealership/vehicle/`, `frontend/src/app/vehicles/vehicle-form.spec.ts`.
- A new module has the same set of files, names and test classes as `vehicle`, unless `INTENT.md` says otherwise.

## Commands
- Java: before any `./mvnw`, `./test.sh` or `./run.sh`, set `JAVA_HOME` as described in `CLAUDE.local.md`.
- All tests: `./test.sh` (Git Bash, from the repo root).
- One backend test class or method (in `backend/`): `./mvnw test -Dtest=VehicleServiceTest` or `./mvnw test -Dtest='VehicleServiceTest#create*'`.
- One frontend spec (in `frontend/`): `npx ng test --watch=false --include=src/app/vehicles/vehicle-form.spec.ts`.
- Frontend formatting: `npm run format` (Prettier, `frontend/.prettierrc`).

## Backend: things the code does not make obvious
- Errors go only through `GlobalExceptionHandler`. Throw `NotFoundException` (404) or `ConflictException` (409). Do not build `ResponseEntity` errors in controllers.
- 400 body: `ProblemDetail` with `detail: "Validation failed"` and `errors: [{field, message}]`. The list is sorted by `field` (null first), then by `message`. A non-field error has `field: null`. 404/409 have only `detail` and no `errors`.
- Exception messages follow fixed templates that tests compare exactly:
  - `"<Entity> with id <id> not found"`
  - `"<Entity> with <FIELD> <value> already exists"` (e.g. `Vehicle with VIN ... already exists`)
- Validation messages are English, lowercase, without the field name (`must not be blank`). The locale is fixed to `en` in `application.properties`, so do not add message bundles.
- Validation lives in annotations on the request record. Cross-record rules (uniqueness, status transitions) live in the service.
- Uniqueness has three layers:
  1. a named SQL constraint `uq_<table>_<column>`;
  2. `existsBy<Field>` on create and `existsBy<Field>AndIdNot` on update, both throwing `ConflictException`;
  3. the existing `DataIntegrityViolationException` handler as a safety net.
  The exception is a rule with no DB constraint, recorded as a decision in `INTENT.md` (Z8).
- Lists are sorted with `findAll(Sort.by("id"))`. Service tests stub exactly that call.
- DTOs are records. Mapping is a `final` `<Entity>Mapper` with static `toEntity`, `updateEntity`, `toResponse`. No MapStruct, no Lombok.
- Controllers:
  - POST returns 201 with a `Location` header (`ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")`);
  - PUT returns 200;
  - DELETE returns 204.
- Enums are stored as strings (`@Enumerated(STRING)`, `length = 20`). An unknown value yields 400 `must be one of: ...` automatically.
- Services: `@Transactional(readOnly = true)` on the class, `@Transactional` on writing methods.

## Database and migrations
- Flyway, `backend/src/main/resources/db/migration/V<n>__<snake_case>.sql`. The next number is the highest existing `V<n>` plus one. Never edit an existing migration.
- `ddl-auto=validate`: every mapped column must exist in a migration with a matching type and length, or the app does not start.
- New fields on existing entities are optional with a default, so existing request bodies stay valid:
  - in the request, a primitive `boolean` or a nullable type mapped to the default;
  - in the migration, `DEFAULT`;
  - the old constructor signatures are kept, so existing tests compile unchanged.
- Seed data is a migration, so it is also present in `@DataJpaTest`. Repository tests use values that cannot collide with the seed (e.g. VIN `...0000004`, emails `@example.test`).
- Do not modify or add data to the seeded vehicles 1-3. They are the documented starting state, see `INTENT.md` §8.

## Frontend: things the code does not make obvious
- Forms use Signal Forms (`@angular/forms/signals`: `form()`, `[formRoot]`, `[formField]`), not reactive or template-driven forms.
- The client validates only `required` (`'<Label> is required'`). Format and range rules are left to the backend. Server errors are mapped with `problemDetailToFormErrors` from `shared/problem-detail.ts`.
- Reads use `httpResource` in `<entity>-api.ts`. Writes use `HttpClient` and are awaited in components with `firstValueFrom`. State is signals only, no store.
- API URLs are relative and start with `/api` (proxied by `proxy.conf.json` to `:8080`).
- Naming:
  - classes without the `Component` suffix (`VehicleList`);
  - files without `.component` (`vehicle-list.ts` + `vehicle-list.html`);
  - no per-component CSS;
  - `inject()` and `protected readonly` fields.
- Route order in `<entity>.routes.ts`: `''`, `'new'`, `':id/edit'`, `':id'` (`new` must come before `:id`). Feature routes are lazy-loaded from `app.routes.ts`, and the nav link goes in `app.html`.
- Route params arrive through `withComponentInputBinding()` as `input()` with a `transform` (`numberAttribute`, `optionalNumberAttribute`).
- Styling is the global, class-free `src/styles.css`. The only classes are `.error` and `nav a.active`. Do not add new ones.

## Domain code
- Business rules (status transitions, handover readiness, date checks) are plain Java with no Spring or JPA dependencies, covered by unit tests.
- Time comes from the injected `java.time.Clock` bean (`Europe/Warsaw`).
  - Never call `LocalDate.now()` without a clock.
  - Bean Validation date constraints use the same clock through `ClockProvider`.
  - Tests use `Clock.fixed(...)`.

## Definition of done
- `./test.sh` passes, and its summary output is shown in the reply.
- Every new rule or branch of logic has a test that fails without it.
- Existing vehicle API tests pass unchanged (backward compatibility).

## Working rules
- Keep diffs small and focused on the requested step. The owner reviews every diff.
- Commit only after the owner accepts. Use Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).
- No new dependencies (Maven or npm) without the owner's consent.
- Do not touch files unrelated to the task, including formatting-only changes.
- Never change or delete a test to make it pass. If a test looks wrong, say so and wait.
- Step-by-step procedures (generating a feature slice) belong in the `feature-slice` skill, not here.
