package io.provenance.buffer;

import io.provenance.config.ProvenanceConfig;

public class MetadataBuffer {

	private double sendRate;
	private double receiveRate;
	private long commitTime;
	private final long heartbeatInterval = 300000;
	private boolean flag;
	
	public void consume() throws InterruptedException {
		while(true) {
			synchronized (this) {
				long currentTime = System.currentTimeMillis();
				if((currentTime-commitTime) < heartbeatInterval)
					wait(heartbeatInterval - (currentTime-commitTime));
				if(flag == true) {
					ProvenanceConfig.getSink().ingestNodeRate(sendRate, receiveRate);
					flag = false;
				} else
					ProvenanceConfig.getSink().ingestHeartbeat();
				commitTime = System.currentTimeMillis();
	        }
		}
	}
	
	public void produceSentRate(double sendRate) throws InterruptedException {
		synchronized (this) {
			this.sendRate = sendRate;
			flag = true;
			notify();
        }
	}
	
	public void produceReceiveRate(double receiveRate) throws InterruptedException {
		synchronized (this) {
			this.receiveRate = receiveRate;
			flag = true;
			notify();
        }
	}
	
	public void produceRates(double sendRate, double receiveRate) throws InterruptedException {
		synchronized (this) {
			this.sendRate = sendRate;
			this.receiveRate = receiveRate;
			flag = true;
			notify();
        }
	}
}