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

## Feature A: co dał skill, co ręcznie

### Co wygenerował skill (commit `990ac4f`)
Wywołanie `/feature-slice`: `VehicleDocument` zagnieżdżony pod `Vehicle`, segment `documents`, operacje list, get, create.

Pola:
- `type` (enum z kontraktu);
- `number`;
- `issueDate`: nie z przyszłości, przez app `Clock`;
- opcjonalne `expiryDate`;
- `status`: tylko do odczytu, `PENDING` przy tworzeniu.

Wynik:
- **Backend:**
  - migracja `V6__vehicle_document.sql`: FK `vehicle_id` bez kaskady, dokumenty bez seedu;
  - encja, dwa enumy, repozytorium (`findAllByVehicleId`, `findByIdAndVehicleId`), serwis z kontrolą rodzica, request i response, mapper, kontroler pod `/api/vehicles/{vehicleId}/documents`;
  - `config/ClockConfig.java`: bean `Clock` (Europe/Warsaw) i `ValidationConfigurationCustomizer`, czyli decyzja R8 (opcja 1).
- **Testy backendu:**
  - kontroler (14), serwis (7), repozytorium (3);
  - `VehicleDeleteWithVehicleDocumentsIntegrationTest`: usunięcie pojazdu z dokumentami daje 409, a nie 500. Test przeszedł bez `flush()` w `VehicleService`, więc serwis pojazdów został bez zmian (Z13).
- **Frontend:**
  - `vehicle-documents/`: lista, detal, formularz tylko do tworzenia, trasy `''` / `new` / `:id`, spec formularza (7 testów);
  - trasa zagnieżdżona przed `vehicles` w `app.routes.ts`;
  - link w istniejącej sekcji „Documents” w `vehicle-detail.html`.
- **Wynik testów:** `./test.sh` zielony, backend 82, frontend 25. Seedowane pojazdy 1–3 bez zmian.

### Błąd wyłapany w pętli walidacji: test zegara niczego nie dowodził
- **Problem:** pierwsza runda była zielona, ale stały „dziś” w teście kontrolera był ustawiony na prawdziwą dzisiejszą datę (2026-11-18). Test „jutro daje 400” przeszedłby także z zegarem systemowym, więc nie sprawdzał, czy `ClockProvider` jest w ogóle podpięty.
- **Jak wyłapane:** przegląd asercji po zielonym wyniku, z pytaniem „czy ten test padnie, jeśli usunę konfigurację?”.
- **Poprawka:**
  - Stały zegar przeniesiony w przeszłość (2025-06-15).
  - Teraz 400 dla 2025-06-16 pojawia się tylko wtedy, gdy walidator używa zegara aplikacji, bo dla zegara systemowego ta data jest przeszłością.
  - Po zmianie testy dalej są zielone, czyli `ClockProvider` działa naprawdę.
- **Wniosek:** zielony test nie znaczy, że test coś sprawdza. Zasada trafiła do `tests.md`.

### Luki w skillu ujawnione przez tę encję (poprawione w tekście skilla, commit `5bd46cf`, bez regeneracji)
1. **Pola tylko do odczytu.**
   - Problem: skill nie opisywał pola ustawianego przez system (`status`).
   - Rozwiązanie użyte przy generowaniu: pola nie ma w request ani w formularzu, konstruktor encji ustawia `PENDING`, jest getter bez settera, pole jest w response i w UI.
   - Teraz to reguła w `SKILL.md` krok 3.
2. **Nazwa wyświetlana encji z kilku słów.**
   - Problem: szablony komunikatów używały surowej nazwy klasy (`VehicleDocument with id ...`).
   - Teraz: `SKILL.md` krok 2 definiuje nazwę wyświetlaną (klasa rozbita na słowa, albo liczba pojedyncza segmentu URL). Komunikaty i teksty UI jej używają: `Document with id 5 not found`.
3. **Folder kontra segment URL w nested.**
   - Problem: szkielet frontendu używał `<folder>` także jako ścieżki trasy i URL-a. Tu folder to `vehicle-documents`, a segment `documents`.
   - Teraz: osobny placeholder `<segment>` dla URL-i, tras i linków. `<folder>` zostaje tylko dla ścieżek plików.
4. **Akronimy w etykietach enumów.**
   - Problem: reguła „sentence case” dawała `Coc` i `Oc policy`.
   - Teraz: akronimy zostają wielkimi literami (`COC`, `OC policy`, `Registration POA`).
5. **Stała data w teście zegara.**
   - Problem: `tests.md` nie mówił, że ma być w przeszłości (patrz błąd wyżej).
   - Teraz to wymóg z uzasadnieniem.

### Co zostaje do zrobienia ręcznie (poza wzorcem)
Tej części skill z założenia nie generuje.
- 409 przy dodaniu dokumentu typu, który ma już aktywny (nieodrzucony) dokument. Sprawdzenie tylko w serwisie, zgodnie z Z8.
- `PUT /api/vehicles/{vehicleId}/documents/{documentId}/status`:
  - dozwolone tylko `PENDING → VERIFIED` i `PENDING → REJECTED`, każda inna zmiana daje 409 (R6, Z3);
  - reguła przejść jako czysty kod z testami jednostkowymi.
- `GET /api/vehicles/{vehicleId}/handover-readiness` (R10–R17):
  - wymagane typy i `REGISTRATION_POA` zależny od flagi `registeredByDealer` na pojeździe;
  - `NOT_VERIFIED`, `EXPIRED` (względem daty wydania albo dziś, Z4) i `NO_HANDOVER_DATE`;
  - deterministyczna kolejność (Z7);
  - logika jako czysty kod z `Clock`.
- Zmiana w module pojazdów: `registeredByDealer` według `INTENT.md` §7, ze starymi konstruktorami.
- UI: akcje Verify/Reject dla `PENDING` i blok gotowości do wydania w szczegółach pojazdu (R18).

## Krok 6b: reguły biznesowe ręcznie, test-first

Wszystko poza wzorcem modułu pojazdów zostało napisane ręcznie. Po każdym punkcie był zielony `./test.sh` i osobny commit:

| Commit | Co |
|---|---|
| `a7f65f3` | `VehicleDocumentStatusTransitions` (czysta klasa) + `PUT .../status`: tylko `PENDING → VERIFIED/REJECTED`, reszta 409; `null` i nieznana wartość dają 400, dokument innego pojazdu 404 |
| `4cedbd4` | 409 przy drugim aktywnym dokumencie typu (tylko serwis, Z8); `expiryDate` < `issueDate` daje 400 na polu `expiryDate` (walidator na poziomie rekordu) |
| `b7bc4b3` | `registeredByDealer` w backendzie: migracja V7 `DEFAULT FALSE`, prymitywny `boolean`, stare konstruktory |
| `afb0e63` | `HandoverReadinessCheck` (czysta klasa, 17 testów jednostkowych z INTENT §2), serwis z wstrzykiwanym `Clock`, `GET .../handover-readiness`, dwa testy integracyjne |
| `b4377a0` | blok gotowości w szczegółach pojazdu |
| `f79c2aa` | checkbox `registeredByDealer` w formularzu i Yes/No w szczegółach |
| `a32cd08` | przyciski Verify/Reject przy dokumentach `PENDING`, przeładowanie listy po sukcesie, 409 pokazany jak `deleteError` |

Wynik: backend 143 testy, frontend 34.

### Zgoda na zmianę dwóch istniejących testów
- **Problem:** testy „creates the vehicle and navigates to its details” i „loads the vehicle in edit mode and updates it” w `vehicle-form.spec.ts` sprawdzają całe body żądania przez `toEqual`. Checkbox `registeredByDealer` (R19) dodaje do body nowe pole, więc oba testy musiały się zmienić.
- **Dlaczego AI się zatrzymało:** `CLAUDE.md` zabrania zmiany testu, żeby przeszedł. AI zatrzymało się przed tą zmianą i zapytało właściciela.
- **Decyzja właściciela:** zgoda, bo to nowe wymaganie (R19), a nie dopasowanie testu do kodu.
- **Asercje zostały tak samo ścisłe:**
  - w create pełne body z `registeredByDealer: false` (pole niezaznaczone);
  - w edycji pełne body z wartością z backendu (`true`).
- **Nowy test:** „sends registeredByDealer true when the checkbox is ticked”.
- Testy backendu modułu pojazdów zostały bez żadnej zmiany: nowe przypadki są w osobnych klasach `VehicleRegisteredByDealer*Test`, a `git diff` istniejących plików jest pusty.

### Błąd zerowania flagi przy edycji (wyłapało AI)
- **Problem:** po commicie backendu (`b7bc4b3`) frontend nie znał pola `registeredByDealer`. PUT jest pełną podmianą (INTENT §7), a brak pola daje `false`, więc każda edycja pojazdu w UI zerowałaby flagę ustawioną wcześniej.
- **Jak wyłapane:** AI zgłosiło to przy commicie backendu, zanim błąd trafił do UI.
- **Poprawka:** checkbox w `f79c2aa`. Test edycji z `registeredByDealer: true` z backendu jest teraz testem regresji. Przed implementacją był czerwony właśnie dlatego, że PUT wysyłał body bez flagi.

### Dwa błędne testy poprawione przed commitem
Oba to moje nowe testy z tego samego kroku. W żadnym przypadku nie zmieniono reguły ani kodu pod test.
1. **`HandoverReadinessCheckTest.ordersMissingTypesAndProblemsByDocumentTypeThenReason`.**
   - Problem: test bez daty wydania dawał polisie datę ważności `HANDOVER - 1 dzień` (2026-12-14). Według Z4 bez daty wydania porównujemy z dziś (2026-11-18), więc polisa nie była przeterminowana i `EXPIRED` słusznie się nie pojawiało.
   - Poprawka: dane testu, ważność przed dziś. Reguła została bez zmian.
2. **`vehicle-detail.spec.ts`.**
   - Problem: `beforeEach` czekał na `fixture.whenStable()`, a żądanie gotowości było jeszcze otwarte. `whenStable` czeka na HTTP, więc pierwszy test wisiał 10 s i pociągnął pozostałe.
   - Poprawka: `whenStable` dopiero po `flush` w każdym teście.

Do tego helper w nowym teście checkboxa: samo zdarzenie `change` nie aktualizuje Signal Forms, więc helper używa `click()`, jak użytkownik.

### Uczciwie o test-first
- W punktach 1 (przejścia statusu), 4 (gotowość), 5 (blok gotowości) i przy checkboxie oraz Verify/Reject testy były uruchomione i czerwone przed implementacją (brak klas albo nieprzechodzące asercje).
- W punkcie 2 (409 przy aktywnym typie, `expiryDate` < `issueDate`) testy zostały napisane przed implementacją, ale **nie były najpierw uruchomione na czerwono**. Pierwszy raz uruchomiłem je razem z gotowym kodem. Nie ma więc dowodu, że każdy z nich padał bez implementacji, choć asercje sprawdzają konkretne komunikaty i pola.

## Niezależna recenzja całego diffu

**Jak:**
- Osobny subagent tylko do odczytu przejrzał `git diff upstream/main..HEAD` (24 commity, 86 plików) względem BRIEF.md (zwłaszcza kontraktu API), INTENT.md, CLAUDE.md i oryginalnego modułu pojazdów.
- Dostał źródła do porównania, a nie wnioski autora, żeby jego ocena nie była zakotwiczona w moich.
- Każde ustalenie zostało sprawdzone w kodzie przed decyzją.
- Decyzje, co poprawiamy, podjął właściciel.

**Wynik:**
- Brak błędów P1, czyli niezgodności z kontraktem i błędów poprawności.
- Sprawdzone bez zastrzeżeń:
  - ścieżki, wartości enumów, kształt odpowiedzi gotowości, kody 400/404/409;
  - użycie `Clock`;
  - migracje V4–V7;
  - zgodność wsteczna API pojazdów;
  - brak nowych zależności.

### Poprawione (commity `003d8f8`, `b46d8e8`, `a59180c` i ten)
| Ustalenie | Poprawka |
|---|---|
| P2.1: reguły NIP z INTENT §2 bez testów (tylko `"123"`) | Test parametryzowany: 9 i 11 cyfr, z myślnikami, ze spacjami, pusty string, każdy daje 400 na polu `nip`. Do tego brak pola i `null` dają 201, a request ma `nip == null` |
| P2.2: brak testu serwisu „update z własnym e-mailem” | `CustomerServiceTest.updateKeepsOwnEmail` |
| P2.3: R4 „`status` w body POST jest ignorowany” bez dowodu na poziomie API | Test integracyjny: POST z `"status": "VERIFIED"` daje 201, a w odpowiedzi i w bazie jest `PENDING` |
| P3.1: 409 dla dokumentów tylko na mockach | `VehicleDocumentRulesIntegrationTest` na prawdziwej bazie: drugi aktywny dokument typu daje 409, `REJECTED → VERIFIED` daje 409, nowy dokument po odrzuconym daje 201 |
| P3.4: INTENT §7.7 opisywał dokumenty wewnątrz szczegółów pojazdu | Opis poprawiony zgodnie z kodem: osobna strona dokumentów, a w szczegółach link i blok gotowości |
| Uwaga właściciela: `HandoverReadinessController` miał pełną ścieżkę w `@GetMapping` | `@RequestMapping("/api/vehicles/{vehicleId}")` na klasie, `@GetMapping("/handover-readiness")`, jak w `VehicleController`. Testy bez zmian i dalej zielone |

**Wnioski dla skilla:**
- P2.1 i P2.2 to luki w testach kodu klientów wygenerowanego skillem.
- `tests.md` wymaga teraz:
  - testów granic i pustego stringa dla każdego pola z `@Pattern` (`@ParameterizedTest`);
  - pary testów brak/`null` dla każdego pola opcjonalnego;
  - testu serwisu `updateKeepsOwn<Unique>`.
- Bez regeneracji. Testy klientów dopisano ręcznie, a następna encja dostanie je ze skilla.

### Świadomie zostawione
- **P3.2: komunikat 409 przy duplikacie aktywnego typu:** „Vehicle with id X already has an active document of type INVOICE”.
  - Odbiega od szablonu `"<Entity> with <field> <value> already exists"` z CLAUDE.md, bo ten szablon opisuje unikalność pola encji, a tu konflikt dotyczy pary pojazd + typ i statusu.
  - Kontrakt nie określa treści `detail`.
  - Komunikat jest sprawdzany dokładnie w testach kontrolera, serwisu i integracyjnym.
- **P3.5: wyścig dwóch równoległych POST-ów tego samego typu (Z8):** decyzja z analizy, opisana wyżej razem z opcją B (kolumna wyliczana + UNIQUE) jako następnym krokiem. Do omówienia na rozmowie.
- **P3.6: `ProductionYearValidator` używa `Year.now()` bez `Clock`:**
  - To kod startowy spoza naszego diffu, więc go nie zmieniamy.
  - Dług techniczny: walidator roku produkcji zależy od zegara systemowego, a nie od `Clock` aplikacji.
  - Naprawa byłaby prosta (`ClockProvider` jest już podpięty: `Year.now(clock)` w walidatorze), ale zmienia moduł wzorcowy.
- **P3.3: `GET .../documents/{id}` i ekran szczegółów dokumentu** wykraczają poza kontrakt.
  - Są zgodne z konwencją modułu pojazdów i potrzebne dla nagłówka `Location` po POST.
  - Zostają.

## Po oddaniu: dziura w regule przejść statusu (moja interpretacja)

**Problem:**
- Z3 dopuszczało zmianę statusu tylko z `PENDING`, więc `VERIFIED → REJECTED` dawało 409.
- W połączeniu z R7 (jeden aktywny dokument danego typu) dawało to ślepy zaułek. Scenariusz odnowienia polisy:
  1. polisa OC jest `VERIFIED`, ale wygasa przed datą wydania, więc gotowość pokazuje `EXPIRED`;
  2. nowej polisy nie da się dodać, bo jest już aktywny dokument tego typu (409);
  3. starej nie da się odrzucić, bo `VERIFIED → REJECTED` daje 409.
- Pojazd zostaje na zawsze niegotowy do wydania.

**Jak wyłapane:**
- Przegląd niejasności po oddaniu, przez właściciela, a nie przez AI.
- Pytanie P2 było zapisane w INTENT. Przyjęte założenie (najbardziej restrykcyjne) nie zostało jednak sprawdzone na scenariuszu, który łączy dwie reguły.
- Testy tego nie wyłapały, bo każda reguła miała testy osobno, a nie było testu przepływu „dokument przestał być ważny i trzeba go zastąpić”.

**Decyzja:**
- Dozwolone są `PENDING → VERIFIED`, `PENDING → REJECTED` i `VERIFIED → REJECTED`, reszta daje 409.
- `REJECTED` zostaje stanem końcowym, jak chciała Anna („odrzucony dokument zostaje odrzucony, poprawka to nowy dokument”), więc S1 obowiązuje bez zmian.
- Zaktualizowane: R6, przypadki brzegowe w §2, Z3 i P2 w INTENT.md.
- P2 zostaje częściowo otwarte: czy odrzucenie zweryfikowanego dokumentu wymaga uprawnień albo zapisania powodu (P9).

**Testy (test-first, najpierw czerwone):**
- `VehicleDocumentStatusTransitionsTest`: `VERIFIED, REJECTED` zmienione z `false` na `true`. To zmiana wymagania, a nie test dopasowany do kodu.
- `VehicleDocumentRulesIntegrationTest.replacesVerifiedDocumentByRejectingItAndAddingNewOne`:
  - zweryfikowany dokument blokuje nowy tego typu (409);
  - po odrzuceniu (200) nowy dokument daje 201 i `PENDING`.
- Przed zmianą w `VehicleDocumentStatusTransitions` oba testy padały: pierwszy na wierszu `VERIFIED, REJECTED`, drugi z 409 zamiast 200.

**Wniosek:** reguły, które się nawzajem ograniczają (tu: przejścia statusu i jeden aktywny dokument typu), trzeba sprawdzić na scenariuszu przepływu, a nie tylko każdą osobno.

**UI:** lista dokumentów pokazuje Verify tylko przy `PENDING`, a Reject przy `PENDING` i `VERIFIED`. Odrzucony dokument nie ma akcji. Odnowienie polisy da się więc zrobić z UI. Dwa istniejące testy spec listy zmieniły oczekiwania zgodnie z nowym wymaganiem, do tego doszedł test odrzucenia zweryfikowanego dokumentu.
