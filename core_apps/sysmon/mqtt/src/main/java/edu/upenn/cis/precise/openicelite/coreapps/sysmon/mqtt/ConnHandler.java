package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import java.io.IOException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.DataListener;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.EventListener;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.Info;

/**
 * Handles responses from the broker and caches them
 * @author Hyun Ji Jung
 *
 */
public class ConnHandler {

	private static Logger logger = LogManager.getLogger(ConnHandler.class);

	private HttpConn conn;
	private ArrayList<String> metrics;
	private HashMap<String, ArrayList<EventListener>> eventListeners;
	private HashMap<String, ArrayList<DataListener>> dataListeners;

    private DbConn dbConn;
	private long interval;
	private Timer timer;


	protected ConnHandler(Properties properties) {
	    String host = properties.getProperty("host", "localhost");
	    int port = Integer.parseInt(properties.getProperty("port", "15672"));
	    String dbFile = properties.getProperty("db", "logs/metrics.db");
	    int interval = Integer.parseInt(properties.getProperty("interval", "5000"));
        String user = properties.getProperty("user","guest");
        String password = properties.getProperty("password", "guest");

	    conn = new HttpConn(host, port, user, password);
	    dbConn = new DbConn(dbFile);
	    createTables();
	    metrics = new ArrayList<>();
	    eventListeners = new HashMap<>();
	    dataListeners = new HashMap<>();
	    setInterval(interval);
    }

	private void createTables() {
		dbConn.connect();
	    dbConn.createConnectionTable();
	    dbConn.createChannelTable();
	    dbConn.disconnect();
    }

	/**
	 * Sets the rate of data collection
	 * @param interval Interval between each collection, in milliseconds
	 */
	protected void setInterval(long interval) {
		if (interval <= 0) {
			throw new IllegalArgumentException("Interval must be greater than zero");
		}
		this.interval = interval;
		logger.info("Collection interval changed to {}", interval);
	}

	/**
	 * Starts collecting data periodically
	 */
	protected void startTimer() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new GetDataTask(), 0, interval);
		logger.info("Started collecting metrics with interval {}", interval);
	}

	/**
	 * Stops collecting data
	 */
	protected void stopTimer() {
		timer.cancel();
		logger.info("Stopped collecting metrics");
	}

	/**
	 * Start monitoring the specified metric.
	 * Note that the first data will be fetched at the next scheduled request
	 * @param metric Name of metric to start monitoring
	 */
	protected void addMonitor(String metric) {
		// TODO: verify validity of metric
		metrics.add(metric);
		logger.info("Started monitoring {}", metric);
	}

    /**
     * Stops monitoring the specified metric.
     * Will silently ignore calls with a metric that isn't being monitored.
     * @param metric Name of metric to stop monitoring
     */
	protected void removeMonitor(String metric) {
	    if (metrics.contains(metric)) {
	        metrics.remove(metric);
            logger.info("Stopped monitoring {}", metric);
        }
    }

	protected void addDataListener(String metric, DataListener listener) {
		if (dataListeners.containsKey(metric)) {
			dataListeners.get(metric).add(listener);
		} else {
			ArrayList<DataListener> ls = new ArrayList<>();
			ls.add(listener);
			dataListeners.put(metric, ls);
		}
		logger.info("DataListener attached to {}", metric);
	}

	protected void addEventListener(String metric, EventListener listener) {
		if (eventListeners.containsKey(metric)) {
			eventListeners.get(metric).add(listener);
		} else {
			ArrayList<EventListener> ls = new ArrayList<>();
			ls.add(listener);
			eventListeners.put(metric, ls);
		}
		logger.info("EventListener attached to {}", metric);
	}

	protected List<Info> requestInfoList(String metric) {
		List<Info> result = new ArrayList<>();
		JsonArray array;
		try {
			array = conn.get(metric).getAsJsonArray();
			if (array == null) {
			    // means that no data was returned from the server
                return null;
            }
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error while communicating with broker: ", e);
			return result;
		}
	    return InfoParser.parseList(metric, array);
	}


	private class GetDataTask extends TimerTask {

		@Override
		public void run() {
		    logger.info("Requesting metrics from broker");
		    dbConn.connect();
			for (String m : metrics) {
				List<Info> resp = requestInfoList(m);
				if (resp == null) {
				    notifyNoData("metric");
                } else {
                    notifyListeners(m, resp);
                    dbConn.insertList(m, resp);
                }
			}
			dbConn.disconnect();
		}

		private void notifyListeners(String metric, List<Info> response) {
			if (dataListeners.containsKey(metric)) {
				for (DataListener i : dataListeners.get(metric)) {
					i.handleData(response);
				}
			}
			if (eventListeners.containsKey(metric)) {
				for (EventListener i : eventListeners.get(metric)) {
					// TODO: do this
				}
			}
		}

		private void notifyNoData(String metric) {
			if (dataListeners.containsKey(metric)) {
				for (DataListener i : dataListeners.get(metric)) {
					i.onNotAvailable();
				}
			}
			if (eventListeners.containsKey(metric)) {
				for (EventListener i : eventListeners.get(metric)) {
					i.onNotAvailable();
				}
			}
		}
	}
}
