import { ExcelEmployeeRow, ExcelImportPreviewResponse } from '../types';

/**
 * Normalizes phone numbers (adding leading 0 if 9 digits, removing spaces and dashes).
 */
export function normalizePhoneNumber(phone: string): string {
  if (!phone) return '';
  let clean = phone.replace(/[\s-]/g, '');
  if (clean.startsWith('+250')) {
    clean = '0' + clean.substring(4);
  } else if (clean.startsWith('250') && clean.length === 12) {
    clean = '0' + clean.substring(3);
  } else if (
    clean.length === 9 &&
    ['7', '8', '9', '2', '3'].includes(clean[0])
  ) {
    clean = '0' + clean;
  }
  return clean;
}

/**
 * Parses meal status to standard ATE or DID_NOT_EAT.
 */
export function parseMealStatus(raw: string): string | null {
  if (!raw) return null;
  const clean = raw.trim().toLowerCase().replace(/[_-\s]/g, '');
  if (['ate', 'yes', 'true', '1'].includes(clean)) {
    return 'ATE';
  }
  if (['notate', 'didnoteat', 'no', 'false', '0'].includes(clean)) {
    return 'DID_NOT_EAT';
  }
  return null;
}

/**
 * Unzips an XLSX file using standard browser DecompressionStream.
 */
async function unzipXlsx(buffer: ArrayBuffer): Promise<Map<string, string>> {
  const fileMap = new Map<string, string>();
  const bytes = new Uint8Array(buffer);
  const view = new DataView(buffer);

  let pos = 0;
  while (pos + 30 <= bytes.length) {
    const signature = view.getUint32(pos, true);
    if (signature !== 0x04034b50) break; // End of local headers

    const compression = view.getUint16(pos + 8, true);
    const compressedSize = view.getUint32(pos + 18, true);
    const fileNameLen = view.getUint16(pos + 26, true);
    const extraLen = view.getUint16(pos + 28, true);

    const nameBytes = bytes.subarray(pos + 30, pos + 30 + fileNameLen);
    const fileName = new TextDecoder().decode(nameBytes);

    const dataStart = pos + 30 + fileNameLen + extraLen;
    const dataEnd = dataStart + compressedSize;

    if (dataEnd <= bytes.length && compressedSize > 0) {
      const compData = bytes.subarray(dataStart, dataEnd);

      if (compression === 0) {
        // Uncompressed
        fileMap.set(fileName, new TextDecoder().decode(compData));
      } else if (compression === 8 && typeof DecompressionStream !== 'undefined') {
        // Deflate
        try {
          const ds = new DecompressionStream('deflate-raw');
          const writer = ds.writable.getWriter();
          writer.write(compData);
          writer.close();
          const response = new Response(ds.readable);
          const decompressedText = await response.text();
          fileMap.set(fileName, decompressedText);
        } catch {
          // Fallback if decompression stream errors on individual file
        }
      }
    }

    pos = dataEnd;
    // Skip to next 4-byte boundary or search for next signature if descriptor used
    while (pos + 4 <= bytes.length && view.getUint32(pos, true) !== 0x04034b50 && view.getUint32(pos, true) !== 0x02014b50) {
      pos++;
    }
  }

  return fileMap;
}

/**
 * Parses shared strings XML into a string array.
 */
function parseSharedStrings(xmlStr: string): string[] {
  const strings: string[] = [];
  const parser = new DOMParser();
  const doc = parser.parseFromString(xmlStr, 'application/xml');
  const sis = doc.getElementsByTagName('si');
  for (let i = 0; i < sis.length; i++) {
    const textNodes = sis[i].getElementsByTagName('t');
    let itemText = '';
    for (let j = 0; j < textNodes.length; j++) {
      itemText += textNodes[j].textContent || '';
    }
    strings.push(itemText.trim());
  }
  return strings;
}

/**
 * Parses worksheet XML into a 2D array of string cells.
 */
function parseWorksheet(xmlStr: string, sharedStrings: string[]): string[][] {
  const rows: string[][] = [];
  const parser = new DOMParser();
  const doc = parser.parseFromString(xmlStr, 'application/xml');
  const rowNodes = doc.getElementsByTagName('row');

  for (let r = 0; r < rowNodes.length; r++) {
    const rowNode = rowNodes[r];
    const cellNodes = rowNode.getElementsByTagName('c');
    const rowValues: string[] = [];

    for (let c = 0; c < cellNodes.length; c++) {
      const cell = cellNodes[c];
      const type = cell.getAttribute('t');
      const rAttr = cell.getAttribute('r') || '';

      // Determine column index from A1, B1, C1 notation
      const colLetter = rAttr.replace(/[0-9]/g, '');
      let colIdx = c;
      if (colLetter) {
        let n = 0;
        for (let i = 0; i < colLetter.length; i++) {
          n = n * 26 + (colLetter.charCodeAt(i) - 64);
        }
        colIdx = n - 1;
      }

      while (rowValues.length < colIdx) {
        rowValues.push('');
      }

      const vNode = cell.getElementsByTagName('v')[0];
      const tNode = cell.getElementsByTagName('t')[0];

      let value = '';
      if (type === 's' && vNode) {
        const sIdx = parseInt(vNode.textContent || '0', 10);
        value = sharedStrings[sIdx] || '';
      } else if (type === 'inlineStr' && tNode) {
        value = tNode.textContent || '';
      } else if (vNode) {
        value = vNode.textContent || '';
      }

      rowValues[colIdx] = value.trim();
    }

    if (rowValues.some((v) => v !== '')) {
      rows.push(rowValues);
    }
  }

  return rows;
}

/**
 * Parses raw CSV text into 2D string array.
 */
function parseCsv(text: string): string[][] {
  const lines = text.split(/\r?\n/);
  const rows: string[][] = [];

  for (const line of lines) {
    if (!line.trim()) continue;
    // Handle comma, semicolon, and tab delimiters
    const delimiter = line.includes('\t') ? '\t' : line.includes(';') ? ';' : ',';
    const cols = line.split(delimiter).map((c) => c.replace(/^["']|["']$/g, '').trim());
    if (cols.some((c) => c !== '')) {
      rows.push(cols);
    }
  }

  return rows;
}

/**
 * Parses month string to month number (1-12).
 */
function parseMonthName(str: string): number {
  if (!str) return 0;
  const s = str.trim().toLowerCase();
  if (s.startsWith('jan')) return 1;
  if (s.startsWith('feb')) return 2;
  if (s.startsWith('mar')) return 3;
  if (s.startsWith('apr')) return 4;
  if (s.startsWith('may')) return 5;
  if (s.startsWith('jun')) return 6;
  if (s.startsWith('jul')) return 7;
  if (s.startsWith('aug')) return 8;
  if (s.startsWith('sep')) return 9;
  if (s.startsWith('oct')) return 10;
  if (s.startsWith('nov')) return 11;
  if (s.startsWith('dec')) return 12;
  return 0;
}

/**
 * Parses a date header string (e.g., "22nd aug", "23-08-2026", "2026-08-22").
 */
function parseClientDateHeader(text: string, defaultYear: number = new Date().getFullYear()): string | null {
  if (!text) return null;
  const t = text.trim().toLowerCase();

  // YYYY-MM-DD
  if (/^\d{4}-\d{1,2}-\d{1,2}$/.test(t)) {
    return t;
  }

  // DD/MM/YYYY or DD-MM-YYYY
  const dmyMatch = t.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})$/);
  if (dmyMatch) {
    const d = String(parseInt(dmyMatch[1], 10)).padStart(2, '0');
    const m = String(parseInt(dmyMatch[2], 10)).padStart(2, '0');
    let y = parseInt(dmyMatch[3], 10);
    if (y < 100) y += 2000;
    return `${y}-${m}-${d}`;
  }

  // e.g. "22nd aug", "22nd august", "22 aug", "22-aug"
  const dayMonthMatch = t.match(/^(\d{1,2})(?:st|nd|rd|th)?[\s-_/]*([a-z]+)?$/);
  if (dayMonthMatch) {
    const day = parseInt(dayMonthMatch[1], 10);
    let month = dayMonthMatch[2] ? parseMonthName(dayMonthMatch[2]) : new Date().getMonth() + 1;
    if (month === 0) month = new Date().getMonth() + 1;
    const d = String(day).padStart(2, '0');
    const m = String(month).padStart(2, '0');
    return `${defaultYear}-${m}-${d}`;
  }

  return null;
}

/**
 * Validates 2D raw data matrix and produces ExcelImportPreviewResponse.
 */
function buildPreviewFromRows(matrix: string[][]): ExcelImportPreviewResponse {
  if (matrix.length === 0) {
    throw new Error('The uploaded file is empty.');
  }

  // Check for Matrix Multi-Date Format across top rows
  let matrixHeaderRowIdx = -1;
  let nameColIdx = -1;
  const dateCols: { colIdx: number; dateStr: string }[] = [];

  for (let r = 0; r < Math.min(4, matrix.length); r++) {
    const row = matrix[r];
    const foundDates: { colIdx: number; dateStr: string }[] = [];
    let foundName = -1;

    row.forEach((cellVal, cIdx) => {
      const clean = cellVal.trim().toLowerCase().replace(/[^a-z]/g, '');
      if (clean.includes('name') || clean === 'employee' || clean === 'nom' || clean === 'amazina') {
        foundName = cIdx;
      } else if (!clean.includes('total') && !clean.includes('amountpaid')) {
        const parsedDate = parseClientDateHeader(cellVal);
        if (parsedDate) {
          foundDates.push({ colIdx: cIdx, dateStr: parsedDate });
        }
      }
    });

    if (foundDates.length > 0) {
      matrixHeaderRowIdx = r;
      nameColIdx = foundName >= 0 ? foundName : 0;
      dateCols.push(...foundDates);
      break;
    }
  }

  // If Matrix Format Detected:
  if (dateCols.length > 0 && matrixHeaderRowIdx >= 0) {
    const previewRows: ExcelEmployeeRow[] = [];
    let validCount = 0;
    let seq = 1;

    for (let r = matrixHeaderRowIdx + 1; r < matrix.length; r++) {
      const row = matrix[r];
      const empName = (row[nameColIdx] || '').trim();
      if (!empName) continue;
      const lower = empName.toLowerCase();
      if (lower.startsWith('total') || lower.startsWith('summary')) continue; // Skip total row

      for (const { colIdx, dateStr } of dateCols) {
        const cellVal = (row[colIdx] || '').trim();
        const numClean = cellVal.replace(/[^0-9.]/g, '');
        let amount = 0;
        let mealStatus = 'Not Ate';

        if (numClean && !isNaN(parseFloat(numClean))) {
          const parsed = parseFloat(numClean);
          if (parsed > 0) {
            amount = parsed;
            mealStatus = 'Ate';
          }
        }

        previewRows.push({
          rowNumber: seq++,
          employeeName: empName,
          telephone: '',
          position: 'Worker',
          mealDate: dateStr,
          mealStatus,
          amountUsed: amount,
          status: 'VALID',
          valid: true,
          duplicate: false,
        });
        validCount++;
      }
    }

    return {
      totalRows: previewRows.length,
      validRows: validCount,
      invalidRows: 0,
      duplicateRows: 0,
      rows: previewRows,
      summaryMessage: `Extracted ${previewRows.length} daily meal records across ${dateCols.length} dates.`,
    };
  }

  // Fallback to Standard Single-Date Format:
  const headerRow = matrix[0];
  const colMap = new Map<string, number>();

  headerRow.forEach((h, idx) => {
    const clean = h.toLowerCase().replace(/[^a-z]/g, '');
    if (clean.includes('name') || clean === 'employee') colMap.set('name', idx);
    else if (clean.includes('phone') || clean.includes('telephone') || clean.includes('tel') || clean.includes('mobile')) colMap.set('phone', idx);
    else if (clean.includes('position') || clean.includes('role') || clean.includes('job') || clean.includes('title')) colMap.set('position', idx);
    else if (clean.includes('meal') || clean.includes('status')) colMap.set('mealstatus', idx);
    else if (clean.includes('amount') || clean.includes('cost') || clean.includes('price')) colMap.set('amount', idx);
  });

  if (!colMap.has('name') || !colMap.has('phone')) {
    colMap.set('name', 0);
    colMap.set('phone', 1);
    colMap.set('position', 2);
    colMap.set('mealstatus', 3);
    colMap.set('amount', 4);
  }

  const nameCol = colMap.get('name') ?? 0;
  const phoneCol = colMap.get('phone') ?? 1;
  const posCol = colMap.get('position') ?? 2;
  const statusCol = colMap.get('mealstatus') ?? 3;
  const amountCol = colMap.get('amount') ?? 4;

  const seenPhones = new Set<string>();
  const previewRows: ExcelEmployeeRow[] = [];

  let validCount = 0;
  let invalidCount = 0;
  let duplicateCount = 0;

  for (let r = 1; r < matrix.length; r++) {
    const row = matrix[r];
    const displayRowNumber = r + 1;

    const empName = row[nameCol] || '';
    const phoneRaw = row[phoneCol] || '';
    const position = row[posCol] || 'Employee';
    const mealStatusRaw = row[statusCol] || 'Ate';
    const amountRaw = row[amountCol] || '';

    const errors: string[] = [];

    if (!empName.trim()) {
      errors.push('Employee Name is required.');
    }

    const normalizedPhone = normalizePhoneNumber(phoneRaw);
    if (!phoneRaw.trim()) {
      errors.push('Telephone is required.');
    } else if (normalizedPhone.length < 8 || normalizedPhone.length > 15) {
      errors.push(`Invalid telephone format ('${phoneRaw}'). Must be 8-15 digits.`);
    }

    const parsedStatus = parseMealStatus(mealStatusRaw);
    if (!parsedStatus) {
      errors.push(`Invalid Meal Status ('${mealStatusRaw}'). Expected 'Ate' or 'Not Ate'.`);
    }

    let parsedAmount: number | null = 1500;
    if (parsedStatus === 'DID_NOT_EAT') {
      parsedAmount = 0;
    } else {
      if (amountRaw.trim()) {
        const num = parseFloat(amountRaw.replace(/[^0-9.]/g, ''));
        if (isNaN(num) || num < 0) {
          errors.push('Amount Used must be a valid positive number.');
        } else {
          parsedAmount = num;
        }
      } else {
        parsedAmount = 1500;
      }
    }

    let isDuplicate = false;
    let duplicateReason: string | undefined;

    if (errors.length === 0 && normalizedPhone) {
      if (seenPhones.has(normalizedPhone)) {
        isDuplicate = true;
        duplicateReason = `Duplicate: Telephone '${phoneRaw}' appears multiple times in file.`;
      } else {
        seenPhones.add(normalizedPhone);
      }
    }

    let rowStatus: string;
    let errorReasonText: string | undefined;

    if (errors.length > 0) {
      rowStatus = 'INVALID';
      errorReasonText = errors.join(' ');
      invalidCount++;
    } else if (isDuplicate) {
      rowStatus = 'DUPLICATE';
      errorReasonText = duplicateReason;
      duplicateCount++;
    } else {
      rowStatus = 'VALID';
      validCount++;
    }

    previewRows.push({
      rowNumber: displayRowNumber,
      employeeName: empName,
      telephone: normalizedPhone || phoneRaw,
      position: position || 'Employee',
      mealStatus: parsedStatus === 'ATE' ? 'Ate' : 'Not Ate',
      amountUsed: parsedAmount,
      status: rowStatus,
      valid: rowStatus === 'VALID',
      duplicate: isDuplicate,
      errorReason: errorReasonText,
      errorMessages: errors,
    });
  }

  return {
    totalRows: previewRows.length,
    validRows: validCount,
    invalidRows: invalidCount,
    duplicateRows: duplicateCount,
    rows: previewRows,
    summaryMessage: `Read ${previewRows.length} rows: ${validCount} valid, ${duplicateCount} duplicates, ${invalidCount} invalid.`,
  };
}

/**
 * Main parser entry point: parses Excel (.xlsx, .xls) or CSV on the client side.
 */
export async function parseExcelOrCsvClient(file: File): Promise<ExcelImportPreviewResponse> {
  const fileName = file.name.toLowerCase();

  if (fileName.endsWith('.csv') || fileName.endsWith('.txt')) {
    const text = await file.text();
    const matrix = parseCsv(text);
    return buildPreviewFromRows(matrix);
  }

  // Try parsing as XLSX
  const buffer = await file.arrayBuffer();
  try {
    const files = await unzipXlsx(buffer);
    const sharedStringsXml = files.get('xl/sharedStrings.xml') || '';
    const sheet1Xml =
      files.get('xl/worksheets/sheet1.xml') ||
      files.get('xl/worksheets/Sheet1.xml') ||
      '';

    if (sheet1Xml) {
      const sharedStrings = sharedStringsXml ? parseSharedStrings(sharedStringsXml) : [];
      const matrix = parseWorksheet(sheet1Xml, sharedStrings);
      if (matrix.length > 0) {
        return buildPreviewFromRows(matrix);
      }
    }
  } catch (err) {
    console.warn('Direct zip parsing failed, trying text/csv fallback:', err);
  }

  // If XML spreadsheet / plain text fallback
  const text = await file.text();
  if (text.includes('<Table') || text.includes('<Row')) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(text, 'application/xml');
    const rowNodes = doc.getElementsByTagName('Row');
    const matrix: string[][] = [];
    for (let i = 0; i < rowNodes.length; i++) {
      const cellNodes = rowNodes[i].getElementsByTagName('Cell');
      const rowVals: string[] = [];
      for (let j = 0; j < cellNodes.length; j++) {
        rowVals.push(cellNodes[j].textContent?.trim() || '');
      }
      if (rowVals.some((v) => v !== '')) matrix.push(rowVals);
    }
    if (matrix.length > 0) return buildPreviewFromRows(matrix);
  }

  const csvMatrix = parseCsv(text);
  return buildPreviewFromRows(csvMatrix);
}
