package edu.upenn.cis.precise.openicelite.drivers.core;

import java.util.HashMap;

/**
 * Generic information holder for medical devices
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public class DeviceInfo {
    private static final String DEFAULT_DEVICE_ID = "d0676e31-6c29-4fce-8b44-1485f86dcf34";

    private String deviceId = DEFAULT_DEVICE_ID;
    private String deviceType = "Unknown";
    private HashMap<String, String> attributes = new HashMap<>();

    public DeviceInfo() {

    }

    public DeviceInfo(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Return the device ID
     *
     * @return the device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Set the device ID
     *
     * @param deviceId device ID to be set
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Return the device type
     *
     * @return the device type
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * Set the device type
     *
     * @param deviceType device type to be set
     */
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * Return the device's attributes as a hash map
     *
     * @return the device's attributes
     */
    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Return a device's attribute based on key
     *
     * @param key the corresponding key of the attribute
     * @return the corresponding value of the key
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Set the device's attributes given a hash map
     *
     * @param attributes device's attributes to be set
     */
    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Associates the specified value with the specified key
     * in this map. If the map previously contained a mapping
     * for the key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }
}
