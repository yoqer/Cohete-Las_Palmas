package info.openrocket.swing.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Field;

import javax.swing.Timer;

import javax.swing.AbstractButton;
import javax.swing.BoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;

import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.theme.UITheme;
import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.util.SwingPreferences;
import net.miginfocom.swing.MigLayout;

/**
 * A compact spinner widget with an integrated visual progress indicator and unit selector.
 * <p>
 * Features:
 * <ul>
 *   <li>Progress bar at bottom showing value relative to range</li>
 *   <li>Drag vertically (up/down) or horizontally (left/right) on buttons to adjust value</li>
 *   <li>Up or Right = increase, Down or Left = decrease</li>
 *   <li>Hold SHIFT while dragging for fine control (smaller steps or increased sensitivity)</li>
 *   <li>Single-click on buttons still works normally</li>
 * </ul>
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class SpinnerWithSlider extends JPanel {

	private DraggableSpinner spinner;
	private UnitSelector unitSelector;
	private final BoundedRangeModel sliderModel;
	private final DoubleModel model;
	private final boolean showUnitSelector;

	private static final double REFERENCE_DPI = 96.0;
	private static final double BASE_PROGRESS_BAR_HEIGHT = 4.0;
	private static final double BASE_SENSITIVITY = 2.0;
	private static final double BASE_PROGRESS_BAR_PADDING = 1.0; // Extra padding above progress bar

	private static Color disabledProgressColor;

	static {
		initColors();
	}

	private static void initColors() {
		updateColors();
		UITheme.Theme.addUIThemeChangeListener(SpinnerWithSlider::updateColors);
	}

	public static void updateColors() {
		disabledProgressColor = GUIUtil.getUITheme().getDisabledProgressColor();
	}

	/**
	 * Creates a SpinnerWithSlider with a linear progress bar range.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   minimum value for the progress bar
	 * @param max   maximum value for the progress bar
	 */
	public SpinnerWithSlider(DoubleModel model, double min, double max) {
		this(model, model.getSliderModel(min, max), true);
	}

	/**
	 * Creates a SpinnerWithSlider with a linear progress bar range.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   minimum value for the progress bar
	 * @param max   maximum value for the progress bar
	 * @param showUnitSelector whether to show the unit selector (default: true)
	 */
	public SpinnerWithSlider(DoubleModel model, double min, double max, boolean showUnitSelector) {
		this(model, model.getSliderModel(min, max), showUnitSelector);
	}

	/**
	 * Creates a SpinnerWithSlider with dynamic progress bar bounds.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   model defining the minimum progress bar value
	 * @param max   model defining the maximum progress bar value
	 */
	public SpinnerWithSlider(DoubleModel model, DoubleModel min, DoubleModel max) {
		this(model, model.getSliderModel(min, max), true);
	}

	/**
	 * Creates a SpinnerWithSlider with dynamic progress bar bounds.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   model defining the minimum progress bar value
	 * @param max   model defining the maximum progress bar value
	 * @param showUnitSelector whether to show the unit selector (default: true)
	 */
	public SpinnerWithSlider(DoubleModel model, DoubleModel min, DoubleModel max, boolean showUnitSelector) {
		this(model, model.getSliderModel(min, max), showUnitSelector);
	}

	/**
	 * Creates a SpinnerWithSlider with a non-linear (exponential) progress bar range.
	 * Values scale linearly from min to mid, then exponentially from mid to max.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   minimum value for the progress bar
	 * @param mid   midpoint value (where linear scaling transitions to exponential)
	 * @param max   maximum value for the progress bar
	 */
	public SpinnerWithSlider(DoubleModel model, double min, double mid, double max) {
		this(model, model.getSliderModel(min, mid, max), true);
	}

	/**
	 * Creates a SpinnerWithSlider with a non-linear (exponential) progress bar range.
	 * Values scale linearly from min to mid, then exponentially from mid to max.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   minimum value for the progress bar
	 * @param mid   midpoint value (where linear scaling transitions to exponential)
	 * @param max   maximum value for the progress bar
	 * @param showUnitSelector whether to show the unit selector (default: true)
	 */
	public SpinnerWithSlider(DoubleModel model, double min, double mid, double max, boolean showUnitSelector) {
		this(model, model.getSliderModel(min, mid, max), showUnitSelector);
	}

	/**
	 * Creates a SpinnerWithSlider with a non-linear progress bar range and dynamic maximum.
	 * Values scale linearly from min to mid, then exponentially from mid to max.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   minimum value for the progress bar
	 * @param mid   midpoint value (where linear scaling transitions to exponential)
	 * @param max   model defining the maximum progress bar value
	 */
	public SpinnerWithSlider(DoubleModel model, double min, double mid, DoubleModel max) {
		this(model, model.getSliderModel(min, mid, max), true);
	}

	/**
	 * Creates a SpinnerWithSlider with a non-linear progress bar range and dynamic maximum.
	 * Values scale linearly from min to mid, then exponentially from mid to max.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   minimum value for the progress bar
	 * @param mid   midpoint value (where linear scaling transitions to exponential)
	 * @param max   model defining the maximum progress bar value
	 * @param showUnitSelector whether to show the unit selector (default: true)
	 */
	public SpinnerWithSlider(DoubleModel model, double min, double mid, DoubleModel max, boolean showUnitSelector) {
		this(model, model.getSliderModel(min, mid, max), showUnitSelector);
	}

	private SpinnerWithSlider(DoubleModel model, BoundedRangeModel sliderModel, boolean showUnitSelector) {
		this.model = model;
		this.sliderModel = sliderModel;
		this.showUnitSelector = showUnitSelector;
		initialize();
	}

	private void initialize() {
		spinner = new DraggableSpinner(model, sliderModel);
		spinner.setEditor(new SpinnerEditor(spinner));

		setLayout(new MigLayout("insets 0, gap 0", "[grow][shrink]", "[]"));

		if (showUnitSelector && model.getUnitGroup() != UnitGroup.UNITS_NONE) {
			unitSelector = new UnitSelector(model);
			add(spinner, "growx, split 2");
			add(unitSelector, "growx 0");
		} else {
			add(spinner, "growx");
		}

		sliderModel.addChangeListener(e -> spinner.repaint());
	}

	/**
	 * Custom JSpinner with progress bar overlay and drag-to-adjust functionality.
	 */
	private static class DraggableSpinner extends JSpinner {
		private final BoundedRangeModel sliderModel;
		private final DoubleModel model;
		private Color progressColor;
		private final int progressBarHeight;
		private final int extraHeight;
		
		// Cached values for performance
		private Field cachedMinField;
		private Field cachedMaxField;
		private boolean shouldCenterAtZero = false;
		private double cachedActualMin = Double.NEGATIVE_INFINITY;
		private double cachedActualMax = Double.POSITIVE_INFINITY;
		private boolean cacheValid = false;
		
		// Repaint throttling for better drag performance
		private final Timer repaintTimer;
		private boolean pendingRepaint = false;

		public DraggableSpinner(DoubleModel model, BoundedRangeModel sliderModel) {
			super(model.getSpinnerModel());
			this.model = model;
			this.sliderModel = sliderModel;

			double dpi = GUIUtil.getDPI();
			this.progressBarHeight = (int) Math.round(BASE_PROGRESS_BAR_HEIGHT * (dpi / REFERENCE_DPI));
			int padding = (int) Math.round(BASE_PROGRESS_BAR_PADDING * (dpi / REFERENCE_DPI));
			this.extraHeight = progressBarHeight + padding;

			// Get enabled progress color
			try {
				progressColor = UIManager.getColor("Component.accentColor");
			} catch (Exception e) { /* Fallback */ }
			if (progressColor == null) {
				progressColor = new Color(100, 150, 255);
			}

			// Initialize reflection fields once
			initializeReflection();
			
			// Setup repaint throttling timer (repaints at most every 16ms ~60fps)
			repaintTimer = new Timer(16, e -> {
				if (pendingRepaint) {
					repaintProgressBar();
					pendingRepaint = false;
				}
			});
			repaintTimer.setRepeats(false);
			
			setupButtonLogic();
			this.addChangeListener(e -> {
				// Invalidate cache when model changes
				cacheValid = false;
				// Throttle repaints for better performance during dragging
				scheduleRepaint();
			});
			
			// Also listen to slider model changes to invalidate cache
			sliderModel.addChangeListener(e -> {
				cacheValid = false;
				scheduleRepaint();
			});
		}
		
		/**
		 * Schedules a repaint with throttling to avoid excessive repaints during dragging.
		 */
		private void scheduleRepaint() {
			if (!pendingRepaint) {
				pendingRepaint = true;
				if (!repaintTimer.isRunning()) {
					repaintTimer.start();
				}
			}
		}
		
		/**
		 * Repaints only the progress bar area for better performance.
		 */
		private void repaintProgressBar() {
			if (isDisplayable()) {
				int y = getHeight() - progressBarHeight - (int) Math.round(BASE_PROGRESS_BAR_PADDING * (GUIUtil.getDPI() / REFERENCE_DPI));
				repaint(0, y, getWidth(), progressBarHeight + (int) Math.round(BASE_PROGRESS_BAR_PADDING * (GUIUtil.getDPI() / REFERENCE_DPI)));
			}
		}
		
		private void initializeReflection() {
			try {
				cachedMinField = sliderModel.getClass().getDeclaredField("min");
				cachedMaxField = sliderModel.getClass().getDeclaredField("max");
				cachedMinField.setAccessible(true);
				cachedMaxField.setAccessible(true);
			} catch (Exception e) {
				// Reflection not available, will use normal behavior
				cachedMinField = null;
				cachedMaxField = null;
			}
		}
		
		private void updateCache() {
			if (cacheValid) {
				return;
			}
			
			shouldCenterAtZero = false;
			cachedActualMin = Double.NEGATIVE_INFINITY;
			cachedActualMax = Double.POSITIVE_INFINITY;
			
			if (cachedMinField != null && cachedMaxField != null) {
				try {
					DoubleModel minModel = (DoubleModel) cachedMinField.get(sliderModel);
					DoubleModel maxModel = (DoubleModel) cachedMaxField.get(sliderModel);
					
					// Get values in current unit
					cachedActualMin = model.getCurrentUnit().toUnit(minModel.getValue());
					cachedActualMax = model.getCurrentUnit().toUnit(maxModel.getValue());
					
					// Check if range crosses zero
					shouldCenterAtZero = (cachedActualMin < 0 && cachedActualMax > 0);
					cacheValid = true;
				} catch (Exception e) {
					// If reflection fails, fall back to normal behavior
					shouldCenterAtZero = false;
					cacheValid = true; // Mark as valid to avoid retrying
				}
			} else {
				cacheValid = true; // Mark as valid to avoid retrying
			}
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension preferred = super.getPreferredSize();
			if (preferred != null) {
				return new Dimension(preferred.width, preferred.height + extraHeight);
			}
			return preferred;
		}

		@Override
		public Dimension getMinimumSize() {
			Dimension minimum = super.getMinimumSize();
			if (minimum != null) {
				return new Dimension(minimum.width, minimum.height + extraHeight);
			}
			return minimum;
		}

		private void setupButtonLogic() {
			for (Component c : this.getComponents()) {
				if (c != this.getEditor() && c instanceof AbstractButton btn) {
					for (MouseListener ml : btn.getMouseListeners()) {
						btn.removeMouseListener(ml);
					}
					for (MouseMotionListener mml : btn.getMouseMotionListeners()) {
						btn.removeMouseMotionListener(mml);
					}

					ButtonDragHandler handler = new ButtonDragHandler(btn);
					btn.addMouseListener(handler);
					btn.addMouseMotionListener(handler);
					btn.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
				}
			}
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			int sliderMin = sliderModel.getMinimum();
			int sliderMax = sliderModel.getMaximum();
			int sliderVal = sliderModel.getValue();

			if (sliderMax <= sliderMin) {
				return;
			}

			// Update cache if needed
			updateCache();

			// Use disabled color when spinner is disabled
			Color drawColor = isEnabled() ? progressColor : SpinnerWithSlider.disabledProgressColor;
			
			// Create graphics context without antialiasing for better performance (just drawing rectangles)
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(drawColor);

			if (shouldCenterAtZero) {
				// Draw centered at 0
				double currentValue = model.getCurrentUnit().toUnit(model.getValue());
				int width = getWidth();
				int centerX = width / 2;
				int y = getHeight() - progressBarHeight;
				
				if (Math.abs(currentValue) < 1e-10) {
					// At zero, draw nothing
				} else if (currentValue > 0) {
					// Positive: draw from center to right
					double pct = Math.min(1.0, currentValue / cachedActualMax);
					int barWidth = (int)(width / 2.0 * pct);
					if (barWidth > 0) {
						g2.fillRect(centerX, y, barWidth, progressBarHeight);
					}
				} else {
					// Negative: draw from center to left
					double pct = Math.min(1.0, Math.abs(currentValue) / Math.abs(cachedActualMin));
					int barWidth = (int)(width / 2.0 * pct);
					if (barWidth > 0) {
						g2.fillRect(centerX - barWidth, y, barWidth, progressBarHeight);
					}
				}
			} else {
				// Normal behavior: draw from left to right
				double pct = Math.max(0.0, Math.min(1.0, (double)(sliderVal - sliderMin) / (sliderMax - sliderMin)));
				int barWidth = (int)(getWidth() * pct);
				if (barWidth > 0) {
					g2.fillRect(0, getHeight() - progressBarHeight, barWidth, progressBarHeight);
				}
			}
			g2.dispose();
		}

		/**
		 * Handles mouse interaction for spinner buttons.
		 * Distinguishes between single-clicks (step once) and drags (continuous adjustment).
		 */
		private class ButtonDragHandler extends MouseAdapter {
			private static final SwingPreferences prefs = (SwingPreferences) Application.getPreferences();

			private final AbstractButton button;

			private int startX;
			private int startY;
			private int lastStepCount;
			private int sensitivity;
			private boolean isDragging;
			private boolean lastFineControl;

			private static final int DRAG_THRESHOLD = 3;

			public ButtonDragHandler(AbstractButton button) {
				this.button = button;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				startX = e.getXOnScreen();
				startY = e.getYOnScreen();
				lastStepCount = 0;
				isDragging = false;

				// Initialize lastFineControl based on current shift state
				lastFineControl = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

				// Calculate base sensitivity
				double userSens = prefs.getSpinnerDragSensitivity();
				sensitivity = (int) Math.round(BASE_SENSITIVITY * (GUIUtil.getDPI() / REFERENCE_DPI) / userSens);
				if (sensitivity < 1) {
					sensitivity = 1;
				}

				// Commit pending text edits
				try {
					DraggableSpinner.this.commitEdit();
				} catch (Exception ignored) { }

				button.getModel().setArmed(true);
				button.getModel().setPressed(true);
				button.repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int currentX = e.getXOnScreen();
				int currentY = e.getYOnScreen();

				if (!isDragging) {
					int deltaX = currentX - startX;
					int deltaY = startY - currentY;

					// Check if we've exceeded the drag threshold in either direction
					if (Math.abs(deltaX) >= DRAG_THRESHOLD || Math.abs(deltaY) >= DRAG_THRESHOLD) {
						isDragging = true;
						button.getModel().setArmed(false);	// Disarm button so release won't fire ActionEvent
					} else {
						return;
					}
				}

				boolean fineControl = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

				// Reset tracking when shift state changes to prevent jumps
				if (fineControl != lastFineControl) {
					startX = currentX;
					startY = currentY;
					lastStepCount = 0;
					lastFineControl = fineControl;
				}

				int deltaX = currentX - startX;
				int deltaY = startY - currentY;

				// Up/Right = positive (increase), Down/Left = negative (decrease)
				int totalDelta = deltaX + deltaY;

				int stepCount = totalDelta / sensitivity;

				// Apply steps incrementally since last update
				int stepsToApply = stepCount - lastStepCount;
				if (stepsToApply != 0) {
					applySteps(stepsToApply, fineControl);
					lastStepCount = stepCount;
				}
			}

			/**
			 * Applies the given number of steps to the spinner value.
			 * Respects the slider's min/max bounds, not the DoubleModel's bounds.
			 * The slider position ranges from 0 (min) to 1000 (max).
			 * @param steps Number of steps (positive = increase, negative = decrease)
			 * @param useFineSteps Whether to use fine stepping (1/10th of normal step)
			 */
			private void applySteps(int steps, boolean useFineSteps) {
				SpinnerModel spinnerModel = DraggableSpinner.this.getModel();
				// Slider position range: 0 = min, 1000 = max
				final int SLIDER_MIN = sliderModel.getMinimum();
				final int SLIDER_MAX = sliderModel.getMaximum();

				for (int i = 0; i < Math.abs(steps); i++) {
					// Check if we're at the slider's boundary before trying to step
					int currentSliderPos = sliderModel.getValue();
					if (steps > 0 && currentSliderPos >= SLIDER_MAX) {
						// Already at slider's maximum, stop
						break;
					}
					if (steps < 0 && currentSliderPos <= SLIDER_MIN) {
						// Already at slider's minimum, stop
						break;
					}

					Object newValue;
					if (steps > 0) {
						newValue = useFineSteps ? getFineNextValue(spinnerModel) : spinnerModel.getNextValue();
					} else {
						newValue = useFineSteps ? getFinePreviousValue(spinnerModel) : spinnerModel.getPreviousValue();
					}
					if (newValue != null) {
						DraggableSpinner.this.setValue(newValue);
						// Check again after setting value to see if we hit the boundary
						int newSliderPos = sliderModel.getValue();
						if ((steps > 0 && newSliderPos >= SLIDER_MAX) || (steps < 0 && newSliderPos <= SLIDER_MIN)) {
							// Reached slider boundary, stop
							break;
						}
					} else {
						// Reached min/max boundary (from spinner model), stop applying steps
						break;
					}
				}
			}

			private Object getFineNextValue(SpinnerModel spinnerModel) {
				if (spinnerModel instanceof DoubleModel.ValueSpinnerModel vsm) {
					return vsm.getFineNextValue();
				}
				// For other models (e.g. integers), fall back to normal stepping
				return spinnerModel.getNextValue();
			}

			private Object getFinePreviousValue(SpinnerModel spinnerModel) {
				if (spinnerModel instanceof DoubleModel.ValueSpinnerModel vsm) {
					return vsm.getFinePreviousValue();
				}
				// For other models (e.g. integers), fall back to normal stepping
				return spinnerModel.getPreviousValue();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// If armed, setPressed(false) will fire the standard ActionEvent (single-click step)
				// If we dragged, armed was set to false, so no event fires
				button.getModel().setPressed(false);
				button.getModel().setArmed(false);
				button.repaint();
				isDragging = false;
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (!isDragging) {
					button.getModel().setRollover(true);
					button.repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.getModel().setRollover(false);
				button.repaint();
			}
		}
	}

	public JSpinner getSpinner() {
		return spinner;
	}

	public UnitSelector getUnitSelector() {
		return unitSelector;
	}

	public BoundedRangeModel getSliderModel() {
		return sliderModel;
	}

	public DoubleModel getModel() {
		return model;
	}

	public JTextField getTextField() {
		return ((SpinnerEditor) spinner.getEditor()).getTextField();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		spinner.setEnabled(enabled);
		if (unitSelector != null) {
			unitSelector.setEnabled(enabled);
		}
		spinner.repaint();
	}
}
