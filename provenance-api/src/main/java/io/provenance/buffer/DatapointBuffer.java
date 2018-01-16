package io.provenance.buffer;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;

import io.provenance.config.ProvenanceConfig;
import io.provenance.types.Datapoint;

public class DatapointBuffer {

	private List<Datapoint> buffer;
	private final int capacity;
	
	public DatapointBuffer(int capacity) {
		this.buffer = new ArrayList<Datapoint>();
		this.capacity = capacity;
	}
	
	public void consume() throws InterruptedException {
		while(true) {
			synchronized (this) {
				if(buffer.size()<capacity)
                    wait();
				ProvenanceConfig.getSink().ingest(Iterables.toArray(buffer, Datapoint.class));
				buffer.clear();
	        }
		}
	}
	
	public String[] produce(Datapoint[] dps) throws InterruptedException {
		String[] ids = new String[dps.length];
		synchronized (this) {
			for(int i=0; i<dps.length; i++) {
				ids[i] = dps[i].getId();
				buffer.add(dps[i]);
			}
			if(buffer.size() >= capacity)
				notify();
        }
		return ids;
	}
}