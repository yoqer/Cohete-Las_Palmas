package info.openrocket.core.file.svg.export;

import java.awt.Color;
import java.util.Objects;

/**
 * Immutable container describing how SVG outlines should be rendered.
 * At the moment it only tracks stroke characteristics but it can be
 * extended later (fill colors, dash styles, etc.).
 */
public class SVGExportOptions {
	private final Color strokeColor;
	private final double strokeWidthMm;
	private final boolean drawCrosshair;
	private final Color crosshairColor;
	private final boolean showLabels;
	private final Color labelColor;
	private final double paperWidthM;
	private final double paperHeightM;
	private final double partSpacingM;

	public SVGExportOptions(Color strokeColor, double strokeWidthMm) {
		this(strokeColor, strokeWidthMm, true, strokeColor, true, strokeColor, Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
	}

	public SVGExportOptions(Color strokeColor, double strokeWidthMm, boolean drawCrosshair) {
		this(strokeColor, strokeWidthMm, drawCrosshair, strokeColor, true, strokeColor, Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
	}

	public SVGExportOptions(Color strokeColor, double strokeWidthMm, boolean drawCrosshair, Color crosshairColor) {
		this(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, true, strokeColor, Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
	}

	public SVGExportOptions(Color strokeColor, double strokeWidthMm, boolean drawCrosshair, Color crosshairColor, boolean showLabels) {
		this(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, strokeColor, Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
	}

	public SVGExportOptions(Color strokeColor, double strokeWidthMm, boolean drawCrosshair, Color crosshairColor, boolean showLabels, Color labelColor) {
		this(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, labelColor, Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
	}

	public SVGExportOptions(Color strokeColor, double strokeWidthMm, boolean drawCrosshair, Color crosshairColor, boolean showLabels, Color labelColor, double paperWidthM, double paperHeightM, double partSpacingM) {
		this.strokeColor = Objects.requireNonNull(strokeColor, "strokeColor");
		this.strokeWidthMm = strokeWidthMm;
		this.drawCrosshair = drawCrosshair;
		this.crosshairColor = Objects.requireNonNull(crosshairColor, "crosshairColor");
		this.showLabels = showLabels;
		this.labelColor = Objects.requireNonNull(labelColor, "labelColor");
		this.paperWidthM = paperWidthM;
		this.paperHeightM = paperHeightM;
		this.partSpacingM = partSpacingM;
	}

	public Color getStrokeColor() {
		return strokeColor;
	}

	public double getStrokeWidthMm() {
		return strokeWidthMm;
	}

	public boolean isDrawCrosshair() {
		return drawCrosshair;
	}

	public Color getCrosshairColor() {
		return crosshairColor;
	}

	public boolean isShowLabels() {
		return showLabels;
	}

	public Color getLabelColor() {
		return labelColor;
	}

	public SVGExportOptions withStrokeColor(Color color) {
		return new SVGExportOptions(color, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, labelColor, paperWidthM, paperHeightM, partSpacingM);
	}

	public SVGExportOptions withStrokeWidth(double strokeWidth) {
		return new SVGExportOptions(strokeColor, strokeWidth, drawCrosshair, crosshairColor, showLabels, labelColor, paperWidthM, paperHeightM, partSpacingM);
	}

	public SVGExportOptions withDrawCrosshair(boolean drawCrosshair) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, labelColor, paperWidthM, paperHeightM, partSpacingM);
	}

	public SVGExportOptions withCrosshairColor(Color color) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, color, showLabels, labelColor, paperWidthM, paperHeightM, partSpacingM);
	}

	public SVGExportOptions withShowLabels(boolean showLabels) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, labelColor, paperWidthM, paperHeightM, partSpacingM);
	}

	public SVGExportOptions withLabelColor(Color color) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, color, paperWidthM, paperHeightM, partSpacingM);
	}

	public double getPaperWidthM() {
		return paperWidthM;
	}

	public double getPaperHeightM() {
		return paperHeightM;
	}

	public double getPartSpacingM() {
		return partSpacingM;
	}

	public SVGExportOptions withPaperSize(double paperWidthM, double paperHeightM) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, labelColor, paperWidthM, paperHeightM, partSpacingM);
	}

	public SVGExportOptions withPartSpacing(double partSpacingM) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor, showLabels, labelColor, paperWidthM, paperHeightM, partSpacingM);
	}
}

