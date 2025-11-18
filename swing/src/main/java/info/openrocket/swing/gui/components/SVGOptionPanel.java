package info.openrocket.swing.gui.components;

import com.itextpdf.text.Rectangle;
import info.openrocket.core.preferences.ApplicationPreferences;
import net.miginfocom.swing.MigLayout;
import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.swing.gui.print.PaperSize;
import info.openrocket.swing.gui.print.PrintUnit;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import info.openrocket.swing.gui.print.PaperOrientation;
import java.awt.Component;
import java.awt.Font;
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
	
	// Page size selection
	private JComboBox<Object> pageSizeCombo;
	private PaperSize selectedPageSize = PaperSize.getDefault();
	private static final String CUSTOM_PAGE_SIZE = "Custom";
	private static final String PAGE_SIZE_SEPARATOR = "__PAGE_SIZE_SEPARATOR__";
	
	// Page orientation
	private PaperOrientation pageOrientation = PaperOrientation.PORTRAIT;
	private ButtonGroup orientationGroup;
	private JLabel pageOrientationLabel;
	private JRadioButton portraitRadioButton;
	private JRadioButton landscapeRadioButton;
	
	// Custom page size
	private double customPageWidth = 0.21; // Default A4 width in meters
	private double customPageHeight = 0.297; // Default A4 height in meters
	private DoubleModel pageWidthModel;
	private DoubleModel pageHeightModel;
	private JLabel pageWidthLabel;
	private JSpinner pageWidthSpinner;
	private UnitSelector pageWidthUnitSelector;
	private JLabel pageHeightLabel;
	private JSpinner pageHeightSpinner;
	private UnitSelector pageHeightUnitSelector;
	
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
		super(new MigLayout("insets 0", "[fill, grow][]1[fill, grow]", ""));
		this.showCrosshairToggle = showCrosshairToggle;

		// Left column: Stroke, Crosshair, and Label settings
		JPanel leftPanel = new JPanel(new MigLayout());
		
		// Stroke color
		JLabel label = new JLabel(trans.get("SVGOptionPanel.lbl.strokeColor"));
		label.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeColor.ttip"));
		leftPanel.add(label);
		colorChooser = new ColorChooserButton(prefs.getSVGStrokeColor());
		colorChooser.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeColor.ttip"));
		leftPanel.add(colorChooser, "wrap");

		// Stroke width
		label = new JLabel(trans.get("SVGOptionPanel.lbl.strokeWidth"));
		label.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeWidth.ttip"));
		leftPanel.add(label);
		DoubleModel dm = new DoubleModel(this, "StrokeWidth", UnitGroup.UNITS_STROKE_WIDTH, 0.001, 10);
		dm.setValue(prefs.getSVGStrokeWidth());
		JSpinner spin = new JSpinner(dm.getSpinnerModel());
		spin.setToolTipText(trans.get("SVGOptionPanel.lbl.strokeWidth.ttip"));
		spin.setEditor(new SpinnerEditor(spin, 5));
		leftPanel.add(spin);
		leftPanel.add(new UnitSelector(dm), "growx, wrap para");

		if (showCrosshairToggle) {
			crosshairCheckbox = new JCheckBox(trans.get("SVGOptionPanel.lbl.crosshair"));
			crosshairCheckbox.setToolTipText(trans.get("SVGOptionPanel.lbl.crosshair.ttip"));
			drawCrosshair = prefs.isSVGDrawCrosshair();
			crosshairCheckbox.setSelected(drawCrosshair);
			leftPanel.add(crosshairCheckbox, "span, wrap");

			crosshairColorLabel = new JLabel(trans.get("SVGOptionPanel.lbl.crosshairColor"));
			crosshairColorLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.crosshairColor.ttip"));
			leftPanel.add(crosshairColorLabel);
			crosshairColorChooser = new ColorChooserButton(prefs.getSVGCrosshairColor());
			crosshairColorChooser.setToolTipText(trans.get("SVGOptionPanel.lbl.crosshairColor.ttip"));
			leftPanel.add(crosshairColorChooser, "wrap para");

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
		leftPanel.add(showLabelsCheckbox, "spanx, wrap");

		// Label color
		labelColorLabel = new JLabel(trans.get("SVGOptionPanel.lbl.labelColor"));
		labelColorLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.labelColor.ttip"));
		leftPanel.add(labelColorLabel);
		labelColorChooser = new ColorChooserButton(prefs.getSVGLabelColor());
		labelColorChooser.setToolTipText(trans.get("SVGOptionPanel.lbl.labelColor.ttip"));
		leftPanel.add(labelColorChooser, "wrap");

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

		// Right column: Page size settings
		JPanel rightPanel = new JPanel(new MigLayout());
		
		// Page size selection
		rightPanel.add(new JLabel(trans.get("SVGOptionPanel.lbl.pageSize")));
		DefaultComboBoxModel<Object> pageSizeModel = new DefaultComboBoxModel<>();
		for (PaperSize size : PaperSize.values()) {
			pageSizeModel.addElement(size);
		}
		pageSizeModel.addElement(PAGE_SIZE_SEPARATOR);
		pageSizeModel.addElement(CUSTOM_PAGE_SIZE);
		pageSizeCombo = new JComboBox<>(pageSizeModel);
		pageSizeCombo.setSelectedItem(selectedPageSize);
		pageSizeCombo.setToolTipText(trans.get("SVGOptionPanel.lbl.pageSize.ttip"));
		pageSizeCombo.setRenderer(new PageSizeComboBoxRenderer(pageSizeCombo.getRenderer()));
		pageSizeCombo.addActionListener(e -> {
			// Prevent separator from being selected
			if (PAGE_SIZE_SEPARATOR.equals(pageSizeCombo.getSelectedItem())) {
				pageSizeCombo.setSelectedItem(selectedPageSize);
			} else {
				updatePageSizeControls();
			}
		});
		rightPanel.add(pageSizeCombo, "growx, wrap");
		
		// Page orientation
		pageOrientationLabel = new JLabel(trans.get("SVGOptionPanel.lbl.pageOrientation"));
		rightPanel.add(pageOrientationLabel);
		orientationGroup = new ButtonGroup();
		portraitRadioButton = new JRadioButton(trans.get("SVGOptionPanel.radio.portrait"));
		portraitRadioButton.setToolTipText(trans.get("SVGOptionPanel.radio.portrait.ttip"));
		portraitRadioButton.setSelected(pageOrientation == PaperOrientation.PORTRAIT);
		landscapeRadioButton = new JRadioButton(trans.get("SVGOptionPanel.radio.landscape"));
		landscapeRadioButton.setToolTipText(trans.get("SVGOptionPanel.radio.landscape.ttip"));
		landscapeRadioButton.setSelected(pageOrientation == PaperOrientation.LANDSCAPE);
		orientationGroup.add(portraitRadioButton);
		orientationGroup.add(landscapeRadioButton);
		portraitRadioButton.addActionListener(e -> {
			pageOrientation = PaperOrientation.PORTRAIT;
			updatePageSizeControls();
		});
		landscapeRadioButton.addActionListener(e -> {
			pageOrientation = PaperOrientation.LANDSCAPE;
			updatePageSizeControls();
		});
		rightPanel.add(portraitRadioButton, "split 2");
		rightPanel.add(landscapeRadioButton, "wrap para");
		
		// Page width
		pageWidthLabel = new JLabel(trans.get("SVGOptionPanel.lbl.pageWidth"));
		pageWidthLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.pageWidth.ttip"));
		pageWidthModel = new DoubleModel(this, "CustomPageWidth", UnitGroup.UNITS_LENGTH, 0.001, 10.0);

		//// Initialize with default paper size dimensions (with orientation)
		double[] initialDimensions = getOrientedPaperSizeDimensions(selectedPageSize, pageOrientation);
		pageWidthModel.setValue(initialDimensions[0]);
		pageWidthSpinner = new JSpinner(pageWidthModel.getSpinnerModel());
		pageWidthSpinner.setToolTipText(trans.get("SVGOptionPanel.lbl.pageWidth.ttip"));
		pageWidthSpinner.setEditor(new SpinnerEditor(pageWidthSpinner, 5));
		pageWidthUnitSelector = new UnitSelector(pageWidthModel);
		pageWidthSpinner.setEnabled(false);
		pageWidthUnitSelector.setEnabled(false);
		pageWidthLabel.setEnabled(false);
		rightPanel.add(pageWidthLabel);
		rightPanel.add(pageWidthSpinner, "split 2");
		rightPanel.add(pageWidthUnitSelector, "growx, wrap");

		// Page height
		pageHeightLabel = new JLabel(trans.get("SVGOptionPanel.lbl.pageHeight"));
		pageHeightLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.pageHeight.ttip"));
		pageHeightModel = new DoubleModel(this, "CustomPageHeight", UnitGroup.UNITS_LENGTH, 0.001, 10.0);
		pageHeightModel.setValue(initialDimensions[1]);
		pageHeightSpinner = new JSpinner(pageHeightModel.getSpinnerModel());
		pageHeightSpinner.setToolTipText(trans.get("SVGOptionPanel.lbl.pageHeight.ttip"));
		pageHeightSpinner.setEditor(new SpinnerEditor(pageHeightSpinner, 5));
		pageHeightUnitSelector = new UnitSelector(pageHeightModel);
		pageHeightSpinner.setEnabled(false);
		pageHeightUnitSelector.setEnabled(false);
		pageHeightLabel.setEnabled(false);
		rightPanel.add(pageHeightLabel);
		rightPanel.add(pageHeightSpinner, "split 2");
		rightPanel.add(pageHeightUnitSelector, "growx, wrap para");

		// Part spacing
		spacingLabel = new JLabel(trans.get("SVGOptionPanel.lbl.partSpacing"));
		spacingLabel.setToolTipText(trans.get("SVGOptionPanel.lbl.partSpacing.ttip"));
		spacingModel = new DoubleModel(this, "PartSpacing", UnitGroup.UNITS_LENGTH, 0.0, 1.0);
		spacingModel.setValue(partSpacing);
		spacingSpinner = new JSpinner(spacingModel.getSpinnerModel());
		spacingSpinner.setToolTipText(trans.get("SVGOptionPanel.lbl.partSpacing.ttip"));
		spacingSpinner.setEditor(new SpinnerEditor(spacingSpinner, 5));
		spacingUnitSelector = new UnitSelector(spacingModel);
		rightPanel.add(spacingLabel);
		rightPanel.add(spacingSpinner, "split 2");
		rightPanel.add(spacingUnitSelector, "growx, wrap para");
		
		// Add both panels to main panel with separator
		add(leftPanel, "aligny top");
		JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
		add(separator, "growy, spany, gaptop para, gapbottom para");
		add(rightPanel, "aligny top");
	}
	
	private void updatePageSizeControls() {
		Object selected = pageSizeCombo.getSelectedItem();
		boolean isCustom = CUSTOM_PAGE_SIZE.equals(selected);
		
		if (isCustom) {
			// Enable widgets for custom size
			pageWidthLabel.setEnabled(true);
			pageWidthSpinner.setEnabled(true);
			pageWidthUnitSelector.setEnabled(true);
			pageHeightLabel.setEnabled(true);
			pageHeightSpinner.setEnabled(true);
			pageHeightUnitSelector.setEnabled(true);
			// Disable orientation label and radio buttons in custom mode
			pageOrientationLabel.setEnabled(false);
			portraitRadioButton.setEnabled(false);
			landscapeRadioButton.setEnabled(false);
			selectedPageSize = null;
		} else if (selected instanceof PaperSize) {
			// Disable widgets and update values to match selected paper size with orientation
			selectedPageSize = (PaperSize) selected;
			double[] dimensions = getOrientedPaperSizeDimensions(selectedPageSize, pageOrientation);
			pageWidthModel.setValue(dimensions[0]);
			pageHeightModel.setValue(dimensions[1]);
			pageWidthLabel.setEnabled(false);
			pageWidthSpinner.setEnabled(false);
			pageWidthUnitSelector.setEnabled(false);
			pageHeightLabel.setEnabled(false);
			pageHeightSpinner.setEnabled(false);
			pageHeightUnitSelector.setEnabled(false);
			// Enable orientation label and radio buttons when using standard paper size
			pageOrientationLabel.setEnabled(true);
			portraitRadioButton.setEnabled(true);
			landscapeRadioButton.setEnabled(true);
		}
	}
	
	private double[] getPaperSizeDimensions(PaperSize paperSize) {
		if (paperSize == null) {
			// Fallback to A4
			return new double[]{0.21, 0.297};
		}
		Rectangle rect = paperSize.getSize();

		// Convert from points to meters
		double width = PrintUnit.POINTS.toMeters(rect.getWidth());
		double height = PrintUnit.POINTS.toMeters(rect.getHeight());
		return new double[]{width, height};
	}
	
	private double[] getOrientedPaperSizeDimensions(PaperSize paperSize, PaperOrientation orientation) {
		double[] dimensions = getPaperSizeDimensions(paperSize);
		double width = dimensions[0];
		double height = dimensions[1];
		
		// Apply orientation - landscape swaps width and height
		if (orientation == PaperOrientation.LANDSCAPE) {
			return new double[]{height, width};
		} else {
			return new double[]{width, height};
		}
	}

	/**
	 * Custom combobox renderer that supports separators and italic text for "Custom" option.
	 */
	@SuppressWarnings("rawtypes")
	private static class PageSizeComboBoxRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 1L;
		private final JSeparator separator;
		private final ListCellRenderer defaultRenderer;

		public PageSizeComboBoxRenderer(ListCellRenderer defaultRenderer) {
			this.defaultRenderer = defaultRenderer;
			this.separator = new JSeparator(JSeparator.HORIZONTAL);
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index,
													  boolean isSelected, boolean cellHasFocus) {
			String str = (value == null) ? "" : value.toString();
			// Handle separator
			if (PAGE_SIZE_SEPARATOR.equals(str)) {
				return separator;
			}

			// Use default renderer
			Component component = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			// Make "Custom" italic
			if (CUSTOM_PAGE_SIZE.equals(str) && component instanceof JLabel label) {
				Font font = label.getFont();
				if (font != null) {
					label.setFont(font.deriveFont(Font.ITALIC));
				}
			}

			return component;
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
	
	public boolean isCustomPageSize() {
		return selectedPageSize == null;
	}
	
	// Getter methods required by DoubleModel (via reflection)
	public double getCustomPageWidth() {
		return customPageWidth;
	}
	
	// Setter called by DoubleModel - just set the field, don't call the model to avoid infinite loop
	public void setCustomPageWidth(double width) {
		this.customPageWidth = width;
	}
	
	public double getCustomPageHeight() {
		return customPageHeight;
	}
	
	// Setter called by DoubleModel - just set the field, don't call the model to avoid infinite loop
	public void setCustomPageHeight(double height) {
		this.customPageHeight = height;
	}
	
	public double getPartSpacing() {
		return partSpacing;
	}
	
	// Setter called by DoubleModel - just set the field, don't call the model to avoid infinite loop
	public void setPartSpacing(double spacing) {
		this.partSpacing = spacing;
	}

	
	// Get page dimensions in meters (width, height)
	public double[] getPageDimensions() {
		if (isCustomPageSize()) {
			return new double[]{customPageWidth, customPageHeight};
		} else if (selectedPageSize != null) {
			return getOrientedPaperSizeDimensions(selectedPageSize, pageOrientation);
		} else {
			// Fallback to A4
			return new double[]{0.21, 0.297};
		}
	}
	
	public PaperOrientation getPageOrientation() {
		return pageOrientation;
	}
	
	public void setPageOrientation(PaperOrientation orientation) {
		this.pageOrientation = orientation;
		if (portraitRadioButton != null) {
			portraitRadioButton.setSelected(orientation == PaperOrientation.PORTRAIT);
		}
		if (landscapeRadioButton != null) {
			landscapeRadioButton.setSelected(orientation == PaperOrientation.LANDSCAPE);
		}
		updatePageSizeControls();
	}
}
