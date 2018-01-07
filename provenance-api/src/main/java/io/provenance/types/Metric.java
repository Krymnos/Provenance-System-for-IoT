package io.provenance.types;

public enum Metric {
	METER("meterid"),
	METRIC("metricid"),
	LOCATION("loc"),
	LINE("line"),
	CLASS("class"),
	APPLICATION("app"),
	CREATE_TIME("ctime"),
	SEND_TIME("stime"),
	RECEIVE_TIME("rtime");
	
	private String value;

    Metric(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static Metric fromValue(String text) {
      for (Metric metric : Metric.values()) {
        if (String.valueOf(metric.value).equals(text)) {
          return metric;
        }
      }
      return null;
    }
}
