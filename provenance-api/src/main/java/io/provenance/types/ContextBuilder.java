package io.provenance.types;

import java.util.Date;

public class ContextBuilder {

	private Context context;
	
	public ContextBuilder() {
		context = new Context();
	}
	
	public Context build() {
		return context;
	}
	
	public ContextBuilder setLocation(Location loc) {
		context.setLoc(loc);
		return this;
	}
	
	public ContextBuilder setTimestamp(Date date) {
		context.setTimestamp(date);
		return this;
	}

	public ContextBuilder setSendTimestamp(Date date) {
		context.setSendTime(date);
		return this;
	}
	
	public ContextBuilder setReceiveTimestamp(Date date) {
		context.setReceiveTime(date);
		return this;
	}
	
	public ContextBuilder setLineNo(Long lineNo) {
		context.setLineNo(lineNo);;
		return this;
	}

	public ContextBuilder setAppName(String appName) {
		context.setAppName(appName);
		return this;
	}

	public ContextBuilder setClassName(String className) {
		context.setClassName(className);
		return this;
	}
	
	public ContextBuilder setMetricId(String metricId) {
		context.setMetricId(metricId);
		return this;
	}
	
	public ContextBuilder setMeterId(String meterId) {
		context.setMeterId(meterId);
		return this;
	}
}
