package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

/**
 * A listener interface for receiving data from the system monitor.
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public interface Listener {

    /**
     * Called when no data is available for the requested metric.
     */
	void onNotAvailable();
}
