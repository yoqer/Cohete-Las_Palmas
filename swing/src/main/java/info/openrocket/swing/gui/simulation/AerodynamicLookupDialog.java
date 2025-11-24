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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

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

	private JTextField dragPathField;
	private JLabel dragSummaryLabel;
	private JButton clearDragButton;
	private FieldSeparatorComboBox dragSeparatorCombo;

	private JTextField stabilityPathField;
	private JLabel stabilitySummaryLabel;
	private JButton clearStabilityButton;
	private FieldSeparatorComboBox stabilitySeparatorCombo;

	private static Color textColor;
	private static Color infoTextColor;

	static {
		initColors();
	}

	public AerodynamicLookupDialog(Window owner, SimulationOptions options) {
		super(owner, trans.get("simedtdlg.AeroLookup.title"), ModalityType.APPLICATION_MODAL);
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
		JPanel content = new JPanel(new MigLayout("fill, insets dialog, gap para"));
		setContentPane(content);

		content.add(createLookupSection(trans.get("simedtdlg.AeroLookup.dragLabel"), false),
				"spanx, growx, wrap rel");
		content.add(createLookupSection(trans.get("simedtdlg.AeroLookup.stabilityLabel"), true),
				"spanx, growx, wrap para");

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancel = new JButton(trans.get("button.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		JButton ok = new JButton(trans.get("button.ok"));
		ok.addActionListener(e -> applyAndClose());
		buttons.add(ok);

		content.add(buttons, "spanx, growx");
	}

	private JPanel createLookupSection(String labelText, boolean stability) {
		JPanel panel = new JPanel(new MigLayout("fillx, insets 0, gap rel"));
		panel.setBorder(BorderFactory.createTitledBorder(labelText));

		JTextField pathField = new JTextField();
		pathField.setEditable(false);
		pathField.setColumns(25);
		panel.add(pathField, "growx, pushx");

		JButton browse = new JButton(trans.get("simedtdlg.AeroLookup.browse"));
		panel.add(browse);

		JButton clear = new JButton(trans.get("simedtdlg.AeroLookup.clear"));
		panel.add(clear, "wrap");

		JLabel separatorLabel = new JLabel(trans.get("SimExpPan.lbl.Fieldsepstr"));
		String tip = trans.get("SimExpPan.lbl.longA1") + trans.get("SimExpPan.lbl.longA2");
		separatorLabel.setToolTipText(tip);
		panel.add(separatorLabel, "gapleft para, gapright rel");

		FieldSeparatorComboBox separatorCombo = new FieldSeparatorComboBox();
		separatorCombo.setToolTipText(tip);
		panel.add(separatorCombo, "wrap");

		JLabel summary = new JLabel(trans.get("simedtdlg.AeroLookup.summary.none"));
		summary.setForeground(infoTextColor);
		panel.add(summary, "spanx, growx, wrap para");

		// Example format textarea
		String exampleKey = stability ? "simedtdlg.AeroLookup.example.stability" : "simedtdlg.AeroLookup.example.drag";
		JTextArea example = new JTextArea(trans.get(exampleKey));
		example.setEditable(false);
		example.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN,
				example.getFont().getSize()));
		example.setBackground(Color.WHITE);
		example.setForeground(Color.BLACK);
		example.setFocusable(false);
		example.setRows(3);
		JScrollPane exampleScroll = new JScrollPane(example);
		exampleScroll.setBorder(BorderFactory.createTitledBorder(trans.get("simedtdlg.AeroLookup.formatHint")));
		panel.add(exampleScroll, "spanx, growx");

		if (stability) {
			stabilityPathField = pathField;
			stabilitySummaryLabel = summary;
			clearStabilityButton = clear;
			stabilitySeparatorCombo = separatorCombo;
			browse.addActionListener(e -> chooseStabilityLookup());
			clear.addActionListener(e -> {
				stabilityCsv = null;
				stabilityTable = null;
				updateDisplays();
			});
			separatorCombo.addActionListener(e -> {
				if (stabilityCsv != null && stabilityTable != null) {
					reloadStabilityLookup();
				}
			});
		} else {
			dragPathField = pathField;
			dragSummaryLabel = summary;
			clearDragButton = clear;
			dragSeparatorCombo = separatorCombo;
			browse.addActionListener(e -> chooseDragLookup());
			clear.addActionListener(e -> {
				dragCsv = null;
				dragTable = null;
				updateDisplays();
			});
			separatorCombo.addActionListener(e -> {
				if (dragCsv != null && dragTable != null) {
					reloadDragLookup();
				}
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
		updateSectionDisplay(dragCsv, dragTable, dragPathField, dragSummaryLabel, clearDragButton);
		updateSectionDisplay(stabilityCsv, stabilityTable, stabilityPathField, stabilitySummaryLabel,
				clearStabilityButton);
	}

	private void updateSectionDisplay(Path path, MachAoALookup table, JTextField field, JLabel summaryLabel,
			JButton clearButton) {
		// Update separator combo box if a file is loaded
		if (path != null && table != null) {
			if (field == dragPathField && dragSeparatorCombo != null) {
				dragSeparatorCombo.setSeparatorChar(dragSeparator);
			} else if (field == stabilityPathField && stabilitySeparatorCombo != null) {
				stabilitySeparatorCombo.setSeparatorChar(stabilitySeparator);
			}
		}
		if (field == null || summaryLabel == null) {
			return;
		}
		if (path == null || table == null) {
			field.setText("");
			summaryLabel.setText(trans.get("simedtdlg.AeroLookup.summary.none"));
			if (clearButton != null) {
				clearButton.setEnabled(false);
			}
			return;
		}
		field.setText(path.toString());
		if (clearButton != null) {
			clearButton.setEnabled(true);
		}
		summaryLabel.setText(formatLookupSummary(trans, table));
	}

	private JFileChooser createCsvFileChooser(Path currentPath) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(trans.get("simedtdlg.AeroLookup.chooseTitle"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				trans.get("simedtdlg.AeroLookup.csvFilter"), "csv", "txt", "dat");
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
				String.format(trans.get("simedtdlg.AeroLookup.error"), path, reason),
				trans.get("simedtdlg.AeroLookup.errorTitle"),
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
			return String.format(translator.get("simedtdlg.AeroLookup.summaryWithAoA"),
					machMin, machMax, aoaMin, aoaMax, columns);
		}
		return String.format(translator.get("simedtdlg.AeroLookup.summaryNoAoA"),
				machMin, machMax, columns);
	}

	private static String formatDouble(double value) {
		DecimalFormat format = new DecimalFormat("0.###");
		return format.format(value);
	}
}
