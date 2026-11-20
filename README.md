# RouteCost

Aplikacja webowa do zarządzania pojazdami, rejestrowania kosztów ich eksploatacji i pilnowania terminów obsługi. Frontend React komunikuje się z backendem Spring Boot przez REST API; PostgreSQL przechowuje dane, a RabbitMQ obsługuje asynchroniczne powiadomienia.

## Spis treści

- [O projekcie](#o-projekcie)
- [Funkcjonalności](#funkcjonalności)
- [Technologie](#technologie)
- [Architektura](#architektura)
- [Migracje Flyway](#migracje-flyway)
- [Struktura projektu](#struktura-projektu)
- [Model danych](#model-danych)
- [Diagram ERD](#diagram-erd)
- [Uwierzytelnianie i bezpieczeństwo](#uwierzytelnianie-i-bezpieczeństwo)
- [Reguły biznesowe](#reguły-biznesowe)
- [Asynchroniczne przypomnienia](#asynchroniczne-przypomnienia)
- [REST API i Swagger](#rest-api-i-swagger)
- [Wymagania środowiskowe](#wymagania-środowiskowe)
- [Lokalne uruchomienie bez Dockera](#lokalne-uruchomienie-bez-dockera)
- [Uruchomienie przez Docker Compose](#uruchomienie-przez-docker-compose)
- [Zmienne środowiskowe](#zmienne-środowiskowe)
- [Testy](#testy)
- [Dane demonstracyjne](#dane-demonstracyjne)

## O projekcie

RouteCost porządkuje informacje o eksploatacji pojazdu, które często są rozproszone między notatkami i rachunkami. Użytkownik prowadzi historię tankowań, wydatków i terminów obsługi, a następnie analizuje statystyki i koszty utrzymania.

## Funkcjonalności

- Rejestracja, logowanie, odtwarzanie sesji i wylogowanie.
- Obsługa kont USER i ADMIN oraz administracja kontami.
- Zarządzanie pojazdami i ich szczegółami.
- Historia tankowań, statystyki paliwa i spalanie liczone między pełnymi tankowaniami.
- Historia wydatków z filtrowaniem według kategorii i dat oraz statystykami.
- Przypomnienia serwisowe, zmiana statusu, filtrowanie i ponowna aktywacja.
- Dashboard, wykresy i raport kosztów dla pojazdu.
- Powiadomienia w aplikacji o nadchodzących i zaległych terminach.

## Technologie

| Obszar | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 4.0.9, Spring MVC, Spring Security, Spring Data JPA/Hibernate, Bean Validation, Maven |
| API | REST, Springdoc OpenAPI 3.0.1 |
| Baza i migracje | PostgreSQL 17, Flyway |
| Komunikacja asynchroniczna | RabbitMQ 4, Spring AMQP |
| Frontend | React 19, TypeScript, Vite, React Router, Axios, TanStack Query, React Hook Form, Zod, Recharts, Lucide React |
| Testy | JUnit, Spring Boot Test, Vitest, Testing Library |
| Uruchamianie | Docker, Docker Compose |

## Architektura

Frontend jest aplikacją SPA i wywołuje endpointy REST backendu. Backend dzieli kod na moduły domenowe; typowy przepływ żądania przechodzi przez kontroler, serwis i repozytorium JPA do PostgreSQL. Flyway utrzymuje schemat bazy migracjami V1–V8. Scheduler i komponenty Spring AMQP realizują osobny przepływ zdarzeń przypomnień.

```mermaid
flowchart LR
    B[Przeglądarka] -->|pobiera aplikację| F[Frontend React<br/>renderowanie UI]
    F <-->|REST API<br/>HTTP/JSON| API[Backend Spring Boot<br/>kontrolery → serwisy → repozytoria]
    API <-->|JPA| DB[(PostgreSQL 17<br/>Flyway V1–V8)]
    API -->|publikacja zdarzeń| MQ[(RabbitMQ 4)]
    MQ -->|zdarzenia do konsumenta| API
    API -->|zapis przetworzonych powiadomień| DB
```

W Compose backend, frontend, PostgreSQL i RabbitMQ działają jako osobne usługi. Backend uruchamia się po uzyskaniu statusu healthy przez PostgreSQL i RabbitMQ. Baza korzysta z nazwanego wolumenu Compose.

## Migracje Flyway

Flyway zarządza schematem PostgreSQL. Pliki migracji SQL znajdują się w `backend/src/main/resources/db/migration/` i są wersjonowane kolejno od V1 do V8. Przy starcie backendu Spring Boot uruchamia Flyway automatycznie (`spring.flyway.enabled=true`), zanim aplikacja zacznie korzystać ze schematu; Hibernate sprawdza zgodność mapowania przez `spring.jpa.hibernate.ddl-auto=validate`.

Na nowej, pustej bazie Flyway tworzy schemat, wykonując dostępne migracje w kolejności. W typowym uruchomieniu nie trzeba ręcznie wywoływać migracji. Zastosowanych plików migracji nie należy edytować; kolejne zmiany schematu powinny być wprowadzane jako nowa migracja.

## Struktura projektu

```text
RouteCost/
├── backend/
│   └── src/
│       ├── main/java/pl/routecost/       # moduły domenowe backendu
│       ├── main/resources/db/migration/  # migracje Flyway V1–V8
│       └── test/java/pl/routecost/       # testy backendowe
├── frontend/
│   └── src/
│       ├── api/                          # klient HTTP i obsługa błędów
│       ├── features/                     # moduły widoków i funkcji
│       ├── routes/                       # routing i zabezpieczone trasy
│       └── components/                   # współdzielone komponenty
├── docs/erd/routecost.dbml               # źródło diagramu ERD
├── docker-compose.yml
└── README.md
```

Moduły backendu obejmują m.in. `admin`, `dashboard`, `expense`, `fuel`, `notification`, `reminder`, `user` i `vehicle`. Frontend grupuje widoki według tych samych obszarów funkcjonalnych.

## Model danych

Schemat końcowy definiowany przez migracje Flyway V1–V8 zawiera osiem tabel:

| Encja | Rola w modelu |
|---|---|
| `users` | Konto, rola i stan aktywności użytkownika. |
| `vehicles` | Pojazd należący do użytkownika. |
| `fuel_entries` | Tankowania i dane do obliczeń paliwa. |
| `expenses` | Wydatki związane z pojazdem, w PLN. |
| `reminders` | Terminy i statusy przypomnień dla pojazdu. |
| `reminder_notifications` | Trwałe zdarzenia/outbox powiadomień. |
| `refresh_tokens` | Skróty tokenów odświeżających i ich rodziny. |
| `user_notifications` | Powiadomienia użytkownika wraz ze stanem odczytu. |

Użytkownik może mieć wiele pojazdów i tokenów odświeżających. Pojazd grupuje tankowania, wydatki i przypomnienia. Przypomnienie może wygenerować zdarzenie outbox, a powiadomienie użytkownika wskazuje źródłowe zdarzenie.
## Diagram ERD

Diagram jest zgodny ze schematem po migracjach Flyway V1–V8. Jego źródło DBML znajduje się w [docs/erd/routecost.dbml](docs/erd/routecost.dbml).

![Diagram ERD RouteCost](docs/erd/routecost-erd.svg)

## Uwierzytelnianie i bezpieczeństwo

- Hasła są przechowywane jako skróty BCrypt.
- Access token JWT jest ważny 15 minut i przesyłany w `Authorization: Bearer <token>`.
- Refresh token jest losowy; baza przechowuje jego SHA-256. Token przesyłany jest w cookie `HttpOnly`, którego odświeżenie rotuje token. Wykrycie ponownego użycia unieważnionego tokenu unieważnia jego aktywną rodzinę.
- Frontend przechowuje access token w pamięci i współdzieli odświeżenie sesji dla równoczesnych odpowiedzi 401.
- Publiczna rejestracja nadaje rolę USER. Endpointy `/api/admin/**` wymagają roli ADMIN.
- Dostęp do pojazdów i powiązanych danych jest ograniczony do właściciela; rola ADMIN nie omija tej kontroli.
- Rola i status konta są sprawdzane z bazą podczas uwierzytelniania żądań. Dezaktywacja konta blokuje dostęp i unieważnia aktywne tokeny odświeżające.
- Odświeżenie i wylogowanie wymagają cookie z `/api/auth` oraz nagłówka `X-RouteCost-Auth: 1`. CORS z credentials jest ograniczony do skonfigurowanego originu.

## Reguły biznesowe

- Tankowania nie mogą poprzedzać zakupu pojazdu ani naruszać chronologii przebiegu.
- Spalanie wyliczane jest dla odcinków między kolejnymi pełnymi tankowaniami; paliwo z tankowań pośrednich sumuje się do następnego pełnego tankowania.
- Koszt tankowania i kwota wydatku muszą być dodatnie. Wydatki są zapisywane w PLN.
- Przebieg podany przy tankowaniu lub wydatku podlega kontroli chronologii pojazdu.
- Data przypomnienia nie może poprzedzać daty zakupu pojazdu. Reaktywacja zakończonego lub anulowanego przypomnienia zwiększa jego wersję aktywacji.
- Operacje na pojazdach i zasobach zależnych sprawdzają właściciela.
- Administrator nie może zdezaktywować ani zdegradować własnego konta ani odebrać uprawnień ostatniemu aktywnemu administratorowi.

## Asynchroniczne przypomnienia

1. Scheduler skanuje aktywne przypomnienia domyślnie co 60 sekund, uwzględniając terminy z najbliższych 7 dni i terminy zaległe.
2. Serwis zapisuje zdarzenia ze statusem `PENDING` w tabeli `reminder_notifications`. Ograniczenie unikalności generacji zapobiega tworzeniu tego samego zdarzenia ponownie.
3. Publisher wysyła identyfikator zdarzenia do RabbitMQ i po publisher confirm oznacza rekord jako `PUBLISHED`.
4. Konsument sprawdza, czy przypomnienie jest nadal aktywne i czy wersja aktywacji się zgadza. W transakcji tworzy `user_notifications` i oznacza zdarzenie jako `PROCESSED`; nieaktualne zdarzenie otrzymuje status `DISCARDED`.
5. Ponowne dostarczenie zdarzenia jest obsługiwane idempotentnie; unikalność `user_notifications.reminder_notification_id` zabezpiecza przed duplikatem.
6. Listener wykonuje do trzech prób z przerwami 250 ms i 500 ms. Po ich wyczerpaniu wiadomość trafia do dead-letter queue (DLQ).

Kolejka główna: `routecost.reminder.notifications.v2`; DLQ: `routecost.reminder.notifications.v2.dlq`. Zachowana jest starsza kolejka `routecost.reminder.notifications` do obsługi pozostałych wiadomości. Zapis w bazie i publikacja do RabbitMQ nie są jedną transakcją rozproszoną; ponowne dostarczenia zabezpiecza idempotentne przetwarzanie. Użytkownik przegląda swoje powiadomienia w aplikacji, sprawdza licznik nieprzeczytanych i oznacza je jako przeczytane.

## REST API i Swagger

Po uruchomieniu backendu dokumentacja dostępna jest pod adresem [Swagger UI](http://localhost:8081/swagger-ui.html); OpenAPI JSON: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs). Dla chronionych endpointów należy przekazać access token w nagłówku Bearer. Rejestracja, logowanie, odświeżenie i wylogowanie są ścieżkami uwierzytelniania; pozostałe endpointy wymagają uwierzytelnienia, a `/api/admin/**` dodatkowo roli ADMIN.

Poniższa tabela jest skróconym zestawieniem najważniejszych tras, a nie pełnym opisem kontraktu. Pełne parametry, modele żądań i odpowiedzi oraz możliwość wywoływania endpointów z poziomu Swagger UI są dostępne w interaktywnej dokumentacji i OpenAPI JSON. Chronione operacje autoryzuje się access tokenem jako `Bearer`; endpointy administracyjne wymagają roli `ADMIN`. Rejestracja, logowanie, odświeżenie i wylogowanie są publicznymi trasami auth, przy czym odświeżenie i wylogowanie dodatkowo korzystają z refresh cookie oraz nagłówka `X-RouteCost-Auth: 1`.

| Obszar | Główne endpointy |
|---|---|
| Uwierzytelnianie | `POST /api/auth/register`, `/login`, `/refresh`, `/logout`; `GET /api/auth/me` |
| Pojazdy | `GET/POST /api/vehicles`; `GET/PUT/DELETE /api/vehicles/{vehicleId}` |
| Tankowania | `GET/POST /api/vehicles/{vehicleId}/fuel-entries`; `GET/PUT/DELETE /api/vehicles/{vehicleId}/fuel-entries/{entryId}`; `GET /api/vehicles/{vehicleId}/fuel-statistics` |
| Wydatki | `GET/POST /api/vehicles/{vehicleId}/expenses`; `GET/PUT/DELETE /api/vehicles/{vehicleId}/expenses/{expenseId}`; `GET /api/vehicles/{vehicleId}/expense-statistics` |
| Przypomnienia | `GET/POST /api/vehicles/{vehicleId}/reminders`; `GET/PUT/DELETE /api/vehicles/{vehicleId}/reminders/{reminderId}`; `PATCH .../{reminderId}/status`; `POST .../{reminderId}/reactivate` |
| Dashboard | `GET /api/dashboard/summary`; `GET /api/vehicles/{vehicleId}/cost-summary` |
| Administracja | `GET /api/admin/users`, `/api/admin/dashboard`; `PATCH /api/admin/users/{userId}/role`, `/status` |
| Powiadomienia | `GET /api/notifications`, `/unread-count`; `PATCH /api/notifications/{notificationId}/read`, `/read-all` |

Przykład sprawdzenia endpointu publicznej dokumentacji:

```bash
curl http://localhost:8081/v3/api-docs
```

Przykład wywołania wymagającego tokenu:

```bash
curl -H "Authorization: Bearer <access-token>" \
  "http://localhost:8081/api/vehicles"
```

### Przykładowa odpowiedź błędu

Niepoprawne pole w żądaniu obsłużonym przez Bean Validation zwraca HTTP `400 Bad Request`. `ApiExceptionHandler` ustawia tytuł i opis problemu oraz mapę `errors` z nazwami pól i komunikatami walidacji. Przykładowy kształt odpowiedzi:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more request fields are invalid.",
  "errors": {
    "email": "must be a well-formed email address"
  }
}
```

Wartości w `errors` zależą od pola i naruszonego ograniczenia. Dla błędów domenowych handler zwraca odpowiedni kod HTTP oraz opis w `detail`, np. `404` dla nieznanego zasobu, `409` dla konfliktu stanu lub `400` dla błędu chronologii.

## Wymagania środowiskowe

Do uruchomienia w kontenerach wymagane są Docker Engine/Desktop z Docker Compose oraz wolne porty `5432`, `5672`, `15672`, `8081` i `5174` (lub porty ustawione w `.env`). Aplikacja nie wymaga lokalnej instalacji Javy ani Node.js przy uruchomieniu Compose. Przy pracy bez kontenerów potrzebne są dodatkowo JDK 21, Node.js/npm, PostgreSQL 17 i RabbitMQ 4.

## Uruchomienie aplikacji przez Docker Compose

Poniższa instrukcja opisuje uruchomienie całego projektu RouteCost w środowisku lokalnym za pomocą Docker Compose. Nie trzeba osobno instalować ani konfigurować PostgreSQL, RabbitMQ, backendu ani frontendu — wszystkie usługi zostaną uruchomione w kontenerach.

### Krok 1. Przygotowanie środowiska

Przed rozpoczęciem upewnij się, że masz zainstalowane:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) z obsługą Docker Compose,
- Git — jeżeli pobierasz projekt bezpośrednio z repozytorium.

Uruchom Docker Desktop i poczekaj, aż silnik Docker będzie gotowy.

### Krok 2. Pobranie projektu i przejście do katalogu głównego

Sklonuj repozytorium:

```powershell
git clone https://github.com/XPiraniaX/RouteCost.git
```

Przejdź do katalogu projektu:

```powershell
cd RouteCost
```

Wszystkie kolejne polecenia z tej instrukcji należy wykonywać w katalogu głównym repozytorium, w którym znajduje się plik `compose.yaml` lub `docker-compose.yml`.

### Krok 3. Przygotowanie konfiguracji środowiskowej

Utwórz lokalny plik `.env` na podstawie dostarczonego przykładu:

```powershell
Copy-Item .env.example .env
```

Otwórz plik `.env` w edytorze tekstu i uzupełnij wymagane wartości:

- `DB_PASSWORD` — hasło użytkownika PostgreSQL,
- `RABBITMQ_PASSWORD` — hasło użytkownika RabbitMQ,
- `JWT_SECRET_BASE64` — sekret wykorzystywany do podpisywania tokenów JWT.

Pozostałe ustawienia mogą pozostać domyślne, jeżeli nie ma potrzeby zmiany portów lub parametrów usług.

Sekret JWT musi być losowym kluczem o długości co najmniej 256 bitów, zakodowanym w Base64. Można go wygenerować w PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
$rng.Dispose()
$secret
```

Wygenerowaną wartość skopiuj do `JWT_SECRET_BASE64` w pliku `.env`.

**Ważne:** plik `.env` zawiera lokalne sekrety i nie powinien być publikowany ani dodawany do repozytorium Git. Nie należy również używać przykładowych haseł z `.env.example` jako haseł do rzeczywistego wdrożenia.

### Krok 4. Budowanie obrazów i uruchomienie usług

W katalogu głównym projektu wykonaj:

```powershell
docker compose up --build -d
```

Polecenie zbuduje obrazy aplikacji, pobierze wymagane obrazy usług i uruchomi kontenery w tle.

Pierwsze uruchomienie może potrwać kilka minut, ponieważ Docker musi pobrać zależności i zbudować backend oraz frontend.

Sprawdź stan kontenerów:

```powershell
docker compose ps
```

Poczekaj, aż usługi zakończą inicjalizację. Sam fakt uruchomienia kontenera nie oznacza jeszcze, że aplikacja jest gotowa do obsługi żądań.

### Krok 5. Sprawdzenie działania aplikacji

Po uruchomieniu usług otwórz w przeglądarce:

| Usługa | Adres |
|---|---|
| Frontend RouteCost | http://localhost:5174 |
| Backend | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| OpenAPI JSON | http://localhost:8081/v3/api-docs |
| RabbitMQ Management | http://localhost:15672 |

Dodatkowo usługi udostępniają porty:

- PostgreSQL: `localhost:5432`
- RabbitMQ AMQP: `localhost:5672`

Panel RabbitMQ Management wymaga zalogowania danymi ustawionymi w `RABBITMQ_USERNAME` i `RABBITMQ_PASSWORD` w pliku `.env`.

Przy pierwszym uruchomieniu backend automatycznie wykonuje migracje bazy danych za pomocą Flyway. Dopiero po zakończeniu inicjalizacji backendu można korzystać z funkcjonalności aplikacji.

Aby sprawdzić, czy backend poprawnie wystartował, można również otworzyć Swagger UI i zweryfikować dostępność dokumentacji REST API.

### Krok 6. Logi i diagnostyka

Jeżeli aplikacja nie uruchomi się poprawnie, w pierwszej kolejności sprawdź stan kontenerów:

```powershell
docker compose ps
```

Wyświetlenie logów wszystkich usług:

```powershell
docker compose logs -f
```

Wyświetlenie logów wybranej usługi:

```powershell
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
docker compose logs -f rabbitmq
```

Logi backendu są szczególnie przydatne przy problemach z połączeniem z bazą danych, RabbitMQ, konfiguracją JWT lub migracjami Flyway.

Aby zakończyć śledzenie logów, użyj `Ctrl+C`. Nie zatrzymuje to kontenerów.

### Krok 7. Zatrzymanie aplikacji

Aby zatrzymać wszystkie usługi bez usuwania zapisanych danych, wykonaj:

```powershell
docker compose down
```

Przy kolejnym uruchomieniu wystarczy:

```powershell
docker compose up -d
```

Nie ma potrzeby ponownego budowania obrazów, o ile nie zmieniono kodu ani konfiguracji wymagającej przebudowania.

Jeżeli zmieniono kod aplikacji i chcesz ponownie zbudować obrazy:

```powershell
docker compose up --build -d
```

**Uwaga dotycząca danych:** PostgreSQL przechowuje dane w wolumenie Docker `routecost_postgres_data`. Zwykłe `docker compose down` nie usuwa tego wolumenu, więc dane pozostają zachowane pomiędzy uruchomieniami.

Polecenie:

```powershell
docker compose down -v
```

usuwa również wolumeny, w tym `routecost_postgres_data`, a tym samym wszystkie dane zapisane w lokalnej bazie PostgreSQL. Używaj go wyłącznie wtedy, gdy świadomie chcesz wyczyścić lokalne środowisko i rozpocząć pracę od pustej bazy.

### Adresy i porty usług

Domyślne porty publikowane na komputerze użytkownika:

| Usługa | Port |
|---|---:|
| Frontend | `5174` |
| Backend | `8081` |
| PostgreSQL | `5432` |
| RabbitMQ AMQP | `5672` |
| RabbitMQ Management | `15672` |

Porty frontend i backend można zmienić za pomocą zmiennych `FRONTEND_PORT` i `BACKEND_PORT` w pliku `.env`.

## Zmienne środowiskowe

Konfiguracja aplikacji jest przekazywana za pomocą zmiennych środowiskowych. Przykładowe wartości i dostępne ustawienia znajdują się w pliku `.env.example`.

Poniższa tabela przedstawia najważniejsze zmienne wykorzystywane przez Docker Compose:

| Zmienna | Znaczenie |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | Dane logowania do PostgreSQL. |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Dane logowania do RabbitMQ. |
| `JWT_SECRET_BASE64` | Sekret wykorzystywany do podpisywania tokenów JWT, zakodowany w Base64. |
| `REFRESH_TOKEN_EXPIRATION` | Czas ważności refresh tokenu; domyślnie `P7D`. |
| `REFRESH_COOKIE_NAME`, `REFRESH_COOKIE_SECURE`, `REFRESH_COOKIE_SAME_SITE` | Nazwa i atrybuty cookie wykorzystywanego do odświeżania tokenu. `SameSite=None` wymaga `Secure=true`. |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD`, `INITIAL_ADMIN_FIRST_NAME`, `INITIAL_ADMIN_LAST_NAME` | Opcjonalne dane początkowego administratora. |
| `BACKEND_PORT`, `FRONTEND_PORT` | Porty publikowane na hoście; domyślnie odpowiednio `8081` i `5174`. |
| `SPRING_PROFILES_ACTIVE` | Aktywny profil Spring Boot; Compose domyślnie ustawia `dev`. |

Wewnątrz sieci Docker Compose backend łączy się z PostgreSQL i RabbitMQ za pomocą nazw usług `postgres` i `rabbitmq`.

Przy uruchamianiu backendu poza Docker Compose można dodatkowo skonfigurować m.in. `DATABASE_URL`, `RABBITMQ_HOST` i `RABBITMQ_PORT`.

Dla lokalnego środowiska HTTP atrybut `Secure` cookie jest domyślnie wyłączony. W środowisku wdrożeniowym korzystającym z HTTPS należy odpowiednio skonfigurować `REFRESH_COOKIE_SECURE=true` oraz pozostałe atrybuty cookie.

Szczegółowe wartości domyślne i pozostałe dostępne parametry należy sprawdzić w pliku `.env.example` oraz konfiguracji aplikacji.

## Testy

Projekt zawiera testy jednostkowe i integracyjne backendu oraz testy jednostkowe frontendu. Dodatkowo dostępne są narzędzia do analizy statycznej kodu i budowania wersji produkcyjnej.

### Wymagania środowiskowe

Do uruchomienia pełnej weryfikacji projektu wymagane są:

- JDK 21,
- Maven Wrapper (dostarczony w repozytorium),
- Node.js i npm,
- Docker Desktop z działającym Docker Engine.

Zależności frontendu należy wcześniej zainstalować za pomocą `npm ci`. Backend wykorzystuje Testcontainers, które automatycznie uruchamiają izolowane kontenery PostgreSQL 17 i RabbitMQ 4. Nie ma potrzeby ręcznego przygotowywania bazy danych ani brokera.

### Weryfikacja całego projektu

Najprostszym sposobem sprawdzenia poprawności projektu jest uruchomienie skryptu `test-all.ps1` z katalogu głównego repozytorium:

```powershell
.\test-all.ps1
```

Skrypt kolejno:
1. Sprawdza dostępność wymaganych narzędzi i środowiska.
2. Uruchamia testy backendu.
3. Uruchamia testy jednostkowe frontendu.
4. Wykonuje analizę statyczną kodu (ESLint).
5. Buduje produkcyjną wersję frontendu.

Wyniki poszczególnych etapów są prezentowane niezależnie. Niepowodzenie jednego etapu nie przerywa pozostałych. Skrypt zwraca kod wyjścia `0` wyłącznie wtedy, gdy wszystkie etapy zakończą się sukcesem.

### Testy backendu

Backend wykorzystuje JUnit 5, Spring Boot Test oraz Testcontainers. Testy znajdują się w katalogu `backend/src/test/java`.

Zakres testów obejmuje między innymi:
- obliczanie zużycia paliwa i kosztów eksploatacji;
- walidację danych i reguły domenowe;
- obsługę przypomnień i powiadomień;
- uwierzytelnianie i autoryzację;
- integracyjne testy REST API;
- komunikację z RabbitMQ, przetwarzanie wiadomości i obsługę kolejki błędów (DLQ).

**Samodzielne uruchomienie**

Z katalogu `backend`:

```powershell
.\mvnw.cmd test
```

Podczas uruchamiania Testcontainers automatycznie przygotowuje tymczasowe środowisko testowe obejmujące PostgreSQL 17 i RabbitMQ 4. Kontenery są współdzielone przez testy integracyjne w ramach jednego uruchomienia Maven i sprzątane po zakończeniu testów.

Migracje Flyway są wykonywane automatycznie na testowej bazie danych podczas inicjalizacji kontekstu Spring. Hibernate weryfikuje strukturę bazy za pomocą `ddl-auto=validate`. Profil `test` wyłącza seeder demonstracyjny oraz scheduler przypomnień, pozostawiając aktywną konfigurację RabbitMQ wymaganą przez testy integracyjne.

Wyniki testów znajdują się w katalogu:

`backend/target/surefire-reports/`

### Testy frontendu

Frontend wykorzystuje Vitest, jsdom i Testing Library. Pliki testowe `*.test.*` znajdują się w katalogu `frontend/src`, obok testowanego kodu.

Zakres testów obejmuje między innymi:
- routing i nawigację;
- formularze i walidację;
- komunikację z API;
- widoki funkcjonalne;
- narzędzia pomocnicze.

**Instalacja zależności**

Z katalogu `frontend`:

```powershell
npm ci
```

**Samodzielne uruchomienie weryfikacji**

```powershell
npm run verify
```

Polecenie uruchamia kolejno:
1. Testy jednostkowe (Vitest).
2. Analizę statyczną kodu (ESLint).
3. Budowanie produkcyjne (Vite).

Wszystkie etapy są wykonywane niezależnie, również w przypadku niepowodzenia wcześniejszego etapu. Skrypt zwraca niezerowy kod wyjścia, jeżeli którykolwiek z nich zakończy się błędem.

Jeżeli polityka wykonywania skryptów PowerShell blokuje `npm.ps1`, można użyć równoważnego polecenia:

```powershell
npm.cmd run verify
```

### Dodatkowe informacje

- Testy backendu i frontendu można uruchamiać niezależnie.
- Testcontainers

## Dane demonstracyjne

Lokalne środowisko developerskie tworzy trzy przykładowe konta użytkowników. Każde ma rolę `USER`, jest aktywne i używa hasła zahashowanego istniejącym mechanizmem BCrypt.

| Konto | E-mail | Hasło | Zawartość i zastosowanie |
|---|---|---|---|
| EMPTY | `demo.empty@routecost.local` | `RouteCostDemo1!` | Jedna Toyota Corolla, bez tankowań, wydatków i przypomnień. Do sprawdzania pustych stanów i pierwszego dodawania danych. |
| STANDARD | `demo.standard@routecost.local` | `RouteCostDemo2!` | Jedna Skoda Octavia, 6 tankowań, 5 wydatków z różnych kategorii i 3 przypomnienia: zaległe aktywne, nadchodzące aktywne oraz zakończone. Do typowego scenariusza użytkownika. |
| FULL | `demo.full@routecost.local` | `RouteCostDemo3!` | Toyota Corolla (11 tankowań i 11 wydatków) oraz Kia Ceed (7 tankowań i 7 wydatków), łącznie 18 tankowań i 18 wydatków z różnych kategorii oraz 8 przypomnień o statusach aktywnych, zakończonych i anulowanych. Historia przekracza 10 wpisów dla pierwszego pojazdu, więc pokazuje paginację; daty obejmują wiele miesięcy. Do testowania dashboardu, statystyk, kosztów, spalania, filtrów, paginacji i wykresów. |

Konta są przeznaczone wyłącznie do lokalnego środowiska demonstracyjnego. Seed jest idempotentny: przy każdym starcie sprawdza, czy konto już istnieje; istniejących kont ani ich danych nie zmienia i nie uzupełnia. Hasła nie są wypisywane w logach. Seed działa wyłącznie przy aktywnym profilu Spring `dev` i jest wyłączony, gdy aktywny jest profil `prod` — również wtedy, gdy omyłkowo aktywowano oba profile. Nie powinien być włączany w publicznym wdrożeniu.

Standardowe uruchomienie lokalnego środowiska Docker Compose aktywuje profil `dev` dla backendu:

```bash
docker compose up --build -d
```

Aby uruchomić Compose bez seeda (na przykład przy lokalnym sprawdzaniu konfiguracji produkcyjnej), ustaw profil inny niż `dev`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
docker compose up --build -d
```

Przed kolejnym uruchomieniem developerskim przywróć profil `dev` albo usuń zmienną `SPRING_PROFILES_ACTIVE` z bieżącej sesji PowerShell. Samo zatrzymanie kontenerów nie usuwa bazy ani kont demonstracyjnych.
