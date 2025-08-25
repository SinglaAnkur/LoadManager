package com.venn.LoadManager.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {
    private MoneyUtil() {}

    public static long parseDollarsToCents(String s) {
        if (s == null) throw new IllegalArgumentException("Amount is null");
        String t = s.trim();
        if (t.startsWith("$")) t = t.substring(1);
        BigDecimal bd = new BigDecimal(t).setScale(2, RoundingMode.UNNECESSARY);
        return bd.movePointRight(2).longValueExact();
    }
}