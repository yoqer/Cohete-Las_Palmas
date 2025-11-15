package info.openrocket.swing.gui.export;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.file.svg.export.RingSvgExporter;
import info.openrocket.core.file.svg.export.SVGBuilder;
import info.openrocket.core.file.svg.export.SVGExportOptions;
import info.openrocket.core.rocketcomponent.Bulkhead;
import info.openrocket.core.rocketcomponent.CenteringRing;
import info.openrocket.core.rocketcomponent.RocketComponent;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SVGRocketPartsExporter {
	private static final double PART_PADDING = 0.01; // meters
	private static final double DEFAULT_ROW_WIDTH = 0.35; // meters

	public void export(OpenRocketDocument document, File destination, SVGExportOptions options)
			throws ParserConfigurationException, TransformerException {
		List<Part> parts = collectParts(document);
		if (parts.isEmpty()) {
			throw new IllegalStateException("No centering rings or bulkheads found.");
		}

		SVGBuilder builder = new SVGBuilder();
		double maxDiameter = 0;
		for (Part part : parts) {
			maxDiameter = Math.max(maxDiameter, part.diameter());
		}
		double rowWidth = Math.max(DEFAULT_ROW_WIDTH, maxDiameter + (2 * PART_PADDING) + 0.05);

		double cursorX = 0;
		double cursorY = 0;
		double rowHeight = 0;

		for (Part part : parts) {
			double width = part.diameter() + (2 * PART_PADDING);
			double height = part.diameter() + (2 * PART_PADDING);

			if (cursorX > 0 && cursorX + width > rowWidth) {
				cursorX = 0;
				cursorY += rowHeight + PART_PADDING;
				rowHeight = 0;
			}

			double centerX = cursorX + PART_PADDING + part.outerRadius;
			double centerY = cursorY + PART_PADDING + part.outerRadius;

			RingSvgExporter.renderRing(builder, centerX, centerY, part.outerRadius, part.innerRadius,
					part.holes, options);

			cursorX += width + PART_PADDING;
			rowHeight = Math.max(rowHeight, height);
		}

		builder.writeToFile(destination);
	}

	private List<Part> collectParts(OpenRocketDocument document) {
		List<Part> parts = new ArrayList<>();
		if (document == null || document.getRocket() == null) {
			return parts;
		}
		collectRecursive(document.getRocket(), parts);
		return parts;
	}

	private void collectRecursive(RocketComponent component, List<Part> parts) {
		if (component == null) {
			return;
		}

		if (component instanceof CenteringRing) {
			CenteringRing ring = (CenteringRing) component;
			List<RingSvgExporter.Hole> holes =
					RingSvgExporter.holesFromMotorMounts(ComponentSvgExportService.findSupportingMotorMounts(ring));
			parts.add(Part.fromRing(ring, holes));
		} else if (component instanceof Bulkhead) {
			parts.add(Part.fromBulkhead((Bulkhead) component));
		}

		List<RocketComponent> children = component.getChildren();
		if (children == null || children.isEmpty()) {
			return;
		}
		for (RocketComponent child : children) {
			collectRecursive(child, parts);
		}
	}

	private static final class Part {
		private final double outerRadius;
		private final double innerRadius;
		private final List<RingSvgExporter.Hole> holes;

		private Part(double outerRadius, double innerRadius, List<RingSvgExporter.Hole> holes) {
			this.outerRadius = outerRadius;
			this.innerRadius = innerRadius;
			this.holes = holes;
		}

		static Part fromRing(CenteringRing ring, List<RingSvgExporter.Hole> holes) {
			return new Part(ring.getOuterRadius(), ring.getInnerRadius(),
					holes == null ? Collections.emptyList() : holes);
		}

		static Part fromBulkhead(Bulkhead bulkhead) {
			return new Part(bulkhead.getOuterRadius(), 0, Collections.emptyList());
		}

		double diameter() {
			return outerRadius * 2;
		}
	}
}

