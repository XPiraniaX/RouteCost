# RouteCost

**RouteCost** to aplikacja webowa służąca do zarządzania kosztami eksploatacji pojazdów. Umożliwia rejestrowanie tankowań, wydatków, serwisów i przypomnień, a także analizowanie kosztów utrzymania samochodu.

## Technologie

### Backend
- Java 21
- Spring Boot
- Spring Security
- JWT – uwierzytelnianie i autoryzacja
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway – migracje bazy danych
- RabbitMQ – obsługa zadań asynchronicznych
- Maven

### Frontend
- React
- TypeScript
- Vite
- React Router
- Axios
- TanStack Query
- React Hook Form
- Zod
- Tailwind CSS
- Recharts

### Infrastruktura
- Docker
- Docker Compose
- PostgreSQL
- RabbitMQ

## Funkcjonalności

- Rejestracja i logowanie użytkowników.
- Uwierzytelnianie za pomocą JWT oraz odświeżanie sesji.
- Zarządzanie pojazdami.
- Rejestrowanie tankowań i obliczanie zużycia paliwa.
- Rejestrowanie wydatków związanych z pojazdami.
- Analiza kosztów eksploatacji.
- Przypomnienia dotyczące obsługi i terminów związanych z pojazdem.
- Obsługa zadań asynchronicznych z wykorzystaniem RabbitMQ.
- System uprawnień oparty na rolach USER i ADMIN.

## Konfiguracja

1. Sklonuj repozytorium:

```bash
git clone <URL_REPOZYTORIUM>
cd RouteCost
```

2. Utwórz lokalny plik konfiguracyjny na podstawie przykładowego:

**Windows PowerShell:**
```powershell
Copy-Item .env.example .env
```

3. Otwórz plik `.env` i sprawdź konfigurację środowiska.

Opcjonalnie można skonfigurować początkowe konto administratora za pomocą kompletu zmiennych:

- `INITIAL_ADMIN_EMAIL`
- `INITIAL_ADMIN_PASSWORD`
- `INITIAL_ADMIN_FIRST_NAME`
- `INITIAL_ADMIN_LAST_NAME`

Jeśli zmienne nie zostaną ustawione, aplikacja nie utworzy początkowego konta administratora.

## Uruchomienie aplikacji

W katalogu głównym projektu wykonaj:

```bash
docker compose up --build -d
```

Docker Compose uruchomi wymagane usługi:

- frontend,
- backend,
- PostgreSQL,
- RabbitMQ.

### Sprawdzenie stanu kontenerów

```bash
docker compose ps
```

### Wyświetlanie logów

Wszystkie usługi:

```bash
docker compose logs -f
```

Backend:

```bash
docker compose logs -f backend
```

Frontend:

```bash
docker compose logs -f frontend
```

## Dostęp do aplikacji

Po prawidłowym uruchomieniu aplikacja powinna być dostępna pod adresami:

| Usługa | Adres |
|---|---|
| Frontend | http://localhost:5174 |
| Backend API | http://localhost:8081 |
| RabbitMQ Management | http://localhost:15672 |

Porty PostgreSQL i RabbitMQ mogą być dostępne lokalnie zgodnie z konfiguracją `docker-compose.yml`.

## Zatrzymanie aplikacji

Aby zatrzymać kontenery:

```bash
docker compose down
```

Aby zatrzymać kontenery i usunąć również dane zapisane w wolumenach:

```bash
docker compose down -v
```

**Uwaga:** polecenie `docker compose down -v` usuwa dane bazy danych zapisane w wolumenach. Używaj go tylko wtedy, gdy chcesz wyczyścić środowisko.

## Struktura projektu

```text
RouteCost/
├── backend/          # Aplikacja Spring Boot
├── frontend/         # Aplikacja React + TypeScript
├── docker-compose.yml
├── .env.example
└── README.md
```

## Baza danych

Projekt wykorzystuje PostgreSQL. Schemat bazy danych jest zarządzany przez Flyway, który automatycznie wykonuje dostępne migracje podczas uruchamiania aplikacji.

## Konta demonstracyjne

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

Przed kolejnym uruchomieniem developerskim przywróć profil `dev` albo usuń zmienną `SPRING_PROFILES_ACTIVE` z bieżącej sesji PowerShell. Samo zatrzymanie kontenerów nie usuwa bazy ani kont demonstracyjnych. Nie wykorzystuj tych danych logowania w publicznie dostępnym wdrożeniu.
