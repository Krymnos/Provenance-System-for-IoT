package io.provenance.types;

public class Location {
	
	private String lable;
	private Double latitude;
	private Double longitude;
	private boolean coordinatesSet;

	public Location(String lable) {
		this.lable = lable;
	}
	
	public Location(String lable, double latitude, double longitude) {
		this(lable);
		this.latitude = latitude;
		this.longitude = longitude;
		this.coordinatesSet = true;
	}
	
	public void setLotLong(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.coordinatesSet = true;
	}
	
	public String getLable() {
		return lable;
	}

	public void setLable(String lable) {
		this.lable = lable;
	}
	
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public boolean isCoordinatesSet() {
		return coordinatesSet;
	}
}