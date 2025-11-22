package info.openrocket.swing.gui.scalefigure.caliper;

import info.openrocket.core.l10n.Translator;
import info.openrocket.core.preferences.ApplicationPreferences;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.Unit;
import info.openrocket.core.util.StateChangeListener;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.components.DescriptionArea;
import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.util.Icons;
import info.openrocket.swing.gui.widgets.IconToggleButton;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import java.util.function.Consumer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Dialog for managing caliper settings and controls.
 * Contains the caliper mode toggle, distance display, position controls, and snap buttons.
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class CaliperDialog extends JDialog {
	private static final Translator trans = Application.getTranslator();
	private static final ApplicationPreferences preferences = Application.getPreferences();
	
	private final CaliperManager caliperManager;
	private boolean isOpen = false;
	private boolean minimized = false;
	
	// Components that should be hidden when minimized
	private final JPanel mainPanel;
	private final JLabel distanceLabel;

	// Minimized panel (only distance)
	private final JPanel minimizedPanel;
	
	// Distance panel (shared between main and minimized views)
	private final JPanel distancePanel;
	
	// Help components
	private DescriptionArea componentInfo;
	private IconToggleButton infoBtn;
	
	/**
	 * Create a new CaliperDialog.
	 *
	 * @param owner the parent window
	 * @param caliperManager the caliper manager instance
	 */
	public CaliperDialog(Window owner, CaliperManager caliperManager) {
		super(owner, trans.get("CaliperDialog.title"), Dialog.ModalityType.MODELESS);
		this.caliperManager = caliperManager;
		
		// Hide dialog when closed instead of disposing
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		
		// Track dialog state and disable caliper when closing
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				isOpen = false;
				// Exit snap mode if active
				if (caliperManager.isSnapModeActive()) {
					caliperManager.exitSnapMode();
				}
				// Disable caliper when dialog closes
				caliperManager.setEnabled(false);
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
				// Keep dialog open but track state
			}
		});
		
		// Build the dialog content
		mainPanel = new JPanel(new MigLayout("fill, insets dialog, hidemode 3", "[grow]", "[]rel[]para[]"));
		
		// Mode selection: Radio buttons for Vertical/Horizontal
		JLabel modeLabel = new JLabel(trans.get("CaliperDialog.lbl.mode"));
		mainPanel.add(modeLabel);
		ButtonGroup modeGroup = new ButtonGroup();
		JRadioButton verticalRadio = new JRadioButton(trans.get("CaliperDialog.radio.vertical"));
		JRadioButton horizontalRadio = new JRadioButton(trans.get("CaliperDialog.radio.horizontal"));
		modeGroup.add(verticalRadio);
		modeGroup.add(horizontalRadio);
		
		// Set initial selection based on current mode
		CaliperManager.CaliperMode currentMode = caliperManager.getMode();
		verticalRadio.setSelected(currentMode == CaliperManager.CaliperMode.VERTICAL);
		horizontalRadio.setSelected(currentMode == CaliperManager.CaliperMode.HORIZONTAL);
		
		mainPanel.add(verticalRadio, "gapleft para, split 2");
		mainPanel.add(horizontalRadio, "wrap para");
		
		// Distance and unit in a colored border panel
		Color caliperColor = GUIUtil.getUITheme().getCaliperColor();
		distancePanel = new JPanel(new MigLayout("ins 0"));
		distancePanel.setOpaque(false);
		Border caliperBorder = new LineBorder(caliperColor, 1);
		distancePanel.setBorder(new CompoundBorder(caliperBorder, new EmptyBorder(5, 5, 5, 5)));
		
		// Use a JTextField instead of spinner for better color control
		JTextField distanceField = new JTextField();
		distanceField.setEditable(false);
		distanceField.setBorder(null);
		distanceField.setOpaque(false);
		distanceField.setForeground(caliperColor);
		// Increase font size for the distance result
		Font currentFont = distanceField.getFont();
		Font largerFont = currentFont.deriveFont(currentFont.getSize() + 2f);
		distanceField.setFont(largerFont);
		
		// Get unit selector and set same font size and color
		JComponent unitSelector = caliperManager.getUnitSelector();
		unitSelector.setFont(largerFont);
		unitSelector.setForeground(caliperColor);
		
		// Update the distance field when the model or unit changes
		Consumer<Object> updateDistance = (e) -> {
			DoubleModel distanceModel = caliperManager.getCurrentDistanceModel();
			if (distanceModel != null) {
				double value = distanceModel.getValue();
				Unit unit = distanceModel.getCurrentUnit();
				String formatted = unit.toString(value);
				distanceField.setText(formatted);
			}
		};
		
		// Store the current listener and model so we can remove it when mode changes
		final StateChangeListener[] currentListener = new StateChangeListener[1];
		final DoubleModel[] currentModel = new DoubleModel[1];
		
		// Function to set up listener for current model
		Consumer<Void> setupListener = (v) -> {
			// Remove old listener if it exists
			if (currentListener[0] != null && currentModel[0] != null) {
				currentModel[0].removeChangeListener(currentListener[0]);
			}
			// Add new listener
			currentModel[0] = caliperManager.getCurrentDistanceModel();
			currentListener[0] = new StateChangeListener() {
				@Override
				public void stateChanged(EventObject e) {
					updateDistance.accept(e);
				}
			};
			currentModel[0].addChangeListener(currentListener[0]);
			updateDistance.accept(null);
		};
		
		// Initial listener setup
		setupListener.accept(null);
		
		// Add listeners to update mode and refresh distance field
		verticalRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					caliperManager.setMode(CaliperManager.CaliperMode.VERTICAL);
					setupListener.accept(null);
				}
			}
		});
		horizontalRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					caliperManager.setMode(CaliperManager.CaliperMode.HORIZONTAL);
					setupListener.accept(null);
				}
			}
		});
		
		// Listen to unit selector changes
		caliperManager.getUnitSelector().addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					updateDistance.accept(e);
				}
			}
		});
		
		distancePanel.add(distanceField, "split 2, aligny center");
		distancePanel.add(unitSelector, "gapright unrel");
		
		distanceLabel = new JLabel(trans.get("CaliperDialog.lbl.distance"));
		mainPanel.add(distanceLabel);
		mainPanel.add(distancePanel, "spanx, wrap para");
		
		// Caliper 1 position row
		mainPanel.add(new JLabel(String.format(trans.get("CaliperDialog.lbl.caliperPosition"), 1)));
		JPanel caliper1Panel = new JPanel(new MigLayout("ins 0, fillx", "[][grow][]", ""));
		caliper1Panel.setOpaque(false);
		JSpinner caliper1Spinner = caliperManager.getCaliper1PositionSpinner();
		caliper1Panel.add(caliper1Spinner, "growx");
		
		// Snap button with text - use the actual toggle button but make it show text
		JToggleButton snap1Button = caliperManager.getCaliper1SnapButton();
		// Set text on the toggle button
		snap1Button.setText(trans.get("CaliperDialog.btn.snap"));
		snap1Button.setToolTipText(String.format(trans.get("CaliperDialog.btn.snap.ttip"), 1));
		caliper1Panel.add(snap1Button, "gapleft rel");
		mainPanel.add(caliper1Panel, "growx, wrap");
		
		// Caliper 2 position row
		mainPanel.add(new JLabel(String.format(trans.get("CaliperDialog.lbl.caliperPosition"), 2)));
		JPanel caliper2Panel = new JPanel(new MigLayout("ins 0, fillx", "[][grow][]", ""));
		caliper2Panel.setOpaque(false);
		JSpinner caliper2Spinner = caliperManager.getCaliper2PositionSpinner();
		caliper2Panel.add(caliper2Spinner, "growx");
		
		// Snap button with text - use the actual toggle button but make it show text
		JToggleButton snap2Button = caliperManager.getCaliper2SnapButton();
		// Set text on the toggle button
		snap2Button.setText(trans.get("CaliperDialog.btn.snap"));
		snap2Button.setToolTipText(String.format(trans.get("CaliperDialog.btn.snap.ttip"), 2));
		caliper2Panel.add(snap2Button, "gapleft rel");
		mainPanel.add(caliper2Panel, "growx, wrap para");
		
		// Update visual state of position spinners based on snap mode
		// Add listeners to update styling when snap mode changes
		final Color normalBackground = caliper1Spinner.getBackground();
		final Color normalForeground = ((JSpinner.DefaultEditor) caliper1Spinner.getEditor()).getTextField().getForeground();
		final Color snapHighlightColor = GUIUtil.getUITheme().getCaliperSnapHighlightColor();
		
		// Create a method to update spinner visual state
		java.util.function.Consumer<Void> updateSpinnerStates = (v) -> {
			boolean caliper1Snapping = caliperManager.isSnapModeActive() && 
				caliperManager.getActiveSnapCaliper() != null && 
				caliperManager.getActiveSnapCaliper() == 1;
			boolean caliper2Snapping = caliperManager.isSnapModeActive() && 
				caliperManager.getActiveSnapCaliper() != null && 
				caliperManager.getActiveSnapCaliper() == 2;
			
			// Update caliper 1 spinner
			if (caliper1Snapping) {
				// Highlight with snap highlight color background (lighter shade) and border
				Color highlightBg = new Color(
					Math.min(255, snapHighlightColor.getRed() + (255 - snapHighlightColor.getRed()) / 4),
					Math.min(255, snapHighlightColor.getGreen() + (255 - snapHighlightColor.getGreen()) / 4),
					Math.min(255, snapHighlightColor.getBlue() + (255 - snapHighlightColor.getBlue()) / 4),
					180); // Semi-transparent
				caliper1Spinner.setBackground(highlightBg);
				caliper1Spinner.setBorder(new LineBorder(snapHighlightColor, 2, true));
				// Also highlight the text field inside
				JSpinner.DefaultEditor editor1 = (JSpinner.DefaultEditor) caliper1Spinner.getEditor();
				JTextField textField1 = editor1.getTextField();
				textField1.setBackground(highlightBg);
				textField1.setForeground(Color.BLACK);
				textField1.setBorder(new LineBorder(snapHighlightColor, 1, true));
			} else {
				// Restore normal appearance
				caliper1Spinner.setBackground(normalBackground);
				caliper1Spinner.setBorder(UIManager.getBorder("Spinner.border"));
				JSpinner.DefaultEditor editor1 = (JSpinner.DefaultEditor) caliper1Spinner.getEditor();
				JTextField textField1 = editor1.getTextField();
				textField1.setBackground(normalBackground);
				textField1.setForeground(normalForeground);
				textField1.setBorder(null);
			}
			
			// Update caliper 2 spinner
			if (caliper2Snapping) {
				// Highlight with snap highlight color background (lighter shade) and border
				Color highlightBg = new Color(
					Math.min(255, snapHighlightColor.getRed() + (255 - snapHighlightColor.getRed()) / 4),
					Math.min(255, snapHighlightColor.getGreen() + (255 - snapHighlightColor.getGreen()) / 4),
					Math.min(255, snapHighlightColor.getBlue() + (255 - snapHighlightColor.getBlue()) / 4),
					180); // Semi-transparent
				caliper2Spinner.setBackground(highlightBg);
				caliper2Spinner.setBorder(new LineBorder(snapHighlightColor, 2, true));
				// Also highlight the text field inside
				JSpinner.DefaultEditor editor2 = (JSpinner.DefaultEditor) caliper2Spinner.getEditor();
				JTextField textField2 = editor2.getTextField();
				textField2.setBackground(highlightBg);
				textField2.setForeground(Color.BLACK);
				textField2.setBorder(new LineBorder(snapHighlightColor, 1, true));
			} else {
				// Restore normal appearance
				caliper2Spinner.setBackground(normalBackground);
				caliper2Spinner.setBorder(UIManager.getBorder("Spinner.border"));
				JSpinner.DefaultEditor editor2 = (JSpinner.DefaultEditor) caliper2Spinner.getEditor();
				JTextField textField2 = editor2.getTextField();
				textField2.setBackground(normalBackground);
				textField2.setForeground(normalForeground);
				textField2.setBorder(null);
			}
		};
		
		// Add listeners to update when snap button states change
		// Listen to property changes on the snap buttons
		snap1Button.addPropertyChangeListener("selected", (e) -> updateSpinnerStates.accept(null));
		snap2Button.addPropertyChangeListener("selected", (e) -> updateSpinnerStates.accept(null));
		
		// Also listen to action events on the snap buttons (when user clicks them)
		snap1Button.addActionListener((e) -> {
			// Update after a short delay to ensure CaliperManager has processed the state change
			SwingUtilities.invokeLater(() -> updateSpinnerStates.accept(null));
		});
		snap2Button.addActionListener((e) -> {
			// Update after a short delay to ensure CaliperManager has processed the state change
			SwingUtilities.invokeLater(() -> updateSpinnerStates.accept(null));
		});
		
		// Use a timer to periodically check the snap mode state
		// This ensures the visual state stays in sync even if other code changes the state
		Timer updateTimer = new Timer(100, (e) -> {
			if (caliperManager != null) {
				updateSpinnerStates.accept(null);
			}
		});
		updateTimer.setRepeats(true);
		updateTimer.start();
		
		// Initial update
		updateSpinnerStates.accept(null);
		
		// Debug mode checkbox: always show snap targets
		// Only show this checkbox when in debug mode
		boolean isDebugMode = System.getProperty("openrocket.debug") != null;
		if (isDebugMode) {
			JCheckBox debugCheckbox = new JCheckBox("Always show snap targets");
			debugCheckbox.setSelected(caliperManager.isAlwaysShowSnapTargets());
			debugCheckbox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					caliperManager.setAlwaysShowSnapTargets(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
			mainPanel.add(debugCheckbox, "spanx, wrap para");
		}
		
		// Minimize button
		JButton minimizeButton = new JButton(trans.get("CaliperDialog.btn.minimize"));
		minimizeButton.setToolTipText(trans.get("CaliperDialog.btn.minimize.ttip"));
		minimizeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setMinimized(true);
			}
		});
		
		// Close button
		JButton closeButton = new JButton(trans.get("dlg.but.close"));
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);  // This will disable the caliper via setVisible
			}
		});
		
		// Button panel for minimize, close, and help button
		JPanel buttonPanel = new JPanel(new MigLayout("ins 0, fillx"));
		buttonPanel.setOpaque(false);
		
		// Add component info (help text) - this creates infoBtn
		addComponentInfo(buttonPanel);
		
		// Minimize and close buttons (right side)
		buttonPanel.add(minimizeButton, "align right, split 2, gapleft para");
		buttonPanel.add(closeButton, "align right");
		
		mainPanel.add(buttonPanel, "spanx, growx");
		
		// Create minimized panel (only distance)
		// Note: distancePanel will be moved between mainPanel and minimizedPanel
		minimizedPanel = new JPanel(new MigLayout("fill, insets dialog", "[grow]", "[]"));
		// Make minimized panel clickable to restore (double-click)
		minimizedPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					setMinimized(false);
				}
			}
		});
		minimizedPanel.setToolTipText("Double-click to restore dialog");
		
		// Add main panel to dialog
		add(mainPanel);
		
		// Add Escape key handler to exit snap mode
		// This ensures Escape works even when the dialog has focus
		JRootPane rootPane = getRootPane();
		Action escapeAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (caliperManager != null && caliperManager.isSnapModeActive()) {
					caliperManager.exitSnapMode();
				}
			}
		};
		KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKey, "exitSnapMode");
		rootPane.getActionMap().put("exitSnapMode", escapeAction);
		
		pack();
	}
	
	/**
	 * Add a section to the caliper dialog that displays help information.
	 */
	private void addComponentInfo(JPanel buttonPanel) {
		final String helpText = "<html>" + trans.get("CaliperDialog.info") + "</html>";

		// Component info
		componentInfo = new DescriptionArea(helpText, 5);
		componentInfo.setTextFont(null);
		componentInfo.setVisible(false);
		mainPanel.add(componentInfo, "spanx, growx, wrap para");

		// Component info toggle button
		infoBtn = new IconToggleButton();
		infoBtn.setToolTipText(trans.get("CaliperDialog.btn.ComponentInfo.ttip"));
		infoBtn.setIconScale(1.2f);
		infoBtn.setSelectedIcon(Icons.HELP_ABOUT);
		infoBtn.setIcon(Icons.HELP_ABOUT);
		buttonPanel.add(infoBtn, "pushx, align left, gapright para");

		infoBtn.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				componentInfo.setVisible(e.getStateChange() != ItemEvent.SELECTED);
				pack();
				SwingUtilities.invokeLater(() -> {
					// Scroll to top when showing info
					if (componentInfo.isVisible()) {
						componentInfo.scrollToTop();
					}
				});
			}
		});

		infoBtn.setSelected(true);
	}
	
	/**
	 * Show the dialog and mark it as open.
	 * Enables the caliper when opening, disables when closing.
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			isOpen = true;
			// Check preference for whether to open minimized
			boolean shouldOpenMinimized = preferences.getBoolean(
					ApplicationPreferences.CALIPER_OPEN_MINIMIZED, false);
			if (shouldOpenMinimized) {
				// Open in minimized mode
				if (!minimized) {
					setMinimized(true);
				}
			} else {
				// Reset to normal size when opening
				if (minimized) {
					setMinimized(false);
				}
			}
			// Enable caliper when dialog opens
			caliperManager.setEnabled(true);
		} else {
			isOpen = false;
			// Exit snap mode if active
			if (caliperManager.isSnapModeActive()) {
				caliperManager.exitSnapMode();
			}
			// Disable caliper when dialog closes
			caliperManager.setEnabled(false);
		}
		super.setVisible(visible);
	}
	
	/**
	 * Set the minimized state of the dialog.
	 * When minimized, only the distance panel is shown.
	 *
	 * @param minimized true to minimize, false to restore
	 */
	private void setMinimized(boolean minimized) {
		if (this.minimized == minimized) {
			return;
		}
		this.minimized = minimized;
		
		if (minimized) {
			// Remove distancePanel from mainPanel and add it to minimizedPanel
			mainPanel.remove(distancePanel);
			minimizedPanel.removeAll(); // Clear any existing components
			minimizedPanel.add(distancePanel, "growx, wrap");
			
			// Remove main panel and add minimized panel
			remove(mainPanel);
			add(minimizedPanel);
			pack();
		} else {
			// Remove distancePanel from minimizedPanel and add it back to mainPanel
			minimizedPanel.remove(distancePanel);
			// distancePanel should already be in mainPanel's layout, but we need to ensure it's there
			// The layout manager will handle the positioning based on the constraints
			// We need to re-add it with the correct constraints
			mainPanel.remove(distanceLabel);
			mainPanel.remove(distancePanel);
			mainPanel.add(distanceLabel, 3);
			mainPanel.add(distancePanel, "spanx, wrap para", 4);
			
			// Remove minimized panel and add main panel
			remove(minimizedPanel);
			add(mainPanel);
			pack();
		}
	}
	
	/**
	 * Restore the dialog to normal size.
	 * Called when the caliper button is pressed.
	 */
	public void restore() {
		if (minimized) {
			setMinimized(false);
		}
	}
	
	/**
	 * Check if the dialog is currently open.
	 *
	 * @return true if the dialog is open, false otherwise
	 */
	public boolean isOpen() {
		return isOpen && isVisible();
	}
}
