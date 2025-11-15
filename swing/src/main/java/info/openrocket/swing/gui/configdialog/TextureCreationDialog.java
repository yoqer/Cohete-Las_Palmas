package info.openrocket.swing.gui.configdialog;

import info.openrocket.core.l10n.Translator;
import info.openrocket.core.preferences.ApplicationPreferences;
import info.openrocket.core.rocketcomponent.FinSet;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.startup.Application;
import net.miginfocom.swing.MigLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.text.Normalizer;
import java.util.Locale;

public class TextureCreationDialog {
	private static final Translator trans = Application.getTranslator();
	private static final ApplicationPreferences prefs = Application.getPreferences();

	private final Component parent;
	private final RocketComponent component;
	private final JPanel dialogPanel;
	private final JSpinner dpiSpinner;
	private final JTextField fileNameField;
	private final JCheckBox outlineCheckbox;
	private final JSpinner outlineWidthSpinner;

	public TextureCreationDialog(Component parent, RocketComponent component) {
		this.parent = parent;
		this.component = component;
		dialogPanel = new JPanel(new MigLayout("ins 0", "[right][grow]", "[][][][]"));

		SpinnerNumberModel dpiModel = new SpinnerNumberModel(prefs.getTextureGenerationDPI(), 10d, 2000d, 10d);
		dpiSpinner = new JSpinner(dpiModel);
		dpiSpinner.setEditor(new JSpinner.NumberEditor(dpiSpinner, "0"));
		JLabel dpiLabel = new JLabel(trans.get("AppearanceCfg.createTexture.lbl.dpi"));
		dpiLabel.setToolTipText(trans.get("AppearanceCfg.createTexture.lbl.ttip.dpi"));
		dpiSpinner.setToolTipText(trans.get("AppearanceCfg.createTexture.lbl.ttip.dpi"));
		dialogPanel.add(dpiLabel);
		dialogPanel.add(dpiSpinner, "wrap");

		fileNameField = new JTextField(20);
		fileNameField.setText(defaultFileName());
		dialogPanel.add(new JLabel(trans.get("AppearanceCfg.createTexture.lbl.filename")));
		dialogPanel.add(fileNameField, "growx, wrap");

		if (component instanceof FinSet) {
			outlineCheckbox = new JCheckBox(trans.get("AppearanceCfg.createTexture.lbl.outline"));
			outlineCheckbox.setSelected(prefs.isTextureGenerationDrawOutline());
			outlineCheckbox.setToolTipText(trans.get("AppearanceCfg.createTexture.lbl.ttip.outline"));
			dialogPanel.add(outlineCheckbox, "span 2, wrap");

			// Outline width
			int storedWidth = prefs.getTextureGenerationOutlinePx();
			SpinnerNumberModel outlineModel = new SpinnerNumberModel(Math.max(0, storedWidth), 0, 50, 1);
			outlineWidthSpinner = new JSpinner(outlineModel);
			JLabel outlineWidthLabel = new JLabel(trans.get("AppearanceCfg.createTexture.lbl.outlineWidth"));
			String outlineWidthTooltip = trans.get("AppearanceCfg.createTexture.lbl.ttip.outlineWidth");
			outlineWidthLabel.setToolTipText(outlineWidthTooltip);
			outlineWidthSpinner.setToolTipText(outlineWidthTooltip);
			boolean outlineEnabled = outlineCheckbox.isSelected();
			outlineWidthLabel.setEnabled(outlineEnabled);
			outlineWidthSpinner.setEnabled(outlineEnabled);
			outlineCheckbox.addActionListener(e -> {
				boolean enabled = outlineCheckbox.isSelected();
				outlineWidthLabel.setEnabled(enabled);
				outlineWidthSpinner.setEnabled(enabled);
			});
			dialogPanel.add(outlineWidthLabel);
			dialogPanel.add(outlineWidthSpinner, "wrap");
		} else {
			outlineCheckbox = null;
			outlineWidthSpinner = null;
		}
	}

	public TextureCreationParameters showDialog() {
		int option = JOptionPane.showConfirmDialog(parent, dialogPanel,
				trans.get("AppearanceCfg.createTexture.dialog.title"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (option != JOptionPane.OK_OPTION) {
			return null;
		}

		try {
			dpiSpinner.commitEdit();
		} catch (java.text.ParseException ex) {
			AppearancePanel.showCreateTextureError(parent,
					trans.get("AppearanceCfg.createTexture.msg.invalidDpi"));
			return null;
		}

		String requestedFileName = slugifyFileName(fileNameField.getText());
		if (requestedFileName.isEmpty()) {
			AppearancePanel.showCreateTextureError(parent,
					trans.get("AppearanceCfg.createTexture.msg.invalidFilename"));
			return null;
		}

		// Save preferences
		prefs.setTextureGenerationDPI((Double) dpiSpinner.getValue());
		if (outlineCheckbox != null) {
			prefs.setTextureGenerationDrawOutline(outlineCheckbox.isSelected());
			prefs.setTextureGenerationOutlinePx(((Number) outlineWidthSpinner.getValue()).intValue());
		}

		boolean drawOutline = outlineCheckbox != null && outlineCheckbox.isSelected();
		int outlineWidth = outlineWidthSpinner != null ? ((Number) outlineWidthSpinner.getValue()).intValue() : 0;
		return new TextureCreationParameters(((Number) dpiSpinner.getValue()).doubleValue(),
				requestedFileName, drawOutline, outlineWidth);
	}

	private String defaultFileName() {
		String name = component.getName();
		if (name == null || name.trim().isEmpty()) {
			name = component.getComponentName();
		}
		if (name == null) {
			name = "";
		}
		String slug = slugifyFileName(name);
		return slug.isEmpty() ? "texture" : slug;
	}

	private String slugifyFileName(String value) {
		if (value == null) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");
	}

	public record TextureCreationParameters(double dpi, String fileName, boolean drawFinOutline, int outlineWidthPx) { }
}
