package com.example.administrator.blegattcharacteristicnotification;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2017/4/1 0001.
 */
public class BleGattAdvertiser {

    BluetoothGattServer mGattServer;
    private BleUuids mBleUuids;
    private BluetoothGattCharacteristic characteristic6;
    AdvertiseSettings.Builder settingBuilder;
    Activity activity;
    BluetoothManager mManager;
    BluetoothAdapter mAdapter;
    private static final String TAG = "GattActivity";
    BluetoothLeAdvertiser mLeAdvertiser;
    AdvertiseData.Builder advBuilder;
    private AdvertiseCallback mBleAdvCallback;
    boolean readyToAdvertise = false;

    private final BluetoothGattServerCallback mGattServerCallback
            = new BluetoothGattServerCallback(){

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState){
            Log.d("GattServer", "Our gatt server connection state changed, new state ");
            Log.d("GattServer", Integer.toString(newState));
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("GattServer", "Our gatt server service was added.");
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d("GattServer", "Our gatt characteristic was read.");
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d("GattServer", "onCharacteristicReadRequest: offset is " + offset);
            if (offset < characteristic.getValue().length) {
                if (characteristic.getValue().length >= (offset + 22)) {
                    byte[] data = Arrays.copyOfRange(characteristic.getValue(), offset, offset + 22);
                    Log.d("GattServer", "onCharacteristicReadRequest: let's send " + new String(data) + " to peripheral");
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0,
                            data);
                }
                else {
                    byte[] data = Arrays.copyOfRange(characteristic.getValue(), offset, offset + characteristic.getValue().length);
                    Log.d("GattServer", "onCharacteristicReadRequest: let's send " + new String(data) + " to peripheral");
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0,
                            data);
                }
            }
            else {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0,
                        Arrays.copyOfRange(characteristic.getValue(), 0, 0));
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            String chaValue = new String(value);
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d("GattServer", "The value from the write request is " + chaValue);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status)
        {
            Log.d("GattServer", "onNotificationSent");
            super.onNotificationSent(device, status);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d("GattServer", "Our gatt server descriptor was read.");
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GattServer", "Our gatt server descriptor was written.");
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            if (descriptor.getUuid().compareTo(UUID.fromString(mBleUuids.getNotificationDescriptorUuid())) == 0) {
                Log.i(TAG, "Let's change characteristic to see if notification is sent out or not!");
                characteristic6.setValue("Let's change characteristic to see if notification is sent out or not!");
                mGattServer.notifyCharacteristicChanged(device, characteristic6, false);
            }
        }

    };

    public BleGattAdvertiser(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            Log.e(TAG, "The Android Version of you device is too low to run in BLE Peripheral mode. "
                + " Please make sure your running an Android with version >= 5.0"
            );
            return;
        }
        this.activity = activity;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBleAdvCallback = new AdvertiseCallback() {

                @Override
                public void onStartFailure(int errorCode) {
                    Log.d("advertise", "onStartFailure");
                }

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.d("advertise", "onStartSuccess");
                }
            };
        }
        if(!isBluetoothEnabled())
        {
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // The REQUEST_ENABLE_BT constant passed to startActivityForResult() is a locally defined integer (which must be greater than 0), that the system passes back to you in your onActivityResult()
            // implementation as the requestCode parameter.
            int REQUEST_ENABLE_BT = 1;
            activity.startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
            Toast.makeText(activity, "Please enable bluetooth and execute the application again.",
                    Toast.LENGTH_LONG).show();
        }
        mBleUuids = new BleUuids();
        int sts;
        sts = init();
        if(0  > sts)
        {
            if(-1 == sts)
                Toast.makeText(activity, "this device is without bluetooth module",
                        Toast.LENGTH_LONG).show();
            if(-2 == sts)
                Toast.makeText(activity, "this device do not support Bluetooth low energy",
                        Toast.LENGTH_LONG).show();
            if(-3 == sts)
                Toast.makeText(activity, "this device do not support to be a BLE peripheral, " +
                                "please buy nexus 6 or 9 then try again",
                        Toast.LENGTH_LONG).show();
        }
        if(mAdapter == null)
            return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (mAdapter.isMultipleAdvertisementSupported() == false) {
                Log.w(TAG, "Multiple Advertisement Is Not Supported!");
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (mLeAdvertiser == null)
                mLeAdvertiser = mAdapter.getBluetoothLeAdvertiser();
            if (mLeAdvertiser == null) {
                String errMsg = "Your device is running an Android with Version >= 5.0. But we still can't "
                        + "get a BLE advertiser from the device. Most likely the manufacturer forgot to "
                        + "implement the BLE peripheral mode";
                Log.e(TAG, errMsg);
                Toast.makeText(activity, errMsg, Toast.LENGTH_LONG).show();
                return;
            }
        }
        readyToAdvertise = true;

    }

    public void performBleGattAdvertising() {
        if (readyToAdvertise) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Log.i(TAG, "Starting BLE Gatt Advertising");
                mLeAdvertiser.startAdvertising(settingBuilder.build(),
                        advBuilder.build(), mBleAdvCallback);
            }
        }
    }

    private boolean isBluetoothEnabled(){
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    private int init(){

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (null == settingBuilder) {
                settingBuilder = new AdvertiseSettings.Builder();
                settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
                settingBuilder.setConnectable(true);
                settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
            }
        }
        if(mManager == null)
        {
            mManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);

            if(mManager == null) {
                Log.e(TAG, "Can't get a BLE manager from your Android device. "
                    + "Please make sure that BLE is supported from your phone"
                );
                return -1;
            }
            if(!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Log.e(TAG, "BLE is not supported by your device");
                return -2;
            }
        }

        if(mAdapter == null)
        {
            mAdapter = mManager.getAdapter();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                if (false == mAdapter.isMultipleAdvertisementSupported()) {
                    Log.w(TAG, "MultipleAdvertisement Is Not Supported");
                }
            }
        }

        if(mGattServer == null)
        {
            mGattServer = mManager.openGattServer(activity, mGattServerCallback);
            if(mGattServer == null) {
                Log.e(TAG, "Shit! openGattServer returned null, which means "
                    + "Gatt Server is not supported on your Android device"
                );
                return -4;
            }
            addGattService();
        }

        if(advBuilder == null)
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                advBuilder = new AdvertiseData.Builder();
                Log.i(TAG, "Creating Ble Advertisement Data Builder");
                mAdapter.setName("SimplePeripheral");
                advBuilder.setIncludeDeviceName(true).addServiceUuid(new ParcelUuid(UUID.fromString(mBleUuids.getServiceUuid())));
            }
        }

        return 0;
    }

    private void addGattService()
    {
        if(null == mGattServer)
            return;

        characteristic6 = new BluetoothGattCharacteristic(
                UUID.fromString(mBleUuids.getCharacteristicUuid()),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_WRITE|BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        String Char6 = new String("Initialized Value Of The Characteristic");
        characteristic6.setValue(Char6.getBytes());
        BluetoothGattDescriptor NotificationDescriptor = new BluetoothGattDescriptor(
                UUID.fromString(mBleUuids.getNotificationDescriptorUuid()),
                BluetoothGattDescriptor.PERMISSION_WRITE|BluetoothGattDescriptor.PERMISSION_READ
        );
        NotificationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        characteristic6.addDescriptor(NotificationDescriptor);
        BluetoothGattService deviceInfoService = new BluetoothGattService(
                UUID.fromString(mBleUuids.getServiceUuid()),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        deviceInfoService.addCharacteristic(characteristic6);

        mGattServer.addService(deviceInfoService);
        Log.i(TAG, "Great: addDeviceInfoService is called.");
        List<BluetoothGattDescriptor> gattDescs = characteristic6.getDescriptors();
        if (gattDescs != null) {
            for (BluetoothGattDescriptor gattDesc : gattDescs) {
                Log.i(TAG, "desc: uuid: " + gattDesc.getUuid());
            }
        }
    }

}
