package com.example.brenabishopweighttracker;

import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;

/**
 * Main activity for the Weight Tracker app.
 * Focuses on screen behavior and delegates validation, data access,
 * goal evaluation, and SMS delivery to focused components.
 */
public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 100;

    private WeightRepository repository;
    private GoalEvaluator goalEvaluator;
    private TrendAnalyzer trendAnalyzer;
    private SmsNotificationService smsNotificationService;

    private int currentUserId = -1;
    private int selectedWeightId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        repository = new WeightRepository(databaseHelper);
        goalEvaluator = new GoalEvaluator();
        trendAnalyzer = new TrendAnalyzer();
        smsNotificationService = new SmsNotificationService(this);

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

            String validationMessage = InputValidator.validateCredentials(username, password);
            if (validationMessage != null) {
                showShortMessage(validationMessage);
                return;
            }

            int userId = repository.loginUser(username, password);

            if (userId != -1) {
                currentUserId = userId;
                showWeightHistoryScreen();
            } else {
                showShortMessage("Invalid login. Try again or create an account.");
            }
        });

        buttonCreateAccount.setOnClickListener(view -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            String validationMessage = InputValidator.validateCredentials(username, password);
            if (validationMessage != null) {
                showShortMessage(validationMessage);
                return;
            }

            boolean created = repository.createUser(username, password);

            if (created) {
                showShortMessage("Account created. You can now log in.");
            } else {
                showShortMessage("Username already exists.");
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
        Button buttonLogout = findViewById(R.id.buttonLogout);

        double goalWeight = repository.getGoalWeight(currentUserId);

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

        buttonLogout.setOnClickListener(view -> {
            currentUserId = -1;
            selectedWeightId = -1;
            showLoginScreen();
        });
    }

    private void loadWeightGrid(
            GridLayout gridWeightHistory,
            TextView textProgressMessage,
            double goalWeight
    ) {
        gridWeightHistory.removeAllViews();
        gridWeightHistory.setColumnCount(4);

        addGridText(gridWeightHistory, "Date", true);
        addGridText(gridWeightHistory, "Weight", true);
        addGridText(gridWeightHistory, "Update", true);
        addGridText(gridWeightHistory, "Delete", true);

        List<WeightEntry> history = repository.getWeightHistory(currentUserId);

        // The repository returns entries oldest-to-newest for analysis.
        // Display them newest-first to preserve the original dashboard behavior.
        for (int index = history.size() - 1; index >= 0; index--) {
            WeightEntry entry = history.get(index);
            int weightId = entry.getId();
            String date = entry.getEntryDate();
            double weight = entry.getWeight();

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
            deleteButton.setOnClickListener(view -> deleteWeight(weightId));
            gridWeightHistory.addView(deleteButton);
        }

        TrendAnalyzer.TrendResult trendResult = trendAnalyzer.analyze(history, goalWeight);
        updateProgressMessage(textProgressMessage, trendResult, goalWeight);
    }

    private void deleteWeight(int weightId) {
        boolean deleted = repository.deleteWeight(currentUserId, weightId);

        if (deleted) {
            showShortMessage("Weight entry deleted.");
            showWeightHistoryScreen();
        } else {
            showShortMessage("Unable to delete weight entry.");
        }
    }

    private void updateProgressMessage(
            TextView textProgressMessage,
            TrendAnalyzer.TrendResult trendResult,
            double goalWeight
    ) {
        TrendAnalyzer.TrendDirection direction = trendResult.getDirection();

        if (direction == TrendAnalyzer.TrendDirection.NO_DATA) {
            textProgressMessage.setText("No weight entries yet.");
            return;
        }

        double latestWeight = trendResult.getLatestWeight();

        if (direction == TrendAnalyzer.TrendDirection.NO_GOAL) {
            textProgressMessage.setText(
                    "Latest Weight: " + formatWeight(latestWeight) + " lbs | "
                            + "Set a goal to calculate your trend."
            );
            return;
        }

        String progressText;
        if (direction == TrendAnalyzer.TrendDirection.GOAL_REACHED) {
            progressText = "Latest Weight: " + formatWeight(latestWeight)
                    + " lbs | Goal reached!";
        } else {
            double difference = Math.abs(latestWeight - goalWeight);
            progressText = "Latest Weight: " + formatWeight(latestWeight)
                    + " lbs | " + formatWeight(difference) + " lbs from goal";
        }

        if (direction == TrendAnalyzer.TrendDirection.INSUFFICIENT_DATA) {
            progressText += "\nRecent Trend: Add one more entry to calculate a trend.";
        } else if (trendResult.getEntriesAnalyzed() >= 2) {
            progressText += "\nRecent Trend: " + getTrendLabel(direction)
                    + " | " + formatWeight(trendResult.getDistanceChange())
                    + " lbs " + getDistanceChangeLabel(direction)
                    + " over " + trendResult.getEntriesAnalyzed() + " entries"
                    + " | Avg change: "
                    + formatSignedWeight(trendResult.getAverageChange())
                    + " lbs/entry";
        }

        textProgressMessage.setText(progressText);
    }

    private String getTrendLabel(TrendAnalyzer.TrendDirection direction) {
        switch (direction) {
            case TOWARD_GOAL:
                return "Moving toward goal";
            case AWAY_FROM_GOAL:
                return "Moving away from goal";
            case STABLE:
                return "Stable";
            case GOAL_REACHED:
                return "Goal reached";
            default:
                return "Not enough data";
        }
    }

    private String getDistanceChangeLabel(TrendAnalyzer.TrendDirection direction) {
        if (direction == TrendAnalyzer.TrendDirection.AWAY_FROM_GOAL) {
            return "farther from goal";
        }

        if (direction == TrendAnalyzer.TrendDirection.STABLE) {
            return "change in distance from goal";
        }

        return "closer to goal";
    }

    private String formatWeight(double value) {
        return String.format("%.1f", value);
    }

    private String formatSignedWeight(double value) {
        return String.format("%+.1f", value);
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

    private void showWeightSettingsScreen(
            boolean isUpdate,
            String existingDate,
            String existingWeight
    ) {
        setContentView(R.layout.activity_weight_settings);

        TextView textSettingsTitle = findViewById(R.id.textSettingsTitle);
        EditText editTextWeightDate = findViewById(R.id.editTextWeightDate);
        EditText editTextDailyWeight = findViewById(R.id.editTextDailyWeight);
        EditText editTextGoalWeight = findViewById(R.id.editTextGoalWeight);
        EditText editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);

        Button buttonSaveWeight = findViewById(R.id.buttonSaveWeight);
        Button buttonSaveGoal = findViewById(R.id.buttonSaveGoal);
        Button buttonEnableSms = findViewById(R.id.buttonEnableSms);
        Button buttonBackToDashboard = findViewById(R.id.buttonBackToDashboard);

        TextView textSmsStatus = findViewById(R.id.textSmsStatus);

        editTextWeightDate.setOnClickListener(view -> showDatePicker(editTextWeightDate));

        double currentGoal = repository.getGoalWeight(currentUserId);
        String savedPhoneNumber = repository.getPhoneNumber(currentUserId);

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

        buttonSaveWeight.setOnClickListener(view -> saveWeight(
                isUpdate,
                editTextWeightDate,
                editTextDailyWeight
        ));

        buttonSaveGoal.setOnClickListener(view -> saveGoal(
                editTextGoalWeight,
                editTextPhoneNumber
        ));

        buttonEnableSms.setOnClickListener(view -> requestSmsPermission());
        buttonBackToDashboard.setOnClickListener(view -> showWeightHistoryScreen());
    }

    private void saveWeight(
            boolean isUpdate,
            EditText editTextWeightDate,
            EditText editTextDailyWeight
    ) {
        String date = editTextWeightDate.getText().toString().trim();
        String weightText = editTextDailyWeight.getText().toString().trim();

        if (date.isEmpty()) {
            showShortMessage("Enter a date and weight.");
            return;
        }

        InputValidator.NumericValidationResult validation =
                InputValidator.validatePositiveNumber(weightText, "weight");

        if (!validation.isValid()) {
            showShortMessage(validation.getErrorMessage());
            return;
        }

        double weight = validation.getValue();
        boolean saved;

        if (isUpdate && selectedWeightId != -1) {
            saved = repository.updateWeight(currentUserId, selectedWeightId, date, weight);
        } else {
            saved = repository.addWeight(currentUserId, date, weight);
        }

        if (saved) {
            showShortMessage("Weight saved.");
            evaluateGoalAndNotify(weight);
            showWeightHistoryScreen();
        } else {
            showShortMessage("Unable to save weight.");
        }
    }

    private void saveGoal(EditText editTextGoalWeight, EditText editTextPhoneNumber) {
        String goalText = editTextGoalWeight.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        InputValidator.NumericValidationResult validation =
                InputValidator.validatePositiveNumber(goalText, "goal weight");

        if (!validation.isValid()) {
            showShortMessage(validation.getErrorMessage());
            return;
        }

        boolean saved = repository.saveGoalWeight(
                currentUserId,
                validation.getValue(),
                phoneNumber
        );

        if (saved) {
            showShortMessage("Goal weight saved.");
        } else {
            showShortMessage("Unable to save goal weight.");
        }
    }

    private void requestSmsPermission() {
        if (smsNotificationService.hasSmsPermission()) {
            showShortMessage("SMS permission already granted.");
            return;
        }

        smsNotificationService.requestSmsPermission(this, SMS_PERMISSION_REQUEST_CODE);
    }

    private void updateSmsStatusText(TextView textSmsStatus) {
        textSmsStatus.setText(smsNotificationService.getPermissionStatusText());
    }

    private void evaluateGoalAndNotify(double currentWeight) {
        double goalWeight = repository.getGoalWeight(currentUserId);

        if (!goalEvaluator.isGoalReached(currentWeight, goalWeight)) {
            return;
        }

        String phoneNumber = repository.getPhoneNumber(currentUserId);
        SmsNotificationService.SendResult result =
                smsNotificationService.sendGoalReachedMessage(phoneNumber);

        String smsMessage;

        switch (result) {
            case SENT:
                smsMessage = "A congratulations text was also sent to your saved phone number.";
                break;

            case MISSING_PHONE_NUMBER:
                smsMessage = "Add a phone number if you would like to receive goal notifications by text.";
                break;

            case PERMISSION_REQUIRED:
                smsMessage = "SMS permission is not enabled, but your goal has still been reached.";
                break;

            default:
                smsMessage = "";
                break;
        }

        showGoalReachedDialog(currentWeight, goalWeight, smsMessage);
    }

    private void showGoalReachedDialog(
            double currentWeight,
            double goalWeight,
            String additionalMessage
    ) {
        String message =
                "You reached your goal weight!\n\n"
                        + "Goal Weight: " + goalWeight + " lbs\n"
                        + "Current Weight: " + currentWeight + " lbs\n\n"
                        + additionalMessage;

        new AlertDialog.Builder(this)
                .setTitle("🎉 Goal Reached! 🎉")
                .setMessage(message)
                .setPositiveButton("Celebrate!", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
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

    private void showShortMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showLongMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showShortMessage("SMS permission granted.");
            } else {
                showLongMessage("SMS permission denied. App will still work without SMS alerts.");
            }
        }
    }
}
