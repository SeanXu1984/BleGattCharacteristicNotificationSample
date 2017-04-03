package com.example.administrator.blegattcharacteristicnotification;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

public class GattActivity extends AppCompatActivity {

    private static final String TAG = "GattActivity";
    private BleGattScanner mBleGattScanner;
    private BleGattAdvertiser mBleGattAdvertiser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gatt);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Button advertiseButton = (Button) findViewById(R.id.btnAdvertise);
        if (advertiseButton != null) {
            advertiseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        String errorMsg = "The Android Version of you device is too low "
                                + "to run BLE in Peripheral Mode. BLE peripheral mode is supported "
                                + "starting from Android 5.0";
                        Log.e(TAG, errorMsg);
                        Toast.makeText(GattActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                    else {
                        if (mBleGattAdvertiser != null) {
                            mBleGattAdvertiser.performBleGattAdvertising();
                        }
                    }
                }
            });
        }
        Button rescanButton = (Button) findViewById(R.id.btnRescan);
        if (rescanButton != null) {
            rescanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBleGattScanner != null) {
                        mBleGattScanner.performBleGattScan();
                    }
                }
            });
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        Button advertiseButton = (Button) findViewById(R.id.btnAdvertise);
        Button rescanButton = (Button) findViewById(R.id.btnRescan);
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.btnCentralMode:
                if (checked) {
                    // Pirates are the best
                    advertiseButton.setVisibility(View.GONE);
                    rescanButton.setVisibility(View.VISIBLE);
                    mBleGattScanner = new BleGattScanner(GattActivity.this);
                    break;
                }
            case R.id.btnPeripheralMode:
                if (checked) {
                    // Ninjas rule
                    rescanButton.setVisibility(View.GONE);
                    advertiseButton.setVisibility(View.VISIBLE);
                    mBleGattAdvertiser = new BleGattAdvertiser(GattActivity.this);
                    break;
                }
        }
    }

}
