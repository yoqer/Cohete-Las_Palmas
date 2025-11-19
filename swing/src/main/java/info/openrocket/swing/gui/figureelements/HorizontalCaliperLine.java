package info.openrocket.swing.gui.figureelements;

import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.theme.UITheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A horizontal line element for the caliper measurement tool.
 * Draws a horizontal line with a draggable handle on the left side.
 *
 * @author OpenRocket Team
 */
public class HorizontalCaliperLine implements FigureElement {

	private static final float HANDLE_RECT_WIDTH = 36.0f;  // Wide rectangle
	private static final float HANDLE_RECT_HEIGHT = 22.0f;  // Narrow height for elongated look
	private static final float HANDLE_TRIANGLE_WIDTH = 26.0f;  // Triangle to the right
	private static final float LINE_WIDTH_NORMAL = 2.0f;  // Normal line thickness
	private static final float LINE_WIDTH_HOVER = 4.0f;  // Thicker line when hovering
	private static final Font HANDLE_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 30);

	private double y;  // Y position in model coordinates
	private boolean isHovered = false;  // Whether the mouse is hovering over this line
	private String handleLabel = "";

	private static Color lineColor;
	private static Color handleColor;

	static {
		initColors();
	}

	/**
	 * Create a new HorizontalCaliperLine at the specified y coordinate.
	 *
	 * @param y the y position in model coordinates (meters)
	 */
	public HorizontalCaliperLine(double y) {
		this.y = y;
	}

	private static void initColors() {
		updateColors();
		UITheme.Theme.addUIThemeChangeListener(HorizontalCaliperLine::updateColors);
	}

	public static void updateColors() {
		// Use theme-dependent caliper color
		lineColor = GUIUtil.getUITheme().getCaliperColor();

		// Use an even brighter color for the handle
		Color baseColor = lineColor;
		handleColor = new Color(
				Math.min(255, baseColor.getRed() + 50),
				Math.min(255, baseColor.getGreen() + 50),
				Math.min(255, baseColor.getBlue() + 50),
				baseColor.getAlpha()
		);
	}

	/**
	 * Set the y position of the caliper line in model coordinates.
	 *
	 * @param y the y position in meters
	 */
	public void setY(double y) {
		this.y = y;
	}

	/**
	 * Get the y position of the caliper line in model coordinates.
	 *
	 * @return the y position in meters
	 */
	public double getY() {
		return y;
	}

	/**
	 * Set whether the mouse is hovering over this caliper line.
	 *
	 * @param hovered true if hovering, false otherwise
	 */
	public void setHovered(boolean hovered) {
		this.isHovered = hovered;
	}

	/**
	 * Check if the mouse is hovering over this caliper line.
	 *
	 * @return true if hovering, false otherwise
	 */
	public boolean isHovered() {
		return isHovered;
	}

	/**
	 * Set the label displayed inside the handle (e.g., "1" or "2").
	 *
	 * @param label the label text
	 */
	public void setHandleLabel(String label) {
		this.handleLabel = label != null ? label : "";
	}

	@Override
	public void paint(Graphics2D g2, double scale) {
		paint(g2, scale, null);
	}

	@Override
	public void paint(Graphics2D g2, double scale, Rectangle visible) {
		// Use a graphics copy for screen-space drawing to avoid altering the main context
		Graphics2D g2Screen = (Graphics2D) g2.create();
		try {
			// Calculate screen coordinates for the handle
			AffineTransform transform = g2.getTransform();
			Point2D.Double modelPoint = new Point2D.Double(0, y);
			Point2D.Double screenPoint = new Point2D.Double();
			transform.transform(modelPoint, screenPoint);

			if (Double.isNaN(screenPoint.y) || Double.isInfinite(screenPoint.y)) {
				return;
			}

			// Use the visible rectangle's left X coordinate to position the handle correctly
			// This ensures the handle stays at the left of the visible area after window resize
			double handleX_screen = visible != null ? visible.x : 0.0;
			double handleY_screen = screenPoint.y;

			// Reset transform to draw in screen coordinates
			g2Screen.setTransform(new AffineTransform());
			
			// Draw horizontal line covering the full visible area width
			// Use the visible rectangle to determine the line bounds
			double lineLeft = visible != null ? visible.x : 0.0;
			double lineRight = visible != null ? (visible.x + visible.width) : 20000;
			Line2D.Double screenLine = new Line2D.Double(lineLeft, handleY_screen, lineRight, handleY_screen);
			g2Screen.setStroke(new BasicStroke(isHovered ? LINE_WIDTH_HOVER : LINE_WIDTH_NORMAL, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2Screen.setColor(lineColor);
			g2Screen.draw(screenLine);

			// Draw marker handle as one continuous path: elongated rectangle on left, triangle to the right
			// Create a single manifold path for the entire marker shape
			Path2D.Double marker = new Path2D.Double();

			double rectLeft = handleX_screen;
			double rectRight = handleX_screen + HANDLE_RECT_WIDTH;
			double rectTop = handleY_screen - HANDLE_RECT_HEIGHT / 2;
			double rectBottom = handleY_screen + HANDLE_RECT_HEIGHT / 2;
			double triangleBaseX = rectRight + HANDLE_TRIANGLE_WIDTH;

			// Marker shape path
			marker.moveTo(rectLeft, rectTop);		// Top-left of rectangle
			marker.lineTo(rectLeft, rectBottom);	// Bottom-left of rectangle
			marker.lineTo(rectRight, rectBottom);	// Bottom-right of rectangle
			marker.lineTo(triangleBaseX, handleY_screen);	// Point of triangle
			marker.lineTo(rectRight, rectTop);	// Top-right of rectangle
			marker.closePath();

			// Fill the entire marker shape
			g2Screen.setColor(handleColor);
			g2Screen.fill(marker);

			// Draw darker border around the entire marker shape
			Color borderColor = new Color(
					Math.max(0, lineColor.getRed() - 100),
					Math.max(0, lineColor.getGreen() - 100),
					Math.max(0, lineColor.getBlue() - 100),
					255
			);
			g2Screen.setColor(borderColor);
			g2Screen.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2Screen.draw(marker);

			// Draw label (1 or 2) inside the handle rectangle if provided
			if (handleLabel != null && !handleLabel.isEmpty()) {
				g2Screen.setFont(HANDLE_LABEL_FONT);
				FontRenderContext frc = g2Screen.getFontRenderContext();
				Rectangle2D textBounds = HANDLE_LABEL_FONT.getStringBounds(handleLabel, frc);
				double textWidth = textBounds.getWidth();
				double textHeight = textBounds.getHeight();
				// Position text horizontally centered within rectangle
				double textX = rectLeft + (HANDLE_RECT_WIDTH / 2.0) - textWidth / 2.0;
				double textY = handleY_screen + textHeight / 4.0;
				g2Screen.setColor(Color.BLACK);
				g2Screen.drawString(handleLabel, (float) textX, (float) textY);
			}
		} finally {
			g2Screen.dispose();
		}
	}
}

