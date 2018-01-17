package edu.upenn.cis.precise.openicelite.middleware.mqtt.type;

import edu.upenn.cis.precise.openicelite.drivers.core.DeviceInfo;

import java.util.HashMap;

/**
 * Represent an object used to hold dongle information
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public class DongleInfo {
    private String dongleId;
    private HashMap<String, DeviceInfo> devices;
    private long lastUpdated = 0;

    public DongleInfo(String dongleId) {
        this.dongleId = dongleId;
        this.devices = new HashMap<>();
    }

    public String getDongleId() {
        return dongleId;
    }

    public void setDongleId(String dongleId) {
        this.dongleId = dongleId;
    }

    public HashMap<String, DeviceInfo> getDevices() {
        return devices;
    }

    public void setDevices(HashMap<String, DeviceInfo> devices) {
        this.devices = devices;
    }

    public void addDevice(DeviceInfo info) {
        this.devices.put(info.getDeviceId(), info);
    }

    public void removeDevice(String deviceId) {
        this.devices.remove(deviceId);
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
