# RouteCost — frontend, etap 2

## Uruchomienie

Z katalogu głównego repozytorium:

```powershell
# Przy pierwszym uruchomieniu skopiuj .env.example do .env i uzupełnij sekrety.
docker compose up -d --build
```

Domyślne adresy: frontend http://localhost:5174, API http://localhost:8081.
Porty można ustawić przez `FRONTEND_PORT` i `BACKEND_PORT` w głównym `.env`.
Zatrzymanie: `docker compose stop` (zachowuje dane).

Frontend lokalnie, przy działającym backendzie:

```powershell
cd frontend
npm.cmd ci
# W .env.local ustaw VITE_API_BASE_URL=http://localhost:8081 dla backendu z Compose.
# Dla backendu uruchomionego lokalnie na 8080 użyj http://localhost:8080.
npm.cmd run dev
```

Origin frontendu musi być dozwolony w konfiguracji CORS backendu.
`npm.cmd` omija problem blokowania skryptu `npm.ps1` przez politykę PowerShell.
Na Linux/macOS można użyć `npm`.

## Funkcjonalności i projekt wizualny

- Ciemny layout zgodny z istniejącymi ekranami autoryzacji; lokalny font Inter,
  ikony Lucide, stonowany zielony akcent, wspólne tokeny, hierarchia typografii
  i odstępy oparte na 4 px.
- Sidebar, aktywna nawigacja, dane użytkownika, wylogowanie i mobilne menu
  z obsługą klawiatury, Escape oraz przywracaniem focusu.
- Dashboard: pięć liczników API, powitanie, szybkie dodawanie i pięć ostatnich
  pojazdów. Tankowania, wydatki i przypomnienia w nawigacji są wyłączone.
- Lista pojazdów: paginacja serwerowa (8 na stronę), filtr paliwa, szczegóły
  i edycja. Dodawanie i edycja używają tego samego formularza.
- Walidacja Zod / React Hook Form odpowiada DTO, w tym zakresom liczb,
  dacie zakupu, precyzji ceny i zakazowi cofania przebiegu. Błędy pól zwrócone
  przez API pojawiają się przy polach.
- Usuwanie z natywnym dialogiem i informacją o trwałym usunięciu danych
  powiązanych. TanStack Query odświeża pojazdy, raporty i dashboard po mutacjach.
- Raport pojazdu: dane podstawowe, koszt i liczba tankowań, dostępne średnie
  spalanie, wydatki według walut i kategorii oraz miesięczne wykresy Recharts.
  Raport jest ładowany osobno, po wejściu w szczegóły pojazdu.
- Paliwo i cena zakupu pozostają bez waluty zgodnie z kontraktem API.
  Pozostałe wydatki mają osobne zestawienia i wykresy dla każdej waluty.
  Brak danych spalania ma opisowy komunikat. Nie są generowane przykładowe dane.
- Wspólne stany puste, skeletony, błędy z ponowieniem, komunikaty sukcesu,
  focus, ograniczenie animacji i tabelaryczny odczyt danych wykresów.
- Formularze przechodzą do jednej kolumny na telefonie; tabele mają własny
  obszar przewijania, aby nie rozszerzać całej strony.

## Kontrakty

Wykorzystano istniejący Axios, interceptor Bearer/401, AuthProvider i QueryClient.
Token pozostaje w pamięci, jak w etapie 1; odświeżenie strony wymaga logowania.

| Operacja | Endpoint |
| --- | --- |
| Dashboard | `GET /api/dashboard/summary` |
| Lista | `GET /api/vehicles?page=0&size=8&fuelType=PETROL` |
| Szczegóły | `GET /api/vehicles/{id}` |
| Dodawanie | `POST /api/vehicles` |
| Edycja | `PUT /api/vehicles/{id}` |
| Usuwanie | `DELETE /api/vehicles/{id}` |
| Raport | `GET /api/vehicles/{id}/cost-summary` |

Filtr jest opcjonalny. Paliwa: PETROL, DIESEL, LPG, HYBRID, ELECTRIC, OTHER.
Nie zmieniono backendu, kontraktów, migracji ani struktury backend/frontend.

## Weryfikacja

```powershell
cd frontend
npm.cmd run lint
npm.cmd run build
npm.cmd test
npm.cmd audit --omit=dev
```

Wynik: 31/31 testów Vitest. Testy obejmują chronione ścieżki, walidację, dashboard, filtr i paginację,
ponawianie błędnego żądania, dodawanie, edycję, potwierdzenie usuwania,
odświeżanie cache, błędy pól, rozdzielenie walut i niedostępne spalanie.
Testy komponentów używają mocków HTTP; nie zastępują testów przeglądarkowych.

13.11.2026 uruchomiono Compose i wykonano dodatkowe żądania do rzeczywistego API:

- logowanie, dashboard, CRUD, filtr paliwa i paginacja;
- `401` bez sesji, `400` przy cofnięciu przebiegu, `404` po usunięciu;
- wzrost i powrót licznika dashboardu po dodaniu/usunięciu pojazdu;
- pusty raport i brak dostępnego spalania;
- raport z dwoma rzeczywistymi wpisami testowymi paliwa: koszt 540 bez waluty,
  dwa miesiące i spalanie 8 l/100 km;
- wydatki testowe 100 PLN i 50 EUR: osobne sumy, kategorie i miesiące.

Pojazdy testowe oraz powiązane dane usunięto po weryfikacji.

Ograniczenia: automatyzacja przeglądarki została zatrzymana przez narzędzie,
ponieważ nie mogło wiarygodnie ustalić bieżącego URL w Windows. Nie wykonano
pełnej kontroli wizualnej widoków w przeglądarce ani na fizycznych urządzeniach.
Przed odbiorem wizualnym należy przejrzeć dashboard, listę, formularz, szczegóły,
wykresy i dialog przy szerokościach 1440, 1024, 768 i 390 px, także dla długich
nazw, stanów pustych, błędów i ładowania.

Audyt zależności produkcyjnych: 0 podatności. Instalacja zgłosiła 10 podatności
w zależnościach deweloperskich (8 moderate, 2 critical); nie stosowano
`npm audit fix --force`. Build może zgłaszać ostrzeżenie rozmiaru głównego
chunka oraz komentarzy PURE w zewnętrznym Zod; nie są to błędy kompilacji.

## Zmienione pliki

- `package.json`, `package-lock.json`: Recharts, lokalny Inter.
- `src/App.tsx`, `src/main.tsx`: trasy i wspólne style/fonty.
- `src/design-tokens.css`, `src/app.css`: tokeny i responsywny interfejs.
- `src/layouts/AppLayout.tsx`: główny layout.
- `src/components/ui.tsx`, `src/utils/format.ts`: wspólne komponenty i formatowanie.
- `src/features/dashboard/api.ts`, `DashboardPage.tsx`: dashboard.
- `src/features/vehicles/api.ts`: typy i dostęp do API.
- `src/features/vehicles/vehicleSchema.ts`: walidacja.
- `src/features/vehicles/VehiclesPage.tsx`, `VehicleTable.tsx`: lista.
- `src/features/vehicles/VehicleFormPage.tsx`: dodawanie i edycja.
- `src/features/vehicles/VehicleDetailPage.tsx`, `CostReport.tsx`: szczegóły i raport.
- `src/features/vehicles/DeleteVehicleDialog.tsx`: potwierdzenie usuwania.
- `src/features/vehicles/vehicleSchema.test.ts`, `vehicles.test.tsx`,
  `src/routes/ProtectedRoute.test.tsx`: testy.
- `src/pages/HomePage.tsx`: usunięty placeholder, zastąpiony dashboardem.
- `README.md`: instrukcja i raport etapu.
