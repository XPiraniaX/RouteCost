# RouteCost — etap 3: tankowania

## Funkcjonalności

- Aktywna zakładka **Tankowania** (`/fuel`) i skrót ze szczegółów pojazdu.
- Wybór pojazdu, także spoza pierwszych 50 wyników: przycisk wczytania kolejnej
  strony pojazdów. Link z parametrem `vehicleId` otwiera wskazany pojazd.
- Historia: data, przebieg, litry (do 3 miejsc), całkowity koszt, cena za litr
  z API (do 4 miejsc), pełny zbiornik i spalanie odcinka z API.
- Paginacja po 10 wpisów, sortowanie od najnowszych/najstarszych, stabilne
  sortowanie pomocnicze po `id`. Wybór pojazdu i parametry historii są w URL.
  Zmiana pojazdu/sortowania resetuje stronę; nieistniejąca strona jest korygowana.
- Dodawanie i edycja przez wspólny formularz React Hook Form + Zod.
  Wysyłane są tylko `entryDate`, `odometer`, `liters`, `totalCost`, `fullTank`.
  Cena za litr i spalanie nie są wysyłane ani liczone przez frontend.
- Walidacja: data od zakupu do dziś, pełne nieujemne kilometry w zakresie Java
  Integer, dodatnie litry (7 cyfr całkowitych / 3 ułamkowe), dodatni koszt
  (10 cyfr całkowitych / 2 ułamkowe). Nie narzucono minimum równego bieżącemu
  przebiegowi pojazdu, ponieważ API pozwala dodawać wpisy historyczne.
- Backend ostatecznie sprawdza monotoniczność przebiegu według daty i ID,
  również przy edycji i usuwaniu. Błędy pozostawiają formularz i wpisane dane.
- Usuwanie przez wspólny natywny dialog z potwierdzeniem i obsługą błędów.
- Zapis blokuje pola i przycisk. Sukcesy pokazuje istniejący layout aplikacji.
- Po mutacjach odświeżane są klucze `fuel/{vehicleId}`, `vehicles` i `dashboard`:
  historia, szczegóły wpisów, statystyki, raporty i liczniki.
- Statystyki całej historii: średnie spalanie, liczba tankowań, suma litrów,
  koszt i liczba poprawnych interwałów. Niedostępne spalanie to **Brak danych**.
- Recharts: spalanie na odcinkach z bieżącej strony historii oraz miesięczne
  koszty całej historii. Zakresy opisano przy wykresach. Braki pozostają `null`,
  `connectNulls=false`; nie powstają sztuczne wartości ani własne obliczenia spalania.
- Tabela na telefonie przechodzi w bloki z etykietami pól. Zachowano tokeny,
  kolorystykę, typografię, skeletony, stany puste, retry, focus i wspólne komponenty.
  Wykres miesięczny ma również tabelaryczny odczyt danych.

## Kontrakty i ograniczenia

| Operacja | Endpoint |
| --- | --- |
| Historia | `GET /api/vehicles/{vehicleId}/fuel-entries` |
| Jeden wpis | `GET /api/vehicles/{vehicleId}/fuel-entries/{entryId}` |
| Dodawanie | `POST /api/vehicles/{vehicleId}/fuel-entries` |
| Edycja | `PUT /api/vehicles/{vehicleId}/fuel-entries/{entryId}` |
| Usuwanie | `DELETE /api/vehicles/{vehicleId}/fuel-entries/{entryId}` |
| Statystyki | `GET /api/vehicles/{vehicleId}/fuel-statistics` |
| Miesięczne koszty | `GET /api/vehicles/{vehicleId}/cost-summary` |

Lista wysyła `page`, `size=10` i powtórzony parametr `sort`, np.
`sort=entryDate,desc&sort=id,desc`. API **nie obsługuje zakresu dat** — formularz
filtra nie został dodany, żeby nie filtrować pozornie tylko jednej strony.

`FuelConsumptionCalculator` sumuje paliwo po poprzednim pełnym tankowaniu,
włącznie z częściowymi wpisami i kolejnym pełnym tankowaniem. Wynik wymaga
dodatniego dystansu. Średnia globalna jest ważona dystansem. Frontend pobiera
gotowe wyniki; nie powiela algorytmu.

API nie udostępnia szeregu średniej globalnej narastająco w czasie. Dlatego
wykres pokazuje **średnie na odcinkach**, nie historyczny przebieg średniej
globalnej. Nie pobiera automatycznie całej historii tylko na potrzeby wykresu.
Opcjonalna przyszła zmiana API: osobny endpoint szeregu odcinków z datą początku,
końca, dystansem i spalaniem oraz filtrem dat. Nie wdrożono tej zmiany.

Tankowania nie mają waluty. Żadne kwoty nie są oznaczane jako PLN ani łączone
z wydatkami. Bieżący przebieg pojazdu nie jest automatycznie aktualizowany przez
backendowy moduł fuel; formularz nie zmienia danych pojazdu osobnym żądaniem.

Nie zmieniono backendu, kontraktów, Flyway ani `AuthProvider`. Używane są obecne
Axios, interceptor 401, chronione trasy i QueryClient. Sesja nadal pozostaje
w pamięci, zgodnie z dotychczasowym mechanizmem autoryzacji.

## Uruchomienie i testowanie

Z katalogu głównego, przy uzupełnionym `.env`:

```powershell
docker compose up -d --build
```

Otwórz http://localhost:5174, zaloguj się i wybierz **Tankowania**. API domyślnie
działa na http://localhost:8081. Porty można zmienić w głównym `.env`.

```powershell
cd frontend
npm.cmd ci
npm.cmd run lint
npm.cmd run build
npm.cmd test
node scripts/test-fuel-integration.mjs
```

Skrypt integracyjny korzysta z rzeczywistego API. Tworzy odrębne konto testowe
z losowymi danymi i tymczasowy pojazd. Pojazd z danymi usuwa w `finally`;
konto pozostaje, ponieważ API nie udostępnia jego usunięcia. Uruchamiaj go
w środowisku testowym. Inny adres API: zmienna `ROUTECOST_API_URL`.

Scenariusz do sprawdzenia w interfejsie:

1. Dodaj pojazd z datą zakupu wcześniejszą niż wpisy. Otwórz jego tankowania.
2. Dodaj pełne tankowanie: 1000 km, 40 l, koszt 240.
3. Dodaj późniejsze częściowe: 1200 km, 10 l, koszt 60.
4. Dodaj jeszcze późniejsze pełne: 1500 km, 30 l, koszt 180.
5. Sprawdź: 3 wpisy, 80 l, koszt 480 bez waluty, 1 interwał, 8 l/100 km.
6. Edytuj częściowe na 15 l i koszt 90 — średnia powinna wynosić 9 l/100 km.
7. Spróbuj podać przebieg kolidujący z późniejszym wpisem — powinien pojawić
   się błąd, bez utraty danych formularza.
8. Usuń częściowe po potwierdzeniu — średnia powinna wynosić 6 l/100 km.
   Usunięcie zamykającego pełnego tankowania przywraca brak danych spalania.
9. Sprawdź zmianę pojazdu, sortowanie, kolejną stronę, stan pusty i szerokość
   mobilną. Po wylogowaniu wszystkie trasy fuel powinny prowadzić do logowania.

## Faktycznie wykonana weryfikacja

Wyniki końcowe: **lint bez błędów i ostrzeżeń**, **build poprawny**,
**66/66 testów Vitest (7 plików) poprawnych**. Ostrzeżenia builda opisano niżej.

Skrypt integracyjny przeszedł na działającym Docker Compose 13.11.2026:
CRUD, sortowanie, paginacja, statystyki 8 → 9 → 6 l/100 km, utrata dostępnego
spalania po usunięciu odcinka, miesięczne koszty bez waluty, odświeżenie liczników
dashboardu oraz odpowiedzi `400`, `401`, `404`. Dane pojazdu testowego usunięto.

Testy Vitest obejmują walidację, dokładność DTO, pobieranie i przełączanie
pojazdów, paginację/sortowanie, dodawanie/edycję/usuwanie, blokadę zapisu,
odświeżanie cache, błędy API i zachowanie `null` na wykresie. Mocki komponentów
Recharts w testach sprawdzają przekazane dane, nie renderowanie SVG w przeglądarce.

Nie wykonano ręcznej kontroli wizualnej w przeglądarce. Testy komponentów,
kompilacja i żądania HTTP nie zastępują takiej kontroli. Build zachowuje znane
ostrzeżenie rozmiaru głównego chunka i komentarzy PURE z biblioteki Zod.

## Lista zmienionych plików etapu 3

- `src/features/fuel/api.ts`
- `src/features/fuel/fuelSchema.ts`
- `src/features/fuel/FuelPage.tsx`
- `src/features/fuel/FuelFormPage.tsx`
- `src/features/fuel/FuelHistory.tsx`
- `src/features/fuel/FuelStatistics.tsx`
- `src/features/fuel/FuelCharts.tsx`
- `src/features/fuel/fuel.css`
- `src/features/fuel/fuel.test.tsx`
- `src/features/fuel/fuelSchema.test.ts`
- `src/features/fuel/FuelCharts.test.tsx`
- `src/components/ConfirmDialog.tsx` — wspólny dialog.
- `src/components/charts/MonthlyChart.tsx` — wydzielony istniejący wykres.
- `src/utils/date.ts` — wspólna obsługa dat.
- `src/features/vehicles/CostReport.tsx` — użycie wspólnego wykresu.
- `src/features/vehicles/DeleteVehicleDialog.tsx` — użycie wspólnego dialogu.
- `src/features/vehicles/VehicleDetailPage.tsx` — skrót do tankowań.
- `src/features/vehicles/vehicleSchema.ts` — wspólna funkcja daty.
- `src/App.tsx`, `src/layouts/AppLayout.tsx`, `src/main.tsx` — trasy, nawigacja, CSS.
- `src/routes/ProtectedRoute.test.tsx` — chronione trasy fuel.
- `scripts/test-fuel-integration.mjs` — powtarzalny test rzeczywistego API.
- `README.md`, `FUEL.md` — dokumentacja.
