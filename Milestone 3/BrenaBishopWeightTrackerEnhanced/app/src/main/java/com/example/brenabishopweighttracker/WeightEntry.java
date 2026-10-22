package com.example.brenabishopweighttracker;

/**
 * Represents one historical weight record independently of the Android Cursor API.
 * The date sort key allows the application to order MM/DD/YYYY values chronologically
 * without changing the database schema during the algorithms and data structures milestone.
 */
public class WeightEntry {

    private final int id;
    private final String entryDate;
    private final double weight;
    private final long dateSortKey;

    public WeightEntry(int id, String entryDate, double weight) {
        this.id = id;
        this.entryDate = entryDate;
        this.weight = weight;
        this.dateSortKey = createDateSortKey(entryDate);
    }

    public int getId() {
        return id;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public double getWeight() {
        return weight;
    }

    public long getDateSortKey() {
        return dateSortKey;
    }

    /**
     * Converts an MM/DD/YYYY date into YYYYMMDD so numeric ordering is chronological.
     * Invalid values sort before valid application-generated dates.
     */
    private static long createDateSortKey(String entryDate) {
        if (entryDate == null) {
            return Long.MIN_VALUE;
        }

        String[] parts = entryDate.trim().split("/");
        if (parts.length != 3) {
            return Long.MIN_VALUE;
        }

        try {
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            if (month < 1 || month > 12 || day < 1 || day > 31 || year < 1) {
                return Long.MIN_VALUE;
            }

            return (year * 10000L) + (month * 100L) + day;
        } catch (NumberFormatException exception) {
            return Long.MIN_VALUE;
        }
    }
}
