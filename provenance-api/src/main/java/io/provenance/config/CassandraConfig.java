package io.provenance.config;

public class CassandraConfig {

	private String IP;
	private int port;
	private String keyspaceName = "provenance";
    private String replicationStrategy = "SimpleStrategy";
    private String tableName = "provenanceTable";
    private int replicationFactor = 1;

    /**
	 * Creates a new cassandra config using cassandra IP address. 
	 * 
	 * @param  IP  IP address of cassandra.
	 */
    
	public CassandraConfig(String IP) {
		this.IP = IP;
	}
	
	/**
	 * Creates a new cassandra config using cassandra IP address and port.
	 *
	 * @param  IP  IP address of cassandra.
	 * @param  port  port of cassandra.
	 */
	
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
	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}

	public String getReplicationStrategy() {
		return replicationStrategy;
	}
	public void setReplicationStrategy(String replicationStrategy) {
		this.replicationStrategy = replicationStrategy;
	}

	public int getReplicationFactor() {
		return replicationFactor;
	}
	public void setReplicationFactor(int replicationFactor) {
		this.replicationFactor = replicationFactor;
	}

	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
}