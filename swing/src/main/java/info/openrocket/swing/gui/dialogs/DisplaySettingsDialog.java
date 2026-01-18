package info.openrocket.swing.gui.dialogs;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.preferences.DocumentPreferences;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.gui.components.ColorChooserButton;
import info.openrocket.swing.gui.theme.UITheme;
import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.util.Icons;
import info.openrocket.swing.gui.util.SwingPreferences;
import net.miginfocom.swing.MigLayout;

import info.openrocket.swing.gui.scalefigure.RocketPanel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;

/**
 * Dialog for configuring design view display settings.
 * Supports setting background colors and text colors for 2D and 3D views.
 * 
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class DisplaySettingsDialog extends JDialog {
	private static final Translator trans = Application.getTranslator();
	private static final SwingPreferences prefs = (SwingPreferences) Application.getPreferences();

	private final DocumentPreferences docPreferences;
	
	private ColorChooserButton color2DButton;
	private ColorChooserButton color3DButton;
	private ColorChooserButton textColor2DButton;
	private ColorChooserButton textColor3DButton;
	
	private JButton reset2DBgButton;
	private JButton reset3DBgButton;
	private JButton reset2DTextButton;
	private JButton reset3DTextButton;
	
	private final Color original2DBgColor;
	private final Color original3DBgColor;
	private final Color original2DTextColor;
	private final Color original3DTextColor;
	
	public DisplaySettingsDialog(Window parent, OpenRocketDocument document) {
		super(parent, trans.get("RocketPanel.dlg.displaySettings.title"), Dialog.ModalityType.APPLICATION_MODAL);

		this.docPreferences = document.getDocumentPreferences();
		
		// Get current colors from document preferences (null if not explicitly set)
		original2DBgColor = docPreferences.getColor(DocumentPreferences.PREF_2D_BACKGROUND_COLOR, null);
		original3DBgColor = docPreferences.getColor(DocumentPreferences.PREF_3D_BACKGROUND_COLOR, null);
		original2DTextColor = docPreferences.getColor(DocumentPreferences.PREF_2D_TEXT_COLOR, null);
		original3DTextColor = docPreferences.getColor(DocumentPreferences.PREF_3D_TEXT_COLOR, null);
		
		init();
	}
	
	private void init() {
		JPanel panel = new JPanel(new MigLayout("fill, ins 15", "[][grow][]"));

		// 2D View background color
		JLabel label2D = new JLabel(trans.get("RocketPanel.dlg.displaySettings.2DBackground"));
		panel.add(label2D, "gapright unrel");
		
		Color initial2DColor = getEffectiveColor(original2DBgColor,
				prefs.getDefault2DBackgroundColor(), 
				UITheme.getColor(UITheme.Keys.BACKGROUND));
		color2DButton = new ColorChooserButton(initial2DColor);
		color2DButton.addColorPropertyChangeListener(e -> {
			Color newColor = (Color) e.getNewValue();
			saveColorIfDifferent(DocumentPreferences.PREF_2D_BACKGROUND_COLOR, newColor, 
					UITheme.getColor(UITheme.Keys.BACKGROUND));
			updateResetButtonState(reset2DBgButton, newColor,
					prefs.getDefault2DBackgroundColor(), UITheme.getColor(UITheme.Keys.BACKGROUND));
			update2DView();
		});
		panel.add(color2DButton, "growx");
		
		reset2DBgButton = createResetButton(color2DButton, DocumentPreferences.PREF_2D_BACKGROUND_COLOR,
				prefs.getDefault2DBackgroundColor(), UITheme.getColor(UITheme.Keys.BACKGROUND),
				this::update2DView);
		panel.add(reset2DBgButton, "wrap");

		// 2D View text color
		JLabel label2DText = new JLabel(trans.get("RocketPanel.dlg.displaySettings.2DTextColor"));
		panel.add(label2DText, "gapright unrel");

		Color initial2DTextColor = getEffectiveColor(original2DTextColor,
				prefs.getDefault2DTextColor(),
				UITheme.getColor(UITheme.Keys.TEXT));
		textColor2DButton = new ColorChooserButton(initial2DTextColor);
		textColor2DButton.addColorPropertyChangeListener(e -> {
			Color newColor = (Color) e.getNewValue();
			saveColorIfDifferent(DocumentPreferences.PREF_2D_TEXT_COLOR, newColor,
					UITheme.getColor(UITheme.Keys.TEXT));
			updateResetButtonState(reset2DTextButton, newColor,
					prefs.getDefault2DTextColor(), UITheme.getColor(UITheme.Keys.TEXT));
			updateTextColors();
		});
		panel.add(textColor2DButton, "growx");

		reset2DTextButton = createResetButton(textColor2DButton, DocumentPreferences.PREF_2D_TEXT_COLOR,
				prefs.getDefault2DTextColor(), UITheme.getColor(UITheme.Keys.TEXT),
				this::updateTextColors);
		panel.add(reset2DTextButton, "wrap para");
		
		// 3D View background color
		JLabel label3D = new JLabel(trans.get("RocketPanel.dlg.displaySettings.3DBackground"));
		panel.add(label3D, "gapright unrel");
		
		Color initial3DColor = getEffectiveColor(original3DBgColor, 
				prefs.getDefault3DBackgroundColor(), 
				UITheme.getColor(UITheme.Keys.BACKGROUND));
		color3DButton = new ColorChooserButton(initial3DColor);
		color3DButton.addColorPropertyChangeListener(e -> {
			Color newColor = (Color) e.getNewValue();
			saveColorIfDifferent(DocumentPreferences.PREF_3D_BACKGROUND_COLOR, newColor, 
					UITheme.getColor(UITheme.Keys.BACKGROUND));
			updateResetButtonState(reset3DBgButton, newColor, 
					prefs.getDefault3DBackgroundColor(), UITheme.getColor(UITheme.Keys.BACKGROUND));
			update3DView();
		});
		panel.add(color3DButton, "growx");
		
		reset3DBgButton = createResetButton(color3DButton, DocumentPreferences.PREF_3D_BACKGROUND_COLOR,
				prefs.getDefault3DBackgroundColor(), UITheme.getColor(UITheme.Keys.BACKGROUND),
				this::update3DView);
		panel.add(reset3DBgButton, "wrap");
		
		// 3D View text color
		JLabel label3DText = new JLabel(trans.get("RocketPanel.dlg.displaySettings.3DTextColor"));
		panel.add(label3DText, "gapright unrel");
		
		Color initial3DTextColor = getEffectiveColor(original3DTextColor, 
				prefs.getDefault3DTextColor(), 
				UITheme.getColor(UITheme.Keys.TEXT));
		textColor3DButton = new ColorChooserButton(initial3DTextColor);
		textColor3DButton.addColorPropertyChangeListener(e -> {
			Color newColor = (Color) e.getNewValue();
			saveColorIfDifferent(DocumentPreferences.PREF_3D_TEXT_COLOR, newColor, 
					UITheme.getColor(UITheme.Keys.TEXT));
			updateResetButtonState(reset3DTextButton, newColor, 
					prefs.getDefault3DTextColor(), UITheme.getColor(UITheme.Keys.TEXT));
			updateTextColors();
		});
		panel.add(textColor3DButton, "growx");
		
		reset3DTextButton = createResetButton(textColor3DButton, DocumentPreferences.PREF_3D_TEXT_COLOR,
				prefs.getDefault3DTextColor(), UITheme.getColor(UITheme.Keys.TEXT),
				this::updateTextColors);
		panel.add(reset3DTextButton, "wrap para*2");
		
		// Buttons
		JButton saveAsDefaultButton = new JButton(trans.get("RocketPanel.btn.saveAsDefault"));
		saveAsDefaultButton.setToolTipText(trans.get("RocketPanel.btn.saveAsDefault.ttip"));
		saveAsDefaultButton.addActionListener(e -> {
			saveAsDefaults();
		});
		panel.add(saveAsDefaultButton, "span 3, split 2, right");
		
		JButton okButton = new JButton(trans.get("button.ok"));
		okButton.addActionListener(e -> {
			dispose();
		});
		panel.add(okButton, "gap para");
		
		JButton cancelButton = new JButton(trans.get("button.cancel"));
		cancelButton.addActionListener(e -> {
			// Revert to original colors (null means not set, will use theme/default)
			docPreferences.putColor(DocumentPreferences.PREF_2D_BACKGROUND_COLOR, original2DBgColor);
			docPreferences.putColor(DocumentPreferences.PREF_3D_BACKGROUND_COLOR, original3DBgColor);
			docPreferences.putColor(DocumentPreferences.PREF_2D_TEXT_COLOR, original2DTextColor);
			docPreferences.putColor(DocumentPreferences.PREF_3D_TEXT_COLOR, original3DTextColor);
			update2DView();
			update3DView();
			updateTextColors();
			dispose();
		});
		panel.add(cancelButton, "gap para");
		
		add(panel);
		pack();
		setLocationRelativeTo(getParent());
		
		GUIUtil.setDisposableDialogOptions(this, okButton);
		
		// Initialize reset button states
		updateResetButtonState(reset2DBgButton, color2DButton.getSelectedColor(), 
				prefs.getDefault2DBackgroundColor(), UITheme.getColor(UITheme.Keys.BACKGROUND));
		updateResetButtonState(reset3DBgButton, color3DButton.getSelectedColor(), 
				prefs.getDefault3DBackgroundColor(), UITheme.getColor(UITheme.Keys.BACKGROUND));
		updateResetButtonState(reset2DTextButton, textColor2DButton.getSelectedColor(), 
				prefs.getDefault2DTextColor(), UITheme.getColor(UITheme.Keys.TEXT));
		updateResetButtonState(reset3DTextButton, textColor3DButton.getSelectedColor(), 
				prefs.getDefault3DTextColor(), UITheme.getColor(UITheme.Keys.TEXT));
	}
	
	/**
	 * Get the effective color: document preference -> SwingPreferences default -> theme default
	 */
	private Color getEffectiveColor(Color docColor, Color defaultColor, Color themeDefault) {
		if (docColor != null) {
			return docColor;
		}
		if (defaultColor != null) {
			return defaultColor;
		}
		return themeDefault;
	}
	
	/**
	 * Save color to document preferences only if different from theme default.
	 */
	private void saveColorIfDifferent(String prefKey, Color newColor, Color themeDefault) {
		if (newColor != null && !newColor.equals(themeDefault)) {
			docPreferences.putColor(prefKey, newColor);
		} else {
			docPreferences.putColor(prefKey, null);
		}
	}
	
	/**
	 * Create a reset button for a color chooser.
	 */
	private JButton createResetButton(ColorChooserButton colorButton, String prefKey, 
			Color defaultColor, Color themeDefault, Runnable updateAction) {
		JButton resetButton = new JButton(Icons.RESET);
		resetButton.setToolTipText(trans.get("RocketPanel.btn.reset.ttip"));
		resetButton.addActionListener(e -> {
			Color effectiveDefault = getEffectiveColor(null, defaultColor, themeDefault);
			colorButton.setSelectedColor(effectiveDefault);
			docPreferences.putColor(prefKey, null);
			updateResetButtonState(resetButton, effectiveDefault, defaultColor, themeDefault);
			updateAction.run();
		});
		
		// Set initial button state based on current color
		Color currentColor = colorButton.getSelectedColor();
		updateResetButtonState(resetButton, currentColor, defaultColor, themeDefault);
		
		return resetButton;
	}
	
	/**
	 * Update the reset button's enabled state based on whether the color is at default.
	 */
	private void updateResetButtonState(JButton resetButton, Color currentColor, 
			Color defaultColor, Color themeDefault) {
		Color effectiveDefault = getEffectiveColor(null, defaultColor, themeDefault);
		boolean isAtDefault = currentColor != null && currentColor.equals(effectiveDefault);
		resetButton.setEnabled(!isAtDefault);
	}
	
	/**
	 * Save current colors as defaults to SwingPreferences.
	 */
	private void saveAsDefaults() {
		// Get current effective colors from buttons (which show the effective color)
		Color current2DBg = color2DButton.getSelectedColor();
		Color current3DBg = color3DButton.getSelectedColor();
		Color current2DText = textColor2DButton.getSelectedColor();
		Color current3DText = textColor3DButton.getSelectedColor();
		
		// Only save if different from theme defaults
		saveDefaultIfDifferent(current2DBg, 
				UITheme.getColor(UITheme.Keys.BACKGROUND), prefs::setDefault2DBackgroundColor);
		saveDefaultIfDifferent(current3DBg, 
				UITheme.getColor(UITheme.Keys.BACKGROUND), prefs::setDefault3DBackgroundColor);
		saveDefaultIfDifferent(current2DText, 
				UITheme.getColor(UITheme.Keys.TEXT), prefs::setDefault2DTextColor);
		saveDefaultIfDifferent(current3DText, 
				UITheme.getColor(UITheme.Keys.TEXT), prefs::setDefault3DTextColor);
	}
	
	/**
	 * Helper to save default color only if different from theme default.
	 */
	private void saveDefaultIfDifferent(Color currentColor, Color themeDefault, 
			java.util.function.Consumer<Color> setter) {
		if (currentColor != null && !currentColor.equals(themeDefault)) {
			setter.accept(currentColor);
		} else {
			setter.accept(null);
		}
	}
	
	private void update2DView() {
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null) {
			RocketPanel panel = findRocketPanel(window);
			if (panel != null) {
				panel.updateBackgroundColors();
				panel.updateTextColors();
				panel.getFigure().repaint();
			}
		}
	}
	
	private void update3DView() {
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null) {
			RocketPanel panel = findRocketPanel(window);
			if (panel != null) {
				panel.updateBackgroundColors();
				panel.updateTextColors();
				panel.getFigure3d().repaint();
			}
		}
	}
	
	private void updateTextColors() {
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null) {
			RocketPanel panel = findRocketPanel(window);
			if (panel != null) {
				panel.updateTextColors();
				panel.getFigure().repaint();
				panel.getFigure3d().repaint();
			}
		}
	}
	
	/**
	 * Find RocketPanel in the component hierarchy.
	 */
	private RocketPanel findRocketPanel(Component component) {
		if (component instanceof RocketPanel) {
			return (RocketPanel) component;
		}
		if (component instanceof java.awt.Container) {
			java.awt.Container container = (java.awt.Container) component;
			for (Component child : container.getComponents()) {
				RocketPanel panel = findRocketPanel(child);
				if (panel != null) {
					return panel;
				}
			}
		}
		return null;
	}
}