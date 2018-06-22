package edu.upenn.cis.precise.openicelite.middleware.mqtt;

import edu.upenn.cis.precise.openicelite.iomt.api.DeviceInfo;
import edu.upenn.cis.precise.openicelite.middleware.api.IMiddleware;
import edu.upenn.cis.precise.openicelite.middleware.api.ShutdownHook;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

/**
 * A simple application to simulate a medical device dongle and send data
 * periodically to MQTT broker.
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public class DongleSimulator {
    private static final String DONGLE_ID = UUID.randomUUID().toString();
    private static final String DEFAULT_SIMULATION_DEVICE_ID = "825a4dfe-5389-40e1-a48e-c6937607fa39";
    private static final double SENDING_INTERVAL = 1;              // in seconds
    private static final String PROPERTIES_FILE_NAME = "simulator.properties";

    private static final Logger logger = LogManager.getLogger(DongleSimulator.class);

    // Dongle configuration
    private final String dongleId;
    private DeviceInfo deviceInfo;
    private HashMap<String, Object> options;

    private IMiddleware middleware;

    public static void main(String[] args) {
        logger.info("Starting Dongle Simulator...");

        // Load configuration
        ClassLoader loader = DongleSimulator.class.getClassLoader();
        URL url = loader.getResource(PROPERTIES_FILE_NAME);

        String dongleId = DONGLE_ID;
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(DEFAULT_SIMULATION_DEVICE_ID);
        deviceInfo.setDeviceType("MQTT Simulated Device");
        HashMap<String, Object> options = new HashMap<>();

        if (url != null) {
            logger.info("Loading configuration from " + PROPERTIES_FILE_NAME + "...");
            try (InputStream input = loader.getResourceAsStream(PROPERTIES_FILE_NAME)) {
                Properties properties = new Properties();
                properties.load(input);

                // -- dongle ID
                if (properties.containsKey("dongle_id")) {
                    dongleId = properties.getProperty("dongle_id");
                }
                logger.info("-- Dongle ID  : " + dongleId);
                // -- device ID
                if (properties.containsKey("device_id")) {
                    deviceInfo.setDeviceId(properties.getProperty("device_id"));
                }
                logger.info("-- Device ID  : " + deviceInfo.getDeviceId());
                // -- device type
                if (properties.containsKey("device_type")) {
                    deviceInfo.setDeviceType(properties.getProperty("device_type"));
                }
                logger.info("-- Device Type: " + deviceInfo.getDeviceType());

                // -- other properties
                Enumeration<?> p = properties.propertyNames();
                while (p.hasMoreElements()) {
                    String key = (String) p.nextElement();
                    String value = properties.getProperty(key);
                    options.put(key, value);
                }
            } catch (Exception ex) {
                logger.error("Failed to load configuration!", ex);
                System.exit(-1);
            }
        }

        // Start Dongle
        DongleSimulator simulator = new DongleSimulator(dongleId, deviceInfo, options);
        simulator.startMiddleware();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(simulator.middleware));

        simulator.sendData();
    }

    /**
     * Dongle Simulator constructor
     *
     * @param dongleId dongle unique ID (UUID)
     * @param deviceInfo information of connected medical devices
     * @param options additional options to initialize MQTT dongle
     */
    private DongleSimulator(String dongleId, DeviceInfo deviceInfo, HashMap<String, Object> options) {
        this.dongleId = dongleId;
        this.deviceInfo = deviceInfo;
        this.options = options;
    }

    private void startMiddleware() {
        middleware = new Dongle(dongleId);
        middleware.init(options);
        middleware.connect(null, null, null);
    }

    private void sendData() {
        // Looping sending data
        logger.info("Sending data...");
        while (true) {
            try {
                Thread.sleep((long) (SENDING_INTERVAL * 1000));
                middleware.publishId(deviceInfo.getDeviceId(), options.get("data").toString(), null);
            } catch (Exception ex) {
                logger.error("Simulator Exception! Exiting...", ex);
                middleware.disconnect();
                System.exit(0);
            }
        }
    }
}
