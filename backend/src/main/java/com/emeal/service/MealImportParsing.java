package com.emeal.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict interpretation of Excel meal-attendance sheets.
 *
 * <p>Two decisions live here so they can be tested on their own:
 * <ul>
 *   <li><b>Cell value</b>: is it a meal (ATE with an amount), no meal (nothing is recorded), or
 *       an invalid value that must be flagged for review. Imports never create DID_NOT_EAT.</li>
 *   <li><b>Header date</b>: the actual meal date of a column. The month and day always come from the
 *       header (or the sheet name); the import date / today's month is never used.</li>
 * </ul>
 */
public final class MealImportParsing {

    private MealImportParsing() {
    }

    /* ======================================================================
       Cell values
       ====================================================================== */

    public enum Kind { ATE, NO_MEAL, INVALID }

    /** Result of reading one attendance cell. {@code amount} is set only for ATE. */
    public record CellValue(Kind kind, BigDecimal amount, String reason) {
        static CellValue ate(BigDecimal amount) { return new CellValue(Kind.ATE, amount, null); }
        static CellValue noMeal() { return new CellValue(Kind.NO_MEAL, null, null); }
        static CellValue invalid(String reason) { return new CellValue(Kind.INVALID, null, reason); }
    }

    /** Marks meaning "ate" without an amount: the configured standard meal price is used. */
    private static final Set<String> ATE_MARKS = Set.of("ate", "yes", "y", "✓", "✔", "√", "ok");

    /** Marks meaning "no meal": nothing is recorded for that day. */
    private static final Set<String> NO_MEAL_MARKS = Set.of(
            "-", "–", "—", "0", "no", "n", "absent", "abs", "off", "leave",
            "not ate", "notate", "did not eat", "didnoteat", "dne", "none", "nil");

    private static final Pattern NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    /**
     * Reads one attendance cell.
     *
     * @param standardPrice the configured standard meal price, used for "ate" marks and for the value 1
     */
    public static CellValue readMealCell(Cell cell, DataFormatter formatter, BigDecimal standardPrice) {
        if (cell == null) return CellValue.noMeal();
        CellType type = cell.getCellType();
        boolean formula = type == CellType.FORMULA;
        if (formula) {
            type = cell.getCachedFormulaResultType();
        }
        switch (type) {
            case BLANK:
                return CellValue.noMeal();
            case ERROR:
                return CellValue.invalid("The cell contains an Excel error value.");
            case BOOLEAN:
                return cell.getBooleanCellValue() ? CellValue.ate(standardPrice) : CellValue.noMeal();
            case NUMERIC: {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return CellValue.invalid("A date was found where a meal amount was expected.");
                }
                BigDecimal value = new BigDecimal(Double.toString(cell.getNumericCellValue()));
                return classifyAmount(value, standardPrice, value.stripTrailingZeros().toPlainString());
            }
            default:
                // A formula's text result is read from its cached value, never from the formula itself.
                return readText(formula ? cell.getStringCellValue() : formatter.formatCellValue(cell), standardPrice);
        }
    }

    /** Reads a textual cell value ("1,500", "600 RWF", "ate", "-", ...). */
    public static CellValue readText(String raw, BigDecimal standardPrice) {
        String text = raw == null ? "" : raw.trim();
        String lower = text.toLowerCase(Locale.ROOT).replace(' ', ' ').trim();
        if (lower.isEmpty() || NO_MEAL_MARKS.contains(lower)) return CellValue.noMeal();
        if (ATE_MARKS.contains(lower) || "1".equals(lower)) return CellValue.ate(standardPrice);

        String numeric = lower
                .replace("rwf", "")
                .replace("frw", "")
                .replace(",", "")
                .replace(" ", "");
        if (NUMBER.matcher(numeric).matches()) {
            return classifyAmount(new BigDecimal(numeric), standardPrice, text);
        }
        return CellValue.invalid("Unrecognised value '" + text + "'. Use an amount (e.g. 600), 'ate', or leave the cell empty.");
    }

    /** Smallest and largest believable price of one meal; anything outside is flagged, never guessed. */
    public static final BigDecimal MIN_MEAL_AMOUNT = new BigDecimal("100");
    public static final BigDecimal MAX_MEAL_AMOUNT = new BigDecimal("100000");

    /**
     * 0 = no meal; 1 = "ate" mark (standard price). Negative, fractional (e.g. "1.500" typed with a
     * European thousands separator, which would otherwise become 1.5 RWF) and implausible amounts are
     * flagged so they are corrected in the file instead of silently distorting the totals.
     */
    static CellValue classifyAmount(BigDecimal value, BigDecimal standardPrice, String shown) {
        if (value.signum() < 0) return CellValue.invalid("Negative amount '" + shown + "' is not allowed.");
        if (value.signum() == 0) return CellValue.noMeal();
        if (value.compareTo(BigDecimal.ONE) == 0) return CellValue.ate(standardPrice);
        if (value.stripTrailingZeros().scale() > 0) {
            return CellValue.invalid("Amount '" + shown + "' is not a whole RWF amount.");
        }
        if (value.compareTo(MIN_MEAL_AMOUNT) < 0 || value.compareTo(MAX_MEAL_AMOUNT) > 0) {
            return CellValue.invalid("Amount '" + shown + "' RWF is not a believable meal price (expected "
                    + MIN_MEAL_AMOUNT.toPlainString() + "–" + MAX_MEAL_AMOUNT.toPlainString() + ").");
        }
        return CellValue.ate(value.setScale(2));
    }

    /* ======================================================================
       Header dates
       ====================================================================== */

    public enum HeaderKind { DATE, NOT_A_DATE, DAY_WITHOUT_MONTH }

    public record HeaderDate(HeaderKind kind, LocalDate date, Integer day) {
        static HeaderDate of(LocalDate d) { return new HeaderDate(HeaderKind.DATE, d, null); }
        static HeaderDate notADate() { return new HeaderDate(HeaderKind.NOT_A_DATE, null, null); }
        static HeaderDate dayOnly(int day) { return new HeaderDate(HeaderKind.DAY_WITHOUT_MONTH, null, day); }
    }

    private static final Pattern ISO = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
    private static final Pattern DMY = Pattern.compile("^(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{2,4})$");
    private static final Pattern DM = Pattern.compile("^(\\d{1,2})[/.\\-](\\d{1,2})$");
    private static final Pattern DAY_MONTH_NAME =
            Pattern.compile("^(\\d{1,2})(?:st|nd|rd|th)?[\\s\\-_/.]*([a-z]+)\\.?(?:[\\s\\-_/,.]+(\\d{2,4}))?$");
    private static final Pattern MONTH_NAME_DAY =
            Pattern.compile("^([a-z]+)\\.?[\\s\\-_/]*(\\d{1,2})(?:st|nd|rd|th)?(?:[\\s\\-_/,.]+(\\d{2,4}))?$");
    private static final Pattern DAY_ONLY = Pattern.compile("^(\\d{1,2})(?:st|nd|rd|th)?$");

    /**
     * The meal date a header column represents.
     *
     * @param defaultYear year used only when the header itself has no year
     * @param sheetName   used for the month only when the header is a bare day number ("1", "2nd")
     */
    public static HeaderDate readHeaderDate(Cell cell, String text, int defaultYear, String sheetName) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return HeaderDate.of(cell.getLocalDateTimeCellValue().toLocalDate());
        }
        if (text == null) return HeaderDate.notADate();
        String t = text.trim().toLowerCase(Locale.ROOT).replace(' ', ' ');
        if (t.isEmpty()) return HeaderDate.notADate();
        // A year written in the sheet name ("September 2026") wins over the default year.
        int sheetYear = yearIn(sheetName);
        if (sheetYear > 0) defaultYear = sheetYear;

        Matcher m = ISO.matcher(t);
        if (m.matches()) return safe(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));

        m = DMY.matcher(t);
        if (m.matches()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            int y = normaliseYear(Integer.parseInt(m.group(3)));
            return dayMonth(y, a, b);
        }

        m = DM.matcher(t);
        if (m.matches()) {
            return dayMonth(defaultYear, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
        }

        m = DAY_MONTH_NAME.matcher(t);
        if (m.matches()) {
            int month = parseMonth(m.group(2));
            if (month > 0) {
                int year = m.group(3) != null ? normaliseYear(Integer.parseInt(m.group(3))) : defaultYear;
                return safe(year, month, Integer.parseInt(m.group(1)));
            }
        }

        m = MONTH_NAME_DAY.matcher(t);
        if (m.matches()) {
            int month = parseMonth(m.group(1));
            if (month > 0) {
                int year = m.group(3) != null ? normaliseYear(Integer.parseInt(m.group(3))) : defaultYear;
                return safe(year, month, Integer.parseInt(m.group(2)));
            }
        }

        m = DAY_ONLY.matcher(t);
        if (m.matches()) {
            int day = Integer.parseInt(m.group(1));
            if (day < 1 || day > 31) return HeaderDate.notADate();
            int month = parseMonth(sheetName);
            if (month == 0) return HeaderDate.dayOnly(day);
            return safe(defaultYear, month, day);
        }
        return HeaderDate.notADate();
    }

    /** Day/month order (Rwanda); swapped only when the first number cannot be a month day pairing. */
    private static HeaderDate dayMonth(int year, int a, int b) {
        if (b > 12 && a <= 12) return safe(year, a, b); // clearly month/day
        return safe(year, b, a);
    }

    private static HeaderDate safe(int year, int month, int day) {
        try {
            return HeaderDate.of(LocalDate.of(year, month, day));
        } catch (Exception e) {
            return HeaderDate.notADate();
        }
    }

    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(20\\d{2})(?!\\d)");

    /** A four-digit 20xx year contained in the text, or 0. */
    public static int yearIn(String text) {
        if (text == null) return 0;
        Matcher m = YEAR.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static int normaliseYear(int y) {
        return y < 100 ? 2000 + y : y;
    }

    /** Full or abbreviated English month name (also "sept"); 0 if not a month. */
    public static int parseMonth(String str) {
        if (str == null) return 0;
        String s = str.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", " ").trim();
        if (s.isEmpty()) return 0;
        String w = s.split("\\s+")[0];
        String[] names = {"january", "february", "march", "april", "may", "june", "july", "august",
                "september", "october", "november", "december"};
        for (int i = 0; i < names.length; i++) {
            if (w.length() >= 3 && names[i].startsWith(w.length() > names[i].length() ? w.substring(0, names[i].length()) : w)) {
                return i + 1;
            }
            if (w.equals("sept")) return 9;
        }
        return 0;
    }
}
