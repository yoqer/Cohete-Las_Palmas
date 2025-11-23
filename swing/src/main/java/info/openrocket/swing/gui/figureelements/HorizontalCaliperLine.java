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
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class HorizontalCaliperLine implements FigureElement {

	private static final float HANDLE_RECT_WIDTH = 36.0f;  // Wide rectangle
	private static final float HANDLE_RECT_HEIGHT = 22.0f;  // Narrow height for elongated look
	private static final float HANDLE_TRIANGLE_WIDTH = 26.0f;  // Triangle to the right
	private static final float LINE_WIDTH_NORMAL = 2.0f;  // Normal line thickness
	private static final float LINE_WIDTH_HOVER = 4.0f;  // Thicker line when hovering
	private static final Font HANDLE_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 30);
	private static final Font INDICATOR_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 24);  // Font for indicator label
	
	// Out-of-view indicator
	private static final float ARROW_SIZE = 20.0f;  // Size of the arrow indicator
	private static final float ARROW_STROKE_WIDTH = 3.0f;  // Thickness of arrow lines
	private static final float LABEL_OFFSET = 8.0f;  // Distance from arrow to label

	private double y;  // Y position in model coordinates
	private boolean isHovered = false;  // Whether the mouse is hovering over this line
	private boolean isSnapMode = false;  // Whether we're in snap mode (affects transparency)
	private String handleLabel = "";

	private static Color lineColor;
	private static Color handleColor;
	private static Color textColor;

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
		
		// Use theme-dependent text color for labels
		textColor = GUIUtil.getUITheme().getTextColor();

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
	 * Set whether this line is in snap mode (affects transparency).
	 *
	 * @param snapMode true if in snap mode
	 */
	public void setSnapMode(boolean snapMode) {
		this.isSnapMode = snapMode;
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

			// Reset transform to draw in screen coordinates
			g2Screen.setTransform(new AffineTransform());
			
			// Get the actual visible bounds in screen coordinates (after transform reset)
			// Use clip bounds if available, otherwise fall back to visible parameter
			Rectangle screenVisible = visible;
			if (g2Screen.getClipBounds() != null) {
				screenVisible = g2Screen.getClipBounds();
			}
			
			// Use the visible rectangle's left X coordinate to position the handle correctly
			// This ensures the handle stays at the left of the visible area after window resize
			double handleX_screen = screenVisible != null ? screenVisible.x : 0.0;
			double handleY_screen = screenPoint.y;
			
			// Draw horizontal line covering the full visible area width
			// Use the visible rectangle to determine the line bounds
			double lineLeft = screenVisible != null ? screenVisible.x : 0.0;
			double lineRight = screenVisible != null ? (screenVisible.x + screenVisible.width) : 20000;
			Line2D.Double screenLine = new Line2D.Double(lineLeft, handleY_screen, lineRight, handleY_screen);
			g2Screen.setStroke(new BasicStroke(isHovered ? LINE_WIDTH_HOVER : LINE_WIDTH_NORMAL, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			
			// Apply 50% transparency in snap mode
			Color drawColor = lineColor;
			if (isSnapMode) {
				drawColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 128);  // 50% alpha
			}
			g2Screen.setColor(drawColor);
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
			Color drawHandleColor = handleColor;
			if (isSnapMode) {
				drawHandleColor = new Color(handleColor.getRed(), handleColor.getGreen(), handleColor.getBlue(), 128);  // 50% alpha
			}
			g2Screen.setColor(drawHandleColor);
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
			
			// Draw out-of-view indicator if the caliper line is outside the visible area
			if (screenVisible != null) {
				// Both handleY_screen and screenVisible are now in screen coordinates (after transform reset)
				// Add a small margin to avoid flickering when the line is right at the edge
				double margin = 5.0;
				boolean isOutOfView = (handleY_screen < screenVisible.y - margin) || 
				                      (handleY_screen > screenVisible.y + screenVisible.height + margin);
				if (isOutOfView) {
					drawOutOfViewIndicator(g2Screen, handleY_screen, screenVisible);
				}
			}
		} finally {
			g2Screen.dispose();
		}
	}
	
	/**
	 * Get the screen Y position of the caliper line.
	 * This is a helper method to calculate the screen position from the model position.
	 *
	 * @param transform the transform from model to screen coordinates
	 * @return the screen Y position, or Double.NaN if invalid
	 */
	public double getScreenY(AffineTransform transform) {
		Point2D.Double modelPoint = new Point2D.Double(0, y);
		Point2D.Double screenPoint = new Point2D.Double();
		transform.transform(modelPoint, screenPoint);
		return screenPoint.y;
	}
	
	/**
	 * Get the bounds of the out-of-view indicator in screen coordinates.
	 *
	 * @param caliperYScreen the Y position of the caliper line in screen coordinates
	 * @param visible the visible viewport rectangle
	 * @return the bounds of the indicator, or null if not out of view
	 */
	public Rectangle2D.Double getIndicatorBounds(double caliperYScreen, Rectangle visible) {
		if (visible == null) {
			return null;
		}
		
		double margin = 5.0;
		boolean isOutOfView = (caliperYScreen < visible.y - margin) || 
		                      (caliperYScreen > visible.y + visible.height + margin);
		if (!isOutOfView) {
			return null;
		}
		
		// Determine which edge to draw the arrow on
		boolean isTop = caliperYScreen < visible.y;
		double arrowY = isTop ? visible.y : visible.y + visible.height;
		double arrowX = visible.x + visible.width / 2.0;
		
		// Calculate bounds: arrow area + label area
		double minX, maxX, minY, maxY;
		minX = arrowX - ARROW_SIZE / 2.0 - 10; // Add padding
		maxX = arrowX + ARROW_SIZE / 2.0 + 10;
		if (handleLabel != null && !handleLabel.isEmpty()) {
			// Account for label width (approximate)
			minX -= 30;
			maxX += 30;
		}
		
		if (isTop) {
			minY = arrowY;
			maxY = arrowY + ARROW_SIZE + LABEL_OFFSET;
			if (handleLabel != null && !handleLabel.isEmpty()) {
				// Add label height
				maxY += 30; // Approximate label height
			}
		} else {
			maxY = arrowY;
			minY = arrowY - ARROW_SIZE - LABEL_OFFSET;
			if (handleLabel != null && !handleLabel.isEmpty()) {
				// Subtract label height
				minY -= 30; // Approximate label height
			}
		}
		
		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}
	
	/**
	 * Draw an arrow indicator at the edge of the viewport pointing toward the caliper line.
	 *
	 * @param g2Screen the graphics context in screen coordinates
	 * @param caliperY the Y position of the caliper line in screen coordinates
	 * @param visible the visible viewport rectangle
	 */
	private void drawOutOfViewIndicator(Graphics2D g2Screen, double caliperY, Rectangle visible) {
		Color indicatorColor = lineColor;
		if (isSnapMode) {
			indicatorColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 128);
		}
		g2Screen.setColor(indicatorColor);
		g2Screen.setStroke(new BasicStroke(ARROW_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		
		// Determine which edge to draw the arrow on
		boolean isTop = caliperY < visible.y;
		double arrowY = isTop ? visible.y : visible.y + visible.height;
		
		// Position arrow horizontally centered in the visible area
		double arrowX = visible.x + visible.width / 2.0;
		
		// Draw arrow pointing toward the caliper line
		Path2D.Double arrow = new Path2D.Double();
		if (isTop) {
			// Arrow pointing up (caliper is above)
			arrow.moveTo(arrowX, arrowY);
			arrow.lineTo(arrowX - ARROW_SIZE / 2, arrowY + ARROW_SIZE);
			arrow.lineTo(arrowX + ARROW_SIZE / 2, arrowY + ARROW_SIZE);
			arrow.closePath();
		} else {
			// Arrow pointing down (caliper is below)
			arrow.moveTo(arrowX, arrowY);
			arrow.lineTo(arrowX - ARROW_SIZE / 2, arrowY - ARROW_SIZE);
			arrow.lineTo(arrowX + ARROW_SIZE / 2, arrowY - ARROW_SIZE);
			arrow.closePath();
		}
		
		g2Screen.fill(arrow);
		g2Screen.draw(arrow);
		
		// Draw label next to the arrow if available
		if (handleLabel != null && !handleLabel.isEmpty()) {
			g2Screen.setFont(INDICATOR_LABEL_FONT);
			FontRenderContext frc = g2Screen.getFontRenderContext();
			Rectangle2D textBounds = INDICATOR_LABEL_FONT.getStringBounds(handleLabel, frc);
			double textWidth = textBounds.getWidth();
			double textHeight = textBounds.getHeight();
			
			// Position label next to the arrow
			double labelX, labelY;
			if (isTop) {
				// Label below the arrow (arrow points up)
				labelX = arrowX - textWidth / 2.0;  // Horizontally centered
				labelY = arrowY + ARROW_SIZE + LABEL_OFFSET + textHeight;
			} else {
				// Label above the arrow (arrow points down)
				labelX = arrowX - textWidth / 2.0;  // Horizontally centered
				labelY = arrowY - ARROW_SIZE - LABEL_OFFSET;
			}
			
			g2Screen.setColor(textColor);
			g2Screen.drawString(handleLabel, (float) labelX, (float) labelY);
		}
	}
}

