package com.ISA.Restaurant.utils;

import java.time.LocalDateTime;

public class TimeUtility {

    public static LocalDateTime calculateCutoffTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange.toLowerCase()) {
            case "1h" -> now.minusHours(1);
            case "2h" -> now.minusHours(2);
            case "3h" -> now.minusHours(3);
            case "6h" -> now.minusHours(6);
            case "12h" -> now.minusHours(12);
            case "24h" -> now.minusHours(24);
            case "7d" -> now.minusDays(7);
            case "1m" -> now.minusMonths(1);
            case "6m" -> now.minusMonths(6);
            case "1y" -> now.minusYears(1);
            case "all" -> null; // No filter for "all"
            default -> now.minusHours(2); // Default to 2 hours
        };
    }
}
