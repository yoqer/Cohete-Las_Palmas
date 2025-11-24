package info.openrocket.swing.gui.simulation;

import info.openrocket.core.aerodynamics.lookup.CsvMachAoALookup;
import info.openrocket.core.aerodynamics.lookup.MachAoALookup;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.simulation.SimulationOptions;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.gui.components.FieldSeparatorComboBox;
import info.openrocket.swing.gui.theme.UITheme;
import info.openrocket.swing.gui.util.GUIUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Dialog for configuring drag and stability lookup tables loaded from CSV files.
 */
class AerodynamicLookupDialog extends JDialog {

	private static final Translator trans = Application.getTranslator();
	private static final List<String> DRAG_VALUE_COLUMNS = List.of("cd");
	private static final List<String> STABILITY_VALUE_COLUMNS = List.of("cn", "cm", "cp");

	private final SimulationOptions options;

	private Path dragCsv;
	private MachAoALookup dragTable;
	private char dragSeparator = ',';
	private Path stabilityCsv;
	private MachAoALookup stabilityTable;
	private char stabilitySeparator = ',';

	private JLabel dragSummaryLabel;
	private JButton clearDragButton;
	private FieldSeparatorComboBox dragSeparatorCombo;
	private JTextArea dragExampleArea;
	private JScrollPane dragExampleScroll;
	private javax.swing.border.TitledBorder dragExampleBorder;

	private JLabel stabilitySummaryLabel;
	private JButton clearStabilityButton;
	private FieldSeparatorComboBox stabilitySeparatorCombo;
	private JTextArea stabilityExampleArea;
	private JScrollPane stabilityExampleScroll;
	private javax.swing.border.TitledBorder stabilityExampleBorder;

	private static Color textColor;
	private static Color infoTextColor;

	static {
		initColors();
	}

	public AerodynamicLookupDialog(Window owner, SimulationOptions options) {
		super(owner, trans.get("AerodynamicLookupDialog.title"), ModalityType.APPLICATION_MODAL);
		this.options = options;

		this.dragCsv = options.getDragLookupCsvPath();
		this.dragTable = options.getDragLookupTable();
		this.dragSeparator = ','; // Default separator
		this.stabilityCsv = options.getStabilityLookupCsvPath();
		this.stabilityTable = options.getStabilityLookupTable();
		this.stabilitySeparator = ','; // Default separator

		initUI();
		updateDisplays();

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(owner);
	}

	private static void initColors() {
		updateColors();
		UITheme.Theme.addUIThemeChangeListener(AerodynamicLookupDialog::updateColors);
	}

	public static void updateColors() {
		textColor = GUIUtil.getUITheme().getTextColor();
		infoTextColor = GUIUtil.getUITheme().getInformationColor();
	}

	private void initUI() {
		JPanel content = new JPanel(new MigLayout("fill, gap para"));
		setContentPane(content);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab(trans.get("AerodynamicLookupDialog.lbl.drag"),
				createLookupSection(false));
		tabbedPane.addTab(trans.get("AerodynamicLookupDialog.lbl.stability"),
				createLookupSection(true));
		content.add(tabbedPane, "spanx, growx, growy, wrap para");

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancel = new JButton(trans.get("button.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		JButton ok = new JButton(trans.get("button.ok"));
		ok.addActionListener(e -> applyAndClose());
		buttons.add(ok);

		content.add(buttons, "spanx, growx");
	}

	private JPanel createLookupSection(boolean stability) {
		JPanel panel = new JPanel(new MigLayout("fill, ins para, gap para"));

		// Load from file button
		JButton loadButton = new JButton(trans.get("AerodynamicLookupDialog.btn.loadFromFile"));
		panel.add(loadButton);

		// Clear button
		JButton clear = new JButton(trans.get("AerodynamicLookupDialog.btn.clear"));
		panel.add(clear, "wrap");

		// Summary label (on its own row to allow wrapping)
		JLabel summary = new JLabel("<html>" + trans.get("AerodynamicLookupDialog.summary.none") + "</html>");
		summary.setForeground(infoTextColor);
		panel.add(summary, "spanx, growx, wrap");

		// Field separator
		JLabel separatorLabel = new JLabel(trans.get("SimExpPan.lbl.Fieldsepstr"));
		String tip = trans.get("SimExpPan.lbl.longA1") + trans.get("SimExpPan.lbl.longA2");
		separatorLabel.setToolTipText(tip);
		panel.add(separatorLabel, "gapleft para, gapright rel");

		FieldSeparatorComboBox separatorCombo = new FieldSeparatorComboBox();
		separatorCombo.setToolTipText(tip);
		panel.add(separatorCombo, "wrap para");

		// Example format textarea
		String exampleKey = stability ? "AerodynamicLookupDialog.example.stability" : "AerodynamicLookupDialog.example.drag";
		JTextArea example = new JTextArea(trans.get(exampleKey));
		example.setEditable(false);
		example.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN,
				example.getFont().getSize()));
		example.setBackground(Color.WHITE);
		example.setForeground(Color.GRAY);
		example.setFocusable(false);
		example.setRows(3);
		JScrollPane exampleScroll = new JScrollPane(example);
		javax.swing.border.TitledBorder exampleBorder = BorderFactory.createTitledBorder(trans.get("AerodynamicLookupDialog.lbl.formatHint"));
		exampleScroll.setBorder(exampleBorder);
		panel.add(exampleScroll, "spanx, growx, growy");

		if (stability) {
			stabilitySummaryLabel = summary;
			clearStabilityButton = clear;
			stabilitySeparatorCombo = separatorCombo;
			stabilityExampleArea = example;
			stabilityExampleScroll = exampleScroll;
			stabilityExampleBorder = exampleBorder;
			loadButton.addActionListener(e -> chooseStabilityLookup());
			clear.addActionListener(e -> {
				stabilityCsv = null;
				stabilityTable = null;
				updateDisplays();
			});
			separatorCombo.addActionListener(e -> {
				// Just update the example format display, don't reload the file
				// The separator is only used when loading a new file
				updateExampleFormat(true);
			});
		} else {
			dragSummaryLabel = summary;
			clearDragButton = clear;
			dragSeparatorCombo = separatorCombo;
			dragExampleArea = example;
			dragExampleScroll = exampleScroll;
			dragExampleBorder = exampleBorder;
			loadButton.addActionListener(e -> chooseDragLookup());
			clear.addActionListener(e -> {
				dragCsv = null;
				dragTable = null;
				updateDisplays();
			});
			separatorCombo.addActionListener(e -> {
				// Just update the example format display, don't reload the file
				// The separator is only used when loading a new file
				updateExampleFormat(false);
			});
		}

		return panel;
	}


	private void chooseDragLookup() {
		JFileChooser chooser = createCsvFileChooser(dragCsv);
		chooser.setCurrentDirectory(Application.getPreferences().getDefaultDirectory());
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		Application.getPreferences().setDefaultDirectory(chooser.getCurrentDirectory());
		Path selected = chooser.getSelectedFile().toPath();
		try {
			char separator = dragSeparatorCombo.getSeparatorChar();
			MachAoALookup table = CsvMachAoALookup.fromCsv(selected, DRAG_VALUE_COLUMNS, separator);
			dragCsv = normalizePath(selected);
			dragTable = table;
			dragSeparator = separator;
			updateDisplays();
		} catch (RuntimeException ex) {
			showLookupError(selected, ex);
		}
	}

	private void chooseStabilityLookup() {
		JFileChooser chooser = createCsvFileChooser(stabilityCsv);
		chooser.setCurrentDirectory(Application.getPreferences().getDefaultDirectory());
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		Application.getPreferences().setDefaultDirectory(chooser.getCurrentDirectory());
		Path selected = chooser.getSelectedFile().toPath();
		try {
			char separator = stabilitySeparatorCombo.getSeparatorChar();
			MachAoALookup table = CsvMachAoALookup.fromCsv(selected, STABILITY_VALUE_COLUMNS, separator);
			stabilityCsv = normalizePath(selected);
			stabilityTable = table;
			stabilitySeparator = separator;
			updateDisplays();
		} catch (RuntimeException ex) {
			showLookupError(selected, ex);
		}
	}

	private void reloadDragLookup() {
		if (dragCsv == null) {
			return;
		}
		try {
			char separator = dragSeparatorCombo.getSeparatorChar();
			MachAoALookup table = CsvMachAoALookup.fromCsv(dragCsv, DRAG_VALUE_COLUMNS, separator);
			dragTable = table;
			dragSeparator = separator;
			updateDisplays();
		} catch (RuntimeException ex) {
			showLookupError(dragCsv, ex);
		}
	}

	private void reloadStabilityLookup() {
		if (stabilityCsv == null) {
			return;
		}
		try {
			char separator = stabilitySeparatorCombo.getSeparatorChar();
			MachAoALookup table = CsvMachAoALookup.fromCsv(stabilityCsv, STABILITY_VALUE_COLUMNS, separator);
			stabilityTable = table;
			stabilitySeparator = separator;
			updateDisplays();
		} catch (RuntimeException ex) {
			showLookupError(stabilityCsv, ex);
		}
	}

	private void applyAndClose() {
		if (dragCsv == null || dragTable == null) {
			options.clearDragLookup();
		} else {
			options.setDragLookup(dragCsv, dragTable);
		}

		if (stabilityCsv == null || stabilityTable == null) {
			options.clearStabilityLookup();
		} else {
			options.setStabilityLookup(stabilityCsv, stabilityTable);
		}

		dispose();
	}

	private void updateDisplays() {
		updateSectionDisplay(dragCsv, dragTable, dragSummaryLabel, clearDragButton, true);
		updateSectionDisplay(stabilityCsv, stabilityTable, stabilitySummaryLabel, clearStabilityButton, false);
		updateExampleFormat(false);
		updateExampleFormat(true);
	}

	private void updateSectionDisplay(Path path, MachAoALookup table, JLabel summaryLabel,
			JButton clearButton, boolean isDrag) {
		// Update separator combo box if a file is loaded
		if (path != null && table != null) {
			if (isDrag && dragSeparatorCombo != null) {
				dragSeparatorCombo.setSeparatorChar(dragSeparator);
			} else if (!isDrag && stabilitySeparatorCombo != null) {
				stabilitySeparatorCombo.setSeparatorChar(stabilitySeparator);
			}
		}
		if (summaryLabel == null) {
			return;
		}
		if (path == null || table == null) {
			summaryLabel.setText("<html>" + trans.get("AerodynamicLookupDialog.summary.none") + "</html>");
			summaryLabel.setToolTipText(null);
			if (clearButton != null) {
				clearButton.setEnabled(false);
			}
			return;
		}
		String fileName = path.getFileName() != null ? path.getFileName().toString() : path.toString();
		String detail = formatLookupSummary(trans, table);
		String fullText = fileName + " - " + detail;
		// Use HTML to allow text wrapping, and escape HTML special characters
		String escapedText = fullText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		summaryLabel.setText("<html>" + escapedText + "</html>");
		summaryLabel.setToolTipText(fullText);
		if (clearButton != null) {
			clearButton.setEnabled(true);
		}
	}

	private void updateExampleFormat(boolean stability) {
		JTextArea exampleArea = stability ? stabilityExampleArea : dragExampleArea;
		JScrollPane exampleScroll = stability ? stabilityExampleScroll : dragExampleScroll;
		javax.swing.border.TitledBorder exampleBorder = stability ? stabilityExampleBorder : dragExampleBorder;
		FieldSeparatorComboBox separatorCombo = stability ? stabilitySeparatorCombo : dragSeparatorCombo;
		Path csvPath = stability ? stabilityCsv : dragCsv;
		MachAoALookup table = stability ? stabilityTable : dragTable;

		if (exampleArea == null || exampleScroll == null || exampleBorder == null) {
			return;
		}

		char separator = separatorCombo != null ? separatorCombo.getSeparatorChar() : ',';
		String separatorStr = String.valueOf(separator);

		if (csvPath != null && table != null) {
			// Show loaded data
			try {
				List<String> lines = Files.readAllLines(csvPath);
				// Filter out empty lines and comments
				List<String> dataLines = lines.stream()
						.map(String::trim)
						.filter(line -> !line.isEmpty() && !line.startsWith("#"))
						.limit(20) // Limit to first 20 data lines for display
						.collect(Collectors.toList());
				String loadedData = String.join("\n", dataLines);
				exampleArea.setText(loadedData);
				exampleArea.setForeground(Color.BLACK);
				exampleBorder.setTitle(trans.get("AerodynamicLookupDialog.lbl.loadedData"));
			} catch (IOException e) {
				// If we can't read the file, fall back to example
				updateExampleFormatText(exampleArea, exampleBorder, separatorStr, stability);
			}
		} else {
			// Show example format
			updateExampleFormatText(exampleArea, exampleBorder, separatorStr, stability);
		}
		exampleScroll.repaint();
	}

	private void updateExampleFormatText(JTextArea exampleArea, javax.swing.border.TitledBorder exampleBorder,
			String separator, boolean stability) {
		String exampleKey = stability ? "AerodynamicLookupDialog.example.stability" : "AerodynamicLookupDialog.example.drag";
		String exampleText = trans.get(exampleKey);
		// Replace commas with the selected separator
		exampleText = exampleText.replace(",", separator);
		exampleArea.setText(exampleText);
		exampleArea.setForeground(Color.GRAY);
		exampleBorder.setTitle(trans.get("AerodynamicLookupDialog.lbl.formatHint"));
	}

	private JFileChooser createCsvFileChooser(Path currentPath) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(trans.get("AerodynamicLookupDialog.title.choose"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				trans.get("AerodynamicLookupDialog.csvFilter"), "csv", "txt", "dat");
		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (currentPath != null) {
			java.io.File currentFile = currentPath.toFile();
			if (currentFile.getParentFile() != null) {
				chooser.setCurrentDirectory(currentFile.getParentFile());
			}
			chooser.setSelectedFile(currentFile);
		}
		return chooser;
	}

	private void showLookupError(Path path, Exception ex) {
		String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
		JOptionPane.showMessageDialog(this,
				String.format(trans.get("AerodynamicLookupDialog.error.msg"), path, reason),
				trans.get("AerodynamicLookupDialog.error.title"),
				JOptionPane.ERROR_MESSAGE);
	}

	private static Path normalizePath(Path path) {
		return path == null ? null : path.toAbsolutePath().normalize();
	}

	static String formatLookupSummary(Translator translator, MachAoALookup table) {
		String columns = table.getValueColumns().stream()
				.map(name -> name.toUpperCase(Locale.ROOT))
				.collect(java.util.stream.Collectors.joining(", "));
		String machMin = formatDouble(table.getMinMach());
		String machMax = formatDouble(table.getMaxMach());
		if (table.hasAoA()) {
			String aoaMin = formatDouble(table.getMinAoA());
			String aoaMax = formatDouble(table.getMaxAoA());
			return String.format(translator.get("AerodynamicLookupDialog.summaryWithAoA"),
					machMin, machMax, aoaMin, aoaMax, columns);
		}
		return String.format(translator.get("AerodynamicLookupDialog.summaryNoAoA"),
				machMin, machMax, columns);
	}

	private static String formatDouble(double value) {
		DecimalFormat format = new DecimalFormat("0.###");
		return format.format(value);
	}
}
