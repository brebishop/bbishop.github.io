package com.example.brenabishopweighttracker;

import android.database.Cursor;

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

    public boolean updateWeight(int weightId, String entryDate, double weight) {
        return databaseHelper.updateWeight(weightId, entryDate, weight);
    }

    public boolean deleteWeight(int weightId) {
        return databaseHelper.deleteWeight(weightId);
    }

    public Cursor getWeightsForUser(int userId) {
        return databaseHelper.getWeightsForUser(userId);
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
