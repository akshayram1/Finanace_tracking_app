package com.example.transac1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private TextView tvParsingStatus, tvParsedSms, tvFirebaseStatus;
    private EditText etUserName;
    private Button btnParseSms;
    private DatabaseReference mDatabase;

    // Separate patterns for credit and debit messages
    private Pattern creditPattern;
    private Pattern debitPattern;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        tvParsingStatus = findViewById(R.id.tv_parsing_status);
        tvParsedSms = findViewById(R.id.tv_parsed_sms);
        etUserName = findViewById(R.id.et_user_name);
        btnParseSms = findViewById(R.id.btn_parse_sms);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference("financialMessages");

        // Firebase status TextView
        tvFirebaseStatus = findViewById(R.id.tv_firebase_status);
        tvFirebaseStatus.setText("Firebase Status: Waiting...");

        // Define separate regex patterns for credit and debit transactions
        creditPattern = Pattern.compile(
                "Dear SBI User, your A/c ([A-Za-z0-9-]+)-credited by Rs\\.?(\\d+(?:\\.\\d+)?)" +
                        "\\s+on\\s+(\\d{2}[A-Za-z]{3}\\d{2})\\s+transfer\\s+from\\s+([A-Za-z\\s]+)\\s+Ref\\s+No\\s+(\\d+)\\s*-SBI",
                Pattern.CASE_INSENSITIVE
        );

        debitPattern = Pattern.compile(
                "Dear UPI user A\\/C\\s+([A-Za-z0-9-]+)\\s+debited\\s+by\\s+(\\d+\\.\\d+)\\s+" +
                        "on\\s+date\\s+(\\d{2}[A-Za-z]{3}\\d{2})\\s+trf\\s+to\\s+([A-Za-z\\s]+)\\s+Refno\\s+(\\d+)\\." +
                        "\\s+If\\s+not\\s+u\\?\\s+call\\s+1800111109\\.\\s+-SBI",
                Pattern.CASE_INSENSITIVE
        );

        // Check and request SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            initializeSmsParsing();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        }

        // Set onClickListener for the button
        btnParseSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvParsingStatus.setText("Scanning SMS...");
                readSmsAndParse();
            }
        });
    }

    // Function to write parsed SMS data to Firebase
    private void writeToFirebase(String accountNumber, String transactionType, String amount, String transactionDate, String referenceNo, String personName) {
        String userName = etUserName.getText().toString().trim();
        if (userName.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract month from transactionDate (format: 12Oct24 -> Oct)
        String month = transactionDate.substring(2, 5);

        // Create the path under the user's name, then under the month
        DatabaseReference userRef = mDatabase.child(userName).child(month);

        String messageId = userRef.push().getKey();
        FinancialMessage message = new FinancialMessage(accountNumber, transactionType, amount, transactionDate, referenceNo, personName);

        if (messageId != null) {
            userRef.child(messageId).setValue(message).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    tvFirebaseStatus.setText("Firebase Status: Data Uploaded");
                } else {
                    tvFirebaseStatus.setText("Firebase Status: Upload Failed");
                }
            });
        }
    }

    // Function to parse financial SMS using separate regex for credit and debit
    private void parseFinancialSms(String smsBody) {
        Matcher creditMatcher = creditPattern.matcher(smsBody);
        Matcher debitMatcher = debitPattern.matcher(smsBody);

        if (creditMatcher.find()) {
            // Extracting information from credit message
            String accountNumber = creditMatcher.group(1);
            String amount = creditMatcher.group(2);
            String transactionDate = creditMatcher.group(3);
            String personName = creditMatcher.group(4).trim(); // Extracting name
            String referenceNo = creditMatcher.group(5);

            writeToFirebase(accountNumber, "credited", amount, transactionDate, referenceNo, personName);

            // Append parsed data to the TextView (optional, for UI display)
            String result = "Account: " + accountNumber + "\nType: credited\nAmount: " + amount +
                    "\nDate: " + transactionDate + "\nRef No: " + referenceNo + "\nFrom: " + personName + "\n\n";
            tvParsedSms.append(result);

        } else if (debitMatcher.find()) {
            // Extracting information from debit message
            String accountNumber = debitMatcher.group(1);
            String amount = debitMatcher.group(2);
            String transactionDate = debitMatcher.group(3);
            String personName = debitMatcher.group(4).trim(); // Extracting name
            String referenceNo = debitMatcher.group(5);

            writeToFirebase(accountNumber, "debited", amount, transactionDate, referenceNo, personName);

            // Append parsed data to the TextView (optional, for UI display)
            String result = "Account: " + accountNumber + "\nType: debited\nAmount: " + amount +
                    "\nDate: " + transactionDate + "\nRef No: " + referenceNo + "\nTo: " + personName + "\n\n";
            tvParsedSms.append(result);
        }
    }

    private void initializeSmsParsing() {
        btnParseSms.setEnabled(true);  // Enable the button
    }

    // Function to read SMS and parse financial messages
    private void readSmsAndParse() {
        try {
            Uri uri = Uri.parse("content://sms/inbox");
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String smsBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    parseFinancialSms(smsBody);
                }
                cursor.close();
            }

            tvParsingStatus.setText("Parsing Completed!");

        } catch (Exception e) {
            e.printStackTrace();
            tvParsingStatus.setText("Error occurred while parsing SMS");
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSmsParsing();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}