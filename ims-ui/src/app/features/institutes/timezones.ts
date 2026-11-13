import { UiSelectOption } from '../../shared/ui-select';

export const DEFAULT_INSTITUTE_TIMEZONE = 'Asia/Kolkata';

export function timezoneSelectOptions(include?: string | null): UiSelectOption[] {
  const zones = new Set(ianaTimeZones());
  zones.add(DEFAULT_INSTITUTE_TIMEZONE);
  zones.add('UTC');
  const extra = include?.trim();
  if (extra) {
    zones.add(extra);
  }
  return [...zones]
    .sort((a, b) => a.localeCompare(b))
    .map((value) => ({ value, label: formatTimezoneLabel(value) }));
}

function ianaTimeZones(): string[] {
  try {
    const zones = Intl.supportedValuesOf('timeZone');
    return zones.length > 0 ? zones : [DEFAULT_INSTITUTE_TIMEZONE, 'UTC'];
  } catch {
    return [DEFAULT_INSTITUTE_TIMEZONE, 'UTC'];
  }
}

function formatTimezoneLabel(timeZone: string): string {
  const offset = timezoneOffsetLabel(timeZone);
  const name = timeZone.replaceAll('_', ' ');
  return offset ? `${name} (${offset})` : name;
}

function timezoneOffsetLabel(timeZone: string): string | null {
  try {
    return (
      new Intl.DateTimeFormat('en-US', {
        timeZone,
        timeZoneName: 'shortOffset',
      })
        .formatToParts(new Date())
        .find((part) => part.type === 'timeZoneName')?.value ?? null
    );
  } catch {
    return null;
  }
}
