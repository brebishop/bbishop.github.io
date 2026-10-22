package com.example.brenabishopweighttracker;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides a single data-access boundary for the activity.
 * DatabaseHelper remains responsible for SQLite implementation details.
 */
public class WeightRepository {

    private final DatabaseHelper databaseHelper;

    public WeightRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public boolean createUser(String username, String password) {
        return databaseHelper.createUser(username, password);
    }

    public int loginUser(String username, String password) {
        return databaseHelper.loginUser(username, password);
    }

    public boolean addWeight(int userId, String entryDate, double weight) {
        return databaseHelper.addWeight(userId, entryDate, weight);
    }

    public boolean updateWeight(int userId, int weightId, String entryDate, double weight) {
        return databaseHelper.updateWeight(userId, weightId, entryDate, weight);
    }

    public boolean deleteWeight(int userId, int weightId) {
        return databaseHelper.deleteWeight(userId, weightId);
    }

    public Cursor getWeightsForUser(int userId) {
        return databaseHelper.getWeightsForUser(userId);
    }

    /**
     * Converts SQLite rows into an application-level ArrayList and sorts the
     * entries chronologically from oldest to newest. This gives algorithms a
     * reusable collection that is independent of Cursor column positions.
     */
    public List<WeightEntry> getWeightHistory(int userId) {
        List<WeightEntry> history = new ArrayList<>();
        Cursor cursor = databaseHelper.getWeightsForUser(userId);

        try {
            while (cursor.moveToNext()) {
                history.add(new WeightEntry(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getDouble(2)
                ));
            }
        } finally {
            cursor.close();
        }

        Collections.sort(
                history,
                Comparator.comparingLong(WeightEntry::getDateSortKey)
        );

        return history;
    }

    public boolean saveGoalWeight(int userId, double goalWeight, String phoneNumber) {
        return databaseHelper.saveGoalWeight(userId, goalWeight, phoneNumber);
    }

    public double getGoalWeight(int userId) {
        return databaseHelper.getGoalWeight(userId);
    }

    public String getPhoneNumber(int userId) {
        return databaseHelper.getPhoneNumber(userId);
    }
}
