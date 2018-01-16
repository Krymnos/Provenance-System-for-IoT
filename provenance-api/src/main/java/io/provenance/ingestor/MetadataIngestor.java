package io.provenance.ingestor;

import io.provenance.buffer.MetadataBuffer;

public class MetadataIngestor extends Thread {

	private MetadataBuffer metadataBuffer;
	
	public MetadataIngestor(MetadataBuffer metadataBuffer) {
		this.metadataBuffer = metadataBuffer;
	}
	
	@Override
	public void run() {
		while(!isInterrupted()) {
			try {
				metadataBuffer.consume();
			} catch (InterruptedException e) {
				System.out.println("[ERROR] : ".concat(e.getMessage()));
			}
		}
    }
}