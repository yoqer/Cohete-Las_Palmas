package info.openrocket.swing.gui.components;

import info.openrocket.core.preferences.ApplicationPreferences;
import net.miginfocom.swing.MigLayout;
import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.swing.gui.print.PaperSize;
import info.openrocket.swing.gui.print.PrintUnit;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.DefaultComboBoxModel;
import java.awt.Color;

public class SVGOptionPanel extends JPanel {
	private static final Translator trans = Application.getTranslator();
	private static final ApplicationPreferences prefs = Application.getPreferences();

	private final ColorChooserButton colorChooser;
	private ColorChooserButton crosshairColorChooser;
	private JLabel crosshairColorLabel;
	private ColorChooserButton labelColorChooser;
	private JLabel labelColorLabel;
	private double strokeWidth = 0.1;
	private boolean drawCrosshair = true;
	private boolean showLabels = true;
	private final boolean showCrosshairToggle;
	private JCheckBox crosshairCheckbox;
	private JCheckBox showLabelsCheckbox;
	
	// Paper size selection
	private JComboBox<Object> paperSizeCombo;
	private PaperSize selectedPaperSize = PaperSize.getDefault();
	private static final String CUSTOM_PAPER_SIZE = "Custom";
	
	// Custom paper size
	private double customPaperWidth = 0.21; // Default A4 width in meters
	private double customPaperHeight = 0.297; // Default A4 height in meters
	private DoubleModel customWidthModel;
	private DoubleModel customHeightModel;
	private JLabel customWidthLabel;
	private JSpinner customWidthSpinner;
	private UnitSelector customWidthUnitSelector;
	private JLabel customHeightLabel;
	private JSpinner customHeightSpinner;
	private UnitSelector customHeightUnitSelector;
	
	// Part spacing
	private double partSpacing = 0.01; // Default 10mm spacing in meters
	private DoubleModel spacingModel;
	private JLabel spacingLabel;
	private JSpinner spacingSpinner;
	private UnitSelector spacingUnitSelector;

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
		add(new UnitSelector(dm), "growx, wrap para");

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
			add(crosshairColorChooser, "wrap para");

			crosshairCheckbox.addActionListener(e -> {
				drawCrosshair = crosshairCheckbox.isSelected();
				updateCrosshairInputs();
			});
			updateCrosshairInputs();
		}

		// Show labels checkbox (always shown)
		showLabelsCheckbox = new JCheckBox(trans.get("SVGOptionPanel.lbl.showLabels"));
		showLabelsCheckbox.setToolTipText(trans.get("SVGOptionPanel.lbl.showLabels.ttip"));
		showLabels = prefs.isSVGShowLabels();
		showLabelsCheckbox.setSelected(showLabels);
		add(showLabelsCheckbox, "spanx, wrap");

		// Label color
		labelColorLabel = new JLabel(trans.get("SVGOptionPanel.lbl.labelColor"));
		labelColorLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.labelColor.ttip"));
		add(labelColorLabel);
		labelColorChooser = new ColorChooserButton(prefs.getSVGLabelColor());
		labelColorChooser.setToolTipText(trans.get("SVGOptionPanel.lbl.labelColor.ttip"));
		add(labelColorChooser, "wrap");

		// Enable/disable label color controls based on checkbox
		showLabelsCheckbox.addActionListener(e -> {
			boolean enabled = showLabelsCheckbox.isSelected();
			labelColorLabel.setEnabled(enabled);
			labelColorChooser.setEnabled(enabled);
		});
		// Initial state
		boolean labelsEnabled = showLabelsCheckbox.isSelected();
		labelColorLabel.setEnabled(labelsEnabled);
		labelColorChooser.setEnabled(labelsEnabled);
		
		// Paper size selection
		add(new JLabel(trans.get("SVGOptionPanel.lbl.paperSize")), "spanx, wrap para");
		DefaultComboBoxModel<Object> paperSizeModel = new DefaultComboBoxModel<>();
		for (PaperSize size : PaperSize.values()) {
			paperSizeModel.addElement(size);
		}
		paperSizeModel.addElement(CUSTOM_PAPER_SIZE);
		paperSizeCombo = new JComboBox<>(paperSizeModel);
		paperSizeCombo.setSelectedItem(selectedPaperSize);
		paperSizeCombo.setToolTipText(trans.get("SVGOptionPanel.lbl.paperSize.ttip"));
		paperSizeCombo.addActionListener(e -> updatePaperSizeControls());
		add(paperSizeCombo, "growx, wrap para");
		
		// Custom paper size inputs (initially hidden)
		customWidthLabel = new JLabel(trans.get("SVGOptionPanel.lbl.customPaperWidth"));
		customWidthLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.customPaperWidth.ttip"));
		customWidthModel = new DoubleModel(this, "CustomPaperWidth", UnitGroup.UNITS_LENGTH, 0.001, 10.0);
		customWidthModel.setValue(customPaperWidth);
		customWidthSpinner = new JSpinner(customWidthModel.getSpinnerModel());
		customWidthSpinner.setToolTipText(trans.get("SVGOptionPanel.lbl.customPaperWidth.ttip"));
		customWidthSpinner.setEditor(new SpinnerEditor(customWidthSpinner, 5));
		customWidthUnitSelector = new UnitSelector(customWidthModel);
		customWidthLabel.setVisible(false);
		customWidthSpinner.setVisible(false);
		customWidthUnitSelector.setVisible(false);
		
		customHeightLabel = new JLabel(trans.get("SVGOptionPanel.lbl.customPaperHeight"));
		customHeightLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.customPaperHeight.ttip"));
		customHeightModel = new DoubleModel(this, "CustomPaperHeight", UnitGroup.UNITS_LENGTH, 0.001, 10.0);
		customHeightModel.setValue(customPaperHeight);
		customHeightSpinner = new JSpinner(customHeightModel.getSpinnerModel());
		customHeightSpinner.setToolTipText(trans.get("SVGOptionPanel.lbl.customPaperHeight.ttip"));
		customHeightSpinner.setEditor(new SpinnerEditor(customHeightSpinner, 5));
		customHeightUnitSelector = new UnitSelector(customHeightModel);
		customHeightLabel.setVisible(false);
		customHeightSpinner.setVisible(false);
		customHeightUnitSelector.setVisible(false);
		
		// Part spacing
		spacingLabel = new JLabel(trans.get("SVGOptionPanel.lbl.partSpacing"));
		spacingLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.partSpacing.ttip"));
		spacingModel = new DoubleModel(this, "PartSpacing", UnitGroup.UNITS_LENGTH, 0.0, 1.0);
		spacingModel.setValue(partSpacing);
		spacingSpinner = new JSpinner(spacingModel.getSpinnerModel());
		spacingSpinner.setToolTipText(trans.get("SVGOptionPanel.lbl.partSpacing.ttip"));
		spacingSpinner.setEditor(new SpinnerEditor(spacingSpinner, 5));
		spacingUnitSelector = new UnitSelector(spacingModel);
		add(spacingLabel);
		add(spacingSpinner);
		add(spacingUnitSelector, "growx, wrap para");
		
		updatePaperSizeControls();
	}
	
	private void updatePaperSizeControls() {
		Object selected = paperSizeCombo.getSelectedItem();
		boolean isCustom = CUSTOM_PAPER_SIZE.equals(selected);
		
		if (isCustom) {
			// Show custom size inputs
			if (customWidthLabel.getParent() == null) {
				add(customWidthLabel);
				add(customWidthSpinner);
				add(customWidthUnitSelector, "growx, wrap para");
			}
			customWidthLabel.setVisible(true);
			customWidthSpinner.setVisible(true);
			customWidthUnitSelector.setVisible(true);
			if (customHeightLabel.getParent() == null) {
				add(customHeightLabel);
				add(customHeightSpinner);
				add(customHeightUnitSelector, "growx, wrap para");
			}
			customHeightLabel.setVisible(true);
			customHeightSpinner.setVisible(true);
			customHeightUnitSelector.setVisible(true);
			selectedPaperSize = null;
		} else if (selected instanceof PaperSize) {
			// Hide custom size inputs
			customWidthLabel.setVisible(false);
			customWidthSpinner.setVisible(false);
			customWidthUnitSelector.setVisible(false);
			customHeightLabel.setVisible(false);
			customHeightSpinner.setVisible(false);
			customHeightUnitSelector.setVisible(false);
			selectedPaperSize = (PaperSize) selected;
		}
		revalidate();
		repaint();
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

	public boolean isShowLabels() {
		return showLabelsCheckbox != null ? showLabelsCheckbox.isSelected() : showLabels;
	}

	public void setShowLabels(boolean showLabels) {
		this.showLabels = showLabels;
		if (showLabelsCheckbox != null) {
			showLabelsCheckbox.setSelected(showLabels);
			boolean enabled = showLabels;
			if (labelColorLabel != null) {
				labelColorLabel.setEnabled(enabled);
			}
			if (labelColorChooser != null) {
				labelColorChooser.setEnabled(enabled);
			}
		}
	}

	public Color getLabelColor() {
		return labelColorChooser != null ? labelColorChooser.getSelectedColor() : Color.BLACK;
	}

	public void setLabelColor(Color color) {
		if (labelColorChooser != null) {
			labelColorChooser.setSelectedColor(color);
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
	
	public boolean isCustomPaperSize() {
		return selectedPaperSize == null;
	}
	
	public double getPartSpacing() {
		return partSpacing;
	}

	
	// Get paper dimensions in meters (width, height)
	public double[] getPaperDimensions() {
		if (isCustomPaperSize()) {
			return new double[]{customPaperWidth, customPaperHeight};
		} else if (selectedPaperSize != null) {
			com.itextpdf.text.Rectangle rect = selectedPaperSize.getSize();
			// Convert from points to meters
			double width = PrintUnit.POINTS.toMeters(rect.getWidth());
			double height = PrintUnit.POINTS.toMeters(rect.getHeight());
			return new double[]{width, height};
		} else {
			// Fallback to A4
			return new double[]{0.21, 0.297};
		}
	}
}
