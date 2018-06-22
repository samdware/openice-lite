package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

import java.util.Properties;

/**
 * Interface to monitor system metrics
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public interface ISysmon {

    /**
     * Initializes the system monitor with given options (properties).
     * @param properties Parsed object of the properties file
     */
    void init(Properties properties);

    /**
     * Starts to receive system metrics periodically.
     */
    void start();

    /**
     * Stops receiving system metrics.
     */
    void stop();

    /**
     * Changes the specified options to the given value. Adds the option if it doesn't exist.
     * @param optionName Option to change
     * @param value New value of the given option
     */
	void setOption(String optionName, String value);

    /**
     * Returns the value of the given option, or null if no such option exists.
     * @param optionName Name of the option to find
     * @return
     */
	String getOption(String optionName);

    /**
     * Adds the given metric to the list of metrics to request on the next interval.
     * @param metric Name of the metric to request
     */
	void addMonitor(String metric);

    /**
     * Adds the given metric to the list of metrics to request on the next interval.
     * @param metric Enum of the metric to request
     */
	void addMonitor(Metric metric);

    /**
     * Attaches a listener to the specified metric. Starts monitoring the metric if
     * the monitor doesn't already
     * @param metric Name of the metric to attach the listener to
     * @param listener The listener to be added
     */
	void addListener(String metric, Listener listener);

    /**
     * Attaches a listener to the specified metric. Starts monitoring the metric if
     * the monitor doesn't already
     * @param metric Enum of the metric to attach the listener to
     * @param listener The listener to be added
     */
	void addListener(Metric metric, Listener listener);
}
