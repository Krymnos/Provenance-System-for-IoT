package io.provenance.sink;

import java.util.Map;
import io.provenance.types.Datapoint;

public interface Sink {

	public void connect();
	public void defineSchema();
	public void ingest(Datapoint...datapoints);
	public void ingestHeartbeat(long pipelineDaemonTime, Map<String, Long> neighboursStatus);
	public void ingestNodeRate(double sendRate, double receiveRate);
	public String getSinkFieldName(String fieldName);
	public String getSinkType(String fieldName);
	public void close();
}