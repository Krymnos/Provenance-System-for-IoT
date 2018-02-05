package io.provenance.types;

import io.provenance.config.ProvenanceConfig;

public class Datapoint {
	
	private String id;
	private InputDatapoint[] inputDatapoints;
	private static long counter;
	private Context context;
	
	public Datapoint() {
		id = generateID();
	}
	
	public Datapoint(Context context) {
		this();
		this.context = context;
	}
	
	public Datapoint(InputDatapoint[] inputDatapoints) {
		this();
		this.inputDatapoints = inputDatapoints;
	}
	
	public Datapoint(InputDatapoint[] inputDatapoints, Context context) {
		this();
		this.inputDatapoints = inputDatapoints;
		this.context = context;
	}
	
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}
	
	public InputDatapoint[] getInputDatapoints() {
		return inputDatapoints;
	}

	public void setInputDatapoints(InputDatapoint[] inputDatapoints) {
		this.inputDatapoints = inputDatapoints;
	}

	public String getId() {
		return id;
	}
	
	private String generateID() {
		StringBuilder id = new StringBuilder()
				.append(format(Long.toHexString(System.currentTimeMillis() / 1000), 8))
				.append(format(Long.toHexString(counter++), 6))
				.append(format(ProvenanceConfig.getNodeId(), 6));
		return id.toString();
	}
	
	private String format(String str, int len) {
		if(str.length() >= len)
			return str.substring(0, len);
		else
			return String.format("%" + len + "s", str).replaceAll(" ", "0");
	}
}