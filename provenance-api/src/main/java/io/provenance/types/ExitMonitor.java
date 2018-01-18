package io.provenance.types;

import io.provenance.config.ProvenanceConfig;

public class ExitMonitor {

	private boolean exit;
	
	public void markExit() {
		synchronized (this) {
			if(exit)
				ProvenanceConfig.getSink().close();
			else
				exit = true;
		}
	}
}