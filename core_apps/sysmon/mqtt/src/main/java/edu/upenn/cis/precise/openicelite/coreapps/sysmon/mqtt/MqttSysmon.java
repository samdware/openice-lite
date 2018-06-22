package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import java.util.*;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.*;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.EventListener;

public class MqttSysmon implements ISysmon{

    private static final Map<Metric, String> enumMap = Collections.unmodifiableMap(
            new HashMap<Metric, String>() {{
                put(MetricType.CHANNELS, "channels");
                put(MetricType.CONNECTIONS, "connections");
                put(MetricType.TOPICS, "topics");
                put(MqttMetricType.VHOSTS, "vhosts");
            }}
    );

	private HashMap<String, Object> options;
	private ConnHandler handler;
	
	public MqttSysmon() {
		init("");
	}
	
	@Override
	public void init(String configFilePath) {
		// TODO: read config file
		options = new HashMap<>();
		handler = new ConnHandler(5000);
	}

	/**
	 * Start querying MQTT broker with registered options.
	 */
	@Override
	public void start() {
		handler.startTimer();
	}

	@Override
	public void stop() {
		handler.stopTimer();
	}

	@Override
	public void setOption(String optionName, Object value) {
		options.put(optionName, value);
		
	}

	@Override
	public Object getOption(String optionName) {
		return options.get(optionName);
	}
	
	/**
	 * Start logging a metric
	 * @param metric The name of the metric to log
	 */
	@Override
	public void addMonitor(String metric) {
		// TODO: check validity of metric
		handler.addMonitor(metric);
	}

	/**
	 * Start logging a metric
	 * @param metric The name of the metric to log
	 */
	@Override
	public void addMonitor(Metric metric) {
		// TODO: check validity of metric
		addMonitor(enumMap.get(metric));
	}

	/**
	 * Attaches a listener to the given metric. 
	 * Also instantiates monitoring (logging) if the metric isn't already being monitored 
	 * @param metric The metric 
	 */
	@Override
	public void addListener(String metric, Listener listener) {
		if (listener instanceof DataListener) {
			handler.addDataListener(metric, (DataListener) listener);
		} else if (listener instanceof EventListener) {
			handler.addEventListener(metric, (EventListener) listener);
		}
	}

    /**
     * Attaches a listener to the given metric.
     * Also instantiates monitoring (logging) if the metric isn't already being monitored
     * @param metric The metric
     */
    @Override
    public void addListener(Metric metric, Listener listener) {
        addListener(enumMap.get(metric), listener);
    }
	public Object getState(String metric) {
		return null;
	}
	
}
