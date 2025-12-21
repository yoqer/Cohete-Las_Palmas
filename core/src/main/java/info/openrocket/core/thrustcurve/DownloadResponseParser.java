package info.openrocket.core.thrustcurve;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DownloadResponseParser {

	public static DownloadResponse parse(InputStream in) throws IOException {
		DownloadResponse response = new DownloadResponse();

		// 1. Read InputStream to String
		String jsonString = readStream(in);

		// 2. Parse JSON
		Object root = SimpleJsonParser.parse(jsonString);

		if (!(root instanceof Map)) {
			return response; // Invalid format
		}

		Map<String, Object> rootMap = (Map<String, Object>) root;

		// 3. Extract Results
		if (rootMap.containsKey("results")) {
			List<Object> results = (List<Object>) rootMap.get("results");

			for (Object item : results) {
				if (item instanceof Map) {
					Map<String, Object> resultObj = (Map<String, Object>) item;

					MotorBurnFile mbf = new MotorBurnFile();
					mbf.init();

					// Parse String ID
					if (resultObj.get("motorId") instanceof String) {
						mbf.setMotorId((String) resultObj.get("motorId"));
					}

					if (resultObj.get("simfileId") instanceof String) {
						mbf.setSimfileId((String) resultObj.get("simfileId"));
					}

					if (resultObj.get("format") instanceof String) {
						mbf.setFiletype((String) resultObj.get("format"));
					}

					// Handle Data (Usually Base64 in V1 Download response)
					if (resultObj.get("data") instanceof String dataContent) {
						// MotorBurnFile.decodeFile expects Base64
						mbf.decodeFile(dataContent);
					}

					response.add(mbf);
				}
			}
		}

		// 4. Handle Error (if any)
		if (rootMap.containsKey("error")) {
			response.setError(String.valueOf(rootMap.get("error")));
		}

		return response;
	}

	private static String readStream(InputStream in) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) != -1) {
			result.write(buffer, 0, length);
		}
		return result.toString(StandardCharsets.UTF_8);
	}
}