package edu.upenn.cis.precise.openicelite.apps;

import edu.upenn.cis.precise.openicelite.drivers.core.DeviceInfo;
import edu.upenn.cis.precise.openicelite.drivers.core.IDriver;
import edu.upenn.cis.precise.openicelite.drivers.core.IDriverCallback;
import edu.upenn.cis.precise.openicelite.middleware.core.IMiddleware;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.Dongle;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * Lightweight application to collect data from medical device and publish
 * JSON format message to OpenICE-lite MQTT broker
 * <p>
 * This abstract class also includes an example of main application implementation.
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public abstract class AbstractMonitor {
  // Dongle configuration
  private final String dongleId;
  private DeviceInfo deviceInfo;
  private HashMap<String, Object> options;

  private IMiddleware middleware;
  private IDriver driver;

  /* Sample coding block for main application
  public static void main(String[] args) {
    logger.info("Starting Masimo Dongle...");

    // Load configuration
    ClassLoader loader = AbstractMonitor.class.getClassLoader();
    URL propResource = loader.getResource(PROPERTIES_FILE_NAME);
    File propFile = new File("./" + PROPERTIES_FILE_NAME);

    String dongleId = DONGLE_ID;
    String devicePort = null;
    DeviceInfo deviceInfo = new DeviceInfo();
    HashMap<String, Object> options = new HashMap<>();

    if (propFile.isFile() || propResource != null) {
      logger.info("Loading configuration from " +
          (propFile.isFile() ? "./" : "default ") + PROPERTIES_FILE_NAME + "...");
      try (InputStream input = (propFile.isFile() ?
          new FileInputStream("./" + PROPERTIES_FILE_NAME) :
          loader.getResourceAsStream(PROPERTIES_FILE_NAME))) {
        Properties properties = new Properties();
        properties.load(input);

        // -- dongle ID
        dongleId = properties.getProperty("dongle_id");
        logger.info("-- Dongle ID  : " + dongleId);
        // -- device port
        devicePort = properties.getProperty("device_port");
        logger.info("-- Device Port: " + devicePort);
        // -- device ID
        deviceInfo.setDeviceId(properties.getProperty("device_id"));
        logger.info("-- Device ID  : " + deviceInfo.getDeviceId());
        // -- device type
        deviceInfo.setDeviceType(properties.getProperty("device_type"));
        logger.info("-- Device Type: " + deviceInfo.getDeviceType());

        if (dongleId == null || devicePort == null
            || deviceInfo.getDeviceId() == null
            || deviceInfo.getDeviceType() == null) {
          throw new IllegalArgumentException("Invalid properties file!");
        }

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
    } else {
      logger.error("Cannot find configuration file - " + PROPERTIES_FILE_NAME + "!");
      System.exit(-1);
    }

    // Start Dongle
    Monitor monitor = new Monitor(dongleId, deviceInfo, options);

    try {
      monitor.startMiddleware();
      Runtime.getRuntime().addShutdownHook(new ShutdownHook(monitor.middleware));
    } catch (Exception ex) {
      logger.error("Failed to start MQTT client!", ex);
      System.exit(-1);
    }

    try {
      driver = new Driver();
      monitor.startDriver(driver);
    } catch (Exception ex) {
      logger.error("Failed to start driver!", ex);
      System.exit(-1);
    }
  }
  */

  /**
   * Monitor constructor
   *
   * @param dongleId   dongle unique ID (UUID)
   * @param deviceInfo information of connected medical devices
   * @param options    additional options to initialize MQTT dongle
   */
  public AbstractMonitor(String dongleId, DeviceInfo deviceInfo, HashMap<String, Object> options) {
    this.dongleId = dongleId;
    this.deviceInfo = deviceInfo;
    this.options = options;
  }

  /**
   * Initialize middleware object, then connect to MQTT broker
   */
  public void startMiddleware() {
    middleware = new Dongle(dongleId);
    middleware.init(options);
    middleware.connect(null, null, null);
  }

  /**
   * Initialize driver to connect to the medical device, then subscribe to data stream
   *
   * @param driver IDriver instance to communicate with medical device
   */
  public void startDriver(IDriver driver) {
    this.driver = driver;
    DriverCallback callback = new DriverCallback(deviceInfo.getDeviceId(), middleware);
    driver.subscribe(null, callback);
    middleware.addDevice(deviceInfo);
  }

  private class DriverCallback implements IDriverCallback {
    private final String deviceId;
    private final IMiddleware middleware;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    DriverCallback(String deviceId, IMiddleware middleware) {
      this.deviceId = deviceId;
      this.middleware = middleware;
    }

    /**
     * This demo doesn't need to handle byte array input
     *
     * @param message data to be handled
     */
    @Override
    public void handleMessage(byte[] message) {
    }

    /**
     * Handle new data from driver as a string
     *
     * @param message data to be handled
     */
    @Override
    public void handleMessage(String message) {
      long now = System.currentTimeMillis();
      JSONObject json = new JSONObject();
      json.put("time", dateFormat.format(now));
      json.put("epoch", new Long(now));
      json.put("data", message);

      middleware.publishId(deviceId, json.toString(), null);
    }
  }
}
