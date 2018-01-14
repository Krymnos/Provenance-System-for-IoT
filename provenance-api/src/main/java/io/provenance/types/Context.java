package io.provenance.types;

import java.util.Date;

public class Context {
	
	private Location loc;
	private Long lineNo;
	private String appName;
	private String className;
	private Date timestamp;
	private Date sendTime;
	private Date receiveTime;
	private String meterId;
	private String metricId;

	public static ContextBuilder builder() {
		return new ContextBuilder();
	}
	
	public Location getLoc() {
		return loc;
	}
	public void setLoc(Location loc) {
		this.loc = loc;
	}
	
	public Long getLineNo() {
		return lineNo;
	}
	public void setLineNo(Long lineNo) {
		this.lineNo = lineNo;
	}
	
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public void setTimestamp(Long timestamp) {
		this.timestamp = new Date(timestamp);
	}
	
	public Date getSendTime() {
		return sendTime;
	}
	public void setSendTime(Date sendTime) {
		this.sendTime = sendTime;
	}
	
	public void setSendTime(Long sendTime) {
		this.sendTime = new Date(sendTime);
	}

	public Date getReceiveTime() {
		return receiveTime;
	}
	public void setReceiveTime(Date receiveTime) {
		this.receiveTime = receiveTime;
	}
	
	public void setReceiveTime(Long receiveTime) {
		this.receiveTime = new Date(receiveTime);
	}
	
	public String getMeterId() {
		return meterId;
	}
	public void setMeterId(String meterId) {
		this.meterId = meterId;
	}

	public String getMetricId() {
		return metricId;
	}
	public void setMetricId(String metricId) {
		this.metricId = metricId;
	}
}