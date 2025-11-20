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
 * A vertical line element for the caliper measurement tool.
 * Draws a vertical line with a draggable handle at the top.
 *
 * @author OpenRocket Team
 */
public class CaliperLine implements FigureElement {

	private static final float HANDLE_RECT_WIDTH = 22.0f;  // Narrow width for elongated look
	private static final float HANDLE_RECT_HEIGHT = 36.0f;  // Tall rectangle
	private static final float HANDLE_TRIANGLE_HEIGHT = 26.0f;  // Inverted triangle below
	private static final float LINE_WIDTH_NORMAL = 2.0f;  // Normal line thickness
	private static final float LINE_WIDTH_HOVER = 4.0f;  // Thicker line when hovering
	private static final Font HANDLE_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 30);
	private static final Font INDICATOR_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 24);  // Font for indicator label
	
	// Out-of-view indicator
	private static final float ARROW_SIZE = 20.0f;  // Size of the arrow indicator
	private static final float ARROW_STROKE_WIDTH = 3.0f;  // Thickness of arrow lines
	private static final float LABEL_OFFSET = 8.0f;  // Distance from arrow to label

	private double x;  // X position in model coordinates
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
	 * Create a new CaliperLine at the specified x coordinate.
	 *
	 * @param x the x position in model coordinates (meters)
	 */
	public CaliperLine(double x) {
		this.x = x;
	}

	private static void initColors() {
		updateColors();
		UITheme.Theme.addUIThemeChangeListener(CaliperLine::updateColors);
	}

	public static void updateColors() {
		lineColor = GUIUtil.getUITheme().getCaliperColor();
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
	 * Set the x position of the caliper line in model coordinates.
	 *
	 * @param x the x position in meters
	 */
	public void setX(double x) {
		this.x = x;
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
	 * Get the x position of the caliper line in model coordinates.
	 *
	 * @return the x position in meters
	 */
	public double getX() {
		return x;
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
			Point2D.Double modelPoint = new Point2D.Double(x, 0);
			Point2D.Double screenPoint = new Point2D.Double();
			transform.transform(modelPoint, screenPoint);

			if (Double.isNaN(screenPoint.x) || Double.isInfinite(screenPoint.x)) {
				return;
			}

			double handleX_screen = screenPoint.x;
			double handleY_screen = 0.0;  // Start at the very top

			// Reset transform to draw in screen coordinates
			g2Screen.setTransform(new AffineTransform());
			
			// Get the actual visible bounds in screen coordinates (after transform reset)
			// Use clip bounds if available, otherwise fall back to visible parameter
			Rectangle screenVisible = visible;
			if (g2Screen.getClipBounds() != null) {
				screenVisible = g2Screen.getClipBounds();
			}
			
			// Draw vertical line covering the full viewport height
			// We draw in screen coordinates, from Y=0 to a very large number (e.g. 20000)
			// The clip will ensure it doesn't draw outside the viewport
			Line2D.Double screenLine = new Line2D.Double(handleX_screen, 0, handleX_screen, 20000);
			g2Screen.setStroke(new BasicStroke(isHovered ? LINE_WIDTH_HOVER : LINE_WIDTH_NORMAL, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			
			// Apply 50% transparency in snap mode
			Color drawColor = lineColor;
			if (isSnapMode) {
				drawColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 128);  // 50% alpha
			}
			g2Screen.setColor(drawColor);
			g2Screen.draw(screenLine);

			// Draw marker handle as one continuous path: elongated rectangle at top, inverted triangle below
			// Create a single manifold path for the entire marker shape
			Path2D.Double marker = new Path2D.Double();

			double rectLeft = handleX_screen - HANDLE_RECT_WIDTH / 2;
			double rectRight = handleX_screen + HANDLE_RECT_WIDTH / 2;
			double rectTop = handleY_screen;
			double rectBottom = handleY_screen + HANDLE_RECT_HEIGHT;
			double triangleBaseY = rectBottom + HANDLE_TRIANGLE_HEIGHT;

			// Marker shape path
			marker.moveTo(rectLeft, rectTop);		// Top-left of rectangle
			marker.lineTo(rectRight, rectTop);		// Top-right of rectangle
			marker.lineTo(rectRight, rectBottom);	// Bottom-right of rectangle
			marker.lineTo(handleX_screen, triangleBaseY);	// Point of triangle
			marker.lineTo(rectLeft, rectBottom);	// Bottom-left of rectangle
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
				double textX = handleX_screen - textWidth / 2.0;
				// Position text vertically centered within rectangle
				double textY = rectTop + (HANDLE_RECT_HEIGHT / 2.0) + textHeight / 4.0;
				g2Screen.setColor(Color.BLACK);
				g2Screen.drawString(handleLabel, (float) textX, (float) textY);
			}
			
			// Draw out-of-view indicator if the caliper line is outside the visible area
			if (screenVisible != null) {
				// Both handleX_screen and screenVisible are now in screen coordinates (after transform reset)
				// Add a small margin to avoid flickering when the line is right at the edge
				double margin = 5.0;
				boolean isOutOfView = (handleX_screen < screenVisible.x - margin) || 
				                       (handleX_screen > screenVisible.x + screenVisible.width + margin);
				if (isOutOfView) {
					drawOutOfViewIndicator(g2Screen, handleX_screen, screenVisible);
				}
			}
		} finally {
			g2Screen.dispose();
		}
	}
	
	/**
	 * Draw an arrow indicator at the edge of the viewport pointing toward the caliper line.
	 *
	 * @param g2Screen the graphics context in screen coordinates
	 * @param caliperX the X position of the caliper line in screen coordinates
	 * @param visible the visible viewport rectangle
	 */
	private void drawOutOfViewIndicator(Graphics2D g2Screen, double caliperX, Rectangle visible) {
		Color indicatorColor = lineColor;
		if (isSnapMode) {
			indicatorColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 128);
		}
		g2Screen.setColor(indicatorColor);
		g2Screen.setStroke(new BasicStroke(ARROW_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		
		// Determine which edge to draw the arrow on
		boolean isLeft = caliperX < visible.x;
		double arrowX = isLeft ? visible.x : visible.x + visible.width;
		
		// Position arrow vertically centered in the visible area
		double arrowY = visible.y + visible.height / 2.0;
		
		// Draw arrow pointing toward the caliper line
		Path2D.Double arrow = new Path2D.Double();
		if (isLeft) {
			// Arrow pointing left (caliper is to the left)
			arrow.moveTo(arrowX, arrowY);
			arrow.lineTo(arrowX + ARROW_SIZE, arrowY - ARROW_SIZE / 2);
			arrow.lineTo(arrowX + ARROW_SIZE, arrowY + ARROW_SIZE / 2);
			arrow.closePath();
		} else {
			// Arrow pointing right (caliper is to the right)
			arrow.moveTo(arrowX, arrowY);
			arrow.lineTo(arrowX - ARROW_SIZE, arrowY - ARROW_SIZE / 2);
			arrow.lineTo(arrowX - ARROW_SIZE, arrowY + ARROW_SIZE / 2);
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
			if (isLeft) {
				// Label to the right of the arrow (arrow points left)
				labelX = arrowX + ARROW_SIZE + LABEL_OFFSET;
				labelY = arrowY + textHeight / 4.0;  // Vertically centered
			} else {
				// Label to the left of the arrow (arrow points right)
				labelX = arrowX - ARROW_SIZE - LABEL_OFFSET - textWidth;
				labelY = arrowY + textHeight / 4.0;  // Vertically centered
			}
			
			g2Screen.setColor(textColor);
			g2Screen.drawString(handleLabel, (float) labelX, (float) labelY);
		}
	}
}

