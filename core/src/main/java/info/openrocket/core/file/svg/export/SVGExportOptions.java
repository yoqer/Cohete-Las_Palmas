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

	public SVGExportOptions(Color strokeColor, double strokeWidthMm) {
		this(strokeColor, strokeWidthMm, true, strokeColor);
	}

	public SVGExportOptions(Color strokeColor, double strokeWidthMm, boolean drawCrosshair) {
		this(strokeColor, strokeWidthMm, drawCrosshair, strokeColor);
	}

	public SVGExportOptions(Color strokeColor, double strokeWidthMm, boolean drawCrosshair, Color crosshairColor) {
		this.strokeColor = Objects.requireNonNull(strokeColor, "strokeColor");
		this.strokeWidthMm = strokeWidthMm;
		this.drawCrosshair = drawCrosshair;
		this.crosshairColor = Objects.requireNonNull(crosshairColor, "crosshairColor");
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

	public SVGExportOptions withStrokeColor(Color color) {
		return new SVGExportOptions(color, strokeWidthMm, drawCrosshair, crosshairColor);
	}

	public SVGExportOptions withStrokeWidth(double strokeWidth) {
		return new SVGExportOptions(strokeColor, strokeWidth, drawCrosshair, crosshairColor);
	}

	public SVGExportOptions withDrawCrosshair(boolean drawCrosshair) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, crosshairColor);
	}

	public SVGExportOptions withCrosshairColor(Color color) {
		return new SVGExportOptions(strokeColor, strokeWidthMm, drawCrosshair, color);
	}
}

