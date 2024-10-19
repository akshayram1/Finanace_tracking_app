package com.example.transac1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private Pattern combinedRegex;
    private TextView tvParsingStatus, tvParsedSms;
    private Button btnParseSms;

    private DatabaseReference mDatabase;
    private TextView tvFirebaseStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        tvParsingStatus = findViewById(R.id.tv_parsing_status);
        tvParsedSms = findViewById(R.id.tv_parsed_sms);
        btnParseSms = findViewById(R.id.btn_parse_sms);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference("financialMessages");

        // Firebase status TextView
        tvFirebaseStatus = findViewById(R.id.tv_firebase_status);
        tvFirebaseStatus.setText("Firebase Status: Waiting...");


        // Define the combined regex pattern for financial SMS parsing
        combinedRegex = Pattern.compile(
                "Dear SBI UPI User, ur A/c([A-Za-z0-9]+)\\s+(?<typecred>[A-Za-z]+)\\s+by Rs([+-]?(?:\\d+)?(?:\\.?\\d*))\\s*" +
                        "(?: on ([A-Za-z0-9]+))?\\s*" +
                        "(?: by \\(Ref no (\\d+)\\))?" +
                        "|Dear UPI user A/C ([A-Za-z0-9]+) (?<typedeb>[A-Za-z]+) by ([+-]?(?:\\d+)?(?:\\.?\\d*))" +
                        "(?: on date ([A-Za-z0-9]+))?" +
                        "(?: trf to ([A-Za-z0-9]+(?: [A-Za-z]+)*)(?: Refno (\\d+)))" +
                        "\\.(?: If not u\\? call 1800111109)?\\.?(?: -([A-Za-z]+))?",
                Pattern.CASE_INSENSITIVE
        );



//// Credit SMS Pattern
//        String creditRegex = "Dear SBI User, your A/c ([A-Za-z0-9-]+)-(?<typecred>credited) by Rs\\.?(\\d+(?:\\.\\d+)?)" +
//                "\\s+on\\s+(\\d{2}[A-Za-z]{3}\\d{2})\\s+transfer\\s+from\\s+([A-Za-z\\s]+)\\s+Ref\\s+No\\s+(\\d+)\\s*-SBI";
//
//// Debit SMS Pattern
//        String debitRegex = "Dear UPI user A\\/C\\s+([A-Za-z0-9-]+)\\s+(?<typedeb>debited)\\s+by\\s+(\\d+\\.\\d+)\\s+" +
//                "on\\s+date\\s+(\\d{2}[A-Za-z]{3}\\d{2})\\s+trf\\s+to\\s+([A-Za-z\\s]+)\\s+Refno\\s+(\\d+)\\." +
//                "\\s+If\\s+not\\s+u\\?\\s+call\\s+1800111109\\.\\s+-SBI";
//
//// Compile patterns
//        Pattern creditPattern = Pattern.compile(creditRegex, Pattern.CASE_INSENSITIVE);
//        Pattern debitPattern = Pattern.compile(debitRegex, Pattern.CASE_INSENSITIVE);



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
    private void writeToFirebase(String accountNumber, String transactionType, String amount, String transactionDate, String referenceNo) {
        String messageId = mDatabase.push().getKey();
        FinancialMessage message = new FinancialMessage(accountNumber, transactionType, amount, transactionDate, referenceNo);

        if (messageId != null) {
            mDatabase.child(messageId).setValue(message).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    tvFirebaseStatus.setText("Firebase Status: Data Uploaded");
                } else {
                    tvFirebaseStatus.setText("Firebase Status: Upload Failed");
                }
            });
        }
    }

    // Function to parse financial SMS using regex and add to Firebase
    private void parseFinancialSms(String smsBody) {
        Matcher matcher = combinedRegex.matcher(smsBody);
        if (matcher.find()) {
            String accountNumber = matcher.group(1) != null ? matcher.group(1) : matcher.group(6);
            String transactionType = matcher.group("typecred") != null ? matcher.group("typecred") : matcher.group("typedeb");
            String amount = matcher.group(3) != null ? matcher.group(3) : matcher.group(7);
            String transactionDate = matcher.group(4) != null ? matcher.group(4) : matcher.group(8);
            String referenceNo = matcher.group(5) != null ? matcher.group(5) : matcher.group(10);

            // Append parsed data to the TextView
            String result = "Account: " + accountNumber + "\nType: " + transactionType + "\nAmount: " + amount +
                    "\nDate: " + transactionDate + "\nRef No: " + referenceNo + "\n\n";
            tvParsedSms.append(result);

            // Write data to Firebase
            writeToFirebase(accountNumber, transactionType, amount, transactionDate, referenceNo);
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