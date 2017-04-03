package com.example.administrator.blegattcharacteristicnotification;

/**
 * Created by Administrator on 2017/4/1 0001.
 */
public class BleUuids {
    private final String SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
    private final String CHARACTERISTIC_6_UUID_STRING = "00002A2D-0000-1000-8000-00805f9b34fb";
    private final String NOTIFICATION_DESCRIPTOR_UUID_STRING = "00002902-0000-1000-8000-00805f9b34fb";

    public String getServiceUuid() {
        return SERVICE_DEVICE_INFORMATION;
    }

    public String getCharacteristicUuid() {
        return CHARACTERISTIC_6_UUID_STRING;
    }

    public String getNotificationDescriptorUuid() {
        return NOTIFICATION_DESCRIPTOR_UUID_STRING;
    }

}
