package io.provenance.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import io.provenance.buffer.DatapointBuffer;
import io.provenance.buffer.MetadataBuffer;
import io.provenance.exception.ConfigParseException;
import io.provenance.exception.SetupException;
import io.provenance.ingestor.DatapointIngestor;
import io.provenance.ingestor.MetadataIngestor;
import io.provenance.sink.CassandraSink;
import io.provenance.sink.Sink;
import io.provenance.types.ExitMonitor;
import io.provenance.types.Metric;

public class ProvenanceConfig {

	private static String name;
	private static String nodeId;
	private static String successor;
	private static Sink sink;
	private static String[] metrics;
	private static DatapointIngestor datapointIngestor;
	private static DatapointBuffer datapointBuffer;
	private static MetadataIngestor metaDataIngestor;
	private static MetadataBuffer metaDataBuffer;
	private static ExitMonitor exitMonitor;
	private static Map<String, InetAddress> neighbours;
	
	public static void configure() throws ConfigParseException, SetupException {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			String confLoc = System.getenv("CONF_LOC");
			if(confLoc != null && confLoc.toUpperCase().equals("EVAR"))
				prop.putAll(System.getenv());
			else {
				input = new FileInputStream(System.getenv("provenance_properties"));
				prop.load(input);
			}
			if(prop.containsKey("NODE_ID") && prop.containsKey("SUCCESSOR") && prop.containsKey("SINK") && prop.containsKey("NEIGHBOURS") && prop.containsKey("METRICS")) {
				nodeId = prop.getProperty("NODE_ID");
				successor = prop.getProperty("SUCCESSOR");
				name = prop.containsKey("NODE_NAME") ? prop.getProperty("NODE_NAME") : nodeId;
				String[] metricNames = prop.getProperty("METRICS").split(",");
				metrics = new String[metricNames.length];
				for(int i =0; i< metricNames.length; i++) {
					Metric metricObj = Metric.fromValue(metricNames[i].trim());
					if(metricObj == null)
						throw new ConfigParseException("Invalid metrics specified. (Currently supported metrics 'meterid','metricid','loc','line','class','app','ctime','stime','rtime')");
					metrics[i] = metricObj.name();
				}
				if(prop.getProperty("SINK").toLowerCase().equals("cassandra")) {
					int port = prop.containsKey("CASSANDRA_PORT") ? Integer.parseInt(prop.getProperty("CASSANDRA_PORT")) : 9042;
					CassandraConfig cassandraConfig;
					if(prop.containsKey("CASSANDRA_HOST")) 
						cassandraConfig = new CassandraConfig(prop.getProperty("CASSANDRA_HOST"), port);
					else
						cassandraConfig = new CassandraConfig("127.0.0.1", port);
					if(prop.containsKey("CASSANDRA_KEYSPACE_NAME"))
						cassandraConfig.setKeyspaceName(prop.getProperty("CASSANDRA_KEYSPACE_NAME"));
					if(prop.containsKey("CASSANDRA_TABLE_NAME"))
						cassandraConfig.setTableName(prop.getProperty("CASSANDRA_TABLE_NAME"));
					if(prop.containsKey("CASSANDRA_REPLICATION_STRATEGY"))
						cassandraConfig.setReplicationStrategy(prop.getProperty("CASSANDRA_REPLICATION_STRATEGY"));
					if(prop.containsKey("CASSANDRA_REPLICATION_FACTOR"))
						cassandraConfig.setReplicationFactor(Integer.parseInt(prop.getProperty("CASSANDRA_REPLICATION_FACTOR")));
					sink = new CassandraSink(cassandraConfig);
				} else
					throw new ConfigParseException("No Sink found.");
				neighbours = new HashMap<String, InetAddress>();
				for(String neighbour : prop.getProperty("NEIGHBOURS").split(",")) {
					String neighbourAttributes[] = neighbour.split(":");
					try {
						neighbours.put(neighbourAttributes[0], InetAddress.getByName(neighbourAttributes[1]));
					} catch (UnknownHostException | SecurityException e) {
						throw new SetupException("Failed to connect to neighbours.");
					}
				}
			} else 
				throw new ConfigParseException("Problem loading configurations. ('NODE_ID', 'SUCCESSOR', 'NEIGHBOURS', 'METRICS' and 'SINK' are the required config parameters.)");
			datapointBuffer = new DatapointBuffer(prop.containsKey("BUFFER_CAPACITY") ? Integer.parseInt(prop.getProperty("BUFFER_CAPACITY")) : 1);
			datapointIngestor = new DatapointIngestor(datapointBuffer);
			metaDataBuffer = new MetadataBuffer();
			metaDataIngestor = new MetadataIngestor(metaDataBuffer);
			datapointIngestor.start();
			metaDataIngestor.start();
			exitMonitor = new ExitMonitor();
		}  catch (NullPointerException | SecurityException | IOException e) {
			throw new ConfigParseException("Problem loading configurations.");
		}
	}

	public static String getNodeId() {
		return nodeId;
	}
	
	public static Sink getSink() {
		return sink;
	}

	public static String getName() {
		return name;
	}

	public static String getSuccessor() {
		return successor;
	}

	public static String[] getMetrics() {
		return metrics;
	}

	public static DatapointBuffer getDatapointBuffer() {
		return datapointBuffer;
	}
	
	public static MetadataBuffer getMetaDataBuffer() {
		return metaDataBuffer;
	}
	
	public static DatapointIngestor getDatapointIngestor() {
		return datapointIngestor;
	}

	public static MetadataIngestor getMetaDataIngestor() {
		return metaDataIngestor;
	}
	
	public static ExitMonitor getExitMonitor() {
		return exitMonitor;
	}
	
	public static Map<String, InetAddress> getNeighbours() {
		return neighbours;
	}
}