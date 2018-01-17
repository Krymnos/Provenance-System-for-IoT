package io.provenance.ingestor;

import io.provenance.buffer.MetadataBuffer;
import io.provenance.config.ProvenanceConfig;

public class MetadataIngestor extends Thread {

	private MetadataBuffer metadataBuffer;
	
	public MetadataIngestor(MetadataBuffer metadataBuffer) {
		this.metadataBuffer = metadataBuffer;
	}
	
	@Override
	public void run() {
		metadataBuffer.consume(this);
		ProvenanceConfig.getExitMonitor().markExit();
    }
}