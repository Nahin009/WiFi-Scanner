package com.example.myapplication;

import static androidx.core.app.ServiceCompat.startForeground;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;

    private WifiManager wifiManager;
    private TextView wifiInfoTextView;
    private ListView wifiListView;
    private TextView displayArea;
    private ArrayAdapter<String> wifiListAdapter;

    private EditText roomNo_pos;
    private EditText timeInterval;
    private EditText studentID;
    private EditText noOfTests;
    private String roomNoPos;
    private HashMap<Integer, wifiNodeList> listWifi;
    private LocalDateTime currentDateTime;
    private DateTimeFormatter formatter;
    private String formattedDateTime;
    private String student_id;

    private long SCAN_INTERVAL = 15000; // 15 seconds
    private int COUNT_LIMIT = 20;

    private int batteryLevelBefore = -1;
    private int batteryLevelAfter = -1;


    private int count = 0;
    private Handler handler = new Handler();
    private Runnable scanRunnable;

    private DatabaseReference mDatabase;
    private FirebaseDatabase db;

    AlarmManager alarmManager;

    Intent intent;
    PendingIntent pendingIntent;

    private static final int REQUEST_CODE = 100;


    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void writeToDatabase(){
        currentDateTime = LocalDateTime.now();
        formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy: HH:mm:ss");
        formattedDateTime = currentDateTime.format(formatter);


        String model = Build.MODEL;


        wifiList finalData = new wifiList(model, formattedDateTime, student_id, listWifi);

        DatabaseReference ref1 = mDatabase.child(roomNoPos).push();
//        DatabaseReference ref2 = ref1.push();
        DatabaseReference ref2 = ref1;

        batteryLevelAfter = getBatteryLevel();

        ref2.child("DeviceModel").setValue(finalData.getDeviceModel());
        ref2.child("DateTime").setValue(finalData.getFormattedDateTime());
        ref2.child("StudentID").setValue(finalData.getStudentID());
        ref2.child("BatteryLifeBefore").setValue(batteryLevelBefore);
        ref2.child("BatteryLifeAfter").setValue(batteryLevelAfter);

//        DatabaseReference ref3 = ref2.child("ScanList").push();
        DatabaseReference ref3 = ref2.child("ScanList");
        System.out.println("finalData.getlistOfWifiList = " + finalData.getlistOfWifiList().size());
        for(int i = 1; i <= COUNT_LIMIT; i++){
//            DatabaseReference ref4 = ref3.child(i + "").push();
            DatabaseReference ref4 = ref3.child(i + "");
            HashMap<Integer, wifiNode> wifiNodeHashMap = Objects.requireNonNull(finalData.getlistOfWifiList().get(i)).getNodeList();
            System.out.println("wifiNodeHashMap = " + wifiNodeHashMap.size());
            for(int j = 1; j <= wifiNodeHashMap.size(); j++){
                DatabaseReference ref5 = ref4.child(j + "");
                wifiNode wifiNode = wifiNodeHashMap.get(j);
                assert wifiNode != null;
                ref5.child("SSID").setValue(wifiNode.getSSID());
                ref5.child("Strength").setValue(wifiNode.getStrength());
                ref5.child("MacAddress").setValue(wifiNode.getMacAddress());
                ref5.child("Frequency").setValue(wifiNode.getFrequency());
            }
        }

        displayArea.setText("Congratulations! Data has been written to the database successfully!!!  Go and check the data in " + roomNoPos);

        // Set the alarm to trigger immediately
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
    }


    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void checkAndStartWifiService() {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Request necessary permissions
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                android.Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_CODE
                );
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_CODE
                );
                return;
            }
        }

        // Start the foreground service if permissions are granted
        startWifiForegroundService();
    }

    private void startWifiForegroundService() {
        Intent serviceIntent = new Intent(this, WifiForegroundService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            boolean permissionsGranted = true;

            // Check if all permissions were granted
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = false;
                    break;
                }
            }

            // If all required permissions are granted, start the foreground service
            if (permissionsGranted) {
                startWifiForegroundService();
            } else {
                // Show a toast to the user about the permission requirement
                Toast.makeText(this, "Permissions are required to start the service", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always show the name input screen
        setContentView(R.layout.activity_name_input);

        listWifi = new HashMap<>();

        db = FirebaseDatabase.getInstance("https://wifi-signal-89666-default-rtdb.asia-southeast1.firebasedatabase.app/");
        mDatabase = db.getReference();

//        // Start the foreground service
//        Intent serviceIntent = new Intent(this, WifiForegroundService.class);
//        ContextCompat.startForegroundService(this, serviceIntent);

        // Check and request permissions before starting the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            checkAndStartWifiService();
        }

        initializeNameInputScreen();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel the alarm
        alarmManager.cancel(pendingIntent);
        // Unregister the BroadcastReceiver when the app is destroyed
        unregisterReceiver(wifiScanReceiver);

        listWifi.clear();

//        // Stop the foreground service
        Intent serviceIntent = new Intent(this, WifiForegroundService.class);
        stopService(serviceIntent);

        // Remove any pending scan runnable
        handler.removeCallbacks(scanRunnable);
    }

    private static boolean isValidLong(String str) {
        try {
            // Try parsing the string
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            // Catch the exception if parsing fails
            return false;
        }
    }

    private static boolean isValidInt(String str) {
        try {
            // Try parsing the string
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            // Catch the exception if parsing fails
            return false;
        }
    }

    private void initializeNameInputScreen() {

        studentID = findViewById(R.id.studentId);
        roomNo_pos = findViewById(R.id.roomNo);
        timeInterval = findViewById(R.id.timeInterval);
        noOfTests = findViewById(R.id.noOfTests);
        
        Button submitButton = findViewById(R.id.submitButton);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the user name in SharedPreferences
                student_id = studentID.getText().toString().trim();
                roomNoPos = roomNo_pos.getText().toString().trim();
                String timeIntervalString = timeInterval.getText().toString().trim();
                String noOfTestsString = noOfTests.getText().toString().trim();

                if (!student_id.isEmpty() && !roomNoPos.isEmpty() && timeIntervalString.isEmpty() && noOfTestsString.isEmpty()) {
                    // Proceed to the main WiFi screen
                    setContentView(R.layout.activity_main);
                    initializeWifiScreen();


                } else if (roomNoPos.isEmpty()) {
                    // Show a message indicating that the name cannot be empty
                    Toast.makeText(MainActivity.this, "Please enter Room No", Toast.LENGTH_SHORT).show();
                }
                else if (student_id.isEmpty()) {
                    // Show a message indicating that the name cannot be empty
                    Toast.makeText(MainActivity.this, "Please enter Student ID", Toast.LENGTH_SHORT).show();
                }
                else if (!timeIntervalString.isEmpty() && noOfTestsString.isEmpty()) {
                    if (!isValidLong(timeIntervalString)) {
                        Toast.makeText(MainActivity.this, "ULTAPALTA LIKHISH NAH", Toast.LENGTH_SHORT).show();
                    } else {
                        SCAN_INTERVAL = Long.parseLong(timeIntervalString) * 1000;
                        if (SCAN_INTERVAL <= 0) {
                            Toast.makeText(MainActivity.this, "TIME MACHINE BANAYE NEGATIVE NUMBER DISH", Toast.LENGTH_SHORT).show();
                        } else {
                            // Proceed to the main WiFi screen
                            setContentView(R.layout.activity_main);
                            initializeWifiScreen();
                        }
                    }
                } else if (timeIntervalString.isEmpty()) {
                    if (!isValidInt(noOfTestsString)) {
                        Toast.makeText(MainActivity.this, "ULTAPALTA LIKHISH NAH", Toast.LENGTH_SHORT).show();
                    } else {
                        COUNT_LIMIT = Integer.parseInt(noOfTestsString);
                        if (COUNT_LIMIT <= 0) {
                            Toast.makeText(MainActivity.this, "TIME MACHINE BANAYE NEGATIVE NUMBER DISH", Toast.LENGTH_SHORT).show();
                        } else {
                            // Proceed to the main WiFi screen
                            setContentView(R.layout.activity_main);
                            initializeWifiScreen();
                        }
                    }
                } else {
                    if (!isValidLong(timeIntervalString) || !isValidInt(timeIntervalString) ) {
                        Toast.makeText(MainActivity.this, "ULTAPALTA LIKHISH NAH", Toast.LENGTH_SHORT).show();
                    } else {
                        COUNT_LIMIT = Integer.parseInt(noOfTestsString);
                        SCAN_INTERVAL = Long.parseLong(timeIntervalString) * 1000;

                        if (COUNT_LIMIT <= 0 || SCAN_INTERVAL <= 0) {
                            Toast.makeText(MainActivity.this, "TIME MACHINE BANAYE NEGATIVE NUMBER DISH", Toast.LENGTH_SHORT).show();
                        } else {
                            // Proceed to the main WiFi screen
                            setContentView(R.layout.activity_main);
                            initializeWifiScreen();
                        }
                    }

                }
            }
        });
    }

    private void initializeWifiScreen() {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        displayArea = findViewById(R.id.displayArea);
        batteryLevelBefore = getBatteryLevel();


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check for Wi-Fi permissions
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions are granted, proceed with Wi-Fi scanning
            wifiManager.startScan();
        } else {
            // Request Wi-Fi permissions
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

        wifiInfoTextView = findViewById(R.id.wifiInfoTextView);
        wifiListView = findViewById(R.id.wifiListView);

        wifiListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        wifiListView.setAdapter(wifiListAdapter);

        // Check if Wi-Fi is enabled
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // Schedule periodic WiFi scans
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                wifiManager.startScan();
                handler.postDelayed(this, SCAN_INTERVAL);
            }
        };
        // Schedule the first WiFi scan immediately
        handler.post(scanRunnable);

        // Register the BroadcastReceiver to receive Wi-Fi scan results
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

//        // Start Wi-Fi scan
//        wifiManager.startScan();
    }

    // BroadcastReceiver to handle Wi-Fi scan results
    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
                // Check for Wi-Fi permissions
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Permissions are granted, proceed with Wi-Fi scanning
                    List<ScanResult> scanResults = wifiManager.getScanResults();

                    // Display Wi-Fi information
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    wifiInfoTextView.setText("Connected to: " + wifiInfo.getSSID() + "\nStrength: " + wifiInfo.getRssi() + " dBm");



                    // Display the list of available Wi-Fi networks
                    wifiListAdapter.clear();
                    for (ScanResult result : scanResults) {
                        wifiListAdapter.add(result.SSID + "  |  Strength: " + result.level + " dBm" + "  |  macAddress: " + result.BSSID);
                    }

                    if(count < COUNT_LIMIT){
                        count++;
//
                        wifiNodeList wifiNodeList = new wifiNodeList();
                        HashMap<Integer, wifiNode> wifiNodeHashMap = new HashMap<>();
                        for (int i = 0; i < scanResults.size(); i++) {
                            wifiNode wifiNode = new wifiNode(scanResults.get(i).SSID, scanResults.get(i).level, scanResults.get(i).BSSID, scanResults.get(i).frequency);
                            wifiNodeHashMap.put(i + 1, wifiNode);
                        }
                        wifiNodeList.setNodeList(wifiNodeHashMap);
                        listWifi.put(count, wifiNodeList);
                        if (count == COUNT_LIMIT)
                        {
                            displayArea.setText("ALL " + COUNT_LIMIT + " Scan complete!...Now writing to database");
                            writeToDatabase();
                            count++;
                        }
                        else displayArea.setText("SCANNED..... " + count + "/" + COUNT_LIMIT + " times......Next Scan is Running...");

                    } else if(count >= COUNT_LIMIT + 1){

                        count++;
                        displayArea.setText("Enough Time Wasted... Close the APP and proceed for next test or check the data in " + roomNoPos);
                    }


                    // Notify the adapter that the data set has changed
                    wifiListAdapter.notifyDataSetChanged();

                } else {
                    // Permissions are not granted, request Wi-Fi permissions
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                }

        }
    };

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted, proceed with Wi-Fi scanning
//                wifiManager.startScan();
//            } else {
//                // Permission denied, handle accordingly (e.g., show a message to the user)
//                // You may choose to disable Wi-Fi functionality or inform the user about the need for permissions.
//            }
//        }
//    }

    private int getBatteryLevel() {
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                return (int) ((level / (float) scale) * 100);
            }
        }
        return -1; // Return -1 if battery level can't be determined
    }

}
