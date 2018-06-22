package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

import java.util.List;

public interface DataListener extends Listener {
	void handleData(List<? extends Info> data);
}
