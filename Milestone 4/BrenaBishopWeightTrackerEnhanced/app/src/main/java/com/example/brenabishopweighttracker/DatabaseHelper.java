package com.example.brenabishopweighttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Handles the SQLite database for users, daily weight entries, and goal weight.
 * Version 2 strengthens credential storage, referential integrity, ownership checks,
 * chronological date storage, and preserves existing data during migration.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_WEIGHTS = "daily_weights";
    public static final String TABLE_GOALS = "goal_weights";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createVersionTwoTables(db, TABLE_USERS, TABLE_WEIGHTS, TABLE_GOALS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            migrateVersionOneToTwo(db);
        }
    }

    private void createVersionTwoTables(
            SQLiteDatabase db,
            String usersTable,
            String weightsTable,
            String goalsTable
    ) {
        String createUsersTable = "CREATE TABLE " + usersTable + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password_hash TEXT NOT NULL)";

        String createWeightsTable = "CREATE TABLE " + weightsTable + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "entry_date TEXT NOT NULL, " +
                "weight REAL NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + usersTable + "(id) ON DELETE CASCADE)";

        String createGoalsTable = "CREATE TABLE " + goalsTable + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER UNIQUE NOT NULL, " +
                "goal_weight REAL NOT NULL, " +
                "phone_number TEXT, " +
                "FOREIGN KEY(user_id) REFERENCES " + usersTable + "(id) ON DELETE CASCADE)";

        db.execSQL(createUsersTable);
        db.execSQL(createWeightsTable);
        db.execSQL(createGoalsTable);
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_weights_user_date ON "
                + weightsTable + "(user_id, entry_date DESC)");
    }

    /**
     * Migrates the original schema without dropping user data. Existing plain-text
     * passwords are converted to salted PBKDF2 hashes and MM/DD/YYYY dates are
     * normalized to ISO YYYY-MM-DD storage.
     */
    private void migrateVersionOneToTwo(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=OFF");
        db.beginTransaction();

        try {
            createVersionTwoTables(db, "users_new", "daily_weights_new", "goal_weights_new");

            Cursor users = db.rawQuery("SELECT id, username, password FROM " + TABLE_USERS, null);
            try {
                while (users.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("id", users.getInt(0));
                    values.put("username", users.getString(1));

                    String oldPassword = users.getString(2);
                    String securePassword = PasswordHasher.isHashed(oldPassword)
                            ? oldPassword
                            : PasswordHasher.hashPassword(oldPassword);
                    values.put("password_hash", securePassword);

                    db.insertOrThrow("users_new", null, values);
                }
            } finally {
                users.close();
            }

            Cursor weights = db.rawQuery(
                    "SELECT id, user_id, entry_date, weight FROM " + TABLE_WEIGHTS,
                    null
            );
            try {
                while (weights.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("id", weights.getInt(0));
                    values.put("user_id", weights.getInt(1));
                    values.put("entry_date", toStorageDate(weights.getString(2)));
                    values.put("weight", weights.getDouble(3));
                    db.insertOrThrow("daily_weights_new", null, values);
                }
            } finally {
                weights.close();
            }

            Cursor goals = db.rawQuery(
                    "SELECT id, user_id, goal_weight, phone_number FROM " + TABLE_GOALS,
                    null
            );
            try {
                while (goals.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("id", goals.getInt(0));
                    values.put("user_id", goals.getInt(1));
                    values.put("goal_weight", goals.getDouble(2));
                    values.put("phone_number", goals.getString(3));
                    db.insertOrThrow("goal_weights_new", null, values);
                }
            } finally {
                goals.close();
            }

            db.execSQL("DROP TABLE " + TABLE_WEIGHTS);
            db.execSQL("DROP TABLE " + TABLE_GOALS);
            db.execSQL("DROP TABLE " + TABLE_USERS);

            db.execSQL("ALTER TABLE users_new RENAME TO " + TABLE_USERS);
            db.execSQL("ALTER TABLE daily_weights_new RENAME TO " + TABLE_WEIGHTS);
            db.execSQL("ALTER TABLE goal_weights_new RENAME TO " + TABLE_GOALS);
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_weights_user_date ON "
                    + TABLE_WEIGHTS + "(user_id, entry_date DESC)");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    }

    public boolean createUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password_hash", PasswordHasher.hashPassword(password));

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public int loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id, password_hash FROM " + TABLE_USERS + " WHERE username = ?",
                new String[]{username}
        );

        int userId = -1;

        try {
            if (cursor.moveToFirst()) {
                String storedHash = cursor.getString(1);
                if (PasswordHasher.verifyPassword(password, storedHash)) {
                    userId = cursor.getInt(0);
                }
            }
        } finally {
            cursor.close();
        }

        return userId;
    }

    public boolean addWeight(int userId, String entryDate, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("entry_date", toStorageDate(entryDate));
        values.put("weight", weight);

        long result = db.insert(TABLE_WEIGHTS, null, values);
        return result != -1;
    }

    public boolean updateWeight(int userId, int weightId, String entryDate, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("entry_date", toStorageDate(entryDate));
        values.put("weight", weight);

        int rowsUpdated = db.update(
                TABLE_WEIGHTS,
                values,
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(weightId), String.valueOf(userId)}
        );

        return rowsUpdated > 0;
    }

    public boolean deleteWeight(int userId, int weightId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_WEIGHTS,
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(weightId), String.valueOf(userId)}
        );

        return rowsDeleted > 0;
    }

    public Cursor getWeightsForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, " +
                        "substr(entry_date, 6, 2) || '/' || substr(entry_date, 9, 2) || '/' || substr(entry_date, 1, 4) AS display_date, " +
                        "weight FROM " + TABLE_WEIGHTS +
                        " WHERE user_id = ? ORDER BY " + TABLE_WEIGHTS + ".entry_date DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    public boolean saveGoalWeight(int userId, double goalWeight, String phoneNumber) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("goal_weight", goalWeight);
        values.put("phone_number", phoneNumber);

        int updated = db.update(
                TABLE_GOALS,
                values,
                "user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (updated > 0) {
            return true;
        }

        values.put("user_id", userId);
        long result = db.insert(TABLE_GOALS, null, values);
        return result != -1;
    }

    public double getGoalWeight(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT goal_weight FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        double goalWeight = 0;

        try {
            if (cursor.moveToFirst()) {
                goalWeight = cursor.getDouble(0);
            }
        } finally {
            cursor.close();
        }

        return goalWeight;
    }

    public String getPhoneNumber(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT phone_number FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        String phoneNumber = "";

        try {
            if (cursor.moveToFirst()) {
                phoneNumber = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        return phoneNumber == null ? "" : phoneNumber;
    }

    private static String toStorageDate(String displayDate) {
        if (displayDate == null) {
            return "";
        }

        String trimmed = displayDate.trim();
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return trimmed;
        }

        if (trimmed.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return trimmed.substring(6, 10) + "-"
                    + trimmed.substring(0, 2) + "-"
                    + trimmed.substring(3, 5);
        }

        return trimmed;
    }
}
