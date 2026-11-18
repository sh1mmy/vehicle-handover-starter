# DECISIONS

Gdzie AI się pomyliło, jak to wyłapaliśmy, co zostało zrobione ręcznie i dlaczego. Wymagania i założenia są w `INTENT.md`.

## Analiza wymagań i konwencje

### Stare konstruktory przy nowym polu w istniejącej encji (wyłapało AI)
- **Problem:** pierwszy szkic `INTENT.md` §7 obiecywał, że po dodaniu `registeredByDealer` do pojazdu „istniejące testy nie wymagają zmian”. To nieprawda. Dodanie komponentu do rekordów `VehicleRequest` i `VehicleResponse` oraz parametru do konstruktora `Vehicle` zmienia ich sygnatury, a testy wywołują je wprost (`new VehicleRequest(...)` w `VehicleServiceTest`, `new Vehicle(...)` w `VehicleRepositoryTest`). Nie kompilowałyby się.
- **Jak wyłapane:** AI zauważyło to samo przy ujednolicaniu `boolean` w INTENT i skillu.
- **Decyzja:**
  - Klasy zachowują dodatkowy konstruktor ze starą listą argumentów, delegujący z `false`.
  - Zasada trafiła do `INTENT.md` §7 i `CLAUDE.md`: nowe pola istniejących encji są opcjonalne z wartością domyślną, a stare sygnatury konstruktorów zostają.
  - Zaakceptowane przez właściciela.

### Z8: jeden aktywny dokument danego typu, tylko w serwisie
- **Problem:** H2 nie ma indeksów częściowych (`UNIQUE ... WHERE status <> 'REJECTED'`), więc reguły z kontraktu nie da się zapisać wprost jako constraint.
- **Decyzja (opcja A):** serwis sprawdza `exists...StatusNot(REJECTED)` w `@Transactional` i zwraca 409 z czytelnym `detail`. W bazie nie ma constraintu.
- **Ryzyko (świadome):** dwa równoległe POST-y tego samego typu dla jednego pojazdu mogą oba dostać 201. W salonie z kilkoma użytkownikami jest to mało prawdopodobne.
- **Następny krok (opcja B):**
  - Kolumna wyliczana `active_type = CASE WHEN status <> 'REJECTED' THEN type END` z `UNIQUE (vehicle_id, active_type)`. UNIQUE dopuszcza wiele `NULL`-i, więc odrzucone dokumenty nie kolidują.
  - Przegrany wyścig kończy się wtedy 409 przez istniejący handler `DataIntegrityViolationException`.
  - Koszty: składnia zależy od bazy (w PostgreSQL `STORED`), a `detail` przy wyścigu jest ogólny.
- **Odrzucona opcja C:** blokada pojazdu `PESSIMISTIC_WRITE`. Gwarancja zależałaby od tego, czy każda ścieżka zapisu bierze blokadę, a test wymagałby dwóch wątków.

### R8: data wystawienia nie z przyszłości, deterministycznie
- **Problem:** `@PastOrPresent` domyślnie porównuje z zegarem systemowym, a nie z naszym `Clock`. Testy „dziś/jutro” zależałyby od chwili uruchomienia, a strefa serwera mogłaby przesunąć granicę dnia.
- **Decyzja (opcja 1):** `ClockConfig` z beanem `Clock` (`Europe/Warsaw`) i `ValidationConfigurationCustomizer` ustawiającym `clockProvider(() -> clock)`.
  - Błąd przechodzi zwykłą ścieżką `MethodArgumentNotValidException`, więc format 400 `errors[{field:"issueDate"}]` jest taki sam jak dla innych pól.
  - Serwis i gotowość do wydania używają tego samego `Clock`.
  - `@WebMvcTest` nie skanuje `@Configuration`, więc test kontrolera importuje konfigurację ze stałym `Clock.fixed(...)`.
- **Odrzucona opcja 2:** sprawdzenie w serwisie i nowy wyjątek mapowany w `GlobalExceptionHandler`.
  - Zmienia współdzielony pakiet `error/` i wyjmuje regułę z adnotacji DTO, inaczej niż we wzorcu.
  - Błąd daty pojawiałby się dopiero po innych walidacjach.
  - Test kontrolera z mockowanym serwisem by jej nie sprawdzał.

## Feature B: klienci, iteracja 1

Wygenerowane skillem `/feature-slice` z opisu: firstName, lastName, email (unikalny), phone (wzorzec), nip (opcjonalny, 10 cyfr), pełny CRUD. Wynik został wycofany po przeglądzie, a skill poprawiony. Kod klientów zostanie wygenerowany ponownie, bez ręcznych poprawek.

### Co skill zrobił dobrze
- **Komplet plików:** migracja V4 + seed V5, 7 klas backendu, 3 klasy testów, API, lista, detal, formularz, trasy, lazy route i link w nawigacji.
- **Zgodność ze wzorcem `vehicle`:**
  - kody 201 + `Location`, 200, 204, 404, 400, 409;
  - szablony komunikatów i `Sort.by("id")`;
  - unikalność w trzech warstwach;
  - Signal Forms, `httpResource`, nazwy plików bez `.component`.
- **Dane testowe:** testy repozytorium używają wartości spoza seedu (`repository.*@example.test`).
- **Zakres zmian:** `git status` pokazywał tylko pliki dozwolone w kroku 4 skilla.
- **Wynik testów:** `./test.sh` był zielony w 2. rundzie pętli walidacji (backend 57, frontend 18).

### Czego brakowało
1. **Escape w `@Pattern`.** Pierwsza runda nie skompilowała się: `regexp` miał pojedyncze `\+` zamiast `\\+`.
   - Przyczyna bezpośrednia: plik był zapisany przez heredoc w powłoce, który zjadł jeden backslash.
   - Ten sam błąd zrobi każdy agent, który pomyli zapis regexa z literałem Javy, a skill o tym nie ostrzegał.
   - Ta sama przyczyna sprawiła, że pierwsza wersja pułapki w skillu miała pojedyncze backslashe. Wyłapane przy przeglądzie skomitowanego tekstu i poprawione w osobnym commicie.
2. **Brak testu formatu dla każdego pola z `@Pattern` / `@Email`.** `invalidBody()` łamało `email` i `nip`, ale wzorca telefonu nie sprawdzał żaden wymagany test. Test dla telefonu dopisano ad hoc, poza listą ze skilla.
3. **Wzorzec telefonu `(?=.{9,20}$)\+?[0-9 -]+` przepuszcza same separatory** (np. `"---------"`), bo składa się wyłącznie ze znaków opcjonalnych.
4. **Rozjazd z `INTENT.md`.** Pusty string w `nip` przez API dostaje 400 (nie pasuje do `\d{10}`), a INTENT obiecywał 201 i `null`. Formularz wysyła `null`, więc w UI tego nie widać.
5. **Nagłówek detalu.** Według reguły „pierwsze pole tekstowe” wychodziło samo `Jan` zamiast `Jan Kowalski`. Poprawione w skillu w `07f7d4e` (patrz niżej).
6. **Drobne, bez zmian w skillu (do decyzji później):** encja bez pola liczbowego nie ma oczywistego wariantu testu „zły typ wartości”. Agent wysłał `{}` w polu tekstowym.

### Co poprawiliśmy w skillu
- **Pułapki w `SKILL.md`:**
  - podwójny backslash w `regexp` adnotacji `@Pattern`;
  - wzorzec ma zaczynać się i kończyć znakiem wymaganym, a limit długości idzie do lookahead;
  - pole z `@Pattern` / `@Email` bez testu formatu.
- **`tests.md`:** każde pole z `@Pattern` / `@Email` musi być złamane w `invalidBody()` albo mieć własny test `createReturnsBadRequestProblemWithFieldErrorWhen<Field>HasInvalidFormat` z dokładnym komunikatem.
- **Nagłówek detalu w `SKILL.md`:** pola, po których człowiek rozpoznaje rekord (`firstName` + `lastName`, `name`, `title`, `number`), łączone spacją jak `make` + `model` w pojazdach. Domyślnie pierwsze pole tekstowe.
- **`INTENT.md` R21:** pusty string w `nip` daje 400. Brak pola lub `null` oznacza brak NIP-u.

## Feature B: klienci, iteracja 2

Wygenerowane poprawionym skillem z doprecyzowanym opisem telefonu: 9–20 znaków, opcjonalny `+`, cyfry rozdzielone pojedynczymi spacjami lub myślnikami, początek i koniec cyfrą. Commit `3cdc404`.

### Wynik
- `./test.sh` był zielony w pierwszej rundzie pętli walidacji: backend 57 testów, frontend 18.
- Zero ręcznych poprawek kodu. Prettier nie zmienił żadnego wygenerowanego pliku w `customers/`.
- `git status` przed commitem pokazywał tylko pliki dozwolone w kroku 4 skilla.

### Co się poprawiło względem iteracji 1
- **Escape w regexie:** `@Pattern` ma podwojone backslashe (`"(?=.{9,20}$)\\+?[0-9]+([ -][0-9]+)*"`) i kompiluje się od razu.
- **Telefon:** wzorzec zaczyna się i kończy cyfrą, więc nie przechodzą same separatory, podwójne separatory (`600--100-200`) ani spacja na końcu.
- **Testy formatu:**
  - `email` i `nip` są złamane w `invalidBody()`, z asertowanymi komunikatami.
  - `phone` ma własny test `createReturnsBadRequestProblemWithFieldErrorWhenPhoneHasInvalidFormat`.
  - Spec formularza wysyła też zły telefon w teście „leaves format and range rules to the backend”.
- **Nagłówek detalu:** `firstName lastName`, czyli `Jan Kowalski`.

### Co zostaje otwarte
- Test „zły typ wartości” dla encji bez pola liczbowego nadal wysyła `{}` w polu tekstowym. Skill tego przypadku nie opisuje.
