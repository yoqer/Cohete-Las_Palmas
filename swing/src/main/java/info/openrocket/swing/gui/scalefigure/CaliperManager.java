package info.openrocket.swing.gui.scalefigure;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.rocketcomponent.FlightConfiguration;
import info.openrocket.core.unit.Unit;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.core.util.BoundingBox;
import info.openrocket.core.util.MathUtil;
import info.openrocket.core.util.StateChangeListener;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.components.EditableSpinner;
import info.openrocket.swing.gui.components.UnitSelector;
import info.openrocket.swing.gui.figureelements.CaliperLine;
import info.openrocket.swing.gui.figureelements.HorizontalCaliperLine;
import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.widgets.IconToggleButton;
import info.openrocket.swing.gui.util.Icons;
import info.openrocket.core.startup.Application;
import info.openrocket.core.l10n.Translator;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
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
import java.util.EventObject;

/**
 * Manages the caliper measurement tool for the rocket panel.
 * Handles all caliper-related state, UI components, and interactions.
 *
 * @author OpenRocket Team
 */
public class CaliperManager {

	private static final Translator trans = Application.getTranslator();

	/**
	 * Caliper mode: VERTICAL for measuring horizontal distances, HORIZONTAL for measuring vertical distances.
	 */
	public enum CaliperMode {
		VERTICAL,
		HORIZONTAL
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
	private IconToggleButton toggleButton;
	private JButton modeButton;
	private JPanel displayPanel;
	private JSpinner distanceSpinner;
	private UnitSelector unitSelector;
	private JSpinner caliper1PositionSpinner;
	private JSpinner caliper2PositionSpinner;

	// Models
	private final DoubleModel distanceModel;
	private final DoubleModel horizontalDistanceModel;
	private final DoubleModel caliper1PositionModel;
	private final DoubleModel caliper2PositionModel;
	private boolean updatingCaliperPositionModels = false;

	// State per view type
	private final CaliperState sideViewCaliperState = new CaliperState();
	private final CaliperState backViewCaliperState = new CaliperState();

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
	 */
	public CaliperManager(RocketFigure figure, OpenRocketDocument document,
						  ViewTypeProvider viewTypeProvider, Runnable figureUpdateCallback) {
		this.figure = figure;
		this.document = document;
		this.viewTypeProvider = viewTypeProvider;
		this.figureUpdateCallback = figureUpdateCallback;

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
		// Caliper tool toggle button
		toggleButton = new IconToggleButton();
		toggleButton.setSelectedIcon(Icons.RULER);
		toggleButton.setToolTipText(trans.get("RocketPanel.btn.Caliper"));

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
		displayPanel.add(new JLabel("2:"), "gapleft rel");
		displayPanel.add(caliper2PositionSpinner);

		// Update visibility when caliper is toggled
		toggleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setEnabled(toggleButton.isSelected());
			}
		});
	}

	/**
	 * Get the toggle button for the caliper tool.
	 *
	 * @return the toggle button
	 */
	public IconToggleButton getToggleButton() {
		return toggleButton;
	}

	/**
	 * Get the mode button for switching between vertical and horizontal calipers.
	 *
	 * @return the mode button
	 */
	public JButton getModeButton() {
		return modeButton;
	}

	/**
	 * Get the display panel containing distance and position controls.
	 *
	 * @return the display panel
	 */
	public JPanel getDisplayPanel() {
		return displayPanel;
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
		updateCaliperElements();
		updateCaliperPositionModelsFromState();
		updateCaliperDistance();
		updateCaliperHorizontalDistance();
		figureUpdateCallback.run();
	}

	/**
	 * Get the current caliper mode.
	 *
	 * @return the caliper mode
	 */
	public CaliperMode getMode() {
		return mode;
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
			if (toggleButton != null) {
				toggleButton.setSelected(false);
			}
		}
		if (toggleButton != null) {
			toggleButton.setEnabled(false);
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
		if (toggleButton != null) {
			toggleButton.setEnabled(true);
		}
		if (wasEnabledBefore3d) {
			setEnabled(true);
			if (toggleButton != null) {
				toggleButton.setSelected(true);
			}
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
				caliper1Y = -0.1;
				caliper2Y = 0.1;
			} else {
				// Use symmetric positions based on rocket dimensions
				double halfSpanX = bounds.span().getY() / 2.0;  // Y dimension in back view
				double halfSpanY = bounds.span().getZ() / 2.0;  // Z dimension in back view
				caliper1X = -halfSpanX;
				caliper2X = halfSpanX;
				caliper1Y = -halfSpanY;
				caliper2Y = halfSpanY;
			}
		} else {
			// For side/top views, use fractional positions of rocket length (X dimension)
			// Check if bounds are invalid or if this is the default empty bounding box (0 to 1)
			boolean isEmptyBounds = bounds == null ||
					bounds.span().getX() <= 0 ||
					(MathUtil.equals(bounds.min.getX(), 0.0) && MathUtil.equals(bounds.max.getX(), 1.0));

			if (isEmptyBounds) {
				// Default absolute positions if bounds are invalid or empty (no components)
				caliper1X = 0.15;
				caliper2X = 0.85;
				caliper1Y = -0.1;
				caliper2Y = 0.1;
			} else {
				// Use 15% and 85% of rocket length for X
				double length = bounds.span().getX();
				caliper1X = bounds.min.getX() + 0.15 * length;
				caliper2X = bounds.min.getX() + 0.85 * length;
				// Use symmetric positions for Y based on rocket height
				double halfSpanY = bounds.span().getY() / 2.0;
				caliper1Y = -halfSpanY;
				caliper2Y = halfSpanY;
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
}

