package info.openrocket.swing.gui.export;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.file.svg.export.FinSvgExporter;
import info.openrocket.core.file.svg.export.ProfileSvgExporter;
import info.openrocket.core.file.svg.export.RailButtonSvgExporter;
import info.openrocket.core.file.svg.export.RingSvgExporter;
import info.openrocket.core.file.svg.export.SVGBuilder;
import info.openrocket.core.file.svg.export.SVGExportOptions;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.Bulkhead;
import info.openrocket.core.rocketcomponent.CenteringRing;
import info.openrocket.core.rocketcomponent.FinSet;
import info.openrocket.core.rocketcomponent.NoseCone;
import info.openrocket.core.rocketcomponent.RailButton;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.SymmetricComponent;
import info.openrocket.core.rocketcomponent.Transition;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SVGRocketPartsExporter {
	private static final double PART_PADDING = 0.01; // meters
	private static final double DEFAULT_ROW_WIDTH = 0.35; // meters
	private static final double LABEL_FONT_SIZE_MM = 3.0; // millimeters
	private static final double LABEL_SPACING = 0.002; // meters spacing between part and label

	public void export(OpenRocketDocument document, File destination, SVGExportOptions options)
			throws ParserConfigurationException, TransformerException {
		List<Part> parts = collectParts(document);
		if (parts.isEmpty()) {
			throw new IllegalStateException("No exportable components found.");
		}

		SVGBuilder builder = new SVGBuilder();
		double maxDimension = 0;
		for (Part part : parts) {
			maxDimension = Math.max(maxDimension, Math.max(part.width, part.height));
		}
		double rowWidth = Math.max(DEFAULT_ROW_WIDTH, maxDimension + (2 * PART_PADDING) + 0.05);

		layoutParts(builder, parts, rowWidth, options);

		builder.writeToFile(destination);
	}

	public void export(List<RocketComponent> components, File destination, SVGExportOptions options)
			throws ParserConfigurationException, TransformerException {
		List<Part> parts = collectParts(components);
		if (parts.isEmpty()) {
			throw new IllegalStateException("No exportable components found.");
		}

		SVGBuilder builder = new SVGBuilder();
		double maxDimension = 0;
		for (Part part : parts) {
			maxDimension = Math.max(maxDimension, Math.max(part.width, part.height));
		}
		double rowWidth = Math.max(DEFAULT_ROW_WIDTH, maxDimension + (2 * PART_PADDING) + 0.05);

		layoutParts(builder, parts, rowWidth, options);

		builder.writeToFile(destination);
	}

	private void layoutParts(SVGBuilder builder, List<Part> parts, double rowWidth, SVGExportOptions options) {
		double cursorX = 0;
		double cursorY = 0;
		double rowHeight = 0;
		double labelHeight = options.isShowLabels() ? (LABEL_FONT_SIZE_MM / 1000.0) + LABEL_SPACING : 0; // Convert mm to meters

		for (Part part : parts) {
			double width = part.width + (2 * PART_PADDING);
			double height = part.height + (2 * PART_PADDING) + labelHeight;

			if (cursorX > 0 && cursorX + width > rowWidth) {
				cursorX = 0;
				cursorY += rowHeight + PART_PADDING;
				rowHeight = 0;
			}

			double originX = cursorX + PART_PADDING;
			double originY = cursorY + PART_PADDING;
			part.renderer.render(builder, originX, originY, options);

			// Add label below the part, centered horizontally
			if (options.isShowLabels() && part.label != null && !part.label.trim().isEmpty()) {
				double labelX = originX + (part.width / 2.0);
				double labelY = originY + part.height + LABEL_SPACING + (LABEL_FONT_SIZE_MM / 1000.0);
				builder.addText(labelX, labelY, part.label, LABEL_FONT_SIZE_MM, options.getLabelColor());
			}

			cursorX += width + PART_PADDING;
			rowHeight = Math.max(rowHeight, height);
		}
	}

	private List<Part> collectParts(OpenRocketDocument document) {
		List<Part> parts = new ArrayList<>();
		if (document == null || document.getRocket() == null) {
			return parts;
		}
		collectRecursive(document.getRocket(), parts);
		return parts;
	}

	private List<Part> collectParts(List<RocketComponent> components) {
		List<Part> parts = new ArrayList<>();
		if (components == null || components.isEmpty()) {
			return parts;
		}
		for (RocketComponent component : components) {
			collectRecursive(component, parts);
		}
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
		} else if (component instanceof FinSet) {
			parts.add(Part.fromFinSet((FinSet) component));
		} else if (component instanceof NoseCone) {
			parts.add(Part.fromProfile((NoseCone) component));
		} else if (component instanceof BodyTube) {
			parts.add(Part.fromProfile((BodyTube) component));
		} else if (component instanceof Transition) {
			parts.add(Part.fromProfile((Transition) component));
		} else if (component instanceof RailButton) {
			parts.add(Part.fromRailButton((RailButton) component));
		}

		List<RocketComponent> children = component.getChildren();
		if (children == null || children.isEmpty()) {
			return;
		}
		for (RocketComponent child : children) {
			collectRecursive(child, parts);
		}
	}

	private interface PartRenderer {
		void render(SVGBuilder builder, double originX, double originY, SVGExportOptions options);
	}

	private static final class Part {
		private final double width;
		private final double height;
		private final PartRenderer renderer;
		private final String label;

		private Part(double width, double height, PartRenderer renderer, String label) {
			this.width = width;
			this.height = height;
			this.renderer = renderer;
			this.label = label;
		}

		static Part fromRing(CenteringRing ring, List<RingSvgExporter.Hole> holes) {
			double outerRadius = ring.getOuterRadius();
			double innerRadius = ring.getInnerRadius();
			List<RingSvgExporter.Hole> safeHoles = holes == null ? Collections.emptyList() : holes;
			String label = ring.getName();
			return new Part(outerRadius * 2, outerRadius * 2, (builder, originX, originY, options) -> {
				double centerX = originX + outerRadius;
				double centerY = originY + outerRadius;
				RingSvgExporter.renderRing(builder, centerX, centerY, outerRadius, innerRadius, safeHoles, options);
			}, label);
		}

		static Part fromBulkhead(Bulkhead bulkhead) {
			double outerRadius = bulkhead.getOuterRadius();
			String label = bulkhead.getName();
			return new Part(outerRadius * 2, outerRadius * 2, (builder, originX, originY, options) -> {
				double centerX = originX + outerRadius;
				double centerY = originY + outerRadius;
				RingSvgExporter.renderRing(builder, centerX, centerY, outerRadius, 0, Collections.emptyList(), options);
			}, label);
		}

		static Part fromFinSet(FinSet finSet) {
			FinSvgExporter.Bounds bounds = FinSvgExporter.calculateBounds(finSet);
			double width = bounds.getWidth();
			double height = bounds.getHeight();
			double minX = bounds.getMinX();
			double minY = bounds.getMinY();
			String label = finSet.getName();
			return new Part(width, height, (builder, originX, originY, options) -> {
				double offsetX = originX - minX;
				double offsetY = originY - minY;
				FinSvgExporter.drawFinSet(finSet, builder, offsetX, offsetY, options);
			}, label);
		}

		static Part fromProfile(SymmetricComponent comp) {
			ProfileSvgExporter.Bounds b = ProfileSvgExporter.calculateBounds(comp);
			double width = Math.max(0.001, b.getWidth());
			double height = Math.max(0.001, b.getMaxAbsY() * 2.0); // accommodate top and bottom space
			String label = comp.getName();
			return new Part(width, height, (builder, originX, originY, options) -> {
				// center the full closed outline vertically within its tile
				double midY = originY + (height / 2.0);
				ProfileSvgExporter.drawClosedProfile(comp, builder, originX, midY, options);
			}, label);
		}

		static Part fromRailButton(RailButton railButton) {
			RailButtonSvgExporter.Bounds bounds = RailButtonSvgExporter.calculateBounds(railButton);
			double width = Math.max(0.001, bounds.getWidth());
			double height = Math.max(0.001, bounds.getHeight());
			String label = railButton.getName();
			return new Part(width, height, (builder, originX, originY, options) -> {
				// Rail button reference point is center bottom, so adjust originY to account for bounds
				double adjustedY = originY - bounds.getMinY();
				double adjustedX = originX - bounds.getMinX();
				RailButtonSvgExporter.drawRailButtonProfile(railButton, builder, adjustedX, adjustedY, options);
			}, label);
		}
	}
}

