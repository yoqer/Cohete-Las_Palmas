package info.openrocket.swing.gui.scalefigure.caliper.snap;

import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.util.Coordinate;

import info.openrocket.swing.gui.scalefigure.RocketPanel;
import info.openrocket.swing.gui.scalefigure.caliper.CaliperManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a snap target for caliper lines - either a point or a line edge.
 * Snap targets are geometry features (edges, corners, points) that caliper lines can snap to.
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class CaliperSnapTarget {
	
	/**
	 * Type of snap target: POINT for a specific coordinate, LINE for an edge.
	 */
	public enum SnapType {
		POINT,      // A specific point (e.g., corner, circle point)
		LINE        // A line/edge (e.g., vertical or horizontal edge)
	}
	
	private final SnapType type;
	private final CaliperManager.CaliperMode orientation;  // VERTICAL or HORIZONTAL
	private final Coordinate position;  // Model coordinates
	private final RocketComponent component;  // Reference to the component this target belongs to
	private final String description;  // Optional description for debugging
	
	// For LINE type: start and end points of the line
	private final Coordinate lineStart;
	private final Coordinate lineEnd;
	
	/**
	 * Create a point snap target.
	 *
	 * @param position the position in model coordinates
	 * @param orientation the orientation (VERTICAL for vertical caliper, HORIZONTAL for horizontal caliper)
	 * @param component the component this target belongs to
	 * @param description optional description
	 */
	public CaliperSnapTarget(Coordinate position, CaliperManager.CaliperMode orientation,
							RocketComponent component, String description) {
		this.type = SnapType.POINT;
		this.position = position;
		this.orientation = orientation;
		this.component = component;
		this.description = description;
		this.lineStart = null;
		this.lineEnd = null;
	}
	
	/**
	 * Create a line snap target.
	 *
	 * @param lineStart start point of the line in model coordinates
	 * @param lineEnd end point of the line in model coordinates
	 * @param orientation the orientation (VERTICAL for vertical caliper, HORIZONTAL for horizontal caliper)
	 * @param component the component this target belongs to
	 * @param description optional description
	 */
	public CaliperSnapTarget(Coordinate lineStart, Coordinate lineEnd,
							CaliperManager.CaliperMode orientation,
							RocketComponent component, String description) {
		this.type = SnapType.LINE;
		this.lineStart = lineStart;
		this.lineEnd = lineEnd;
		this.orientation = orientation;
		this.component = component;
		this.description = description;
		// For line targets, position is the midpoint (used for general position)
		// The actual snap value will be determined by view type in getSnapValue()
		this.position = new Coordinate((lineStart.getX() + lineEnd.getX()) / 2.0,
				(lineStart.getY() + lineEnd.getY()) / 2.0,
				(lineStart.getZ() + lineEnd.getZ()) / 2.0);
	}
	
	/**
	 * Get the type of this snap target.
	 */
	public SnapType getType() {
		return type;
	}
	
	/**
	 * Get the orientation of this snap target.
	 */
	public CaliperManager.CaliperMode getOrientation() {
		return orientation;
	}
	
	/**
	 * Get the position in model coordinates.
	 * For POINT targets, this is the point itself.
	 * For LINE targets, this is the midpoint or relevant coordinate.
	 */
	public Coordinate getPosition() {
		return position;
	}
	
	/**
	 * Get the component this snap target belongs to.
	 */
	public RocketComponent getComponent() {
		return component;
	}
	
	/**
	 * Get the description of this snap target.
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Get the start point of the line (for LINE type targets).
	 * Returns null for POINT targets.
	 */
	public Coordinate getLineStart() {
		return lineStart;
	}
	
	/**
	 * Get the end point of the line (for LINE type targets).
	 * Returns null for POINT targets.
	 */
	public Coordinate getLineEnd() {
		return lineEnd;
	}
	
	/**
	 * Check if this snap target is compatible with the given caliper mode.
	 * A vertical caliper can only snap to vertical targets, and vice versa.
	 */
	public boolean isCompatibleWith(CaliperManager.CaliperMode caliperMode) {
		return this.orientation == caliperMode;
	}
	
	/**
	 * Calculate the distance from a point (in model coordinates) to this snap target.
	 * For POINT targets, returns the 2D distance in the view plane.
	 * For LINE targets, returns the perpendicular distance to the line.
	 *
	 * @param point the point in model coordinates
	 * @param viewType the current view type (affects which coordinates are used)
	 * @return the distance in model coordinates
	 */
	public double getDistanceToPoint(Coordinate point, RocketPanel.VIEW_TYPE viewType) {
		double distance;
		if (type == SnapType.POINT) {
			// Calculate 2D distance in the view plane
			if (viewType == RocketPanel.VIEW_TYPE.BackView) {
				// Back view: screen X = Z, screen Y = Y
				if (orientation == CaliperManager.CaliperMode.VERTICAL) {
					// Vertical caliper measures Z coordinate
					distance = Math.abs(position.getZ() - point.getZ());
				} else {
					// Horizontal caliper measures Y coordinate
					distance = Math.abs(position.getY() - point.getY());
				}
			} else {
				// Side/Top view: screen X = X, screen Y = Y
				if (orientation == CaliperManager.CaliperMode.VERTICAL) {
					// Vertical caliper measures X coordinate
					distance = Math.abs(position.getX() - point.getX());
				} else {
					// Horizontal caliper measures Y coordinate
					distance = Math.abs(position.getY() - point.getY());
				}
			}
		} else {
			// For LINE targets, calculate perpendicular distance to the line segment
			distance = distanceToLineSegment(point, lineStart, lineEnd, viewType);
		}
		return distance;
	}
	
	/**
	 * Get the snap value for the given caliper mode and view type.
	 * For VERTICAL mode:
	 *   - Side/Top view: returns the X coordinate
	 *   - Back view: returns the Z coordinate
	 * For HORIZONTAL mode: returns the Y coordinate (same for all views)
	 *
	 * @param caliperMode the caliper mode
	 * @param viewType the current view type
	 * @return the snap value in model coordinates
	 */
	public double getSnapValue(CaliperManager.CaliperMode caliperMode, RocketPanel.VIEW_TYPE viewType) {
		if (caliperMode == CaliperManager.CaliperMode.VERTICAL) {
			// For vertical caliper, use X in side/top view, Z in back view
			if (viewType == RocketPanel.VIEW_TYPE.BackView) {
				return position.getZ();
			} else {
				return position.getX();
			}
		} else {
			// For horizontal caliper, always use Y coordinate
			return position.getY();
		}
	}
	
	/**
	 * Calculate the perpendicular distance from a point to a line segment.
	 */
	private double distanceToLineSegment(Coordinate point, Coordinate lineStart, Coordinate lineEnd, RocketPanel.VIEW_TYPE viewType) {
		if (orientation == CaliperManager.CaliperMode.VERTICAL) {
			// Vertical line: distance depends on view type
			if (viewType == RocketPanel.VIEW_TYPE.BackView) {
				// Back view: vertical caliper measures Z coordinate
				double pz = point.getZ();
				double lineZ = lineStart.getZ();
				return Math.abs(pz - lineZ);
			} else {
				// Side/Top view: vertical caliper measures X coordinate
				double px = point.getX();
				double lineX = lineStart.getX();
				return Math.abs(px - lineX);
			}
		} else {
			// Horizontal line: always measures Y coordinate (same for all views)
			double py = point.getY();
			double lineY = lineStart.getY();
			return Math.abs(py - lineY);
		}
	}
}

