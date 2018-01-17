package io.provenance.ingestor;

import io.provenance.buffer.DatapointBuffer;
import io.provenance.config.ProvenanceConfig;

public class DatapointIngestor extends Thread {

	DatapointBuffer buffer;
	
	public DatapointIngestor(DatapointBuffer buffer) {
		this.buffer = buffer;
	}
	
	@Override
	public void run() {
		buffer.consume(this);
		ProvenanceConfig.getExitMonitor().markExit();		
    }
}