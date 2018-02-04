package io.provenance.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
			input = new FileInputStream(System.getenv("provenance_properties"));
			prop.load(input);
			if(prop.containsKey("id") && prop.containsKey("successor") && prop.containsKey("sink") && prop.containsKey("metrics") && prop.containsKey("neighbours")) {
				nodeId = prop.getProperty("id");
				successor = prop.getProperty("successor");
				name = prop.containsKey("name") ? prop.getProperty("name") : nodeId;
				String[] metricNames = prop.getProperty("metrics").split(",");
				metrics = new String[metricNames.length];
				for(int i =0; i< metricNames.length; i++) {
					Metric metricObj = Metric.fromValue(metricNames[i].trim());
					if(metricObj == null)
						throw new ConfigParseException("Invalid metrics specified in the config file. (Currently supported metrics 'meterid','metricid','loc','line','class','app','ctime','stime','rtime')");
					metrics[i] = metricObj.name();
				}
				if(prop.getProperty("sink").toLowerCase().equals("cassandra")) {
					int port = prop.containsKey("cassandra.port") ? Integer.parseInt(prop.getProperty("cassandra.port")) : 9042;
					CassandraConfig cassandraConfig;
					if(prop.containsKey("cassandra.ip")) 
						cassandraConfig = new CassandraConfig(prop.getProperty("cassandra.ip"), port);
					else
						cassandraConfig = new CassandraConfig("127.0.0.1", port);
					if(prop.containsKey("cassandra.keyspace.name"))
						cassandraConfig.setKeyspaceName(prop.getProperty("cassandra.keyspace.name"));
					if(prop.containsKey("cassandra.table.name"))
						cassandraConfig.setTableName(prop.getProperty("cassandra.table.name"));
					if(prop.containsKey("cassandra.replication.strategy"))
						cassandraConfig.setReplicationStrategy(prop.getProperty("cassandra.replication.strategy"));
					if(prop.containsKey("cassandra.replication.factor"))
						cassandraConfig.setReplicationFactor(Integer.parseInt(prop.getProperty("cassandra.replication.factor")));
					sink = new CassandraSink(cassandraConfig);
				} else
					throw new ConfigParseException("No Sink found.");
				neighbours = new HashMap<String, InetAddress>();
				for(String neighbour : prop.getProperty("neighbours").split(",")) {
					String neighbourAttributes[] = neighbour.split(":");
					neighbours.put(neighbourAttributes[0], InetAddress.getByName(neighbourAttributes[1]));
				}
			} else 
				throw new ConfigParseException("Problem parsing config file. ('id', 'successor', 'sink', 'metrics' and 'neighbours' are the required config parameters.)");
			datapointBuffer = new DatapointBuffer(prop.containsKey("buffer.capacity") ? Integer.parseInt(prop.getProperty("buffer.capacity")) : 1);
			datapointIngestor = new DatapointIngestor(datapointBuffer);
			metaDataBuffer = new MetadataBuffer();
			metaDataIngestor = new MetadataIngestor(metaDataBuffer);
			datapointIngestor.start();
			metaDataIngestor.start();
			exitMonitor = new ExitMonitor();
		} catch (UnknownHostException ukhe) {
			throw new SetupException("Failed to connect to neighbours.");
		} catch (SecurityException se) {
			throw new SetupException("Failed to connect to neighbours.");
		} catch (NullPointerException npe) {
			throw new ConfigParseException("Config file not found. (Make sure 'provenance_properties' points to the config file location.)");
		} catch (FileNotFoundException fnfe) {
			throw new ConfigParseException("Config file not found. (Make sure 'provenance_properties' points to the config file location.)");
		} catch (IOException ioe) {
			throw new ConfigParseException("Problem loading config file. (Make sure 'provenance_properties' points to the config file location and config has proper read permissions.)");
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