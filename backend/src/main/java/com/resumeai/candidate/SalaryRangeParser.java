package com.resumeai.candidate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort parser for the free-text {@code salaryRange} field on a job posting
 * (e.g. "$100,000 - $130,000", "$100k-$130k", "Up to $150K") — powers the Market
 * Reality Check benchmark. This is deliberately a heuristic, not an exact parse:
 * postings aren't structured data, so a couple of numbers with an optional k/K
 * suffix is the most that can be extracted reliably.
 */
public final class SalaryRangeParser {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kK])?");

    private SalaryRangeParser() {}

    /** Returns {min, max} (equal if only one number was found), or null if nothing parseable. */
    public static int[] parse(String raw) {
        if (raw == null || raw.isBlank()) return null;

        List<Long> values = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        while (matcher.find()) {
            String numberPart = matcher.group(1).replace(",", "");
            double value;
            try {
                value = Double.parseDouble(numberPart);
            } catch (NumberFormatException e) {
                continue;
            }
            if (matcher.group(2) != null) {
                value *= 1000;
            }
            values.add(Math.round(value));
        }

        if (values.isEmpty()) return null;
        long min = values.stream().mapToLong(Long::longValue).min().orElseThrow();
        long max = values.stream().mapToLong(Long::longValue).max().orElseThrow();
        return new int[]{(int) min, (int) max};
    }
}
