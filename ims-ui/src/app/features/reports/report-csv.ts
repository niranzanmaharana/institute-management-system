export function csvCell(value: string | null | undefined): string {
  const v = value ?? '';
  if (v.includes(',') || v.includes('"') || v.includes('\n')) {
    return `"${v.replaceAll('"', '""')}"`;
  }
  return v;
}

export function downloadCsv(filename: string, content: string): void {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
