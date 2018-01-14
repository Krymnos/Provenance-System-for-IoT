package io.provenance.types;

public class InputDatapoint {

	private String id;
	private String contribution;
	
	public InputDatapoint(String id, String contrIbution) {
		this.id = id;
		this.contribution = contrIbution;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getContribution() {
		return contribution;
	}
	public void setContribution(String contribution) {
		this.contribution = contribution;
	}
}