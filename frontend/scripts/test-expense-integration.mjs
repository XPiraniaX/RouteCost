import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";

const base = (process.env.ROUTECOST_API_URL || "http://localhost:8081").replace(
  /\/$/,
  "",
);
let token;
let vehicleId;
async function request(
  path,
  { method = "GET", body, status = 200, authenticated = true } = {},
) {
  const response = await fetch(`${base}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token && authenticated ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;
  assert.equal(
    response.status,
    status,
    `${method} ${path}: expected ${status}, got ${response.status}`,
  );
  return data;
}
try {
  const auth = await request("/api/auth/register", {
    method: "POST",
    status: 201,
    body: {
      firstName: "Expense",
      lastName: "Integration",
      email: `expense-${randomUUID()}@example.test`,
      password: `${randomUUID()}Aa1!`,
    },
  });
  token = auth.accessToken;
  const vehicle = await request("/api/vehicles", {
    method: "POST",
    status: 201,
    body: {
      make: "RouteCost QA",
      model: "Expense integration",
      productionYear: 2020,
      fuelType: "PETROL",
      currentOdometer: 9000,
      purchaseDate: "2020-01-01",
      purchasePrice: 10000,
    },
  });
  vehicleId = vehicle.id;
  const root = `/api/vehicles/${vehicleId}`;
  const add = (body) =>
    request(`${root}/expenses`, { method: "POST", status: 201, body });
  const pln = await add({
    expenseDate: "2026-10-20",
    category: "REPAIR",
    amount: 125.5,
    currency: "PLN",
    description: "Naprawa",
    odometer: 5000,
  });
  const eur = await add({
    expenseDate: "2026-10-21",
    category: "TIRES",
    amount: 80,
    currency: "EUR",
    description: null,
    odometer: 5100,
  });

  const page = await request(
    `${root}/expenses?page=0&size=10&sort=expenseDate,desc&sort=id,desc`,
  );
  assert.equal(page.totalElements, 2);
  assert.deepEqual(
    page.content.map((row) => row.id),
    [eur.id, pln.id],
  );
  const filtered = await request(
    `${root}/expenses?category=REPAIR&dateFrom=2026-10-20&dateTo=2026-10-20`,
  );
  assert.equal(filtered.totalElements, 1);
  assert.equal(filtered.content[0].currency, "PLN");

  const stats = await request(`${root}/expense-statistics`);
  assert.equal(stats.expenseCount, 2);
  assert.deepEqual(
    stats.totalsByCurrency.map(({ currency }) => currency).sort(),
    ["EUR", "PLN"],
  );
  assert.equal(
    stats.totalsByCurrency.find((row) => row.currency === "PLN").totalAmount,
    125.5,
  );
  assert.equal(
    stats.totalsByCurrency.find((row) => row.currency === "EUR").totalAmount,
    80,
  );
  assert.equal(stats.monthlyTotals.length, 2);

  const updated = await request(`${root}/expenses/${pln.id}`, {
    method: "PUT",
    body: {
      expenseDate: pln.expenseDate,
      category: pln.category,
      amount: 130,
      currency: "PLN",
      description: pln.description,
      odometer: pln.odometer,
    },
  });
  assert.equal(updated.amount, 130);
  await request(`${root}/expenses/${pln.id}`, {
    method: "DELETE",
    status: 204,
  });
  assert.equal((await request(`${root}/expense-statistics`)).expenseCount, 1);
  await request(`${root}/expenses/${eur.id}`, {
    method: "DELETE",
    status: 204,
  });
  assert.equal((await request(`${root}/expense-statistics`)).expenseCount, 0);
  process.stdout.write(
    "PASS: Expenses CRUD, filters, sorting, pagination DTO and currency-separated statistics\n",
  );
} finally {
  if (vehicleId) {
    await request(`/api/vehicles/${vehicleId}`, {
      method: "DELETE",
      status: 204,
    });
    process.stdout.write(
      "PASS: temporary vehicle and related records removed\n",
    );
  }
}
