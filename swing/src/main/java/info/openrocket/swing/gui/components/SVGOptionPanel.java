package info.openrocket.swing.gui.components;

import info.openrocket.core.preferences.ApplicationPreferences;
import net.miginfocom.swing.MigLayout;
import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import java.awt.Color;

public class SVGOptionPanel extends JPanel {
	private static final Translator trans = Application.getTranslator();
	private static final ApplicationPreferences prefs = Application.getPreferences();

	private final ColorChooserButton colorChooser;
	private ColorChooserButton crosshairColorChooser;
	private JLabel crosshairColorLabel;
	private double strokeWidth = 0.1;
	private boolean drawCrosshair = true;
	private final boolean showCrosshairToggle;
	private JCheckBox crosshairCheckbox;

	public SVGOptionPanel() {
		this(false);
	}

	public SVGOptionPanel(boolean showCrosshairToggle) {
		super(new MigLayout());
		this.showCrosshairToggle = showCrosshairToggle;

		// Stroke color
		JLabel label = new JLabel(trans.get("SVGOptionPanel.lbl.strokeColor"));
		label.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeColor.ttip"));
		add(label);
		colorChooser = new ColorChooserButton(prefs.getSVGStrokeColor());
		colorChooser.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeColor.ttip"));
		add(colorChooser, "wrap");

		// Stroke width
		label = new JLabel(trans.get("SVGOptionPanel.lbl.strokeWidth"));
		label.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeWidth.ttip"));
		add(label);
		DoubleModel dm = new DoubleModel(this, "StrokeWidth", UnitGroup.UNITS_STROKE_WIDTH, 0.001, 10);
		dm.setValue(prefs.getSVGStrokeWidth());
		JSpinner spin = new JSpinner(dm.getSpinnerModel());
		spin.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeWidth.ttip"));
		spin.setEditor(new SpinnerEditor(spin, 5));
		add(spin);
		add(new UnitSelector(dm), "growx, wrap");

		if (showCrosshairToggle) {
			crosshairCheckbox = new JCheckBox(trans.get("SVGOptionPanel.lbl.crosshair"));
			crosshairCheckbox.setToolTipText(trans.get("SVGOptionPanel.lbl.crosshair.ttip"));
			drawCrosshair = prefs.isSVGDrawCrosshair();
			crosshairCheckbox.setSelected(drawCrosshair);
			add(crosshairCheckbox, "span, wrap");

			crosshairColorLabel = new JLabel(trans.get("SVGOptionPanel.lbl.crosshairColor"));
			crosshairColorLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.crosshairColor.ttip"));
			add(crosshairColorLabel);
			crosshairColorChooser = new ColorChooserButton(prefs.getSVGCrosshairColor());
			crosshairColorChooser.setToolTipText(trans.get("SVGOptionPanel.lbl.crosshairColor.ttip"));
			add(crosshairColorChooser, "wrap");

			crosshairCheckbox.addActionListener(e -> {
				drawCrosshair = crosshairCheckbox.isSelected();
				updateCrosshairInputs();
			});
			updateCrosshairInputs();
		}
	}

	public Color getStrokeColor() {
		return colorChooser.getSelectedColor();
	}

	public void setStrokeColor(Color color) {
		colorChooser.setSelectedColor(color);
	}

	public double getStrokeWidth() {
		return strokeWidth;
	}

	public void setStrokeWidth(double strokeWidth) {
		this.strokeWidth = strokeWidth;
	}

	public boolean isDrawCrosshair() {
		return showCrosshairToggle ? drawCrosshair : true;
	}

	public void setDrawCrosshair(boolean drawCrosshair) {
		if (showCrosshairToggle) {
			this.drawCrosshair = drawCrosshair;
			if (crosshairCheckbox != null) {
				crosshairCheckbox.setSelected(drawCrosshair);
				updateCrosshairInputs();
			}
		}
	}

	public Color getCrosshairColor() {
		return showCrosshairToggle && crosshairColorChooser != null ?
				crosshairColorChooser.getSelectedColor() : colorChooser.getSelectedColor();
	}

	public void setCrosshairColor(Color color) {
		if (showCrosshairToggle && crosshairColorChooser != null) {
			crosshairColorChooser.setSelectedColor(color);
		}
	}

	private void updateCrosshairInputs() {
		boolean enabled = drawCrosshair;
		if (crosshairColorLabel != null) {
			crosshairColorLabel.setEnabled(enabled);
		}
		if (crosshairColorChooser != null) {
			crosshairColorChooser.setEnabled(enabled);
		}
	}
}
