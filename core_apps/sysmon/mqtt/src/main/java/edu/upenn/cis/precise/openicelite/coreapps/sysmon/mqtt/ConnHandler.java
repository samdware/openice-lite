package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.DataListener;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.EventListener;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.Info;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ChannelInfo;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ConnectionInfo;

/**
 * Handles responses from the broker and caches them
 * @author Hyun Ji Jung
 *
 */
public class ConnHandler {
	
	private static Logger logger = LogManager.getLogger(ConnHandler.class);
	private static String[] supportedMetrics = {"connections", "channels"};
	
	private HttpConn conn;
	private ArrayList<String> metrics;
	private HashMap<String, ArrayList<EventListener>> eventListeners;
	private HashMap<String, ArrayList<DataListener>> dataListeners;

	private HashMap<String, List<Info>> cache;
	
	private long interval;
	private Timer timer;
	
	protected ConnHandler(long interval) {
		conn = new HttpConn();
		metrics = new ArrayList<>();
		eventListeners = new HashMap<>();
		dataListeners = new HashMap<>();
		setInterval(interval);
		cache = new HashMap<>();
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
	 * Note that the first data will be fetched at the next scheduled 
	 * @param metric
	 */
	protected void addMonitor(String metric) {
		// TODO: verify validity of metric
		metrics.add(metric);
		logger.info("Started monitoring {}", metric);
	}

	protected void removeMonitor(String metric) {
	    if (metrics.contains(metric)) {
	        metrics.remove(metric);
        }
        logger.info("Stopped monitoring {}", metric);
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

	protected boolean verifyMetric(String metric) {
		return Arrays.stream(supportedMetrics).anyMatch(metric::startsWith);
	}
	
	protected List<Info> requestInfoList(String metric) {
		List<Info> result = new ArrayList<>();
		JsonArray array;
		try {
			array = conn.get(metric).getAsJsonArray();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error while communicating with broker: ", e);
			return result;
		}
		
		Iterator<JsonElement> it = array.iterator();
		while (it.hasNext()) {
			JsonElement object = it.next();
			result.add(parseObject(object.getAsJsonObject(), metric));
		}
		return result;
	}
	
	private Info parseObject(JsonObject object, String metric) {
		Info result;
		if ("connections".equals(metric)) {
			result = new ConnectionInfo();
		} else if ("channels".equals(metric)) {
			result = new ChannelInfo();
		} else {
			return null;
		}
		addNestedObject(object, "", result);
		return result;
	}
	
	private void addNestedObject(JsonObject object, String objectName, Info result) {
		String prefix = "".equals(objectName) ? "" : objectName + "_";
		for (Entry<String, JsonElement> entry : object.entrySet()) {
			JsonElement e = entry.getValue();
			if (e.isJsonPrimitive()) {
				JsonPrimitive p = e.getAsJsonPrimitive();
				if 		(p.isString()) result.add(prefix + entry.getKey(), p.getAsString());
				else if (p.isBoolean()) result.add(prefix + entry.getKey(), p.getAsBoolean());
				else if (entry.getKey().equals("rate")) result.add(prefix + entry.getKey(), p.getAsFloat());
				else	 result.add(prefix + entry.getKey(), p.getAsLong());
			} else if (e.isJsonObject()) {
				addNestedObject(e.getAsJsonObject(), prefix + entry.getKey(), result);
			}
		}
	}
	
	private class GetDataTask extends TimerTask {
		
		@Override
		public void run() {
		    logger.info("Requesting metrics from broker");
			for (String m : metrics) {
				List<Info> resp = requestInfoList(m);
				cache.put(m, resp);
				notifyListeners(m, resp);
			}
		}
		
		private void logData(HashMap<String, Object> response) {
			// TODO: implement
		}
		
		private void logError(Exception e, String metric) {
			// TODO: implement
		}
		
		private void notifyListeners(String metric, List<? extends Info> response) {
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
