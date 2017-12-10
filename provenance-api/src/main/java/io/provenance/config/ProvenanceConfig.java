package io.provenance.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.provenance.exception.ConfigParseException;
import io.provenance.sink.CassandraSink;
import io.provenance.sink.Sink;
import io.provenance.types.Metric;

public class ProvenanceConfig {

	private static String name;
	private static Sink sink;
	private static String[] metrics;
	
	public static void configure() throws ConfigParseException {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(System.getenv("provenance.properties"));
			prop.load(input);
			if(prop.containsKey("name") && prop.containsKey("sink") && prop.containsKey("metrics")) {
				name = prop.getProperty("name");
				String[] metricNames = prop.getProperty("metrics").split(",");
				metrics = new String[metricNames.length];
				for(int i =0; i< metricNames.length; i++) {
					Metric metricObj = Metric.fromValue(metricNames[i].trim());
					if(metricObj == null)
						throw new ConfigParseException("Invalid metrics specified in the config file. (Currently supported metrics 'loc','line','class','app','ctime','stime','rtime')");
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
			} else 
				throw new ConfigParseException("Problem parsing config file. ('name', 'sink' and 'metrics' are the required config parameters.)");
		} catch (NullPointerException npe) {
			throw new ConfigParseException("Config file not found. (Make sure 'provenance.properties' points to the config file location.)");
		} catch (FileNotFoundException fnfe) {
			throw new ConfigParseException("Config file not found. (Make sure 'provenance.properties' points to the config file location.)");
		} catch (IOException ioe) {
			throw new ConfigParseException("Problem loading config file. (Make sure 'provenance.properties' points to the config file location and config has proper read permissions.)");
		}
	}
	
	public static String getName() {
		return name;
	}
	
	public static Sink getSink() {
		return sink;
	}
	
	public static String[] getMetrics() {
		return metrics;
	}
}