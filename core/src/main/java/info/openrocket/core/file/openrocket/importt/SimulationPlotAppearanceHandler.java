package info.openrocket.core.file.openrocket.importt;

import java.util.HashMap;
import java.util.Map;

import info.openrocket.core.document.PlotAppearance;
import info.openrocket.core.file.simplesax.AbstractElementHandler;
import info.openrocket.core.file.simplesax.ElementHandler;
import info.openrocket.core.file.simplesax.PlainTextHandler;
import info.openrocket.core.logging.Warning;
import info.openrocket.core.logging.WarningSet;
import info.openrocket.core.util.LineStyle;
import info.openrocket.core.util.ORColor;
import info.openrocket.core.util.StringUtils;

class SimulationPlotAppearanceHandler extends AbstractElementHandler {
	private final Map<String, PlotAppearance> plotAppearances = new HashMap<>();

	@Override
	public ElementHandler openElement(String element, HashMap<String, String> attributes, WarningSet warnings) {
		if (element.equals("series")) {
			return PlainTextHandler.INSTANCE;
		}
		warnings.add("Unknown element '" + element + "', ignoring.");
		return null;
	}

	@Override
	public void closeElement(String element, HashMap<String, String> attributes, String content, WarningSet warnings) {
		if (!element.equals("series")) {
			return;
		}

		String symbol = attributes.get("symbol");
		if (StringUtils.isEmpty(symbol)) {
			warnings.add("Plot appearance series missing symbol, ignoring.");
			return;
		}

		LineStyle lineStyle = (LineStyle) DocumentConfig.findEnum(attributes.get("linestyle"), LineStyle.class);
		ORColor color = parseColor(attributes, warnings);

		PlotAppearance appearance = new PlotAppearance(color, lineStyle);
		if (!appearance.isEmpty()) {
			plotAppearances.put(symbol, appearance);
		}
	}

	public Map<String, PlotAppearance> getPlotAppearances() {
		return plotAppearances;
	}

	private static ORColor parseColor(HashMap<String, String> attributes, WarningSet warnings) {
		String red = attributes.get("red");
		String green = attributes.get("green");
		String blue = attributes.get("blue");
		if (red == null || green == null || blue == null) {
			return null;
		}

		int r, g, b;
		try {
			r = Integer.parseInt(red);
			g = Integer.parseInt(green);
			b = Integer.parseInt(blue);
		} catch (NumberFormatException e) {
			warnings.add(Warning.FILE_INVALID_PARAMETER);
			return null;
		}

		int a = 255;
		String alphaStr = attributes.get("alpha");
		if (alphaStr != null) {
			try {
				a = Integer.parseInt(alphaStr);
			} catch (NumberFormatException e) {
				warnings.add(Warning.FILE_INVALID_PARAMETER);
				return null;
			}
		}

		if (r < 0 || g < 0 || b < 0 || a < 0 || r > 255 || g > 255 || b > 255 || a > 255) {
			warnings.add(Warning.FILE_INVALID_PARAMETER);
			return null;
		}

		return new ORColor(r, g, b, a);
	}
}
