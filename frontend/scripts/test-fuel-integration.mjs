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
    `${method} ${path}: expected ${status}, received ${response.status}`,
  );
  return data;
}
const log = (message) => process.stdout.write(`PASS: ${message}\n`);
try {
  await request("/api/vehicles/1/fuel-statistics", {
    status: 401,
    authenticated: false,
  });
  const auth = await request("/api/auth/register", {
    method: "POST",
    status: 201,
    body: {
      firstName: "Fuel",
      lastName: "Integration",
      email: `fuel-${randomUUID()}@example.test`,
      password: `${randomUUID()}Aa1!`,
    },
  });
  token = auth.accessToken;
  await request("/api/auth/me");
  const before = await request("/api/dashboard/summary");
  const vehicle = await request("/api/vehicles", {
    method: "POST",
    status: 201,
    body: {
      make: "RouteCost QA",
      model: "Fuel integration",
      productionYear: 2020,
      fuelType: "PETROL",
      currentOdometer: 9000,
      purchaseDate: "2020-01-01",
      purchasePrice: 10000,
    },
  });
  vehicleId = vehicle.id;
  const root = `/api/vehicles/${vehicleId}`;
  const empty = await request(`${root}/fuel-statistics`);
  assert.equal(empty.averageConsumptionLitersPer100Km, null);
  assert.equal(empty.fuelEntryCount, 0);
  log("401 without session, authenticated ownership and empty statistics");

  const add = (body) =>
    request(`${root}/fuel-entries`, { method: "POST", status: 201, body });
  const first = await add({
    entryDate: "2026-01-01",
    odometer: 1000,
    liters: 40,
    totalCost: 240,
    fullTank: true,
  });
  const partial = await add({
    entryDate: "2026-01-10",
    odometer: 1200,
    liters: 10,
    totalCost: 60,
    fullTank: false,
  });
  const full = await add({
    entryDate: "2026-02-01",
    odometer: 1500,
    liters: 30,
    totalCost: 180,
    fullTank: true,
  });
  assert.equal(first.consumptionFromPreviousFullTank, null);
  assert.equal(partial.consumptionFromPreviousFullTank, null);
  assert.equal(full.consumptionFromPreviousFullTank, 8);
  assert.equal(full.pricePerLiter, 6);
  const stats = await request(`${root}/fuel-statistics`);
  assert.equal(stats.averageConsumptionLitersPer100Km, 8);
  assert.equal(stats.totalLiters, 80);
  assert.equal(stats.totalCost, 480);
  assert.equal(stats.fuelEntryCount, 3);
  assert.equal(stats.validConsumptionIntervals, 1);
  log("POST, detail DTO and full–partial–full consumption: 8 l/100 km");

  const ascending = await request(
    `${root}/fuel-entries?page=0&size=2&sort=entryDate,asc&sort=id,asc`,
  );
  const descending = await request(
    `${root}/fuel-entries?page=0&size=2&sort=entryDate,desc&sort=id,desc`,
  );
  const next = await request(
    `${root}/fuel-entries?page=1&size=2&sort=entryDate,asc&sort=id,asc`,
  );
  assert.deepEqual(
    ascending.content.map((entry) => entry.id),
    [first.id, partial.id],
  );
  assert.deepEqual(
    descending.content.map((entry) => entry.id),
    [full.id, partial.id],
  );
  assert.equal(next.content[0].id, full.id);
  assert.equal(ascending.totalPages, 2);
  log("server pagination and stable chronological sorting in both directions");

  const dto = {
    entryDate: partial.entryDate,
    odometer: partial.odometer,
    liters: 15,
    totalCost: 90,
    fullTank: false,
  };
  await request(`${root}/fuel-entries/${partial.id}`, {
    method: "PUT",
    body: dto,
  });
  assert.equal(
    (await request(`${root}/fuel-statistics`)).averageConsumptionLitersPer100Km,
    9,
  );
  assert.equal(
    (await request(`${root}/fuel-entries/${full.id}`))
      .consumptionFromPreviousFullTank,
    9,
  );
  for (const change of [
    { entryDate: "2019-12-31" },
    { odometer: 1600 },
    { liters: 1.0001 },
    { totalCost: 0 },
  ]) {
    await request(`${root}/fuel-entries/${partial.id}`, {
      method: "PUT",
      body: { ...dto, ...change },
      status: 400,
    });
  }
  log(
    "PUT recalculates downstream consumption; date, chronology and DTO errors return 400",
  );

  const costs = await request(`${root}/cost-summary`);
  assert.equal(costs.fuel.currency, null);
  assert.deepEqual(
    costs.fuel.monthlyTotals.map((row) => [row.year, row.month, row.totalCost]),
    [
      [2026, 1, 330],
      [2026, 2, 180],
    ],
  );
  assert.equal(
    (await request("/api/dashboard/summary")).fuelEntryCount,
    before.fuelEntryCount + 3,
  );
  log("monthly costs remain currency-free and dashboard reflects new entries");

  await request(`${root}/fuel-entries/${partial.id}`, {
    method: "DELETE",
    status: 204,
  });
  assert.equal(
    (await request(`${root}/fuel-statistics`)).averageConsumptionLitersPer100Km,
    6,
  );
  await request(`${root}/fuel-entries/${full.id}`, {
    method: "DELETE",
    status: 204,
  });
  assert.equal(
    (await request(`${root}/fuel-statistics`)).averageConsumptionLitersPer100Km,
    null,
  );
  await request(`${root}/fuel-entries/${full.id}`, { status: 404 });
  await request(`${root}/fuel-entries/9223372036854775807`, { status: 404 });
  await request(`${root}/fuel-entries/${first.id}`, {
    method: "DELETE",
    status: 204,
  });
  const finalStats = await request(`${root}/fuel-statistics`);
  assert.equal(finalStats.fuelEntryCount, 0);
  assert.equal(finalStats.totalCost, 0);
  assert.equal(finalStats.validConsumptionIntervals, 0);
  assert.equal(finalStats.averageConsumptionLitersPer100Km, null);
  assert.equal(
    (await request("/api/dashboard/summary")).fuelEntryCount,
    before.fuelEntryCount,
  );
  log(
    "DELETE recalculates statistics, removed entries return 404, dashboard count restored",
  );
} finally {
  if (vehicleId) {
    await request(`/api/vehicles/${vehicleId}`, {
      method: "DELETE",
      status: 204,
    });
    log("temporary vehicle and related records removed");
  }
}
