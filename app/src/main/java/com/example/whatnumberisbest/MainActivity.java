package com.example.whatnumberisbest;

import static java.lang.Math.max;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_READ_CALL_LOG = 1;
    private static final int REQUEST_CODE_READ_CONTACTS = 2;
    private static final int REQUEST_CODE_PICK_CONTACT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check permissions and request if necessary
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CODE_READ_CONTACTS);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CODE_READ_CALL_LOG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_CONTACTS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CODE_READ_CALL_LOG);
            }
        } else if (requestCode == REQUEST_CODE_READ_CALL_LOG && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permissions granted
        } else {
            Toast.makeText(this, "Permissions are required to proceed", Toast.LENGTH_LONG).show();
        }
    }

    public void pickContact(View view) {
        pickContact();
    }

    public void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                String contactId = getContactId(contactUri);
                if (contactId != null) {
                    Map<String, Integer> callCounts = getCallCountsForContact(contactId);
                    displayCallCounts(callCounts);
                }
            }
        }
    }

    private String getContactId(Uri contactUri) {
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            cursor.close();
            return contactId;
        }
        return null;
    }

    private Map<String, Integer> getCallCountsForContact(String contactId) {
        Map<String, Integer> callCounts = new HashMap<>();

        Cursor phonesCursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null
        );

        if (phonesCursor != null) {
            while (phonesCursor.moveToNext()) {

                int index = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                index = max(index, 0);
                String phoneNumber = phonesCursor.getString(index);
                String[] parts_to_replace = {" ", "+46", "0046"};
                String[] parts_to_replace_with = {"", "0", "0"};
                int callCount = getCallCountForNumber(phoneNumber);
                // TODO: int totalDuration = getCallDurationForNumber(phoneNumber);

                for (int i = 0; i < parts_to_replace.length; i++) {
                    String part_to_replace = parts_to_replace[i];
                    String part_to_replace_with = parts_to_replace_with[i];
                    String newPhoneNumber = phoneNumber.replace(part_to_replace, part_to_replace_with);
                    callCount += getCallCountForNumber(newPhoneNumber);
                    // TODO: totalDuration += getCallDurationForNumber(newPhoneNumber);
                }
                callCounts.put(phoneNumber, callCount);
            }
            phonesCursor.close();
        }

        return callCounts;
    }

    private int getCallCountForNumber(String phoneNumber) {
        int count = 0;
        Uri callUri = Uri.parse("content://call_log/calls");
        Cursor cursor = getContentResolver().query(
                callUri,
                null,
                CallLog.Calls.NUMBER + " = ?",
                new String[]{phoneNumber},
                null
        );

        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }

        return count;
    }

    private void displayCallCounts(Map<String, Integer> callCounts) {
        TextView textView = findViewById(R.id.textView);
        StringBuilder displayText = new StringBuilder();

        for (Map.Entry<String, Integer> entry : callCounts.entrySet()) {
            displayText.append("Number: ").append(entry.getKey()).append(", Calls: ").append(entry.getValue()).append("\n");
        }

        textView.setText(displayText.toString());
    }
}
