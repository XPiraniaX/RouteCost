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
