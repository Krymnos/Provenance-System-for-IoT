package io.provenance.types;

public class InputDatapoint {

	private String id;
	private String contrIbution;
	
	public InputDatapoint(String id) {
		this.id = id;
	}
	
	public InputDatapoint(String id, String contrIbution) {
		this.id = id;
		this.contrIbution = contrIbution;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getContrIbution() {
		return contrIbution;
	}
	public void setContrIbution(String contrIbution) {
		this.contrIbution = contrIbution;
	}
	
}
