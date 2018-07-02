package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

import java.util.List;

/**
 * A listener interface for receiving particular states or changes in data.
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public interface EventListener extends Listener {
    boolean apply(List<Info> data);
    void onConditionMet(List<Info> data);
}
