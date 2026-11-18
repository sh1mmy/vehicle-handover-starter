# INTENT: dokumenty pojazdu, gotowość do wydania, klienci

Źródła: kontrakt API z `BRIEF.md` (dalej **[K]**) i transkrypcja spotkania (dalej **[T]**, z imieniem mówiącego).
**Kontrakt ma pierwszeństwo przed interpretacją transkrypcji.** Kiedy rozmowa mówi coś innego niż kontrakt albo coś, czego kontrakt nie obejmuje, trzymam się kontraktu, a resztę zapisuję jako założenie lub pytanie.

## 1. Wymagania

### Dokumenty pojazdu

| ID | Wymaganie | Źródło |
|---|---|---|
| R1 | Pojazd ma listę dokumentów: `GET /api/vehicles/{vehicleId}/documents`. | [K] „Lista dokumentów pojazdu” |
| R2 | Typy dokumentów to dokładnie `SALES_AGREEMENT`, `INVOICE`, `COC`, `OC_POLICY`, `REGISTRATION_POA`. | [K] lista `type`; [T] Anna: „Umowa sprzedaży, faktura, świadectwo zgodności, czyli COC, i polisa OC … Do tego pełnomocnictwo do rejestracji” |
| R3 | Dodanie dokumentu: `POST /api/vehicles/{vehicleId}/documents` z polami `type`, `number`, `issueDate` i opcjonalnym `expiryDate`. | [K] „Dodanie dokumentu: `type`, `number`, `issueDate`, opcjonalnie `expiryDate`” |
| R4 | Nowy dokument zawsze ma status `PENDING`. Klient API nie może podać statusu przy tworzeniu. | [K] „Nowy dokument ma status `PENDING`”; [T] Anna: „Jak ktoś wgra skan, dokument czeka na weryfikację.” |
| R5 | Statusy to `PENDING`, `VERIFIED`, `REJECTED`. Zmiana idzie przez `PUT …/documents/{documentId}/status` z body `{"status": …}`. | [K]; [T] Anna: „jest albo zweryfikowany, albo odrzucony” |
| R6 | Dozwolone są tylko przejścia `PENDING → VERIFIED` i `PENDING → REJECTED`. Każda inna zmiana zwraca **409**. | [K] „Niedozwolona zmiana statusu dokumentu również zwraca 409”; [T] Anna: „Odrzucony dokument zostaje odrzucony.” |
| R7 | Pojazd może mieć najwyżej jeden aktywny (`PENDING` lub `VERIFIED`) dokument danego typu. Dodanie kolejnego zwraca **409**. Odrzucone dokumenty zostają w historii. | [K] „Dodanie dokumentu typu, który ma już aktywny (nieodrzucony) dokument, zwraca 409”; [T] Anna: „Jeden aktualny każdego typu. Odrzucone mogą wisieć w historii” |
| R8 | `issueDate` nie może być z przyszłości (dzisiaj jest dozwolone), w przeciwnym razie **400**. | [T] Anna: „Nie może być z przyszłości.” |
| R9 | `expiryDate` jest opcjonalna. | [K] „opcjonalnie `expiryDate`”; [T] Anna: „Nie każdy. Umowa i faktura nie mają.” |

### Gotowość do wydania

| ID | Wymaganie | Źródło |
|---|---|---|
| R10 | `GET /api/vehicles/{vehicleId}/handover-readiness` zwraca `{ready, missingDocumentTypes, problems[{documentType, reason}]}`. | [K] |
| R11 | Wymagane zawsze są `SALES_AGREEMENT`, `INVOICE`, `COC`, `OC_POLICY`. | [T] Anna: „Bez tych czterech auto nie wyjeżdża.” |
| R12 | `REGISTRATION_POA` jest wymagany tylko wtedy, gdy przy pojeździe zaznaczono, że salon rejestruje auto. Domyślnie nie jest. | [T] Anna: „Domyślnie klient rejestruje sam, więc pełnomocnictwo jest wymagane tylko przy autach, gdzie Tomek to zaznaczy.” |
| R13 | Wymagany typ bez aktywnego dokumentu trafia do `missingDocumentTypes`. Typ, który ma tylko odrzucone dokumenty, też tam trafia. | [T] Anna: „Odrzucony to tak, jakby dokumentu nie było, trzeba donieść nowy.” |
| R14 | Aktywny wymagany dokument w statusie `PENDING` daje problem `NOT_VERIFIED`. | [K] `reason`: `NOT_VERIFIED`; [T] Anna: „dokument czeka na weryfikację” |
| R15 | Aktywny wymagany dokument z `expiryDate` < data wydania daje problem `EXPIRED`. Wygaśnięcie dokładnie w dniu wydania jest w porządku. | [T] Anna: „Dokument musi być ważny w dniu wydania auta i ten dzień się jeszcze liczy. Jak polisa kończy się w dniu wydania, to jest w porządku.” |
| R16 | Brak `plannedHandoverDate` daje problem `{documentType: null, reason: NO_HANDOVER_DATE}`, więc `ready` nie może być wtedy `true`. | [K] przykład odpowiedzi; [T] Anna: „jak nie ma ustalonej daty wydania, to nie może pokazywać, że gotowe.” |
| R17 | `ready = true` tylko wtedy, gdy `missingDocumentTypes` i `problems` są puste. | [K] struktura odpowiedzi; [T] Tomek: „gotowe do wydania albo czego brakuje” |
| R18 | Na ekranie pojazdu widać gotowość i braki. | [T] Tomek: „żebym w aucie od razu widział: gotowe do wydania albo czego brakuje.” |
| R19 | Przy pojeździe da się zaznaczyć „salon rejestruje auto”. | [T] Anna: „Tomek może to zaznaczać przy aucie, jak ustali z klientem.” |

### Klienci

| ID | Wymaganie | Źródło |
|---|---|---|
| R20 | Pełny CRUD `/api/customers` według konwencji modułu pojazdów. | [K] „Klienci, według konwencji modułu pojazdów” |
| R21 | Pola to imię, nazwisko, e-mail, telefon i opcjonalny NIP (dla firm). | [T] Anna: „imię, nazwisko, e-mail, telefon, a dla firm NIP.” |
| R22 | E-mail jest unikalny. Duplikat przy create i update zwraca **409**. | [T] Anna: „E-mail nie może się powtarzać.”; konwencja pojazdów (VIN → 409) |
| R23 | Klient nie jest powiązany z pojazdem. | [T] Anna: „Na razie bez powiązań z autami, to później.” |

## 2. Reguły biznesowe i przypadki brzegowe

**Data wydania.** We wszystkich przypadkach chodzi o `plannedHandoverDate` pojazdu.

| Reguła | Przypadek brzegowy | Oczekiwany wynik |
|---|---|---|
| R4 status początkowy | Body POST zawiera `"status": "VERIFIED"` | Pole jest ignorowane, dokument ma `PENDING`. Request nie zna pola `status`. |
| R6 przejścia | `REJECTED → VERIFIED` (wersja Tomka) | 409 |
| R6 | `PENDING → PENDING`, `VERIFIED → VERIFIED` | 409 (to nie jest zmiana stanu) |
| R6 | `VERIFIED → REJECTED` (błąd zauważony po weryfikacji) | 409 według założenia Z3, jest też pytanie P2 |
| R6 | `VERIFIED → PENDING` | 409 |
| R6 | `status` pusty, `null` albo nieznana wartość | 400 (walidacja i istniejący handler enumów), a nie 409 |
| R6 | `documentId` należy do innego pojazdu | 404 |
| R7 jeden aktywny | Jest `INVOICE` w `PENDING` i dodajemy drugi `INVOICE` | 409 |
| R7 | Jest `INVOICE` w `VERIFIED` i dodajemy drugi | 409 |
| R7 | Są tylko odrzucone `INVOICE` i dodajemy nowy | 201 |
| R7 | Ten sam typ istnieje w innym pojeździe | 201 (unikalność dotyczy pary pojazd + typ) |
| R7 | Dwa równoległe POST-y tego samego typu | Nie jest gwarantowane: oba mogą dostać 201 (świadome ryzyko, Z8) |
| R8 issueDate | `issueDate` = dzisiaj według `Clock` (Z11), także gdy zegar systemowy ma już jutro (np. strefa UTC kontra Europe/Warsaw około północy) | 201 |
| R8 | `issueDate` = jutro | 400, `errors[].field = "issueDate"`, ten sam format co pozostałe błędy walidacji (Z16) |
| R8 | Brak `issueDate` | 400 |
| R9 expiryDate | `expiryDate` < `issueDate` | 400 według Z5 |
| R9 | `expiryDate` = `issueDate` | 201 |
| R9 | `expiryDate` podane dla `INVOICE` | 201, dane są przyjmowane (Z6, P4) |
| R3 pojazd | `vehicleId` nie istnieje: GET, POST, PUT status, readiness | 404 |
| Z13 usuwanie pojazdu | `DELETE /api/vehicles/{id}` dla pojazdu z dokumentami (także tylko odrzuconymi) | **409** problem+json, nie 500. FK jest zgłaszany dopiero przy flush/commicie, poza ciałem metody serwisu. Wymaga testu integracyjnego (`@SpringBootTest` + MockMvc), bo `@WebMvcTest` mockuje serwis. Jeśli test pokaże 500, serwis robi `flush()` po `delete`, żeby wyjątek poleciał w transakcji i trafił do istniejącego handlera `DataIntegrityViolationException`. |
| Z13 | `DELETE` pojazdu bez dokumentów | 204, bez zmian |
| R12 POA | Flaga wyłączona, brak POA | POA nie trafia do `missingDocumentTypes` |
| R12 | Flaga wyłączona, ale jest POA (np. `PENDING` albo wygasły) | POA jest ignorowany w gotowości i nie generuje problemów |
| R12 | Flaga włączona, brak POA | `missingDocumentTypes` zawiera `REGISTRATION_POA` |
| R13 brak | Jedyny `COC` jest `REJECTED` | `COC` trafia do `missingDocumentTypes`, nie do `problems` |
| R14 + R15 | Aktywny `OC_POLICY` jest `PENDING` i wygasa przed wydaniem | Dwa problemy: `NOT_VERIFIED` i `EXPIRED` |
| R15 | `expiryDate` = data wydania | Brak problemu |
| R15 | `expiryDate` = data wydania − 1 dzień | `EXPIRED` |
| R15 | Dokument bez `expiryDate` | Nigdy nie dostaje `EXPIRED` |
| R15 + R16 | Brak daty wydania, `expiryDate` < dzisiaj | `EXPIRED` oraz `NO_HANDOVER_DATE`, zgodnie z przykładem w [K] (Z4) |
| R15 + R16 | Brak daty wydania, `expiryDate` ≥ dzisiaj | Tylko `NO_HANDOVER_DATE` |
| R15 | `plannedHandoverDate` w przeszłości | Porównanie z tą datą, bez zmian i bez dodatkowego problemu (P5) |
| R16 | Komplet zweryfikowanych dokumentów, brak daty | `ready: false`, `problems: [{null, NO_HANDOVER_DATE}]` |
| R17 | Pojazd bez żadnych dokumentów, z datą | `ready: false`, 4 typy w `missingDocumentTypes`, `problems: []` |
| R17 kolejność | Wiele braków i problemów | Kolejność jest deterministyczna: typy w kolejności enuma, problemy dokumentów przed `NO_HANDOVER_DATE`, jak w przykładzie [K] (Z7) |
| R22 e-mail | `jan@x.pl` drugi raz | 409 |
| R22 | `Jan@X.pl` i `jan@x.pl` | Dwa różne e-maile, 201 (porównanie dokładne, Z9). Pytanie P6. |
| R22 | E-mail `Jan@X.pl` | Zapisywany i zwracany dokładnie jako `Jan@X.pl` |
| R22 | Update klienta z jego własnym e-mailem | 200 (`existsByEmailAndIdNot`) |
| R21 NIP | Brak NIP-u albo pusty string | 201, NIP zapisany jako `null` |
| R21 | NIP ma 9 lub 11 cyfr | 400 (Z10) |
| R21 | NIP `123-456-78-90` albo `123 456 78 90` | 400, z myślnikami ani spacjami nie przechodzi (Z10) |

## 3. Sprzeczności w rozmowie

| # | Sprzeczność | Co obowiązuje |
|---|---|---|
| S1 | **Odrzucony dokument.** Tomek: „jak odrzucimy, a klient doniesie poprawioną wersję, to po prostu przestawiamy status na zweryfikowany”. Anna: „Odrzucony dokument zostaje odrzucony. Jak klient przyniesie poprawkę, wgrywamy nowy dokument.” | **Wersja Anny.** Jest ostatnia, Anna wprost ją rozstrzyga („Na razie róbcie po mojemu”) i jest zgodna z [K] („aktywny (nieodrzucony)”, 409 na niedozwoloną zmianę statusu). `REJECTED` jest stanem końcowym. Ryzyko: dyrektor może zdecydować inaczej (P1). Zmiana sprowadzałaby się wtedy do jednej reguły przejść. |
| S2 | **Odrzucony = brak?** Anna wcześniej: „Odrzucone mogą wisieć w historii”, Tomek: „Inaczej mamy po trzy faktury”. To nie jest sprzeczność, tylko obawa o bałagan. | Historia zostaje. Lista dokumentów pokazuje wszystkie, a gotowość patrzy tylko na aktywne. W UI można wizualnie odróżnić odrzucone. |
| S3 | **Data ważności.** Anna: „Polisa ma, pełnomocnictwo zwykle też”. [K]: `expiryDate` opcjonalne dla wszystkich typów. | **[K].** Nie wymuszam `expiryDate` dla `OC_POLICY`, bo testy oceniających mogą dodawać polisę bez daty. Pytanie P3. |

## 4. Pytania do klienta

1. **P1** (S1): Czy dyrektor zatwierdził zasadę, że odrzucony dokument zostaje odrzucony?
2. **P2**: Czy zweryfikowany dokument można później odrzucić (np. gdy błąd wyjdzie po weryfikacji)? Teraz dostaje się 409.
3. **P3**: Czy polisa OC musi mieć datę ważności? Polisa bez daty jest dziś zawsze uznawana za ważną.
4. **P4**: Czy umowa i faktura mogą mieć datę ważności, czy system ma ją odrzucać?
5. **P5**: Co z autem, którego planowana data wydania już minęła, a nie zostało wydane? Czy pokazywać to jako problem?
6. **P6**: Czy e-maile różniące się tylko wielkością liter to ten sam e-mail? Dziś to dwa różne e-maile (Z9).
7. **P7**: Czy NIP ma być walidowany sumą kontrolną i czy dopuszczać NIP-y zagraniczne (prefiks kraju)?
8. **P8**: Co zrobić z dokumentami przy usuwaniu pojazdu? Usunąć je razem z pojazdem, czy blokować usunięcie?
9. **P9**: Czy trzeba zapisywać, kto zweryfikował lub odrzucił dokument i kiedy? Anna mówi „przy kontroli”.
10. **P10**: Czy dokumenty można edytować lub usuwać (np. literówka w numerze)? Kontrakt tego nie przewiduje.

## 5. Założenia

| # | Założenie | Uzasadnienie |
|---|---|---|
| Z1 | Flaga przy pojeździe to `registeredByDealer: boolean`, domyślnie `false`. | Anna: „Domyślnie klient rejestruje sam”. To najprostszy sposób na „Tomek zaznacza”. |
| Z2 | Gotowość jest liczona na żądanie, nic nie jest zapisywane. | Gotowość zależy od dat (dziś, data wydania) i zmienia się bez żadnego zapisu, więc stan zapisany w bazie by się rozjeżdżał. |
| Z3 | Jedyny stan, z którego można zmienić status, to `PENDING`. | Anna opisuje tylko weryfikację oczekującego dokumentu. Najbezpieczniejsza interpretacja „niedozwolonej zmiany” z [K]. |
| Z4 | Bez daty wydania `EXPIRED` liczę względem dzisiaj. | Przykład w [K] pokazuje `EXPIRED` obok `NO_HANDOVER_DATE`. Dokument wygasły przed dzisiaj będzie wygasły w każdej przyszłej dacie wydania. |
| Z5 | `expiryDate` < `issueDate` zwraca 400 (błąd na polu `expiryDate`). | Analogicznie do „data z przyszłości = pomyłka przy wpisywaniu” (Anna). |
| Z6 | `expiryDate` jest przyjmowane dla każdego typu. | [K] nie robi wyjątków (P4). |
| Z7 | Kolejność: `missingDocumentTypes` i problemy według kolejności typów w enumie, w obrębie typu `NOT_VERIFIED` przed `EXPIRED`, `NO_HANDOVER_DATE` na końcu. Lista dokumentów jest sortowana po `id`. | Deterministyczne odpowiedzi; przykład [K] ma `NO_HANDOVER_DATE` na końcu; moduł pojazdów sortuje po `id`. |
| Z8 | Unikalność aktywnego dokumentu sprawdza **tylko serwis**: `exists…StatusNot(REJECTED)` w `@Transactional`, 409 z czytelnym `detail`. Bez constraintu w bazie. | Decyzja: opcja A. H2 nie ma indeksów częściowych. Ryzyko wyścigu i opcja B (kolumna wyliczana + UNIQUE) jako następny krok są opisane w `DECISIONS.md`. |
| Z9 | E-mail jest zapisywany i zwracany dokładnie tak, jak przyszedł. Unikalność jest dokładna (`existsByEmail` / `existsByEmailAndIdNot` + constraint `uq_customer_email`), jak VIN w pojazdach. | Klienci mają powstać wyłącznie skillem, więc bez logiki spoza wzorca. Wielkość liter zostaje jako pytanie P6. |
| Z10 | NIP: opcjonalny, dokładnie 10 cyfr, **bez myślników i spacji** (`\d{10}`), bez sumy kontrolnej. Telefon: wymagany, prosty wzorzec: cyfry, spacje i myślniki, opcjonalny `+` tylko na początku, 9–20 znaków. | Minimalna walidacja wyłapująca literówki, bez wymyślania reguł (P7). |
| Z11 | „Dziś” pochodzi z wstrzykiwanego `Clock` (strefa `Europe/Warsaw`). | Testowalność reguł dat i brak zależności od strefy serwera. |
| Z12 | `number` dokumentu: wymagany, max 50 znaków, bez unikalności. | Kontrakt nie podaje reguł; różni wystawcy mogą mieć te same numery. |
| Z13 | Usunięcie pojazdu z dokumentami zwraca 409 (FK, istniejący handler `DataIntegrityViolation`). | Anna dba o historię „przy kontroli”, a kaskadowe usunięcie by ją niszczyło (P8). |
| Z14 | Odpowiedź dokumentu: `id`, `vehicleId`, `type`, `number`, `issueDate`, `expiryDate`, `status`. POST zwraca 201 i `Location`, PUT statusu zwraca 200 z dokumentem. | Konwencja modułu pojazdów („reszta API zgodna z konwencjami”). |
| Z15 | Pola klienta: `firstName`, `lastName`, `email`, `phone`, `nip` (tabela `customer`: `first_name`, `last_name`, `email`, `phone`, `nip`). | Angielskie nazwy jak w module pojazdów; `nip` zostaje po polsku, bo to polski identyfikator bez dobrego odpowiednika (`taxId` sugerowałby też zagraniczne numery, P7). |
| Z16 | `issueDate` nie z przyszłości: `@PastOrPresent` na `DocumentRequest.issueDate`, z `ClockProvider` walidatora podpiętym pod nasz `Clock` (szczegóły w §9). | Ten sam format 400 co inne błędy walidacji, bez zmian w `GlobalExceptionHandler`, deterministyczne testy. |

## 6. Poza zakresem

- Wgrywanie skanów i plików. Anna mówi „wgra skan”, ale [K] nie ma pola na plik.
- SMS-y do klienta. Kasia: „zostawiamy je na kolejny etap”.
- Powiązanie klientów z pojazdami. Anna: „to później”.
- Użytkownicy, role i logowanie, czyli kto weryfikuje (P9).
- Edycja i usuwanie dokumentów (P10). Kontrakt ma tylko listę, dodanie i zmianę statusu.
- Rejestrowanie faktycznego wydania auta i blokada wydania. System tylko pokazuje gotowość.
- Stronicowanie, filtrowanie i wyszukiwanie list.

## 7. Zmiany w module pojazdów (bez łamania obecnego API)

1. **Migracja** `V{n}__vehicle_registered_by_dealer.sql`: `ALTER TABLE vehicle ADD COLUMN registered_by_dealer BOOLEAN DEFAULT FALSE NOT NULL`. Istniejące wiersze i seed dostają `false`.
2. **`Vehicle`**: pole `registeredByDealer` (`boolean`).
3. **`VehicleRequest`**: nowe, **opcjonalne** pole prymitywne `boolean registeredByDealer`. Brak pola lub `null` daje `false` (domyślne zachowanie Jacksona dla typów prymitywnych). Obecne body (bez pola) dalej przechodzą walidację.
   - `VehicleRequest`, `VehicleResponse` i `Vehicle` dostają dodatkowy konstruktor z dotychczasową listą argumentów, delegujący z `false`. Dzięki temu istniejące testy (`new VehicleRequest(...)` itd.) kompilują się bez zmian.
   - Konsekwencja do akceptacji: PUT jest pełną podmianą (tak jak `plannedHandoverDate`), więc stary klient wysyłający PUT bez pola wyzeruje flagę. To jest spójne z obecną semantyką PUT.
4. **`VehicleResponse`**: dodatkowe pole `registeredByDealer` (zawsze `true`/`false`). Dodanie pola do odpowiedzi nie łamie klientów.
5. **`VehicleMapper`**: przepisuje wartość wprost, bez mapowania `null`.
6. **Usuwanie pojazdu**: bez zmian w kodzie. Przy istniejących dokumentach FK daje 409 przez obecny handler (Z13). Pojazd bez dokumentów dalej zwraca 204.
7. **Frontend**:
   - `vehicle-api.ts`: pole `registeredByDealer: boolean` w `Vehicle`.
   - Formularz: checkbox „Dealer registers the vehicle”, domyślnie odznaczony.
   - Detal: kolumna lub wiersz w `dl`.
   - Pusta sekcja „Documents” w `vehicle-detail.html` dostaje listę dokumentów, formularz dodania, akcje Verify/Reject dla `PENDING` i blok gotowości do wydania (R18).
8. **Testy**: w `VehicleControllerTest`, `VehicleServiceTest` i spec formularza dochodzą przypadki „brak pola → false” oraz „true jest zapisywane i zwracane”. Istniejące testy zostają bez zmian, co samo w sobie sprawdza zgodność wsteczną.
9. **Zmiana daty wydania** na pojeździe od razu wpływa na gotowość, bez dodatkowej logiki (Z2).

## 8. Zgodność wsteczna i dane startowe

1. **Istniejące API pojazdów działa bez zmian dla obecnych klientów.**
   - `registeredByDealer` jest w `VehicleRequest` **opcjonalne** (prymitywny `boolean`), brak pola lub `null` oznacza `false`. Stare body bez tego pola dalej przechodzą walidację i zwracają te same kody (201/200).
   - To świadomie inaczej niż `fuel_type` z `V3`, które jest wymagane (`@NotNull`, `SET NOT NULL`). Tam stare body bez `fuelType` dostają 400, tutaj nie.
   - Kolumna: `registered_by_dealer BOOLEAN DEFAULT FALSE NOT NULL`. Default w DDL wystarcza, więc migracja nie robi `UPDATE` seedu (w przeciwieństwie do `V3`).
   - Odpowiedzi dostają tylko nowe pole, żadne istniejące nie zmienia nazwy ani typu. Istniejące testy modułu pojazdów przechodzą bez modyfikacji.
2. **Seed pojazdów zostaje w stanie startowym.**
   - Nie zmieniamy istniejących wierszy z `V2`/`V3` i nie dodajemy do pojazdów 1–3 żadnych dokumentów.
   - Pojazd 3 (Skoda, `TMBJJ7NE8L0000003`) zostaje bez daty wydania, więc od razu pokazuje `NO_HANDOVER_DATE`, a pojazdy 1–2 pokazują komplet braków.
   - Migracje dokumentów zawierają tylko DDL, bez danych.
3. **Seed klientów** używa domeny zarezerwowanej do testów, która nie koliduje z prawdziwymi danymi: `@example.test` (np. `jan.kowalski@example.test`). Telefony i NIP-y są oczywiście fikcyjne.
4. **Nowe migracje** idą od `V4` w kolejności powstawania (klienci przed dokumentami, zgodnie z kolejnością z briefu). Istniejących plików migracji nie edytujemy, bo Flyway sprawdza sumy kontrolne.

## 9. R8 deterministycznie: decyzja

`issueDate` sprawdza `@PastOrPresent`, a walidator dostaje nasz `Clock` przez `ClockProvider` (`ValidationConfigurationCustomizer`), więc błąd ma standardowy format 400 `errors[{field:"issueDate"}]`, a testy używają `Clock.fixed`. Porównanie z alternatywą (sprawdzenie w serwisie) jest w `DECISIONS.md`.
