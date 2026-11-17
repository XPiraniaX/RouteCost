import { describe, expect, it } from "vitest";
import { polishPlural } from "./polishPlural";

describe("polishPlural", () => {
  const entryForms = ["wpis", "wpisy", "wpisów"] as const;

  it.each([
    [0, "wpisów"],
    [1, "wpis"],
    [2, "wpisy"],
    [4, "wpisy"],
    [5, "wpisów"],
    [11, "wpisów"],
    [12, "wpisów"],
    [14, "wpisów"],
    [21, "wpisów"],
    [22, "wpisy"],
    [25, "wpisów"],
    [101, "wpisów"],
    [102, "wpisy"],
  ])("formats %i with the correct noun form", (count, expected) => {
    expect(polishPlural(count, entryForms)).toBe(expected);
  });
});
