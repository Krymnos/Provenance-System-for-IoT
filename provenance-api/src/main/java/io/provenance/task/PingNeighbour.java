package io.provenance.task;

import java.io.IOException;
import java.net.InetAddress;

public class PingNeighbour extends Thread {

	private InetAddress address;
	private long time;
	
	public PingNeighbour(InetAddress address) {
		this.address = address;
	}
	
	@Override
	public void run() {
		try {
			if(address.isReachable(30000))
				this.time = System.currentTimeMillis();
		} catch (IOException e) {
			this.time = 0;
		}
	}
	
	public long getTime() {
		return time;
	}
}