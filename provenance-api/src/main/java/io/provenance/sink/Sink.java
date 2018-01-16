package io.provenance.sink;

import io.provenance.types.Datapoint;

public interface Sink {

	public void connect();
	public void defineSchema();
	public void ingest(Datapoint...datapoints);
	public void defineHeartbeatSchema();
	public void ingestHeartbeat();
	public void defineNodeRateSchema();
	public void ingestNodeRate(double sendRate, double receiveRate);
	public String getSinkFieldName(String fieldName);
	public String getSinkType(String fieldName);
	public void close();
}