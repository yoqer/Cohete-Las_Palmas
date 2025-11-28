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

import javax.swing.AbstractButton;
import javax.swing.BoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;

import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.DoubleModel;
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

	private static final double REFERENCE_DPI = 96.0;
	private static final double BASE_PROGRESS_BAR_HEIGHT = 4.0;
	private static final double BASE_SENSITIVITY = 2.0;
	private static final double BASE_PROGRESS_BAR_PADDING = 1.0; // Extra padding above progress bar

	/**
	 * Creates a SpinnerWithSlider with a linear progress bar range.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   minimum value for the progress bar
	 * @param max   maximum value for the progress bar
	 */
	public SpinnerWithSlider(DoubleModel model, double min, double max) {
		this(model, model.getSliderModel(min, max));
	}

	/**
	 * Creates a SpinnerWithSlider with dynamic progress bar bounds.
	 *
	 * @param model the backing DoubleModel for the value
	 * @param min   model defining the minimum progress bar value
	 * @param max   model defining the maximum progress bar value
	 */
	public SpinnerWithSlider(DoubleModel model, DoubleModel min, DoubleModel max) {
		this(model, model.getSliderModel(min, max));
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
		this(model, model.getSliderModel(min, mid, max));
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
		this(model, model.getSliderModel(min, mid, max));
	}

	private SpinnerWithSlider(DoubleModel model, BoundedRangeModel sliderModel) {
		this.model = model;
		this.sliderModel = sliderModel;
		initialize();
	}

	private void initialize() {
		spinner = new DraggableSpinner(model, sliderModel);
		spinner.setEditor(new SpinnerEditor(spinner));

		setLayout(new MigLayout("insets 0, gap 0", "[grow][shrink]", "[]"));

		if (model.getUnitGroup() != UnitGroup.UNITS_NONE) {
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
		private Color progressColor;
		private final int progressBarHeight;
		private final int extraHeight;

		public DraggableSpinner(DoubleModel model, BoundedRangeModel sliderModel) {
			super(model.getSpinnerModel());
			this.sliderModel = sliderModel;

			double dpi = GUIUtil.getDPI();
			this.progressBarHeight = (int) Math.round(BASE_PROGRESS_BAR_HEIGHT * (dpi / REFERENCE_DPI));
			int padding = (int) Math.round(BASE_PROGRESS_BAR_PADDING * (dpi / REFERENCE_DPI));
			this.extraHeight = progressBarHeight + padding;

			try {
				progressColor = javax.swing.UIManager.getColor("Component.accentColor");
			} catch (Exception e) { /* Fallback */ }
			if (progressColor == null) {
				progressColor = new Color(100, 150, 255);
			}

			setupButtonLogic();
			this.addChangeListener(e -> repaint());
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

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int min = sliderModel.getMinimum();
			int max = sliderModel.getMaximum();
			int val = sliderModel.getValue();

			if (max > min) {
				double pct = Math.max(0.0, Math.min(1.0, (double)(val - min) / (max - min)));
				g2.setColor(progressColor);
				g2.fillRect(0, getHeight() - progressBarHeight, (int)(getWidth() * pct), progressBarHeight);
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
			 * @param steps Number of steps (positive = increase, negative = decrease)
			 * @param useFineSteps Whether to use fine stepping (1/10th of normal step)
			 */
			private void applySteps(int steps, boolean useFineSteps) {
				SpinnerModel spinnerModel = DraggableSpinner.this.getModel();

				for (int i = 0; i < Math.abs(steps); i++) {
					Object newValue;
					if (steps > 0) {
						newValue = useFineSteps ? getFineNextValue(spinnerModel) : spinnerModel.getNextValue();
					} else {
						newValue = useFineSteps ? getFinePreviousValue(spinnerModel) : spinnerModel.getPreviousValue();
					}
					if (newValue != null) {
						DraggableSpinner.this.setValue(newValue);
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
