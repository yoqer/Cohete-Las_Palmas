package info.openrocket.swing.gui.scalefigure.caliper;

import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.Unit;
import info.openrocket.core.util.StateChangeListener;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.util.GUIUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
	
	private final CaliperManager caliperManager;
	private boolean isOpen = false;
	
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
		JPanel panel = new JPanel(new MigLayout("fill, insets dialog", "[grow]", "[]rel[]para[]"));
		
		// Mode selection: Radio buttons for Vertical/Horizontal
		panel.add(new JLabel(trans.get("CaliperDialog.lbl.mode")));
		ButtonGroup modeGroup = new ButtonGroup();
		JRadioButton verticalRadio = new JRadioButton(trans.get("CaliperDialog.radio.vertical"));
		JRadioButton horizontalRadio = new JRadioButton(trans.get("CaliperDialog.radio.horizontal"));
		modeGroup.add(verticalRadio);
		modeGroup.add(horizontalRadio);
		
		// Set initial selection based on current mode
		CaliperManager.CaliperMode currentMode = caliperManager.getMode();
		verticalRadio.setSelected(currentMode == CaliperManager.CaliperMode.VERTICAL);
		horizontalRadio.setSelected(currentMode == CaliperManager.CaliperMode.HORIZONTAL);
		
		panel.add(verticalRadio, "gapleft para, split 2");
		panel.add(horizontalRadio, "wrap para");
		
		// Distance and unit in a colored border panel
		Color caliperColor = GUIUtil.getUITheme().getCaliperColor();
		JPanel distancePanel = new JPanel(new MigLayout("ins 0"));
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
		javax.swing.JComponent unitSelector = caliperManager.getUnitSelector();
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
		
		panel.add(new JLabel(trans.get("CaliperDialog.lbl.distance")));
		panel.add(distancePanel, "spanx, wrap para");
		
		// Caliper 1 position row
		panel.add(new JLabel(String.format(trans.get("CaliperDialog.lbl.caliperPosition"), 1)));
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
		panel.add(caliper1Panel, "growx, wrap");
		
		// Caliper 2 position row
		panel.add(new JLabel(String.format(trans.get("CaliperDialog.lbl.caliperPosition"), 2)));
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
		panel.add(caliper2Panel, "growx, wrap para");
		
		// Update visual state of position spinners based on snap mode
		// Add listeners to update styling when snap mode changes
		final java.awt.Color normalBackground = caliper1Spinner.getBackground();
		final java.awt.Color normalForeground = ((JSpinner.DefaultEditor) caliper1Spinner.getEditor()).getTextField().getForeground();
		final java.awt.Color snapHighlightColor = GUIUtil.getUITheme().getCaliperSnapHighlightColor();
		
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
				java.awt.Color highlightBg = new java.awt.Color(
					Math.min(255, snapHighlightColor.getRed() + (255 - snapHighlightColor.getRed()) / 4),
					Math.min(255, snapHighlightColor.getGreen() + (255 - snapHighlightColor.getGreen()) / 4),
					Math.min(255, snapHighlightColor.getBlue() + (255 - snapHighlightColor.getBlue()) / 4),
					180); // Semi-transparent
				caliper1Spinner.setBackground(highlightBg);
				caliper1Spinner.setBorder(new javax.swing.border.LineBorder(snapHighlightColor, 2, true));
				// Also highlight the text field inside
				JSpinner.DefaultEditor editor1 = (JSpinner.DefaultEditor) caliper1Spinner.getEditor();
				javax.swing.JTextField textField1 = editor1.getTextField();
				textField1.setBackground(highlightBg);
				textField1.setForeground(java.awt.Color.BLACK);
				textField1.setBorder(new javax.swing.border.LineBorder(snapHighlightColor, 1, true));
			} else {
				// Restore normal appearance
				caliper1Spinner.setBackground(normalBackground);
				caliper1Spinner.setBorder(javax.swing.UIManager.getBorder("Spinner.border"));
				JSpinner.DefaultEditor editor1 = (JSpinner.DefaultEditor) caliper1Spinner.getEditor();
				javax.swing.JTextField textField1 = editor1.getTextField();
				textField1.setBackground(normalBackground);
				textField1.setForeground(normalForeground);
				textField1.setBorder(null);
			}
			
			// Update caliper 2 spinner
			if (caliper2Snapping) {
				// Highlight with snap highlight color background (lighter shade) and border
				java.awt.Color highlightBg = new java.awt.Color(
					Math.min(255, snapHighlightColor.getRed() + (255 - snapHighlightColor.getRed()) / 4),
					Math.min(255, snapHighlightColor.getGreen() + (255 - snapHighlightColor.getGreen()) / 4),
					Math.min(255, snapHighlightColor.getBlue() + (255 - snapHighlightColor.getBlue()) / 4),
					180); // Semi-transparent
				caliper2Spinner.setBackground(highlightBg);
				caliper2Spinner.setBorder(new javax.swing.border.LineBorder(snapHighlightColor, 2, true));
				// Also highlight the text field inside
				JSpinner.DefaultEditor editor2 = (JSpinner.DefaultEditor) caliper2Spinner.getEditor();
				javax.swing.JTextField textField2 = editor2.getTextField();
				textField2.setBackground(highlightBg);
				textField2.setForeground(java.awt.Color.BLACK);
				textField2.setBorder(new javax.swing.border.LineBorder(snapHighlightColor, 1, true));
			} else {
				// Restore normal appearance
				caliper2Spinner.setBackground(normalBackground);
				caliper2Spinner.setBorder(javax.swing.UIManager.getBorder("Spinner.border"));
				JSpinner.DefaultEditor editor2 = (JSpinner.DefaultEditor) caliper2Spinner.getEditor();
				javax.swing.JTextField textField2 = editor2.getTextField();
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
			javax.swing.SwingUtilities.invokeLater(() -> updateSpinnerStates.accept(null));
		});
		snap2Button.addActionListener((e) -> {
			// Update after a short delay to ensure CaliperManager has processed the state change
			javax.swing.SwingUtilities.invokeLater(() -> updateSpinnerStates.accept(null));
		});
		
		// Use a timer to periodically check the snap mode state
		// This ensures the visual state stays in sync even if other code changes the state
		javax.swing.Timer updateTimer = new javax.swing.Timer(100, (e) -> {
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
			javax.swing.JCheckBox alwaysShowSnapTargetsCheckbox = new javax.swing.JCheckBox("Always show snap targets");
			alwaysShowSnapTargetsCheckbox.setSelected(caliperManager.isAlwaysShowSnapTargets());
			alwaysShowSnapTargetsCheckbox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					caliperManager.setAlwaysShowSnapTargets(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
			panel.add(alwaysShowSnapTargetsCheckbox, "spanx, wrap para");
		}
		
		// Close button
		JButton closeButton = new JButton(trans.get("dlg.but.close"));
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);  // This will disable the caliper via setVisible
			}
		});
		panel.add(closeButton, "spanx, tag close, align right");
		
		add(panel);
		
		// Install Escape key handler
		GUIUtil.installEscapeCloseOperation(this, new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);  // This will disable the caliper via setVisible
			}
		});
		
		pack();
	}
	
	/**
	 * Show the dialog and mark it as open.
	 * Enables the caliper when opening, disables when closing.
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			isOpen = true;
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
	 * Check if the dialog is currently open.
	 *
	 * @return true if the dialog is open, false otherwise
	 */
	public boolean isOpen() {
		return isOpen && isVisible();
	}
}
