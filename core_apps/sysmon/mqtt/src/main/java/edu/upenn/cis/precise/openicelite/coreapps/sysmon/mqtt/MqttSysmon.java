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

	private ConnHandler handler;
	private Properties properties;
	
	public MqttSysmon() {
	}

	public MqttSysmon(Properties properties) {
	    this.init(properties);
    }

    /**
     * @inhertDoc
     */
    @Override
	public void init(Properties properties) {
        this.properties = properties;
        handler = new ConnHandler(properties);
    }

	@Override
	public void start() {
		handler.startTimer();
	}

	@Override
	public void stop() {
		handler.stopTimer();
	}

	@Override
	public void setOption(String optionName, String value) {
        properties.setProperty(optionName, value);
	}

	@Override
	public String getOption(String optionName) {
		return properties.getProperty(optionName);
	}
	
	@Override
	public void addMonitor(String metric) {
		// TODO: check validity of metric
		handler.addMonitor(metric);
	}

	@Override
	public void addMonitor(Metric metric) {
		addMonitor(enumMap.get(metric));
	}

	@Override
	public void addListener(String metric, Listener listener) {
		if (listener instanceof DataListener) {
			handler.addDataListener(metric, (DataListener) listener);
		} else if (listener instanceof EventListener) {
			handler.addEventListener(metric, (EventListener) listener);
		}
	}

    @Override
    public void addListener(Metric metric, Listener listener) {
        addListener(enumMap.get(metric), listener);
    }

}
