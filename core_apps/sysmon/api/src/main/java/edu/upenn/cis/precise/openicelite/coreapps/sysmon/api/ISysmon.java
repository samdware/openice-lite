package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

public interface ISysmon {
    void init(String configFilePath);
	void start();
	void stop();
	void setOption(String optionName, Object value);
	Object getOption(String optionName);
	
	void addMonitor(String metric);
	void addMonitor(Metric metric);
	void addListener(String metric, Listener listener);
	void addListener(Metric metric, Listener listener);
}
