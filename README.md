# Dealership

Spring Boot backend and Angular frontend.

## Recruitment task

This repository is the starting point of a recruitment task. The task itself (in Polish)
is in [BRIEF.md](BRIEF.md): what to deliver, the API contract to keep, how to hand the work
in, and the transcript of the client meeting the requirements come from.

1. Check the requirements below and run `./test.sh`. It should pass before you change anything.
2. Read `BRIEF.md` in full, including the meeting transcript at the end.
3. Look at the vehicle module (`backend/.../vehicle/`, `frontend/src/app/vehicles/`). It shows
   how code is written in this project, and your work should follow it.
4. Do the task in the order of the deliverables listed in the brief, committing as you go.
5. Before handing in, make sure `./test.sh` passes.

To hand the work in, push it to a public GitHub repository of your own (a fork of this one is
fine) and send us the link.

## Requirements

- JDK 21
- Node.js 22.22.3 or newer 22.x (or 24.15+), with npm

`./run.sh` and `./test.sh` check the Node.js version first and stop with an error if it is
not supported. With nvm, `nvm install && nvm use` in the repository root picks the version
from `.nvmrc`.

Maven is not required: the backend uses the Maven wrapper.

## Running

```bash
./run.sh
```

- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api (the frontend dev server proxies `/api` to it)

The database is an in-memory H2 instance, recreated and seeded on every start.
Stop both applications with `Ctrl+C`.

## Tests

```bash
./test.sh
```

Runs the backend tests (`./mvnw test` in `backend/`) and the frontend tests
(`npm test` in `frontend/`). Both parts always run and a summary of both is printed at the
end. The script exits with a non-zero code if any test fails.

## Oddanie zadania

### Gdzie co jest
| Element | Miejsce |
|---|---|
| Wymagania, pytania do klienta, założenia, poza zakresem | [INTENT.md](INTENT.md) |
| Gdzie AI się pomyliło, co zrobione ręcznie i dlaczego | [DECISIONS.md](DECISIONS.md) |
| Kontekst dla agenta (konwencje projektu) | [CLAUDE.md](CLAUDE.md) |
| Skill `feature-slice` (Agent Skills) | [.claude/skills/feature-slice/](.claude/skills/feature-slice/): `SKILL.md` + `backend.md`, `frontend.md`, `tests.md` |
| Logi sesji z AI | `ai-logs/` |

Feature B (klienci, `/api/customers`) jest wygenerowany wyłącznie skillem. Feature A (dokumenty pojazdu i gotowość do wydania) ma szkielet ze skilla, a reguły biznesowe, endpoint gotowości i UI akcji zostały dopisane ręcznie, test-first. Podział jest opisany w DECISIONS.md.

### Testy
```bash
./test.sh
```
Skrypt uruchamia testy backendu i frontendu i wymaga JDK 21 (patrz „Requirements”). Jeśli `JAVA_HOME` w systemie wskazuje inną wersję, ustaw go tylko dla tego polecenia, np. `JAVA_HOME=/ścieżka/do/jdk-21 ./test.sh`.

### Logi AI
W `ai-logs/` są surowe pliki `.jsonl` sesji Claude Code skopiowane z `~/.claude/projects/` dla tego repozytorium, bez wycinania. Jedyna zmiana to zamaskowany adres e-mail.

### Jak pracowałem z AI
- **Jedna sesja Claude Code, krok po kroku.** O zakresie, założeniach i kolejności decydowałem ja. AI proponowało i implementowało. Każdy diff przeglądałem przed commitem, a commit powstawał dopiero po mojej akceptacji.
- **Skill testowany na dwóch encjach.** Klienci powstali w dwóch iteracjach: wynik pierwszej został wycofany, skill poprawiony i encja wygenerowana ponownie, bez ręcznych poprawek. Potem szkielet dokumentów pojazdu (zasób zagnieżdżony, podzbiór operacji). Po każdym przebiegu luki skilla trafiały do jego tekstu.
- **Niezależna recenzja.** Cały diff przejrzał osobny subagent tylko do odczytu, względem kontraktu API, INTENT.md i CLAUDE.md. Co z recenzji poprawiłem, a co świadomie zostawiłem, opisuje DECISIONS.md.

Na co dzień pracuję z Claude Code i Codexem we własnym orkiestratorze: routing zadań między modelami, cross-model review przed merge i weryfikacja zadania względem kodu. Tutaj w wersji uproszczonej.
