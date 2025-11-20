package info.openrocket.swing.gui.scalefigure.caliper.snap;

import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.util.Coordinate;
import info.openrocket.core.util.CoordinateIF;
import info.openrocket.core.util.Transformation;
import info.openrocket.swing.gui.scalefigure.RocketPanel;
import info.openrocket.swing.gui.scalefigure.caliper.CaliperManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides snap targets for BodyTube components.
 * 
 * For side/top view:
 * - Vertical caliper mode: left edge (x = component start), right edge (x = component end)
 * - Horizontal caliper mode: top edge (y = +radius), bottom edge (y = -radius)
 * 
 * For back view:
 * - Vertical caliper mode: left/right points on outer and inner circles (z = ±radius)
 * - Horizontal caliper mode: top/bottom points on outer and inner circles (y = ±radius)
 *
 * @author OpenRocket Team
 */
public class BodyTubeSnapProvider implements ComponentSnapProvider {
	
	@Override
	public Class<? extends RocketComponent> getComponentClass() {
		return BodyTube.class;
	}
	
	@Override
	public List<CaliperSnapTarget> getSnapTargets(RocketComponent component,
												  RocketPanel.VIEW_TYPE viewType,
												  CaliperManager.CaliperMode caliperMode,
												  Transformation transformation) {
		List<CaliperSnapTarget> targets = new ArrayList<>();
		
		if (!(component instanceof BodyTube tube)) {
			return targets;
		}

		double length = tube.getLength();
		double outerRadius = tube.getOuterRadius();
		double innerRadius = tube.getInnerRadius();
		
		if (viewType == RocketPanel.VIEW_TYPE.SideView || viewType == RocketPanel.VIEW_TYPE.TopView) {
			// Side/Top view: body tube appears as a rectangle
			getSideViewSnapTargets(tube, length, outerRadius, transformation, caliperMode, targets);
		} else if (viewType == RocketPanel.VIEW_TYPE.BackView) {
			// Back view: body tube appears as circles
			getBackViewSnapTargets(tube, outerRadius, innerRadius, transformation, caliperMode, targets);
		}
		
		return targets;
	}
	
	/**
	 * Get snap targets for side/top view.
	 */
	private void getSideViewSnapTargets(BodyTube tube, double length, double radius,
									   Transformation transformation,
									   CaliperManager.CaliperMode caliperMode,
									   List<CaliperSnapTarget> targets) {
		// Component local coordinates: tube extends from (0, -radius) to (length, +radius)
		// Transform to absolute coordinates
		CoordinateIF startBottomIF = transformation.transform(new Coordinate(0, -radius, 0));
		CoordinateIF startTopIF = transformation.transform(new Coordinate(0, radius, 0));
		CoordinateIF endBottomIF = transformation.transform(new Coordinate(length, -radius, 0));
		CoordinateIF endTopIF = transformation.transform(new Coordinate(length, radius, 0));
		
		// Convert to Coordinate
		Coordinate startBottom = new Coordinate(startBottomIF.getX(), startBottomIF.getY(), startBottomIF.getZ());
		Coordinate startTop = new Coordinate(startTopIF.getX(), startTopIF.getY(), startTopIF.getZ());
		Coordinate endBottom = new Coordinate(endBottomIF.getX(), endBottomIF.getY(), endBottomIF.getZ());
		Coordinate endTop = new Coordinate(endTopIF.getX(), endTopIF.getY(), endTopIF.getZ());
		
		if (caliperMode == CaliperManager.CaliperMode.VERTICAL) {
			// Vertical caliper mode: snap to left and right edges (vertical lines)
			// Left edge: from startBottom to startTop
			targets.add(new CaliperSnapTarget(startBottom, startTop,
					CaliperManager.CaliperMode.VERTICAL, tube, "Left edge"));
			// Right edge: from endBottom to endTop
			targets.add(new CaliperSnapTarget(endBottom, endTop,
					CaliperManager.CaliperMode.VERTICAL, tube, "Right edge"));
		} else {
			// Horizontal caliper mode: snap to top and bottom edges (horizontal lines)
			// Top edge: from startTop to endTop
			targets.add(new CaliperSnapTarget(startTop, endTop,
					CaliperManager.CaliperMode.HORIZONTAL, tube, "Top edge"));
			// Bottom edge: from startBottom to endBottom
			targets.add(new CaliperSnapTarget(startBottom, endBottom,
					CaliperManager.CaliperMode.HORIZONTAL, tube, "Bottom edge"));
		}
	}
	
	/**
	 * Get snap targets for back view.
	 */
	private void getBackViewSnapTargets(BodyTube tube, double outerRadius, double innerRadius,
									  Transformation transformation,
									  CaliperManager.CaliperMode caliperMode,
									  List<CaliperSnapTarget> targets) {
		// In back view, the tube appears as circles centered at the component origin
		// The transformation gives us the absolute position
		CoordinateIF centerIF = transformation.transform(Coordinate.ZERO);
		Coordinate center = new Coordinate(centerIF.getX(), centerIF.getY(), centerIF.getZ());
		
		if (caliperMode == CaliperManager.CaliperMode.VERTICAL) {
			// Vertical caliper mode: snap to left and right points on circles
			// Outer circle: left point (z = -outerRadius), right point (z = +outerRadius)
			Coordinate outerLeft = new Coordinate(center.getX(), center.getY(), center.getZ() - outerRadius);
			Coordinate outerRight = new Coordinate(center.getX(), center.getY(), center.getZ() + outerRadius);
			targets.add(new CaliperSnapTarget(outerLeft, CaliperManager.CaliperMode.VERTICAL, tube, "Outer circle left"));
			targets.add(new CaliperSnapTarget(outerRight, CaliperManager.CaliperMode.VERTICAL, tube, "Outer circle right"));
			
			// Inner circle - currently not used because the inner circle is not drawn in back view
			/*if (innerRadius > 0 && innerRadius < outerRadius) {
				Coordinate innerLeft = new Coordinate(center.getX(), center.getY(), center.getZ() - innerRadius);
				Coordinate innerRight = new Coordinate(center.getX(), center.getY(), center.getZ() + innerRadius);
				targets.add(new CaliperSnapTarget(innerLeft, CaliperManager.CaliperMode.VERTICAL, tube, "Inner circle left"));
				targets.add(new CaliperSnapTarget(innerRight, CaliperManager.CaliperMode.VERTICAL, tube, "Inner circle right"));
			}*/
		} else {
			// Horizontal caliper mode: snap to top and bottom points on circles
			// Outer circle: top point (y = +outerRadius), bottom point (y = -outerRadius)
			Coordinate outerTop = new Coordinate(center.getX(), center.getY() + outerRadius, center.getZ());
			Coordinate outerBottom = new Coordinate(center.getX(), center.getY() - outerRadius, center.getZ());
			targets.add(new CaliperSnapTarget(outerTop, CaliperManager.CaliperMode.HORIZONTAL, tube, "Outer circle top"));
			targets.add(new CaliperSnapTarget(outerBottom, CaliperManager.CaliperMode.HORIZONTAL, tube, "Outer circle bottom"));
			
			// Inner circle - currently not used because the inner circle is not drawn in back view
			/*if (innerRadius > 0 && innerRadius < outerRadius) {
				Coordinate innerTop = new Coordinate(center.getX(), center.getY() + innerRadius, center.getZ());
				Coordinate innerBottom = new Coordinate(center.getX(), center.getY() - innerRadius, center.getZ());
				targets.add(new CaliperSnapTarget(innerTop, CaliperManager.CaliperMode.HORIZONTAL, tube, "Inner circle top"));
				targets.add(new CaliperSnapTarget(innerBottom, CaliperManager.CaliperMode.HORIZONTAL, tube, "Inner circle bottom"));
			}*/
		}
	}
}

