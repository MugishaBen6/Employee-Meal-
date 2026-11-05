package com.emeal.service;

import com.emeal.service.MealImportParsing.HeaderKind;
import com.emeal.service.MealImportParsing.Kind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MealImportParsingTest {

    private static final BigDecimal PRICE = new BigDecimal("600.00");

    private static LocalDate date(String header, String sheet) {
        MealImportParsing.HeaderDate h = MealImportParsing.readHeaderDate(null, header, 2026, sheet);
        assertEquals(HeaderKind.DATE, h.kind(), header + " / " + sheet);
        return h.date();
    }

    @Test
    void headerDatesComeFromTheFileNeverFromToday() {
        assertEquals(LocalDate.of(2026, 9, 1), date("1", "September 2026"));
        assertEquals(LocalDate.of(2026, 9, 30), date("30th", "Sept"));
        assertEquals(LocalDate.of(2026, 9, 2), date("2nd sept", "Sheet1"));
        assertEquals(LocalDate.of(2026, 10, 1), date("1st Oct", "Sheet1"));
        assertEquals(LocalDate.of(2026, 10, 22), date("Oct 22", "Sheet1"));
        assertEquals(LocalDate.of(2026, 10, 31), date("31/10/2026", "Sheet1"));
        assertEquals(LocalDate.of(2026, 9, 5), date("05-09-26", "Sheet1"));
        assertEquals(LocalDate.of(2026, 10, 15), date("2026-10-15", "Sheet1"));
        assertEquals(LocalDate.of(2025, 9, 3), date("3", "September 2025"), "year in the sheet name wins");
        // A bare day with no month anywhere is reported, never placed in the current month.
        assertEquals(HeaderKind.DAY_WITHOUT_MONTH, MealImportParsing.readHeaderDate(null, "1st", 2026, "Sheet1").kind());
        assertEquals(HeaderKind.NOT_A_DATE, MealImportParsing.readHeaderDate(null, "31/09/2026", 2026, "x").kind());
        assertEquals(HeaderKind.NOT_A_DATE, MealImportParsing.readHeaderDate(null, "Total", 2026, "September").kind());
        assertEquals(HeaderKind.NOT_A_DATE, MealImportParsing.readHeaderDate(null, "MEAL ATTENDANCE OCTOBER 2026", 2026, "x").kind());
    }

    @Test
    void cellValuesAreExactOrFlagged() {
        assertEquals(new BigDecimal("1500.00"), MealImportParsing.readText("1,500", PRICE).amount());
        assertEquals(new BigDecimal("1500.00"), MealImportParsing.readText("1500 RWF", PRICE).amount());
        assertEquals(PRICE, MealImportParsing.readText("ate", PRICE).amount());
        assertEquals(PRICE, MealImportParsing.readText("1", PRICE).amount());
        assertEquals(Kind.NO_MEAL, MealImportParsing.readText("", PRICE).kind());
        assertEquals(Kind.NO_MEAL, MealImportParsing.readText("-", PRICE).kind());
        assertEquals(Kind.NO_MEAL, MealImportParsing.readText("0", PRICE).kind());
        assertEquals(Kind.NO_MEAL, MealImportParsing.readText("Not Ate", PRICE).kind());
        assertEquals(Kind.INVALID, MealImportParsing.readText("1.500", PRICE).kind(), "1.5 RWF must not be imported");
        assertEquals(Kind.INVALID, MealImportParsing.readText("-600", PRICE).kind());
        assertEquals(Kind.INVALID, MealImportParsing.readText("12", PRICE).kind());
        assertEquals(Kind.INVALID, MealImportParsing.readText("250000", PRICE).kind());
        assertEquals(Kind.INVALID, MealImportParsing.readText("x", PRICE).kind());
    }
}
