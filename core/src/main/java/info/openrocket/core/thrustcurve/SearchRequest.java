package info.openrocket.core.thrustcurve;

import java.util.ArrayList;
import java.util.List;

public class SearchRequest {

	private static final int MAX_RESULTS = 1000;	// Maximum motor results to return for one manufacturer search

	private String manufacturer;
	private String designation;
	private String brand_name;

	private String common_name;
	private String impulse_class;
	private Integer diameter;

	/*
	 * public enum Type {
	 * "SU";
	 * "reload";
	 * "hybrid"
	 * };
	 */
	private String type;

	public void setManufacturer(String manufacturer) {
		this.manufacturer = null;
		if (manufacturer != null) {
			manufacturer = manufacturer.trim();
			if (!manufacturer.isEmpty()) {
				this.manufacturer = manufacturer;
			}
		}
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public void setBrand_name(String brand_name) {
		this.brand_name = brand_name;
	}

	public void setCommon_name(String common_name) {
		if (common_name == null) {
			this.common_name = null;
			return;
		}
		this.common_name = common_name.trim();
		if (this.common_name.isEmpty()) {
			this.common_name = null;
		}
	}

	public void setImpulse_class(String impulse_class) {
		this.impulse_class = null;
		if (impulse_class != null) {
			this.impulse_class = impulse_class.trim();
			if (impulse_class.isEmpty()) {
				this.impulse_class = null;
			}
		}
	}

	public void setDiameter(Integer diameter) {
		this.diameter = diameter;
	}

	public void setDiameter(String diameter) {
		this.diameter = null;
		if (diameter == null) {
			return;
		}
		try {
			this.diameter = Integer.decode(diameter);
		} catch (NumberFormatException ex) {
			this.diameter = null;
		}
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		// Manual JSON construction
		StringBuilder json = new StringBuilder();
		json.append("{");

		List<String> fields = new ArrayList<>();

		if (manufacturer != null){
			fields.add("\"manufacturer\": \"" + escapeJson(manufacturer) + "\"");
		}
		if (designation != null){
			fields.add("\"designation\": \"" + escapeJson(designation) + "\"");
		}
		if (brand_name != null){
			fields.add("\"brandName\": \"" + escapeJson(brand_name) + "\""); // Note: camelCase in V1
		}
		if (common_name != null){
			fields.add("\"commonName\": \"" + escapeJson(common_name) + "\"");
		}
		if (impulse_class != null) {
			fields.add("\"impulseClass\": \"" + escapeJson(impulse_class) + "\"");
		}
		if (diameter != null) {
			fields.add("\"diameter\": " + diameter); // Number, no quotes
		}
		if (type != null) {
			fields.add("\"type\": \"" + escapeJson(type) + "\"");
		}

		// Add required V1 fields
		fields.add("\"maxResults\": " + MAX_RESULTS);

		json.append(String.join(",", fields));
		json.append("}");

		return json.toString();
	}

	private static String escapeJson(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\"", "\\\"");
	}
}
