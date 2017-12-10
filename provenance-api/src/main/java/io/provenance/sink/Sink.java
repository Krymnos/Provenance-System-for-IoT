package io.provenance.sink;

import io.provenance.types.Datapoint;

public interface Sink {
	public void connect();
	public void defineSchema();
	public String[] ingest(Datapoint...datapoints);
	public String getSinkFieldName(String fieldName);
	public String getSinkType(String fieldName);
	public void close();
}
