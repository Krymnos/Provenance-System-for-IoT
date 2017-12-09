package io.provenance.sink;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.datastax.driver.core.Session;

import io.provenance.config.CassandraConfig;
import io.provenance.config.ProvenanceConfig;
import io.provenance.types.Datapoint;
import io.provenance.types.InputDatapoint;

public class CassandraSink implements Sink{

	private CassandraConfig config;
	private Cluster cluster;
    private Session session;

	
	public CassandraSink(CassandraConfig config) {
		this.config = new CassandraConfig(config.getIP(), config.getPort());
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
	    StringBuilder tableQueryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
	    		.append(config.getTableName()).append("(")
	    		.append(getSinkFieldName("ID")).append(" ").append(getSinkType("ID")).append(" ").append("PRIMARY KEY").append(",")
	    		.append(getSinkFieldName("ID")).append(" ").append(getSinkType("IID")).append(",");
	    String[] metrics = ProvenanceConfig.getMetrics();
	    for(int i=0; i<=metrics.length -1; i++) {
	    	tableQueryBuilder = tableQueryBuilder.append(getSinkFieldName("ID")).append(" ").append(getSinkType(metrics[i]));
	    	if(i != metrics.length)
	    		tableQueryBuilder = tableQueryBuilder.append(",");
	    }
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
			default : return null;
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
			default : return null;
		}
	}

	public void ingest(Datapoint...datapoints) {
		for(Datapoint dp : datapoints) {
			StringBuilder insertQueryBuilder = new StringBuilder("INSERT INTO ")
				      .append(config.getTableName()).append("(").append(getSinkFieldName("ID"))
				      .append(",").append(getSinkFieldName("IID")).append(",");
			String[] metrics = ProvenanceConfig.getMetrics();
		    for(int i=0; i<=metrics.length -1; i++) {
		    	insertQueryBuilder = insertQueryBuilder.append(getSinkFieldName(metrics[i]));
		    	if(i != metrics.length)
		    		insertQueryBuilder = insertQueryBuilder.append(",");
		    	else
		    		insertQueryBuilder = insertQueryBuilder.append(")");
		    }
		    insertQueryBuilder = insertQueryBuilder.append("VALUES (").append(getValues(dp)).append("');");
		    String query = insertQueryBuilder.toString();
		    session.execute(query);
		}
	}
	
	private String getValues(Datapoint dp) {
		try {
			ObjectMapper om = new ObjectMapper();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			StringBuilder insertQueryValuesBuilder = new StringBuilder().append(om.writeValueAsString(dp.getId())).append(",");
			Map<String, String> inputDPs = new HashMap<String,String>();
			for(InputDatapoint idp : dp.getInputDatapoints())
				inputDPs.put(idp.getId(), idp.getContrIbution());
			insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(inputDPs)).append(",");
			String[] metrics = ProvenanceConfig.getMetrics();
		    for(int i=0; i<=metrics.length -1; i++) {
		    	switch(metrics[i]) {
			    	case "LOCATION" 	: insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(dp.getContext().getLoc().getLable())); break;
					case "LINE" 		: insertQueryValuesBuilder = insertQueryValuesBuilder.append(dp.getContext().getLineNo()); break;
					case "CLASS" 		: insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(dp.getContext().getClassName())); break;
					case "APPLICATION" 	: insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(dp.getContext().getAppName())); break;
					case "CREATE_TIME" 	: insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(df.format(dp.getContext().getTimestamp()))); break;
					case "SEND_TIME" 	: insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(df.format(dp.getContext().getSendTime()))); break;
					case "RECEIVE_TIME" : insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(df.format(dp.getContext().getReceiveTime()))); break;
					default 			: insertQueryValuesBuilder = insertQueryValuesBuilder.append(om.writeValueAsString(null));
		    	}
		    	if(i != metrics.length)
		    		insertQueryValuesBuilder = insertQueryValuesBuilder.append(",");
		    }
		    return insertQueryValuesBuilder.toString();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void close() {
		session.close();
		cluster.close();
	}

	public Session getSession() {
		return session;
	}
}
