package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

/**
 * An Enum class specifying the metrics that all implementation of sysmon API are required to supply.
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public enum MetricType implements Metric {
    CONNECTIONS,
    CHANNELS,
}
