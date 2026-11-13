import { DEFAULT_INSTITUTE_TIMEZONE, timezoneSelectOptions } from './timezones';

describe('timezoneSelectOptions', () => {
  it('includes the default institute timezone', () => {
    const options = timezoneSelectOptions();
    expect(options.some((opt) => opt.value === DEFAULT_INSTITUTE_TIMEZONE)).toBe(true);
  });

  it('keeps an unknown stored timezone selectable', () => {
    const extra = 'Custom/InstituteZone';
    const options = timezoneSelectOptions(extra);
    expect(options.some((opt) => opt.value === extra)).toBe(true);
  });

  it('labels the default zone with its IANA name', () => {
    const kolkata = timezoneSelectOptions().find((opt) => opt.value === DEFAULT_INSTITUTE_TIMEZONE);
    expect(kolkata?.label).toContain('Asia/Kolkata');
  });
});
