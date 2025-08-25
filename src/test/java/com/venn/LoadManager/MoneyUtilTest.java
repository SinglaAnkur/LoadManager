package com.venn.LoadManager;

import com.venn.LoadManager.util.MoneyUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyUtilTest {

    @Test
    void parseDollarsToCents_WithValidCurrencySymbol_ShouldReturnCents() {
        String load = "$123.45";
        long result = MoneyUtil.parseDollarsToCents(load);
        assertEquals(12345L, result);
    }

    @Test
    void parseDollarsToCents_withoutCurrencySymbol_ShouldReturnCents() {
        String load = "123.45";
        long result = MoneyUtil.parseDollarsToCents(load);
        assertEquals(12345L, result);
    }

    @Test
    void parseDollarsToCents_WhenInputIsNull_ShouldThrowException() {
        String load = null;
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parseDollarsToCents(load),
                "Amount is null");
    }

    @Test
    void parseDollarsToCents_WhenInputIsNonNumeric_ShouldThrowException() {
        String load = "abc";
        assertThrows(NumberFormatException.class, () -> MoneyUtil.parseDollarsToCents(load));
    }
}