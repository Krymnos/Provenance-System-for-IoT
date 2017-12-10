package io.provenance.sink;

import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.gson.Gson;

import io.provenance.config.CassandraConfig;
import io.provenance.config.ProvenanceConfig;
import io.provenance.types.Datapoint;
import io.provenance.types.InputDatapoint;

public class CassandraSink implements Sink{

	private CassandraConfig config;
	private Cluster cluster;
    private Session session;

    public CassandraSink(CassandraConfig config) {
		this.config = config;
		connect();
		defineSchema();
	}
	
	public void connect() {
		Builder b = Cluster.builder().addContactPoint(config.getIP());
        b.withPort(config.getPort());
        cluster = b.build();
        session = cluster.connect();
	}

	public void defineSchema() {
		StringBuilder keyspaceQueryBuilder = new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ")
			      .append(config.getKeyspaceName()).append(" WITH replication = {")
			      .append("'class':'").append(config.getReplicationStrategy())
			      .append("','replication_factor':").append(config.getReplicationFactor())
			      .append("};");
			         
	    String keyspaceQuery = keyspaceQueryBuilder.toString();
	    session.execute(keyspaceQuery);
	    StringBuilder tableQueryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(config.getKeyspaceName()).append(".")
	    		.append(config.getTableName()).append("(")
	    		.append(getSinkFieldName("ID")).append(" ").append(getSinkType("ID")).append(" ").append("PRIMARY KEY").append(",")
	    		.append(getSinkFieldName("IID")).append(" ").append(getSinkType("IID")).append(",");
	    String[] metrics = ProvenanceConfig.getMetrics();
	    boolean locationExist = false;
	    for(int i=0; i<metrics.length; i++) {
	    	tableQueryBuilder = tableQueryBuilder.append(getSinkFieldName(metrics[i])).append(" ").append(getSinkType(metrics[i]));
	    	if(i != metrics.length-1)
	    		tableQueryBuilder = tableQueryBuilder.append(",");
	    	if(metrics[i].equals("LOCATION"))
	    		locationExist = true;
	    }
	    if(locationExist)
	    	tableQueryBuilder = tableQueryBuilder.append(",").append(getSinkFieldName("LAT")).append(" ").append(getSinkType("LAT"))
	    			.append(",").append(getSinkFieldName("LONG")).append(" ").append(getSinkType("LONG"));
	    tableQueryBuilder = tableQueryBuilder.append(");");
	    String tableQuery = tableQueryBuilder.toString();
	    session.execute(tableQuery);
	}
	

	public String getSinkFieldName(String fieldName) {
		switch(fieldName) {
			case "ID"			: return "id";
			case "IID"			: return "inputDPs";
			case "LOCATION" 	: return "location";
			case "LINE" 		: return "line";
			case "CLASS" 		: return "class";
			case "APPLICATION" 	: return "app";
			case "CREATE_TIME" 	: return "ctime";
			case "SEND_TIME" 	: return "stime";
			case "RECEIVE_TIME" : return "rtime";
			case "LAT" 			: return "latitude";
			case "LONG" 		: return "longitude";
			default 			: return null;
		}
	}
	
	public String getSinkType(String fieldName) {
		switch(fieldName) {
			case "ID"			: return "text";
			case "IID"			: return "map<text,text>";
			case "LOCATION" 	: return "text";
			case "LINE" 		: return "bigint";
			case "CLASS" 		: return "text";
			case "APPLICATION" 	: return "text";
			case "CREATE_TIME" 	: return "timestamp";
			case "SEND_TIME" 	: return "timestamp";
			case "RECEIVE_TIME" : return "timestamp";
			case "LAT" 			: return "double";
			case "LONG" 		: return "double";
			default 			: return null;
		}
	}

	public String[] ingest(Datapoint...datapoints) {
		String[] ids = new String[datapoints.length];
		for(int i=0; i<datapoints.length; i++) {
			StringBuilder insertQueryBuilder = new StringBuilder("INSERT INTO ")
					.append(config.getKeyspaceName()).append(".").append(config.getTableName()).append("(").append(getSinkFieldName("ID"))
				      .append(",");
			if(datapoints[i].getInputDatapoints() !=null)
				insertQueryBuilder = insertQueryBuilder.append(getSinkFieldName("IID")).append(",");
			String[] metrics = ProvenanceConfig.getMetrics();
			boolean locationExist = false;
			for(int j=0; j<metrics.length; j++) {
		    	insertQueryBuilder = insertQueryBuilder.append(getSinkFieldName(metrics[j]));
		    	if(j != metrics.length -1)
		    		insertQueryBuilder = insertQueryBuilder.append(",");
		    	if(metrics[i].equals("LOCATION"))
		    		locationExist = true;
		    }
			if(locationExist && datapoints[i].getContext().getLoc() != null && datapoints[i].getContext().getLoc().isCoordinatesSet())
				insertQueryBuilder = insertQueryBuilder.append(",").append(getSinkFieldName("LAT"))
    				.append(",").append(getSinkFieldName("LONG"));
			insertQueryBuilder = insertQueryBuilder.append(")");
		    insertQueryBuilder = insertQueryBuilder.append("VALUES (").append(getValues(datapoints[i]));
		    if(locationExist && datapoints[i].getContext().getLoc() != null && datapoints[i].getContext().getLoc().isCoordinatesSet())
				insertQueryBuilder = insertQueryBuilder.append(",").append(datapoints[i].getContext().getLoc().getLatitude())
		    		.append(",").append(datapoints[i].getContext().getLoc().getLongitude());
		    insertQueryBuilder = insertQueryBuilder.append(");");
		    String query = insertQueryBuilder.toString();
		    session.execute(query);
		}
		return ids;
	}
	
	private String getValues(Datapoint dp) {
		StringBuilder insertQueryValuesBuilder = new StringBuilder().append("'" + dp.getId()+ "'").append(",");
		if(dp.getInputDatapoints() != null) {
			Map<String, String> inputDPs = new HashMap<String,String>();
			for(InputDatapoint idp : dp.getInputDatapoints())
				inputDPs.put(idp.getId(), idp.getContrIbution());
			insertQueryValuesBuilder = insertQueryValuesBuilder.append(new Gson().toJson(inputDPs).replaceAll("\"", "'")).append(",");
		}
		String[] metrics = ProvenanceConfig.getMetrics();
	    for(int i=0; i<metrics.length; i++) {
	    	switch(metrics[i]) {
		    	case "LOCATION" 	: insertQueryValuesBuilder = dp.getContext().getLoc() !=null ? insertQueryValuesBuilder.append("'" + dp.getContext().getLoc().getLable() + "'") : insertQueryValuesBuilder.append("null"); break;
				case "LINE" 		: insertQueryValuesBuilder = insertQueryValuesBuilder.append(dp.getContext().getLineNo()); break;
				case "CLASS" 		: insertQueryValuesBuilder = dp.getContext().getClassName() != null ? insertQueryValuesBuilder.append("'" + dp.getContext().getClassName()+ "'") : insertQueryValuesBuilder.append("null"); break;
				case "APPLICATION" 	: insertQueryValuesBuilder = dp.getContext().getAppName() != null ? insertQueryValuesBuilder.append("'" + dp.getContext().getAppName()+ "'") : insertQueryValuesBuilder.append("null"); break;
				case "CREATE_TIME" 	: insertQueryValuesBuilder = dp.getContext().getTimestamp() != null ? insertQueryValuesBuilder.append(dp.getContext().getTimestamp().getTime()) : insertQueryValuesBuilder.append("null"); break;
				case "SEND_TIME" 	: insertQueryValuesBuilder = dp.getContext().getSendTime() != null ? insertQueryValuesBuilder.append(dp.getContext().getSendTime().getTime()) : insertQueryValuesBuilder.append("null"); break;
				case "RECEIVE_TIME" : insertQueryValuesBuilder = dp.getContext().getReceiveTime() != null ? insertQueryValuesBuilder.append(dp.getContext().getReceiveTime().getTime()) : insertQueryValuesBuilder.append("null"); break;
				default 			: insertQueryValuesBuilder = insertQueryValuesBuilder.append(String.valueOf(null));
	    	}
	    	if(i != metrics.length-1)
	    		insertQueryValuesBuilder = insertQueryValuesBuilder.append(",");
	    }
	    return insertQueryValuesBuilder.toString();
	}
	
	public void close() {
		session.close();
		cluster.close();
	}

	public Session getSession() {
		return session;
	}
}
