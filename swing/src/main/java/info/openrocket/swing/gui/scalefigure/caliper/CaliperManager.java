package info.openrocket.swing.gui.scalefigure.caliper;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.rocketcomponent.ComponentChangeEvent;
import info.openrocket.core.rocketcomponent.ComponentChangeListener;
import info.openrocket.core.rocketcomponent.FlightConfiguration;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.unit.Unit;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.core.util.BoundingBox;
import info.openrocket.core.util.MathUtil;
import info.openrocket.core.util.StateChangeListener;
import info.openrocket.core.util.Transformation;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.components.EditableSpinner;
import info.openrocket.swing.gui.components.UnitSelector;
import info.openrocket.swing.gui.figureelements.CaliperLine;
import info.openrocket.swing.gui.figureelements.HorizontalCaliperLine;
import info.openrocket.swing.gui.scalefigure.RocketFigure;
import info.openrocket.swing.gui.scalefigure.RocketPanel;
import info.openrocket.swing.gui.scalefigure.caliper.snap.CaliperSnapRegistry;
import info.openrocket.swing.gui.scalefigure.caliper.snap.CaliperSnapTarget;
import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.util.Icons;
import info.openrocket.core.startup.Application;
import info.openrocket.core.l10n.Translator;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * Manages the caliper measurement tool for the rocket panel.
 * Handles all caliper-related state, UI components, and interactions.
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class CaliperManager {
	private static final Logger log = LoggerFactory.getLogger(CaliperManager.class);
	private static final Translator trans = Application.getTranslator();

	private static final double SNAP_PIXEL_TOLERANCE = 8.0; // Tolerance in pixels for snap target detection

	/**
	 * Caliper mode: VERTICAL for measuring horizontal distances, HORIZONTAL for measuring vertical distances.
	 */
	public enum CaliperMode {
		VERTICAL,
		HORIZONTAL,
		BOTH			// Vertical and horizontal
	}

	/**
	 * Internal state class for storing caliper positions per view type.
	 */
	private static final class CaliperState {
		double caliper1X = Double.NaN;
		double caliper2X = Double.NaN;
		double caliper1Y = Double.NaN;
		double caliper2Y = Double.NaN;
	}

	// References to external components
	private final RocketFigure figure;
	private final OpenRocketDocument document;
	private final ViewTypeProvider viewTypeProvider;
	private final Runnable figureUpdateCallback;
	private final Runnable focusRequestCallback;

	// Caliper state
	private boolean enabled = false;
	private CaliperMode mode = CaliperMode.VERTICAL;
	private boolean wasEnabledBefore3d = false;

	// Caliper line elements
	private final CaliperLine caliper1Line;
	private final CaliperLine caliper2Line;
	private final HorizontalCaliperLine caliper1HorizontalLine;
	private final HorizontalCaliperLine caliper2HorizontalLine;

	// Caliper positions in model coordinates
	private double caliper1X = Double.NaN;
	private double caliper2X = Double.NaN;
	private double caliper1Y = Double.NaN;
	private double caliper2Y = Double.NaN;

	// Dragging state
	private CaliperLine draggingCaliperLine = null;
	private HorizontalCaliperLine draggingHorizontalCaliperLine = null;

	// UI components
	private JButton caliperButton;
	private JButton modeButton;
	private JPanel displayPanel;
	private JSpinner distanceSpinner;
	private UnitSelector unitSelector;
	private JSpinner caliper1PositionSpinner;
	private JSpinner caliper2PositionSpinner;
	private JToggleButton caliper1SnapButton;
	private JToggleButton caliper2SnapButton;

	// Models
	private final DoubleModel distanceModel;
	private final DoubleModel horizontalDistanceModel;
	private final DoubleModel caliper1PositionModel;
	private final DoubleModel caliper2PositionModel;
	private boolean updatingCaliperPositionModels = false;

	// State per view type
	private final CaliperState sideViewCaliperState = new CaliperState();
	private final CaliperState backViewCaliperState = new CaliperState();

	// Snap mode state
	private boolean snapModeActive = false;
	private Integer activeSnapCaliper = null;  // 1 or 2, or null if not in snap mode
	private CaliperSnapTarget hoveredSnapTarget = null;
	private final List<CaliperSnapTarget> currentSnapTargets = new ArrayList<>();
	private boolean alwaysShowSnapTargets = false;  // Debug mode: always show all snap targets

	/**
	 * Interface for getting the current view type from the panel.
	 */
	public interface ViewTypeProvider {
		RocketPanel.VIEW_TYPE getCurrentViewType();
	}

	/**
	 * Create a new CaliperManager.
	 *
	 * @param figure the rocket figure to add caliper elements to
	 * @param document the document containing the rocket
	 * @param viewTypeProvider provider for the current view type
	 * @param figureUpdateCallback callback to update the figure when caliper state changes
	 * @param focusRequestCallback callback to request focus when entering snap mode
	 */
	public CaliperManager(RocketFigure figure, OpenRocketDocument document,
						  ViewTypeProvider viewTypeProvider, Runnable figureUpdateCallback,
						  Runnable focusRequestCallback) {
		this.figure = figure;
		this.document = document;
		this.viewTypeProvider = viewTypeProvider;
		this.figureUpdateCallback = figureUpdateCallback;
		this.focusRequestCallback = focusRequestCallback;

		// Listen to rocket changes to exit snap mode when rocket structure changes
		Rocket rocket = document.getRocket();
		rocket.addChangeListener(new StateChangeListener() {
			@Override
			public void stateChanged(EventObject e) {
				// Exit snap mode when rocket changes (e.g., component added/removed, undo/redo)
				if (snapModeActive) {
					exitSnapMode();
				}
			}
		});
		
		rocket.addComponentChangeListener(new ComponentChangeListener() {
			@Override
			public void componentChanged(ComponentChangeEvent e) {
				// Exit snap mode when components change (e.g., component modified)
				if (snapModeActive) {
					exitSnapMode();
				}
			}
		});

		// Initialize models
		distanceModel = new DoubleModel(0.0, UnitGroup.UNITS_LENGTH);
		horizontalDistanceModel = new DoubleModel(0.0, UnitGroup.UNITS_LENGTH);

		// Initialize caliper line elements
		caliper1Line = new CaliperLine(0.0);
		caliper1Line.setHandleLabel("1");
		caliper2Line = new CaliperLine(0.0);
		caliper2Line.setHandleLabel("2");
		caliper1HorizontalLine = new HorizontalCaliperLine(0.0);
		caliper1HorizontalLine.setHandleLabel("1");
		caliper2HorizontalLine = new HorizontalCaliperLine(0.0);
		caliper2HorizontalLine.setHandleLabel("2");

		// Initialize position models
		caliper1PositionModel = new DoubleModel(0.0, UnitGroup.UNITS_LENGTH);
		caliper2PositionModel = new DoubleModel(0.0, UnitGroup.UNITS_LENGTH);
		caliper1PositionModel.setCurrentUnit(distanceModel.getCurrentUnit());
		caliper2PositionModel.setCurrentUnit(distanceModel.getCurrentUnit());

		// Set up position model listeners
		caliper1PositionModel.addChangeListener(new StateChangeListener() {
			@Override
			public void stateChanged(EventObject e) {
				if (updatingCaliperPositionModels) {
					return;
				}
				if (mode == CaliperMode.VERTICAL) {
					setCaliperLinePosition(true, caliper1PositionModel.getValue());
				} else {
					setHorizontalCaliperLinePosition(true, caliper1PositionModel.getValue());
				}
			}
		});

		caliper2PositionModel.addChangeListener(new StateChangeListener() {
			@Override
			public void stateChanged(EventObject e) {
				if (updatingCaliperPositionModels) {
					return;
				}
				if (mode == CaliperMode.VERTICAL) {
					setCaliperLinePosition(false, caliper2PositionModel.getValue());
				} else {
					setHorizontalCaliperLinePosition(false, caliper2PositionModel.getValue());
				}
			}
		});

		// Create UI components
		createUIComponents();
	}

	/**
	 * Create the UI components for the caliper tool.
	 */
	private void createUIComponents() {
		// Caliper tool button (regular button to open dialog)
		caliperButton = new JButton();
		caliperButton.setIcon(Icons.RULER);
		caliperButton.setText(trans.get("CaliperManager.btn.Caliper"));
		caliperButton.setToolTipText(trans.get("CaliperManager.btn.Caliper"));

		// Caliper mode toggle button (Vertical/Horizontal)
		modeButton = new JButton("V");
		modeButton.setToolTipText("Switch to horizontal calipers");
		modeButton.setEnabled(false);
		modeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleMode();
			}
		});

		// Caliper display panel
		Color caliperColor = GUIUtil.getUITheme().getCaliperColor();
		displayPanel = new JPanel(new MigLayout("ins 0"));
		displayPanel.setOpaque(false);
		Border caliperBorder = new LineBorder(caliperColor, 1);
		displayPanel.setBorder(new CompoundBorder(caliperBorder, new EmptyBorder(5, 5, 5, 5)));
		displayPanel.setVisible(false);

		// Caliper distance spinner (non-editable)
		distanceSpinner = new JSpinner(distanceModel.getSpinnerModel());
		distanceSpinner.setEnabled(false);
		JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) distanceSpinner.getEditor();
		JTextField textField = editor.getTextField();
		textField.setEditable(false);
		textField.setEnabled(true);
		textField.setForeground(caliperColor);
		displayPanel.add(distanceSpinner, "split 2, aligny center");

		// Caliper unit selector
		unitSelector = new UnitSelector(distanceModel);
		displayPanel.add(unitSelector, "gapright unrel");

		// Keep position models in sync with unit selector
		unitSelector.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					Unit selected = unitSelector.getSelectedUnit();
					if (caliper1PositionModel != null && caliper2PositionModel != null) {
						caliper1PositionModel.setCurrentUnit(selected);
						caliper2PositionModel.setCurrentUnit(selected);
						updateCaliperPositionModelsFromState();
					}
				}
			}
		});

		// Position spinners for each caliper handle
		SpinnerModel caliper1SpinnerModel = caliper1PositionModel.getSpinnerModel();
		SpinnerModel caliper2SpinnerModel = caliper2PositionModel.getSpinnerModel();
		caliper1PositionSpinner = new EditableSpinner(caliper1SpinnerModel);
		caliper2PositionSpinner = new EditableSpinner(caliper2SpinnerModel);
		caliper1PositionSpinner.setEnabled(false);
		caliper2PositionSpinner.setEnabled(false);
		JSpinner.DefaultEditor caliper1Editor = (JSpinner.DefaultEditor) caliper1PositionSpinner.getEditor();
		caliper1Editor.getTextField().setColumns(2);
		JSpinner.DefaultEditor caliper2Editor = (JSpinner.DefaultEditor) caliper2PositionSpinner.getEditor();
		caliper2Editor.getTextField().setColumns(2);

		displayPanel.add(new JLabel("1:"));
		displayPanel.add(caliper1PositionSpinner);
		
		// Snap mode button for caliper 1
		caliper1SnapButton = new JToggleButton();
		caliper1SnapButton.setIcon(Icons.SNAP);
		caliper1SnapButton.setToolTipText(String.format(trans.get("CaliperManager.btn.CaliperSnap"), 1));
		caliper1SnapButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (snapModeActive && activeSnapCaliper != null && activeSnapCaliper == 1) {
					exitSnapMode();
				} else {
					enterSnapMode(1);
				}
			}
		});
		displayPanel.add(caliper1SnapButton, "gapleft rel");
		
		displayPanel.add(new JLabel("2:"), "gapleft rel");
		displayPanel.add(caliper2PositionSpinner);
		
		// Snap mode button for caliper 2
		caliper2SnapButton = new JToggleButton();
		caliper2SnapButton.setIcon(Icons.SNAP);
		caliper2SnapButton.setToolTipText(String.format(trans.get("CaliperManager.btn.CaliperSnap"), 2));
		caliper2SnapButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (snapModeActive && activeSnapCaliper != null && activeSnapCaliper == 2) {
					exitSnapMode();
				} else {
					enterSnapMode(2);
				}
			}
		});
		displayPanel.add(caliper2SnapButton, "gapleft rel");
		
		// Display panel should be visible when caliper is enabled
		displayPanel.setVisible(enabled);
	}

	/**
	 * Get the button for opening the caliper dialog.
	 *
	 * @return the caliper button
	 */
	public JButton getCaliperButton() {
		return caliperButton;
	}

	/**
	 * Get the distance spinner.
	 *
	 * @return the distance spinner
	 */
	public JSpinner getDistanceSpinner() {
		return distanceSpinner;
	}

	/**
	 * Get the unit selector.
	 *
	 * @return the unit selector
	 */
	public UnitSelector getUnitSelector() {
		return unitSelector;
	}

	/**
	 * Get the current distance model (vertical or horizontal based on mode).
	 *
	 * @return the current distance model
	 */
	public DoubleModel getCurrentDistanceModel() {
		return (mode == CaliperMode.HORIZONTAL) ? horizontalDistanceModel : distanceModel;
	}

	/**
	 * Get the caliper 1 position spinner.
	 *
	 * @return the caliper 1 position spinner
	 */
	public JSpinner getCaliper1PositionSpinner() {
		return caliper1PositionSpinner;
	}

	/**
	 * Get the caliper 2 position spinner.
	 *
	 * @return the caliper 2 position spinner
	 */
	public JSpinner getCaliper2PositionSpinner() {
		return caliper2PositionSpinner;
	}

	/**
	 * Get the caliper 1 snap button.
	 *
	 * @return the caliper 1 snap button
	 */
	public JToggleButton getCaliper1SnapButton() {
		return caliper1SnapButton;
	}

	/**
	 * Get the caliper 2 snap button.
	 *
	 * @return the caliper 2 snap button
	 */
	public JToggleButton getCaliper2SnapButton() {
		return caliper2SnapButton;
	}

	/**
	 * Get the current caliper mode.
	 *
	 * @return the current caliper mode
	 */
	public CaliperMode getMode() {
		return mode;
	}

	/**
	 * Set the caliper mode.
	 *
	 * @param newMode the new caliper mode
	 */
	public void setMode(CaliperMode newMode) {
		if (mode != newMode && (newMode == CaliperMode.VERTICAL || newMode == CaliperMode.HORIZONTAL)) {
			mode = newMode;
			// Update distance model based on mode
			if (horizontalDistanceModel != null && distanceSpinner != null && unitSelector != null) {
				if (mode == CaliperMode.HORIZONTAL) {
					distanceSpinner.setModel(horizontalDistanceModel.getSpinnerModel());
					unitSelector.setModel(horizontalDistanceModel);
				} else {
					distanceSpinner.setModel(distanceModel.getSpinnerModel());
					unitSelector.setModel(distanceModel);
				}
			}
			updateCaliperElements();
			figureUpdateCallback.run();
		}
	}

	/**
	 * Set whether the caliper tool is enabled.
	 *
	 * @param enabled true to enable, false to disable
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (modeButton != null) {
			modeButton.setEnabled(enabled);
		}
		if (caliper1PositionSpinner != null && caliper2PositionSpinner != null) {
			caliper1PositionSpinner.setEnabled(enabled);
			caliper2PositionSpinner.setEnabled(enabled);
		}
		if (enabled) {
			// Initialize positions if not already set
			if ((mode == CaliperMode.VERTICAL && (Double.isNaN(caliper1X) || Double.isNaN(caliper2X))) ||
					(mode == CaliperMode.HORIZONTAL && (Double.isNaN(caliper1Y) || Double.isNaN(caliper2Y)))) {
				initializeCaliperPositions();
			}
			// Show panel
			if (displayPanel != null) {
				displayPanel.setVisible(true);
				updateCaliperPositionModelsFromState();
			}
		} else {
			// Hide panel
			if (displayPanel != null) {
				displayPanel.setVisible(false);
			}
		}
		updateCaliperElements();
		figureUpdateCallback.run();
	}

	/**
	 * Check if the caliper tool is enabled.
	 *
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Toggle between vertical and horizontal caliper modes.
	 */
	public void toggleMode() {
		if (mode == CaliperMode.VERTICAL) {
			mode = CaliperMode.HORIZONTAL;
			if (modeButton != null) {
				modeButton.setText("H");
				modeButton.setToolTipText("Switch to vertical calipers");
			}
			// Switch distance model to horizontal
			if (horizontalDistanceModel != null && distanceSpinner != null && unitSelector != null) {
				distanceSpinner.setModel(horizontalDistanceModel.getSpinnerModel());
				unitSelector.setModel(horizontalDistanceModel);
			}
		} else {
			mode = CaliperMode.VERTICAL;
			if (modeButton != null) {
				modeButton.setText("V");
				modeButton.setToolTipText("Switch to horizontal calipers");
			}
			// Switch distance model to vertical
			if (distanceModel != null && distanceSpinner != null && unitSelector != null) {
				distanceSpinner.setModel(distanceModel.getSpinnerModel());
				unitSelector.setModel(distanceModel);
			}
		}
		// Update snap targets if in snap mode
		if (snapModeActive) {
			updateSnapTargets();
		}
		updateCaliperElements();
		updateCaliperPositionModelsFromState();
		updateCaliperDistance();
		updateCaliperHorizontalDistance();
		figureUpdateCallback.run();
	}

	/**
	 * Handle mouse press events for caliper dragging.
	 *
	 * @param screenX the screen X coordinate
	 * @param screenY the screen Y coordinate
	 * @param screenToModel function to convert screen coordinates to model coordinates
	 * @return true if a caliper line was clicked and dragging started
	 */
	public boolean handleMousePressed(int screenX, int screenY,
									  java.util.function.Function<Point, Point2D.Double> screenToModel) {
		if (!enabled) {
			return false;
		}

		Point screenPoint = new Point(screenX, screenY);
		Point2D.Double modelPoint = screenToModel.apply(screenPoint);
		if (modelPoint == null) {
			return false;
		}

		// Check if click is near a caliper line
		double handleTolerance = 0.01; // 1 cm tolerance in model coordinates

		if (mode == CaliperMode.VERTICAL) {
			// Check vertical caliper lines (X proximity)
			if (Math.abs(modelPoint.x - caliper1X) < handleTolerance) {
				draggingCaliperLine = caliper1Line;
				return true;
			}
			if (Math.abs(modelPoint.x - caliper2X) < handleTolerance) {
				draggingCaliperLine = caliper2Line;
				return true;
			}
		} else {
			// Check horizontal caliper lines (Y proximity)
			if (Math.abs(modelPoint.y - caliper1Y) < handleTolerance) {
				draggingHorizontalCaliperLine = caliper1HorizontalLine;
				return true;
			}
			if (Math.abs(modelPoint.y - caliper2Y) < handleTolerance) {
				draggingHorizontalCaliperLine = caliper2HorizontalLine;
				return true;
			}
		}

		return false;
	}

	/**
	 * Handle mouse drag events for caliper dragging.
	 *
	 * @param screenX the screen X coordinate
	 * @param screenY the screen Y coordinate
	 * @param screenToModel function to convert screen coordinates to model coordinates
	 * @return true if a caliper line is being dragged
	 */
	public boolean handleMouseDragged(int screenX, int screenY,
									  java.util.function.Function<Point, Point2D.Double> screenToModel) {
		if (!enabled) {
			return false;
		}

		Point screenPoint = new Point(screenX, screenY);
		Point2D.Double modelPoint = screenToModel.apply(screenPoint);
		if (modelPoint == null) {
			return false;
		}

		// Dragging a vertical caliper line
		if (draggingCaliperLine != null) {
			double newX = modelPoint.x;
			if (draggingCaliperLine == caliper1Line) {
				setCaliperLinePosition(true, newX);
			} else if (draggingCaliperLine == caliper2Line) {
				setCaliperLinePosition(false, newX);
			}
			return true;
		}

		// Dragging a horizontal caliper line
		if (draggingHorizontalCaliperLine != null) {
			double newY = modelPoint.y;
			if (draggingHorizontalCaliperLine == caliper1HorizontalLine) {
				setHorizontalCaliperLinePosition(true, newY);
			} else if (draggingHorizontalCaliperLine == caliper2HorizontalLine) {
				setHorizontalCaliperLinePosition(false, newY);
			}
			return true;
		}

		return false;
	}

	/**
	 * Handle mouse release events to stop caliper dragging.
	 */
	public void handleMouseReleased() {
		draggingCaliperLine = null;
		draggingHorizontalCaliperLine = null;
	}

	/**
	 * Handle mouse move events for caliper hover effects.
	 *
	 * @param screenX the screen X coordinate
	 * @param screenY the screen Y coordinate
	 * @param screenToModel function to convert screen coordinates to model coordinates
	 */
	public void handleMouseMoved(int screenX, int screenY,
								 java.util.function.Function<Point, Point2D.Double> screenToModel) {
		if (!enabled) {
			return;
		}

		Point screenPoint = new Point(screenX, screenY);
		Point2D.Double modelPoint = screenToModel.apply(screenPoint);
		if (modelPoint == null) {
			return;
		}

		// Check if mouse is near a caliper line
		double hoverTolerance = 0.01; // 1 cm tolerance - same as click detection

		boolean repaintNeeded = false;

		if (mode == CaliperMode.VERTICAL && caliper1Line != null && caliper2Line != null) {
			// Check vertical caliper lines (X proximity)
			boolean nearCal1X = Math.abs(modelPoint.x - caliper1X) < hoverTolerance;
			boolean nearCal2X = Math.abs(modelPoint.x - caliper2X) < hoverTolerance;

			// Update hover state for vertical calipers
			boolean cal1XWasHovered = caliper1Line.isHovered();
			boolean cal2XWasHovered = caliper2Line.isHovered();

			caliper1Line.setHovered(nearCal1X);
			caliper2Line.setHovered(nearCal2X);

			repaintNeeded = (cal1XWasHovered != nearCal1X || cal2XWasHovered != nearCal2X);
		} else if (mode == CaliperMode.HORIZONTAL && caliper1HorizontalLine != null && caliper2HorizontalLine != null) {
			// Check horizontal caliper lines (Y proximity)
			boolean nearCal1Y = Math.abs(modelPoint.y - caliper1Y) < hoverTolerance;
			boolean nearCal2Y = Math.abs(modelPoint.y - caliper2Y) < hoverTolerance;

			// Update hover state for horizontal calipers
			boolean cal1YWasHovered = caliper1HorizontalLine.isHovered();
			boolean cal2YWasHovered = caliper2HorizontalLine.isHovered();

			caliper1HorizontalLine.setHovered(nearCal1Y);
			caliper2HorizontalLine.setHovered(nearCal2Y);

			repaintNeeded = (cal1YWasHovered != nearCal1Y || cal2YWasHovered != nearCal2Y);
		}

		// Repaint if hover state changed
		if (repaintNeeded) {
			figureUpdateCallback.run();
		}
	}

	/**
	 * Handle mouse exit events to clear hover state.
	 */
	public void handleMouseExited() {
		if (!enabled) {
			return;
		}

		boolean wasHovered = false;
		if (mode == CaliperMode.VERTICAL && caliper1Line != null && caliper2Line != null) {
			wasHovered = caliper1Line.isHovered() || caliper2Line.isHovered();
			caliper1Line.setHovered(false);
			caliper2Line.setHovered(false);
		} else if (mode == CaliperMode.HORIZONTAL && caliper1HorizontalLine != null && caliper2HorizontalLine != null) {
			wasHovered = caliper1HorizontalLine.isHovered() || caliper2HorizontalLine.isHovered();
			caliper1HorizontalLine.setHovered(false);
			caliper2HorizontalLine.setHovered(false);
		}
		if (wasHovered) {
			figureUpdateCallback.run();
		}
	}

	/**
	 * Update the caliper elements in the figure based on enabled state and mode.
	 */
	public void updateCaliperElements() {
		if (enabled) {
			// Only set snap mode (transparency) on the active snap caliper
			boolean caliper1IsActive = snapModeActive && activeSnapCaliper != null && activeSnapCaliper == 1;
			boolean caliper2IsActive = snapModeActive && activeSnapCaliper != null && activeSnapCaliper == 2;
			
			caliper1Line.setSnapMode(caliper1IsActive);
			caliper2Line.setSnapMode(caliper2IsActive);
			caliper1HorizontalLine.setSnapMode(caliper1IsActive);
			caliper2HorizontalLine.setSnapMode(caliper2IsActive);
			
			if (mode == CaliperMode.VERTICAL) {
				figure.addRelativeExtra(caliper1Line);
				figure.addRelativeExtra(caliper2Line);
			} else {
				figure.addRelativeExtra(caliper1HorizontalLine);
				figure.addRelativeExtra(caliper2HorizontalLine);
			}
		}
		// Note: We don't clear the extras here - that's handled by the caller
	}

	/**
	 * Save the current caliper state for the current view type.
	 */
	public void saveCurrentCaliperState() {
		RocketPanel.VIEW_TYPE viewType = viewTypeProvider.getCurrentViewType();
		CaliperState state = getCaliperStateForView(viewType);
		if (state == null) {
			return;
		}
		state.caliper1X = caliper1X;
		state.caliper2X = caliper2X;
		state.caliper1Y = caliper1Y;
		state.caliper2Y = caliper2Y;
	}

	/**
	 * Load the caliper state for the specified view type.
	 *
	 * @param viewType the view type to load state for
	 */
	public void loadCaliperStateForView(RocketPanel.VIEW_TYPE viewType) {
		CaliperState state = getCaliperStateForView(viewType);
		if (state == null) {
			return;
		}
		caliper1X = state.caliper1X;
		caliper2X = state.caliper2X;
		caliper1Y = state.caliper1Y;
		caliper2Y = state.caliper2Y;

		// If state is uninitialized (NaN), initialize it for this view
		if (Double.isNaN(caliper1X) || Double.isNaN(caliper2X) ||
				Double.isNaN(caliper1Y) || Double.isNaN(caliper2Y)) {
			initializeCaliperPositions();
			return;
		}

		if (caliper1Line != null) {
			caliper1Line.setX(caliper1X);
		}
		if (caliper2Line != null) {
			caliper2Line.setX(caliper2X);
		}
		if (caliper1HorizontalLine != null) {
			caliper1HorizontalLine.setY(caliper1Y);
		}
		if (caliper2HorizontalLine != null) {
			caliper2HorizontalLine.setY(caliper2Y);
		}
		updateCaliperPositionModelsFromState();
		updateCaliperDistance();
		updateCaliperHorizontalDistance();
	}

	/**
	 * Called when switching to 3D view - saves state and disables caliper.
	 */
	public void onSwitchTo3D() {
		saveCurrentCaliperState();
		wasEnabledBefore3d = enabled;
		if (enabled) {
			setEnabled(false);
		}
		if (caliperButton != null) {
			caliperButton.setEnabled(false);
		}
		if (displayPanel != null) {
			displayPanel.setVisible(false);
		}
		if (caliper1PositionSpinner != null && caliper2PositionSpinner != null) {
			caliper1PositionSpinner.setEnabled(false);
			caliper2PositionSpinner.setEnabled(false);
		}
	}

	/**
	 * Called when switching to 2D view - restores caliper state if it was enabled.
	 */
	public void onSwitchTo2D() {
		if (caliperButton != null) {
			caliperButton.setEnabled(true);
		}
		if (wasEnabledBefore3d) {
			setEnabled(true);
			if (displayPanel != null) {
				displayPanel.setVisible(true);
				displayPanel.setPreferredSize(null);
			}
			if (caliper1PositionSpinner != null && caliper2PositionSpinner != null) {
				caliper1PositionSpinner.setEnabled(true);
				caliper2PositionSpinner.setEnabled(true);
			}
			updateCaliperPositionModelsFromState();
		}
		wasEnabledBefore3d = false;
	}

	/**
	 * Get the caliper state for a specific view type.
	 */
	private CaliperState getCaliperStateForView(RocketPanel.VIEW_TYPE viewType) {
		if (viewType == RocketPanel.VIEW_TYPE.BackView) {
			return backViewCaliperState;
		} else if (viewType == RocketPanel.VIEW_TYPE.TopView || viewType == RocketPanel.VIEW_TYPE.SideView) {
			return sideViewCaliperState;
		}
		return null;
	}

	/**
	 * Set the position of a vertical caliper line.
	 */
	private void setCaliperLinePosition(boolean cal1Handle, double position) {
		if (Double.isNaN(position)) {
			return;
		}
		if (cal1Handle) {
			caliper1X = position;
			if (caliper1Line != null) {
				caliper1Line.setX(position);
			}
		} else {
			caliper2X = position;
			if (caliper2Line != null) {
				caliper2Line.setX(position);
			}
		}
		updateCaliperDistance();
		updateCaliperPositionModelsFromState();
		saveCurrentCaliperState();
		figureUpdateCallback.run();
	}

	/**
	 * Set the position of a horizontal caliper line.
	 */
	private void setHorizontalCaliperLinePosition(boolean cal1Handle, double position) {
		if (Double.isNaN(position)) {
			return;
		}
		if (cal1Handle) {
			caliper1Y = position;
			if (caliper1HorizontalLine != null) {
				caliper1HorizontalLine.setY(position);
			}
		} else {
			caliper2Y = position;
			if (caliper2HorizontalLine != null) {
				caliper2HorizontalLine.setY(position);
			}
		}
		updateCaliperHorizontalDistance();
		updateCaliperPositionModelsFromState();
		saveCurrentCaliperState();
		figureUpdateCallback.run();
	}

	/**
	 * Update the caliper distance models from the current state.
	 */
	private void updateCaliperPositionModelsFromState() {
		if (caliper1PositionModel == null || caliper2PositionModel == null) {
			return;
		}
		updatingCaliperPositionModels = true;
		try {
			if (mode == CaliperMode.VERTICAL) {
				if (!Double.isNaN(caliper1X)) {
					caliper1PositionModel.setValue(caliper1X);
				}
				if (!Double.isNaN(caliper2X)) {
					caliper2PositionModel.setValue(caliper2X);
				}
			} else {
				if (!Double.isNaN(caliper1Y)) {
					caliper1PositionModel.setValue(caliper1Y);
				}
				if (!Double.isNaN(caliper2Y)) {
					caliper2PositionModel.setValue(caliper2Y);
				}
			}
		} finally {
			updatingCaliperPositionModels = false;
		}
	}

	/**
	 * Calculate and update the vertical caliper distance.
	 */
	private void updateCaliperDistance() {
		if (Double.isNaN(caliper1X) || Double.isNaN(caliper2X)) {
			return;
		}
		double distance = Math.abs(caliper2X - caliper1X);
		distanceModel.setValue(distance);
	}

	/**
	 * Calculate and update the horizontal caliper distance.
	 */
	private void updateCaliperHorizontalDistance() {
		if (Double.isNaN(caliper1Y) || Double.isNaN(caliper2Y)) {
			return;
		}
		double distance = Math.abs(caliper2Y - caliper1Y);
		if (horizontalDistanceModel != null) {
			horizontalDistanceModel.setValue(distance);
		}
	}

	/**
	 * Initialize caliper positions based on rocket bounds.
	 */
	private void initializeCaliperPositions() {
		FlightConfiguration curConfig = document.getSelectedConfiguration();
		BoundingBox bounds = curConfig.getBoundingBox();
		RocketPanel.VIEW_TYPE currentView = viewTypeProvider.getCurrentViewType();

		if (currentView == RocketPanel.VIEW_TYPE.BackView) {
			// For back view, use symmetric positions around 0 (center of screen)
			if (bounds == null || bounds.span().getY() <= 0) {
				// Default symmetric positions if bounds are invalid
				caliper1X = -0.1;
				caliper2X = 0.1;
				// For horizontal caliper: cal 1 at top (positive Y), cal 2 at bottom (negative Y)
				caliper1Y = 0.1;
				caliper2Y = -0.1;
			} else {
				// Use symmetric positions based on rocket dimensions
				double halfSpanX = bounds.span().getY() / 2.0;  // Y dimension in back view
				double halfSpanY = bounds.span().getZ() / 2.0;  // Z dimension in back view
				caliper1X = -halfSpanX;
				caliper2X = halfSpanX;
				// For horizontal caliper: cal 1 at top (positive Y), cal 2 at bottom (negative Y)
				caliper1Y = halfSpanY;
				caliper2Y = -halfSpanY;
			}
		} else {
			// For side/top views, use fractional positions of rocket length (X dimension)
			// Check if bounds are invalid or if this is the default empty bounding box (0 to 1)
			boolean isEmptyBounds = bounds == null ||
					bounds.span().getX() <= 0 ||
					(MathUtil.equals(bounds.min.getX(), 0.0) && MathUtil.equals(bounds.max.getX(), 1.0));

			if (isEmptyBounds) {
				// Default absolute positions if bounds are invalid or empty (no components)
				// Use positions that are more likely to be visible (closer to center)
				caliper1X = 0.0;
				caliper2X = 0.2;
				// For horizontal caliper: cal 1 at top (positive Y), cal 2 at bottom (negative Y)
				caliper1Y = 0.05;
				caliper2Y = -0.05;
			} else {
				// Use 15% and 85% of rocket length for X
				double length = bounds.span().getX();
				caliper1X = bounds.min.getX() + 0.15 * length;
				caliper2X = bounds.min.getX() + 0.85 * length;
				// Use symmetric positions for Y based on rocket height
				// For horizontal caliper: cal 1 at top (positive Y), cal 2 at bottom (negative Y)
				double halfSpanY = bounds.span().getY() / 2.0;
				caliper1Y = halfSpanY;
				caliper2Y = -halfSpanY;
			}
		}

		if (caliper1Line != null) {
			caliper1Line.setX(caliper1X);
		}
		if (caliper2Line != null) {
			caliper2Line.setX(caliper2X);
		}
		if (caliper1HorizontalLine != null) {
			caliper1HorizontalLine.setY(caliper1Y);
		}
		if (caliper2HorizontalLine != null) {
			caliper2HorizontalLine.setY(caliper2Y);
		}

		updateCaliperDistance();
		updateCaliperHorizontalDistance();
		updateCaliperPositionModelsFromState();
		saveCurrentCaliperState();
	}

	/**
	 * Enter snap mode for the specified caliper.
	 *
	 * @param caliperNumber 1 or 2
	 */
	public void enterSnapMode(int caliperNumber) {
		if (caliperNumber != 1 && caliperNumber != 2) {
			throw new IllegalArgumentException("Caliper number must be 1 or 2");
		}
		snapModeActive = true;
		activeSnapCaliper = caliperNumber;
		hoveredSnapTarget = null;
		updateSnapTargets();
		updateSnapButtonStates();
		figureUpdateCallback.run();
		
		// Request focus on the scroll pane so Escape key works
		if (focusRequestCallback != null) {
			focusRequestCallback.run();
		}
	}

	/**
	 * Exit snap mode.
	 */
	public void exitSnapMode() {
		snapModeActive = false;
		activeSnapCaliper = null;
		if (hoveredSnapTarget != null) {
			hoveredSnapTarget = null;
			figureUpdateCallback.run();  // Update to clear highlight
		}
		currentSnapTargets.clear();
		updateSnapButtonStates();
		figureUpdateCallback.run();
	}

	/**
	 * Check if snap mode is currently active.
	 *
	 * @return true if snap mode is active
	 */
	public boolean isSnapModeActive() {
		return snapModeActive;
	}

	/**
	 * Get the currently active snap caliper (1 or 2), or null if not in snap mode.
	 *
	 * @return the active snap caliper number, or null
	 */
	public Integer getActiveSnapCaliper() {
		return activeSnapCaliper;
	}

	/**
	 * Get the currently hovered snap target, or null if none.
	 *
	 * @return the hovered snap target, or null
	 */
	public CaliperSnapTarget getHoveredSnapTarget() {
		return hoveredSnapTarget;
	}

	/**
	 * Get the list of all current snap targets.
	 *
	 * @return the list of current snap targets
	 */
	public List<CaliperSnapTarget> getCurrentSnapTargets() {
		return new ArrayList<>(currentSnapTargets);
	}

	/**
	 * Set whether to always show all snap targets (debug mode).
	 *
	 * @param alwaysShow true to always show all snap targets, false to only show on hover
	 */
	public void setAlwaysShowSnapTargets(boolean alwaysShow) {
		if (alwaysShowSnapTargets != alwaysShow) {
			alwaysShowSnapTargets = alwaysShow;
			figureUpdateCallback.run();
		}
	}

	/**
	 * Check if always showing snap targets is enabled (debug mode).
	 *
	 * @return true if always showing snap targets is enabled
	 */
	public boolean isAlwaysShowSnapTargets() {
		return alwaysShowSnapTargets;
	}

	/**
	 * Update snap button visual states.
	 */
	private void updateSnapButtonStates() {
		if (caliper1SnapButton != null) {
			boolean selected = snapModeActive && activeSnapCaliper != null && activeSnapCaliper == 1;
			caliper1SnapButton.setSelected(selected);
		}
		if (caliper2SnapButton != null) {
			boolean selected = snapModeActive && activeSnapCaliper != null && activeSnapCaliper == 2;
			caliper2SnapButton.setSelected(selected);
		}
	}

	/**
	 * Update the list of snap targets based on current view type and caliper mode.
	 */
	public void updateSnapTargets() {
		currentSnapTargets.clear();
		
		if (!enabled) {
			return;
		}
		if (!snapModeActive) {
			return;
		}

		RocketPanel.VIEW_TYPE viewType = viewTypeProvider.getCurrentViewType();
		if (viewType == null) {
			return;
		}
		if (viewType.is3d) {
			return;
		}

		// Get component transformations from the figure
		Map<RocketComponent, List<Transformation>> componentTransforms =
				figure.getComponentTransformations();

		// Get snap targets from the registry
		CaliperSnapRegistry registry = CaliperSnapRegistry.getInstance();
		
		for (Map.Entry<RocketComponent, List<Transformation>> entry :
				componentTransforms.entrySet()) {
			RocketComponent component = entry.getKey();
			List<Transformation> transforms = entry.getValue();
			
			for (Transformation transform : transforms) {
				List<CaliperSnapTarget> targets = registry.getSnapTargets(
						component, viewType, mode, transform);
				currentSnapTargets.addAll(targets);
			}
		}
	}

	/**
	 * Find the nearest snap target to the given screen coordinates.
	 *
	 * @param screenX screen X coordinate
	 * @param screenY screen Y coordinate
	 * @param screenToModel function to convert screen to model coordinates
	 * @return the nearest compatible snap target, or null if none within tolerance
	 */
	public CaliperSnapTarget findNearestSnapTarget(int screenX, int screenY,
												   java.util.function.Function<Point, Point2D.Double> screenToModel) {
		if (!snapModeActive) {
			return null;
		}
		if (currentSnapTargets.isEmpty()) {
			return null;
		}

		Point screenPoint = new Point(screenX, screenY);
		Point2D.Double modelPoint = screenToModel.apply(screenPoint);
		if (modelPoint == null) {
			return null;
		}

		RocketPanel.VIEW_TYPE viewType = viewTypeProvider.getCurrentViewType();
		
		// Convert to Coordinate for distance calculation
		// In back view, screen X maps to model Z, screen Y maps to model Y
		// In side/top view, screen X maps to model X, screen Y maps to model Y
		info.openrocket.core.util.Coordinate point;
		if (viewType == RocketPanel.VIEW_TYPE.BackView) {
			// Back view: modelPoint.x is actually Z coordinate
			point = new info.openrocket.core.util.Coordinate(0, modelPoint.y, modelPoint.x);
		} else {
			// Side/Top view: modelPoint.x is X coordinate
			point = new info.openrocket.core.util.Coordinate(modelPoint.x, modelPoint.y, 0);
		}

		// Fixed pixel tolerance: 8 pixels
		// Convert pixel tolerance to model coordinates using the figure's scale
		double scale = figure.getAbsoluteScale();
		double modelTolerance = SNAP_PIXEL_TOLERANCE / scale;

		CaliperSnapTarget nearest = null;
		double minDistance = Double.MAX_VALUE;

		for (CaliperSnapTarget target : currentSnapTargets) {
			if (!target.isCompatibleWith(mode)) {
				continue;
			}

			double distance = target.getDistanceToPoint(point, viewType);
			if (distance < modelTolerance && distance < minDistance) {
				minDistance = distance;
				nearest = target;
			}
		}

		return nearest;
	}

	/**
	 * Handle mouse move in snap mode - update hovered target.
	 *
	 * @param screenX screen X coordinate
	 * @param screenY screen Y coordinate
	 * @param screenToModel function to convert screen to model coordinates
	 */
	public void handleSnapModeMouseMoved(int screenX, int screenY,
										java.util.function.Function<Point, Point2D.Double> screenToModel) {
		if (!snapModeActive) {
			return;
		}

		CaliperSnapTarget nearest = findNearestSnapTarget(screenX, screenY, screenToModel);
		if (nearest != hoveredSnapTarget) {
			hoveredSnapTarget = nearest;
			figureUpdateCallback.run();
		}
	}

	/**
	 * Handle mouse click in snap mode - snap to the nearest target.
	 *
	 * @param screenX screen X coordinate
	 * @param screenY screen Y coordinate
	 * @param screenToModel function to convert screen to model coordinates
	 * @return true if snapping occurred, false otherwise
	 */
	public boolean handleSnapModeMouseClicked(int screenX, int screenY,
											  java.util.function.Function<Point, Point2D.Double> screenToModel) {
		if (!snapModeActive || activeSnapCaliper == null) {
			return false;
		}

		CaliperSnapTarget target = findNearestSnapTarget(screenX, screenY, screenToModel);
		if (target == null) {
			// Clicked on empty space - exit snap mode
			exitSnapMode();
			return false;
		}

		// Snap the caliper to the target
		RocketPanel.VIEW_TYPE viewType = viewTypeProvider.getCurrentViewType();
		double snapValue = target.getSnapValue(mode, viewType);
		boolean isCaliper1 = (activeSnapCaliper == 1);

		if (mode == CaliperMode.VERTICAL) {
			setCaliperLinePosition(isCaliper1, snapValue);
		} else {
			setHorizontalCaliperLinePosition(isCaliper1, snapValue);
		}

		// Exit snap mode after snapping
		exitSnapMode();
		return true;
	}
}

