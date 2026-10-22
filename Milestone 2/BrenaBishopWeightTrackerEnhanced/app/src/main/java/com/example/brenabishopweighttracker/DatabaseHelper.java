package com.example.brenabishopweighttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Handles the SQLite database for users, daily weight entries, and goal weight.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_WEIGHTS = "daily_weights";
    public static final String TABLE_GOALS = "goal_weights";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL)";

        String createWeightsTable = "CREATE TABLE " + TABLE_WEIGHTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "entry_date TEXT NOT NULL, " +
                "weight REAL NOT NULL)";

        String createGoalsTable = "CREATE TABLE " + TABLE_GOALS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER UNIQUE NOT NULL, " +
                "goal_weight REAL NOT NULL, " +
                "phone_number TEXT)";

        db.execSQL(createUsersTable);
        db.execSQL(createWeightsTable);
        db.execSQL(createGoalsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
        onCreate(db);
    }

    public boolean createUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public int loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE username = ? AND password = ?",
                new String[]{username, password}
        );

        int userId = -1;

        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }

        cursor.close();
        return userId;
    }

    public boolean addWeight(int userId, String entryDate, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("entry_date", entryDate);
        values.put("weight", weight);

        long result = db.insert(TABLE_WEIGHTS, null, values);
        return result != -1;
    }

    public boolean updateWeight(int weightId, String entryDate, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("entry_date", entryDate);
        values.put("weight", weight);

        int rowsUpdated = db.update(
                TABLE_WEIGHTS,
                values,
                "id = ?",
                new String[]{String.valueOf(weightId)}
        );

        return rowsUpdated > 0;
    }

    public boolean deleteWeight(int weightId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_WEIGHTS,
                "id = ?",
                new String[]{String.valueOf(weightId)}
        );

        return rowsDeleted > 0;
    }

    public Cursor getWeightsForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, entry_date, weight FROM " + TABLE_WEIGHTS +
                        " WHERE user_id = ? ORDER BY entry_date DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    public boolean saveGoalWeight(int userId, double goalWeight, String phoneNumber) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("goal_weight", goalWeight);
        values.put("phone_number", phoneNumber);

        long result = db.insertWithOnConflict(
                TABLE_GOALS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        return result != -1;
    }

    public double getGoalWeight(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT goal_weight FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        double goalWeight = 0;

        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(0);
        }

        cursor.close();
        return goalWeight;
    }

    public String getPhoneNumber(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT phone_number FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        String phoneNumber = "";

        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(0);
        }

        cursor.close();
        return phoneNumber;
    }
}
