package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

import java.util.List;

/**
 * A listener interface for receiving particular states or changes in data.
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public interface EventListener extends Listener {

    /**
     * Specifies the condition to be notified with the metric. onConditionMet() will be called when this function returns true.
     * @param data List of metrics to apply the condition onto
     * @return Whether to notify the caller or not
     */
    boolean apply(List<Info> data);

    /**
     * Called when apply() returns true.
     * @param data Data that satisfied the condition stated by apply()
     */
    void onConditionMet(List<Info> data);
}
