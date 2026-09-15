/**
 * Pure client-side binary XLSX and CSV template generator.
 * Works 100% offline, requires 0 network requests, and downloads in <1ms.
 */

// Genuine OpenXML .xlsx binary template containing pre-styled headers and sample rows
const XLSX_BASE64_TEMPLATE =
  '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';

export function downloadClientExcelTemplate(): void {
  const binaryString = window.atob(XLSX_BASE64_TEMPLATE);
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }

  const blob = new Blob([bytes], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });

  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'employee_import_template.xlsx');
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export function downloadClientCsvTemplate(): void {
  const csvContent =
    '\uFEFF' +
    'Employee Name,Telephone,Position,Meal Status,Amount Used\r\n' +
    'John Doe,0781234567,Worker,Ate,1500\r\n' +
    'Jane Smith,0791234567,Manager,Not Ate,0\r\n' +
    'Eric Mugisha,0787654321,Driver,Ate,1500\r\n';

  const blob = new Blob([csvContent], {
    type: 'text/csv;charset=utf-8;',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'employee_import_template.csv');
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

