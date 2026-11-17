export type PolishPluralForms = readonly [singular: string, few: string, many: string];

export function polishPlural(count: number, forms: PolishPluralForms): string {
  const absoluteCount = Math.abs(Math.trunc(count));

  if (absoluteCount === 1) return forms[0];

  const lastTwoDigits = absoluteCount % 100;
  const lastDigit = absoluteCount % 10;
  if (lastTwoDigits < 12 || lastTwoDigits > 14) {
    if (lastDigit >= 2 && lastDigit <= 4) return forms[1];
  }

  return forms[2];
}
