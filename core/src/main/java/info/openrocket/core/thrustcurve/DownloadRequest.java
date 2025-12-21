package info.openrocket.core.thrustcurve;

import java.util.ArrayList;

class DownloadRequest {

	private final ArrayList<String> motorIds = new ArrayList<>();

	private String format = null;

	public void add(String motorId) {
		this.motorIds.add(motorId);
	}

	public void setFormat(String format) {
		this.format = format;
	}

	@Override
	public String toString() {
		// V1 Download endpoint expects a specific JSON format
		// Check Swagger, but usually it's just POSTing data to /download
		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"motorIds\": [");

		for (int i = 0; i < motorIds.size(); i++) {
			json.append("\"").append(motorIds.get(i)).append("\"");
			if (i < motorIds.size() - 1) json.append(",");
		}

		json.append("]");
		// format is optional in V1, defaults to all if not specified
		if (format != null) {
			json.append(", \"format\": \"").append(format).append("\"");
		}
		json.append("}");
		return json.toString();
	}

}
