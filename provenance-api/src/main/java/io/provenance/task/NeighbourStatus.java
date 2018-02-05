package io.provenance.task;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import io.provenance.config.ProvenanceConfig;

public class NeighbourStatus {

	public static Map<String, Long> getNeighboursStatus() {
		Map<String, InetAddress> neighbours = ProvenanceConfig.getNeighbours();
		String[] neighbourIDs = neighbours.keySet().toArray(new String[0]);
		Thread[] threads = new Thread[neighbourIDs.length];
		for(int i=0; i<threads.length; i++) {
			threads[i] = new PingNeighbour(neighbours.get(neighbourIDs[i]));
			threads[i].start();
		}
		for(int i=0; i<threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {}
		}
		Map<String, Long> neighbourStatus = new HashMap<String, Long>();
		for(int i=0; i<threads.length; i++) {
			String channel;
			if(neighbourIDs[i].equals(ProvenanceConfig.getSuccessor()))
				channel = ProvenanceConfig.getNodeId().concat(":").concat(neighbourIDs[i]);
			else
				channel = neighbourIDs[i].concat(":").concat(ProvenanceConfig.getNodeId());
			neighbourStatus.put(channel, ((PingNeighbour)threads[i]).getTime());
		}
		return neighbourStatus;
	}
}