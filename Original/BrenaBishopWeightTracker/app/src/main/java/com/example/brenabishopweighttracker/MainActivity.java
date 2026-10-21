package com.example.brenabishopweighttracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.DatePickerDialog;
import java.util.Calendar;

/**
 * Main activity for the Weight Tracker app.
 * Handles login, account creation, weight CRUD actions, goal weight, and SMS permission.
 */
public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 100;

    private DatabaseHelper databaseHelper;
    private int currentUserId = -1;
    private int selectedWeightId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = new DatabaseHelper(this);
        showLoginScreen();
    }

    private void showLoginScreen() {
        setContentView(R.layout.activity_login);

        EditText editTextUsername = findViewById(R.id.editTextUsername);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        buttonLogin.setOnClickListener(view -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter a username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            int userId = databaseHelper.loginUser(username, password);

            if (userId != -1) {
                currentUserId = userId;
                showWeightHistoryScreen();
            } else {
                Toast.makeText(this, "Invalid login. Try again or create an account.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonCreateAccount.setOnClickListener(view -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter a username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean created = databaseHelper.createUser(username, password);

            if (created) {
                Toast.makeText(this, "Account created. You can now log in.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Username already exists.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWeightHistoryScreen() {
        setContentView(R.layout.activity_weight_history);

        TextView textGoalWeight = findViewById(R.id.textGoalWeight);
        TextView textProgressMessage = findViewById(R.id.textProgressMessage);
        GridLayout gridWeightHistory = findViewById(R.id.gridWeightHistory);
        Button buttonAddWeight = findViewById(R.id.buttonAddWeight);
        Button buttonSetGoal = findViewById(R.id.buttonSetGoal);
        Button buttonSmsSettings = findViewById(R.id.buttonSmsSettings);

        double goalWeight = databaseHelper.getGoalWeight(currentUserId);

        if (goalWeight > 0) {
            textGoalWeight.setText("Goal Weight: " + goalWeight + " lbs");
        } else {
            textGoalWeight.setText("Goal Weight: Not set");
        }

        loadWeightGrid(gridWeightHistory, textProgressMessage, goalWeight);

        buttonAddWeight.setOnClickListener(view -> {
            selectedWeightId = -1;
            showWeightSettingsScreen(false, "", "");
        });

        buttonSetGoal.setOnClickListener(view -> {
            selectedWeightId = -1;
            showWeightSettingsScreen(false, "", "");
        });

        buttonSmsSettings.setOnClickListener(view -> requestSmsPermission());
    }

    private void loadWeightGrid(GridLayout gridWeightHistory, TextView textProgressMessage, double goalWeight) {
        gridWeightHistory.removeAllViews();
        gridWeightHistory.setColumnCount(4);

        addGridText(gridWeightHistory, "Date", true);
        addGridText(gridWeightHistory, "Weight", true);
        addGridText(gridWeightHistory, "Update", true);
        addGridText(gridWeightHistory, "Delete", true);

        Cursor cursor = databaseHelper.getWeightsForUser(currentUserId);

        double latestWeight = 0;

        while (cursor.moveToNext()) {
            int weightId = cursor.getInt(0);
            String date = cursor.getString(1);
            double weight = cursor.getDouble(2);

            if (latestWeight == 0) {
                latestWeight = weight;
            }

            addGridText(gridWeightHistory, date, false);
            addGridText(gridWeightHistory, weight + " lbs", false);

            Button updateButton = new Button(this);
            updateButton.setText("Edit");
            updateButton.setOnClickListener(view -> {
                selectedWeightId = weightId;
                showWeightSettingsScreen(true, date, String.valueOf(weight));
            });
            gridWeightHistory.addView(updateButton);

            Button deleteButton = new Button(this);
            deleteButton.setText("Delete");
            deleteButton.setOnClickListener(view -> {
                databaseHelper.deleteWeight(weightId);
                Toast.makeText(this, "Weight entry deleted.", Toast.LENGTH_SHORT).show();
                showWeightHistoryScreen();
            });
            gridWeightHistory.addView(deleteButton);
        }

        cursor.close();

        if (latestWeight == 0) {
            textProgressMessage.setText("No weight entries yet.");
        } else if (goalWeight > 0) {
            double difference = Math.abs(latestWeight - goalWeight);
            textProgressMessage.setText("Latest Weight: " + latestWeight + " lbs | " +
                    difference + " lbs from goal");
        } else {
            textProgressMessage.setText("Latest Weight: " + latestWeight + " lbs");
        }
    }

    private void addGridText(GridLayout gridLayout, String text, boolean isHeader) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setGravity(Gravity.CENTER);

        if (isHeader) {
            textView.setTextSize(16);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        gridLayout.addView(textView);
    }

    private void showWeightSettingsScreen(boolean isUpdate, String existingDate, String existingWeight) {
        setContentView(R.layout.activity_weight_settings);

        TextView textSettingsTitle = findViewById(R.id.textSettingsTitle);
        EditText editTextWeightDate = findViewById(R.id.editTextWeightDate);
        editTextWeightDate.setOnClickListener(view -> showDatePicker(editTextWeightDate));
        EditText editTextDailyWeight = findViewById(R.id.editTextDailyWeight);
        EditText editTextGoalWeight = findViewById(R.id.editTextGoalWeight);
        EditText editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);

        Button buttonSaveWeight = findViewById(R.id.buttonSaveWeight);
        Button buttonSaveGoal = findViewById(R.id.buttonSaveGoal);
        Button buttonEnableSms = findViewById(R.id.buttonEnableSms);
        Button buttonBackToDashboard = findViewById(R.id.buttonBackToDashboard);

        TextView textSmsStatus = findViewById(R.id.textSmsStatus);

        double currentGoal = databaseHelper.getGoalWeight(currentUserId);
        String savedPhoneNumber = databaseHelper.getPhoneNumber(currentUserId);

        if (currentGoal > 0) {
            editTextGoalWeight.setText(String.valueOf(currentGoal));
        }

        if (savedPhoneNumber != null && !savedPhoneNumber.isEmpty()) {
            editTextPhoneNumber.setText(savedPhoneNumber);
        }

        if (isUpdate) {
            textSettingsTitle.setText("Update Weight Entry");
            editTextWeightDate.setText(existingDate);
            editTextDailyWeight.setText(existingWeight);
            buttonSaveWeight.setText("Update Daily Weight");
        }

        updateSmsStatusText(textSmsStatus);

        buttonSaveWeight.setOnClickListener(view -> {
            String date = editTextWeightDate.getText().toString().trim();
            String weightText = editTextDailyWeight.getText().toString().trim();

            if (date.isEmpty() || weightText.isEmpty()) {
                Toast.makeText(this, "Enter a date and weight.", Toast.LENGTH_SHORT).show();
                return;
            }

            double weight = Double.parseDouble(weightText);
            boolean saved;

            if (isUpdate && selectedWeightId != -1) {
                saved = databaseHelper.updateWeight(selectedWeightId, date, weight);
            } else {
                saved = databaseHelper.addWeight(currentUserId, date, weight);
            }

            if (saved) {
                Toast.makeText(this, "Weight saved.", Toast.LENGTH_SHORT).show();
                checkGoalAndSendSms(weight);
                showWeightHistoryScreen();
            } else {
                Toast.makeText(this, "Unable to save weight.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonSaveGoal.setOnClickListener(view -> {
            String goalText = editTextGoalWeight.getText().toString().trim();
            String phoneNumber = editTextPhoneNumber.getText().toString().trim();

            if (goalText.isEmpty()) {
                Toast.makeText(this, "Enter a goal weight.", Toast.LENGTH_SHORT).show();
                return;
            }

            double goalWeight = Double.parseDouble(goalText);
            boolean saved = databaseHelper.saveGoalWeight(currentUserId, goalWeight, phoneNumber);

            if (saved) {
                Toast.makeText(this, "Goal weight saved.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to save goal weight.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonEnableSms.setOnClickListener(view -> requestSmsPermission());

        buttonBackToDashboard.setOnClickListener(view -> showWeightHistoryScreen());
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE
            );

        } else {
            Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSmsStatusText(TextView textSmsStatus) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            textSmsStatus.setText("SMS Permission Status: Granted");
        } else {
            textSmsStatus.setText("SMS Permission Status: Not enabled");
        }
    }

    private void checkGoalAndSendSms(double currentWeight) {
        double goalWeight = databaseHelper.getGoalWeight(currentUserId);
        String phoneNumber = databaseHelper.getPhoneNumber(currentUserId);

        if (goalWeight <= 0) {
            return;
        }

        if (currentWeight <= goalWeight) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {

                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(
                            phoneNumber,
                            null,
                            "Congratulations! You reached your goal weight.",
                            null,
                            null
                    );

                    Toast.makeText(this, "Goal reached. SMS sent.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Goal reached, but no phone number is saved.", Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(this, "Goal reached. SMS permission was not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDatePicker(EditText dateField) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    int displayMonth = month + 1;

                    String selectedDate = String.format(
                            "%02d/%02d/%04d",
                            displayMonth,
                            dayOfMonth,
                            year
                    );

                    dateField.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied. App will still work without SMS alerts.", Toast.LENGTH_LONG).show();
            }
        }
    }
}