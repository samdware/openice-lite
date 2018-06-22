package edu.upenn.cis.precise.openicelite.iomt.api;

import java.util.HashMap;

/**
 * Interface for all driver implementations
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public interface IDriver {
    /**
     * Configure device with attributes (e.g., device ID, type)
     *
     * @param info attributes as a DeviceInfo object
     */
    void setDeviceInfo(DeviceInfo info);

    /**
     * Return device information
     *
     * @return attributes as a DeviceInfo object
     */
    DeviceInfo getDeviceInfo();

    /**
     * Perform some initializing tasks if needed
     *
     * @param options initializing options
     */
    void init(HashMap<String, Object> options);

    /**
     * Initiate a connection to a device
     * This function is blocking but driver must return immediately after
     * success or error. Any background tasks to maintain alive connection must
     * be running on separate threads.
     *
     * @param address device's serial/ethernet address
     * @param name    identified client name (must be unique within the system)
     * @param options additional connecting options
     */
    void connect(String address, String name, HashMap<String, Object> options);

    /**
     * Blocking read a message from connected device
     *
     * @param options additional options
     * @return a message as string
     */
    String read(HashMap<String, Object> options);

    /**
     * Blocking read a message from connected device
     *
     * @param options additional options
     * @return a message as byte array
     */
    byte[] readBytes(HashMap<String, Object> options);

    /**
     * Blocking write a message to connected device
     *
     * @param message a message as string
     */
    void write(String message);

    /**
     * Blocking write a message to connected device
     *
     * @param message a message as byte array
     */
    void write(byte[] message);

    /**
     * Subscribe to the device events (e.g., periodic reported data)
     *
     * @param options  additional options
     * @param callback the class to callback for related events
     */
    void subscribe(HashMap<String, Object> options, IDriverCallback callback);

    /**
     * Unsubscribe to the device events
     */
    void unsubscribe();

    /**
     * Disconnect from device
     */
    void disconnect();

    /**
     * Close and release all associated resource
     */
    void close();

    /**
     * Determine if client is currently connect to a device
     *
     * @return true if connected, false otherwise.
     */
    boolean isConnected();

    /**
     * Set the callback listener to use for events that happen asynchronously
     *
     * @param callback the class to callback for related events
     */
    void setCallback(IDriverCallback callback);
}
