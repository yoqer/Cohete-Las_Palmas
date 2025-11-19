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
	
	private double x;  // X position in model coordinates
	private boolean isHovered = false;  // Whether the mouse is hovering over this line
	private String handleLabel = "";
	
	private static Color lineColor;
	private static Color handleColor;
	
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
	 * Set the x position of the caliper line in model coordinates.
	 * 
	 * @param x the x position in meters
	 */
	public void setX(double x) {
		this.x = x;
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
		// The figure's transform is already applied, so we draw in model coordinates
		// Use different line thickness based on hover state
		float lineWidth = isHovered ? LINE_WIDTH_HOVER : LINE_WIDTH_NORMAL;
		g2.setStroke(new BasicStroke((float)(lineWidth / scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.setColor(lineColor);
		
		// Draw line extending from -1000 to 1000 meters in Y (will be clipped appropriately)
		Line2D.Double line = new Line2D.Double(x, -1000, x, 1000);
		g2.draw(line);
		
		// Draw handle in screen coordinates so it stays the same size and always visible
		// Get the current transform to convert model coordinates to screen coordinates
		AffineTransform transform = g2.getTransform();
		
		// Convert the line X position to screen coordinates
		Point2D.Double modelPoint = new Point2D.Double(x, 0);
		Point2D.Double screenPoint = new Point2D.Double();
		transform.transform(modelPoint, screenPoint);
		
		// Use a graphics copy for screen-space drawing to avoid altering the main context
		Graphics2D g2Screen = (Graphics2D) g2.create();
		try {
			g2Screen.setTransform(new AffineTransform());
			g2Screen.setClip(null);
		
		// Position handle at the very top of the visible area
		// Account for viewport position if visible rectangle is provided
			double handleX_screen = screenPoint.x;
			double handleY_screen = 0.0;  // Start at the very top
			if (visible != null) {
				// Adjust for the visible rectangle's position (viewport offset)
				handleY_screen = visible.y;
			}
		
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
				double textX = handleX_screen - textWidth / 2.0;
				// Position text vertically centered within rectangle
				double textY = rectTop + (HANDLE_RECT_HEIGHT / 2.0) + textHeight / 4.0;
				g2Screen.setColor(Color.BLACK);
				g2Screen.drawString(handleLabel, (float) textX, (float) textY);
			}
		} finally {
			g2Screen.dispose();
		}
	}
}

