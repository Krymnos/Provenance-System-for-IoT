package io.provenance.config;

public class CassandraConfig {

	private String IP;
	private int port;
	private final String keyspaceName = "provenance";
    private final String replicationStrategy = "SimpleStrategy";
    private final String tableName = "provenanceTable";
    private final int replicationFactor = 1;

	public CassandraConfig(String IP) {
		this.IP = IP;
	}
	
	public CassandraConfig(String IP, int port) {
		this(IP);
		this.port = port;
	}
	
	public String getIP() {
		return IP;
	}
	public void setIP(String iP) {
		IP = iP;
	}
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getKeyspaceName() {
		return keyspaceName;
	}

	public String getReplicationStrategy() {
		return replicationStrategy;
	}

	public int getReplicationFactor() {
		return replicationFactor;
	}

	public String getTableName() {
		return tableName;
	}

}
