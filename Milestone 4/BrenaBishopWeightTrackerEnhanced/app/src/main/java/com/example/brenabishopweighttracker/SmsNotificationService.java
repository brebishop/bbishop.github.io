package com.example.brenabishopweighttracker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Isolates Android SMS permission checks and message delivery from MainActivity.
 */
public class SmsNotificationService {

    public enum SendResult {
        SENT,
        PERMISSION_REQUIRED,
        MISSING_PHONE_NUMBER
    }

    private final Context context;

    public SmsNotificationService(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestSmsPermission(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.SEND_SMS},
                requestCode
        );
    }

    public String getPermissionStatusText() {
        return hasSmsPermission()
                ? "SMS Permission Status: Granted"
                : "SMS Permission Status: Not enabled";
    }

    public SendResult sendGoalReachedMessage(String phoneNumber) {
        if (!hasSmsPermission()) {
            return SendResult.PERMISSION_REQUIRED;
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return SendResult.MISSING_PHONE_NUMBER;
        }

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(
                phoneNumber,
                null,
                "Congratulations! You reached your goal weight.",
                null,
                null
        );

        return SendResult.SENT;
    }
}
