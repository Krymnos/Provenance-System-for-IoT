package io.provenance.buffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import io.provenance.config.ProvenanceConfig;

public class MetadataBuffer {

	private double sendRate;
	private double receiveRate;
	private long commitTime;
	private final long heartbeatInterval = 30000;
	private boolean flag;
	private long pipelineDaemonTime;
	private Map<String, Long> neighboursStatus = new HashMap<String, Long>();
	
	public void consume() {
		boolean running = true;
		while(!Thread.currentThread().isInterrupted() && running) {
			synchronized (this) {
				long currentTime = System.currentTimeMillis();
				try {
					if((currentTime-commitTime) < heartbeatInterval)
						wait(heartbeatInterval - (currentTime-commitTime));
				} catch (InterruptedException e) {
					running = false;
				}
				if(flag == true) {
					ProvenanceConfig.getSink().ingestNodeRate(sendRate, receiveRate);
					flag = false;
				}
				long time = System.currentTimeMillis();
				if((time - commitTime) > heartbeatInterval) {
					for(String neighbourID : ProvenanceConfig.getNeighbours().keySet()) {
						long nodePingTime = 0;
						try {
							if(ProvenanceConfig.getNeighbours().get(neighbourID).isReachable(30000))
								nodePingTime = System.currentTimeMillis();
						} catch (IOException ioe) {}
						neighboursStatus.put(neighbourID, nodePingTime);
					}
					ProvenanceConfig.getSink().ingestHeartbeat(pipelineDaemonTime, neighboursStatus);
					commitTime = System.currentTimeMillis();
				}
			}
		}
	}
	
	public void produceSentRate(double sendRate) throws InterruptedException {
		synchronized (this) {
			this.sendRate = sendRate;
			this.flag = true;
			notify();
        }
	}
	
	public void produceReceiveRate(double receiveRate) throws InterruptedException {
		synchronized (this) {
			this.receiveRate = receiveRate;
			this.flag = true;
			notify();
        }
	}
	
	public void produceRates(double sendRate, double receiveRate) throws InterruptedException {
		synchronized (this) {
			this.sendRate = sendRate;
			this.receiveRate = receiveRate;
			this.flag = true;
			notify();
        }
	}
	
	public void markAlive() {
		synchronized (this) {
			this.pipelineDaemonTime = System.currentTimeMillis();
		}
	}
}