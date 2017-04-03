package com.example.administrator.blegattcharacteristicnotification;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2017/4/1 0001.
 */
public class BleGattScanner {
    private static final String TAG = "GattActivity";
    Activity activity;
    Handler mHandler;
    boolean mScanning;
    BluetoothAdapter mAdapter;
    BluetoothManager mManager;
    private boolean readyToScan = false;
    private BluetoothGattCallback mGattCallback;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private BleUuids mBleUuids;
    BluetoothGatt mBluetoothGatt;

    public BleGattScanner(Activity activity) {
        this.activity = activity;
        int sts;
        sts = bleCentralInit();
        if (0 > sts) {
            if (-1 == sts)
                Toast.makeText(activity, "this device is without bluetooth module",
                        Toast.LENGTH_LONG).show();

            if (-2 == sts)
                Toast.makeText(activity, "this device do not support Bluetooth low energy",
                        Toast.LENGTH_LONG).show();

            if (-3 == sts)
                Toast.makeText(activity, "this device do not have a BLE adapter, " +
                                "please buy nexus 6 or 9 then try again",
                        Toast.LENGTH_LONG).show();
        }
        else {
            Log.i(TAG, "BleGattScanner is ready to perform BLE scan");
            mBleUuids = new BleUuids();
            setGattCallback();
            setLeScanCallback();
            readyToScan = true;
        }
    }

    private boolean isReadyToScan() {
        return readyToScan;
    }

    public void performBleGattScan() {
        if (isReadyToScan()) {
            // Perform action on click
            // Stops scanning after 10 seconds.
            long SCAN_PERIOD = 100000;

            mHandler = new Handler();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if (mBleUuids == null) {
                mBleUuids = new BleUuids();
            }
            UUID[] uuids = {UUID.fromString(mBleUuids.getServiceUuid())};
            for (UUID uuid : uuids) {
                Log.i(TAG,"Let's scan the gatt service with service id " + uuid.toString());
            }
            mAdapter.startLeScan(uuids,mLeScanCallback);
        }
    }
    private int bleCentralInit() {
        if(null == mManager)
        {
            mManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            if(null == mManager)
                return -1;
            if(!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
                return -2;
        }
        if(null == mAdapter)
        {
            mAdapter = mManager.getAdapter();
            if(null == mAdapter)
                return -3;
        }
        return 0;
    }

    private void setGattCallback() {
        // BLE Central Side Code
        mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.w(TAG, "onServicesDiscovered succeeds");
                        List<BluetoothGattService> gattServices = gatt.getServices();
                        for (BluetoothGattService gattService : gattServices) {
                            if (gattService.getUuid().compareTo(UUID.fromString(mBleUuids.getServiceUuid())) == 0) {
                                List<BluetoothGattCharacteristic> gattCharacteristics =
                                        gattService.getCharacteristics();
                                for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {

                                    if (characteristic.getUuid().compareTo(UUID.fromString(mBleUuids.getCharacteristicUuid())) == 0) {
                                        Log.i(TAG, "Let's enable the notification for the 6th characteristic");
                                        boolean notificationStatus = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                                        Log.i(TAG, "Let's enable the notification for the descriptor of the 6th characteristic");
                                        if (notificationStatus) {
                                            Log.i(TAG, "Notification got turned on successfully for the 6th characteristic");
                                        }
                                        List<BluetoothGattDescriptor> gattDescs = characteristic.getDescriptors();
                                        for (BluetoothGattDescriptor gattDesc: gattDescs) {
                                            Log.i(TAG, "desc: uuid: " + gattDesc.getUuid());
                                        }
                                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                                UUID.fromString(mBleUuids.getNotificationDescriptorUuid()));
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        mBluetoothGatt.writeDescriptor(descriptor);
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        final byte[] data = characteristic.getValue();
                        if (data != null && data.length > 0) {
                            String chaValue = new String(data);
                            //Log.i(TAG, "The value of the characteristic is: " + stringBuilder.toString());
                            Log.i(TAG, "onCharacteristicRead: The value of the characteristic is: " + chaValue);
                        }
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                    Log.d(TAG, "onDescriptorWrite: ENABLE_NOTIFICATION_VALUE got written to the descriptor of the 6th characteristic successfully");
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    if (characteristic.getUuid().compareTo(UUID.fromString(mBleUuids.getCharacteristicUuid())) == 0) {
                        String tempString = new String(characteristic.getValue());
                        Log.i(TAG, "onCharacteristicChanged: the new value of the characteristic got modified to " + tempString);
                        gatt.readCharacteristic(characteristic);
                    }
                }
            };
    }

    private void setLeScanCallback() {
        mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Trying to connect the gatt server provided by device with MAC address: " + device.getAddress());
                            mBluetoothGatt = device.connectGatt(activity, false, mGattCallback);
                        }
                    });
                }
            };
    }
}