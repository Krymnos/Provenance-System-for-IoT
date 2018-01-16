package io.provenance.ingestor;

import io.provenance.buffer.DatapointBuffer;

public class DatapointIngestor extends Thread {

	DatapointBuffer buffer;
	
	public DatapointIngestor(DatapointBuffer buffer) {
		this.buffer = buffer;
	}
	
	@Override
	public void run() {
		while(!isInterrupted()) {
			try {
				buffer.consume();
			} catch (InterruptedException e) {
				System.out.println("[ERROR] : ".concat(e.getMessage()));
			}
		}
    }
}
