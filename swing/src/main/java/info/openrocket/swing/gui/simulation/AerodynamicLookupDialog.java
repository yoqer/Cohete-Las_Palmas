package info.openrocket.swing.gui.simulation;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import info.openrocket.core.aerodynamics.lookup.CsvMachAoALookup;
import info.openrocket.core.aerodynamics.lookup.MachAoALookup;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.simulation.SimulationOptions;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.gui.components.DescriptionArea;
import info.openrocket.swing.gui.components.StyledLabel;
import info.openrocket.swing.gui.components.StyledLabel.Style;
import info.openrocket.swing.gui.util.GUIUtil;
import net.miginfocom.swing.MigLayout;

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
	private Path stabilityCsv;
	private MachAoALookup stabilityTable;

	private JTextField dragPathField;
	private JLabel dragSummaryLabel;
	private JButton clearDragButton;

	private JTextField stabilityPathField;
	private JLabel stabilitySummaryLabel;
	private JButton clearStabilityButton;

	private final Color textColor;
	private final Color dimTextColor;

	AerodynamicLookupDialog(Window owner, SimulationOptions options) {
		super(owner, trans.get("simedtdlg.AeroLookup.title"), ModalityType.APPLICATION_MODAL);
		this.options = options;

		this.dragCsv = options.getDragLookupCsvPath();
		this.dragTable = options.getDragLookupTable();
		this.stabilityCsv = options.getStabilityLookupCsvPath();
		this.stabilityTable = options.getStabilityLookupTable();

		Color text = GUIUtil.getUITheme().getTextColor();
		Color dim = GUIUtil.getUITheme().getDimTextColor();
		this.textColor = text != null ? text : Color.BLACK;
		this.dimTextColor = dim != null ? dim : Color.GRAY;

		initUI();
		updateDisplays();

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(owner);
	}

	private void initUI() {
		JPanel content = new JPanel(new MigLayout("fill, insets dialog, gap para"));
		setContentPane(content);

		DescriptionArea desc = new DescriptionArea(trans.get("simedtdlg.AeroLookup.desc"), 4);
		content.add(desc, "spanx, growx, wrap para");

		content.add(createLookupSection(trans.get("simedtdlg.AeroLookup.dragLabel"), false),
				"spanx, growx, wrap rel");
		content.add(createLookupSection(trans.get("simedtdlg.AeroLookup.stabilityLabel"), true),
				"spanx, growx, wrap para");

		JTextArea hint = new JTextArea(trans.get("simedtdlg.AeroLookup.formatHint"));
		hint.setEditable(false);
		hint.setWrapStyleWord(true);
		hint.setLineWrap(true);
		hint.setOpaque(false);
		hint.setFocusable(false);
		content.add(hint, "spanx, growx, wrap para");

		content.add(new StyledLabel(trans.get("simedtdlg.AeroLookup.exampleTitle"), Style.BOLD), "spanx, wrap 0");
		JTextArea example = new JTextArea(trans.get("simedtdlg.AeroLookup.example"));
		example.setEditable(false);
		example.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN,
				example.getFont().getSize()));
		example.setOpaque(false);
		example.setFocusable(false);
		content.add(example, "spanx, growx, wrap para");

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
		JPanel panel = new JPanel(new MigLayout("fillx, insets 0, gap rel 0"));
		panel.setBorder(BorderFactory.createTitledBorder(labelText));

		JTextField pathField = new JTextField();
		pathField.setEditable(false);
		pathField.setColumns(28);
		panel.add(pathField, "growx, pushx");

		JButton browse = new JButton(trans.get("simedtdlg.AeroLookup.browse"));
		panel.add(browse);

		JButton clear = new JButton(trans.get("simedtdlg.AeroLookup.clear"));
		panel.add(clear, "wrap");

		JLabel summary = new JLabel(trans.get("simedtdlg.AeroLookup.summary.none"));
		summary.setForeground(dimTextColor);
		panel.add(summary, "spanx, growx");

		if (stability) {
			stabilityPathField = pathField;
			stabilitySummaryLabel = summary;
			clearStabilityButton = clear;
			browse.addActionListener(e -> chooseStabilityLookup());
			clear.addActionListener(e -> {
				stabilityCsv = null;
				stabilityTable = null;
				updateDisplays();
			});
		} else {
			dragPathField = pathField;
			dragSummaryLabel = summary;
			clearDragButton = clear;
			browse.addActionListener(e -> chooseDragLookup());
			clear.addActionListener(e -> {
				dragCsv = null;
				dragTable = null;
				updateDisplays();
			});
		}

		return panel;
	}

	private void chooseDragLookup() {
		JFileChooser chooser = createCsvFileChooser(dragCsv);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		Path selected = chooser.getSelectedFile().toPath();
		try {
			MachAoALookup table = CsvMachAoALookup.fromCsv(selected, DRAG_VALUE_COLUMNS);
			dragCsv = normalizePath(selected);
			dragTable = table;
			updateDisplays();
		} catch (RuntimeException ex) {
			showLookupError(selected, ex);
		}
	}

	private void chooseStabilityLookup() {
		JFileChooser chooser = createCsvFileChooser(stabilityCsv);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		Path selected = chooser.getSelectedFile().toPath();
		try {
			MachAoALookup table = CsvMachAoALookup.fromCsv(selected, STABILITY_VALUE_COLUMNS);
			stabilityCsv = normalizePath(selected);
			stabilityTable = table;
			updateDisplays();
		} catch (RuntimeException ex) {
			showLookupError(selected, ex);
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
		if (field == null || summaryLabel == null) {
			return;
		}
		if (path == null || table == null) {
			field.setText("");
			summaryLabel.setText(trans.get("simedtdlg.AeroLookup.summary.none"));
			summaryLabel.setForeground(dimTextColor);
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
		summaryLabel.setForeground(textColor);
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
