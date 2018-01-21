package io.provenance.sink;

import java.util.HashMap;
import java.util.Map;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
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
		registerNode();
	}
	
    @Override
	public void connect() {
		Builder b = Cluster.builder().addContactPoint(config.getIP());
        b.withPort(config.getPort());
        cluster = b.build();
        session = cluster.connect();
	}

    @Override
	public void defineSchema() {
		defineKeyspace();
		defineDatapointSchema();
		defineNodeSchema();
		defineHeartbeatSchema();
		defineNodeRateSchema();
	}
    
    public void defineKeyspace() {
		StringBuilder keyspaceQueryBuilder = new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ")
			      .append(config.getKeyspaceName()).append(" WITH replication = {")
			      .append("'class':'").append(config.getReplicationStrategy())
			      .append("','replication_factor':").append(config.getReplicationFactor())
			      .append("};");
	    session.execute(keyspaceQueryBuilder.toString());
	}
    
    public void defineDatapointSchema() {
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
	    tableQueryBuilder = tableQueryBuilder.append(",time timeuuid").append(");");
	    session.execute(tableQueryBuilder.toString());
	}
    
    public void defineNodeSchema() {
		StringBuilder tableQueryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(config.getKeyspaceName())
				.append(".node(id text PRIMARY KEY, name text, successor text);");
		session.execute(tableQueryBuilder.toString());
	}
    
    public void defineHeartbeatSchema() {
		StringBuilder tableQueryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(config.getKeyspaceName())
				.append(".heartbeat(id text PRIMARY KEY, time timestamp);");
		session.execute(tableQueryBuilder.toString());
	}
    
    public void defineNodeRateSchema() {
		StringBuilder tableQueryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(config.getKeyspaceName())
				.append(".noderate(id text PRIMARY KEY, time timestamp, srate double, rrate double);");
		session.execute(tableQueryBuilder.toString());
	}
    
    public void registerNode() {
		PreparedStatement preparedQuery = session.prepare("INSERT INTO ".concat(config.getKeyspaceName())
	    		.concat(".node(id, name, successor) VALUES (?, ?, ?)"));
	    session.execute(preparedQuery.bind(ProvenanceConfig.getNodeId(), ProvenanceConfig.getName(), ProvenanceConfig.getSuccessor()));
	}
    
    @Override
	public void ingest(Datapoint...datapoints) {
		BatchStatement batchStatement = new BatchStatement();
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
		    	if(metrics[j].equals("LOCATION"))
		    		locationExist = true;
		    }
			if(locationExist && datapoints[i].getContext().getLoc() != null && datapoints[i].getContext().getLoc().isCoordinatesSet())
				insertQueryBuilder = insertQueryBuilder.append(",").append(getSinkFieldName("LAT"))
    				.append(",").append(getSinkFieldName("LONG"));
			insertQueryBuilder = insertQueryBuilder.append(", time)").append("VALUES (").append(getValues(datapoints[i]));
		    if(locationExist && datapoints[i].getContext().getLoc() != null && datapoints[i].getContext().getLoc().isCoordinatesSet())
				insertQueryBuilder = insertQueryBuilder.append(",").append(datapoints[i].getContext().getLoc().getLatitude())
		    		.append(",").append(datapoints[i].getContext().getLoc().getLongitude());
		    insertQueryBuilder = insertQueryBuilder.append(", now());");
		    batchStatement.add(new SimpleStatement(insertQueryBuilder.toString()));
		}
	    session.execute(batchStatement);
	}
    
    private String getValues(Datapoint dp) {
		StringBuilder insertQueryValuesBuilder = new StringBuilder().append("'" + dp.getId()+ "'").append(",");
		if(dp.getInputDatapoints() != null) {
			Map<String, String> inputDPs = new HashMap<String,String>();
			for(InputDatapoint idp : dp.getInputDatapoints())
				inputDPs.put(idp.getId(), idp.getContribution());
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
				case "METER" 		: insertQueryValuesBuilder = dp.getContext().getMeterId() != null ? insertQueryValuesBuilder.append("'" + dp.getContext().getMeterId()+ "'") : insertQueryValuesBuilder.append("null"); break;
				case "METRIC" 	 	: insertQueryValuesBuilder = dp.getContext().getMetricId() != null ? insertQueryValuesBuilder.append("'" + dp.getContext().getMetricId()+ "'") : insertQueryValuesBuilder.append("null"); break;
				default 			: insertQueryValuesBuilder = insertQueryValuesBuilder.append(String.valueOf(null));
	    	}
	    	if(i != metrics.length-1)
	    		insertQueryValuesBuilder = insertQueryValuesBuilder.append(",");
	    }
	    return insertQueryValuesBuilder.toString();
	}
    
    @Override
	public void ingestHeartbeat() {
		StringBuilder tableQueryBuilder = new StringBuilder("INSERT INTO ").append(config.getKeyspaceName())
	    		.append(".heartbeat(id, time) VALUES ")
	    		.append(String.format("('%s', %d)", ProvenanceConfig.getNodeId(), System.currentTimeMillis()));
		session.execute(tableQueryBuilder.toString());
	}
	
	@Override
	public void ingestNodeRate(double sendRate, double receiveRate) {
		StringBuilder tableQueryBuilder = new StringBuilder("INSERT INTO ").append(config.getKeyspaceName())
	    		.append(".noderate(id, time, srate, rrate) VALUES ")
	    		.append(String.format("('%s', '%d', %f, %f)", ProvenanceConfig.getNodeId(), System.currentTimeMillis(), sendRate, receiveRate));
		session.execute(tableQueryBuilder.toString());
	}
    
	@Override
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
			case "METER" 		: return "meterId";
			case "METRIC" 		: return "metricId";
			default 			: return null;
		}
	}
	
	@Override
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
			case "METER" 		: return "text";
			case "METRIC" 		: return "text";
			default 			: return null;
		}
	}
    
    public void query(String query) {
		ResultSet rs = session.execute(query);
		for(Row r : rs) {
			System.out.println(r.toString());
		}
	}
	
	@Override
	public void close() {
		if(cluster != null && !cluster.isClosed() && session != null && !session.isClosed()) {
			session.close();
			cluster.close();
		}
	}
}