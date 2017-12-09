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
				if(prop.getProperty("sink").toLowerCase().equals("cassandra")) {
					if(prop.containsKey("cassandra.ip")) {
						int port = prop.containsKey("cassandra.port") ? Integer.parseInt(prop.getProperty("cassandra.port")) : 9042;
						sink = new CassandraSink(new CassandraConfig(prop.getProperty("cassandra.ip"), port));
					} else
						throw new ConfigParseException("Cassandera IP not specified.");
				} else
					throw new ConfigParseException("No Sink found.");
				String[] metricNames = prop.getProperty("metrics").split(",");
				metrics = new String[metricNames.length];
				for(int i =0; i< metricNames.length; i++) {
					Metric metricObj = Metric.fromValue(metricNames[i].trim());
					if(metricObj == null)
						throw new ConfigParseException("Invalid metrics specified in the config file. (Currently supported metrics 'loc','line','class','app','ctime','stime','rtime')");
					metrics[i] = metricObj.name();
				}
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