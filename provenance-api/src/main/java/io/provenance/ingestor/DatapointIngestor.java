package io.provenance.ingestor;

import io.provenance.buffer.DatapointBuffer;
import io.provenance.config.ProvenanceConfig;

public class DatapointIngestor extends Thread {

	DatapointBuffer datapointBuffer;
	
	public DatapointIngestor(DatapointBuffer buffer) {
		this.datapointBuffer = buffer;
	}
	
	@Override
	public void run() {
		datapointBuffer.consume();
		ProvenanceConfig.getExitMonitor().markExit();		
    }
}