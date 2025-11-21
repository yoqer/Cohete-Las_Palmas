package info.openrocket.swing.gui.scalefigure.caliper;

import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.gui.util.GUIUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
		super(owner, trans.get("CaliperDialog.title"), ModalityType.DOCUMENT_MODAL);
		this.caliperManager = caliperManager;
		
		// Hide dialog when closed instead of disposing
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		
		// Track dialog state and disable caliper when closing
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				isOpen = false;
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
		
		// Add listeners to update mode
		verticalRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					caliperManager.setMode(CaliperManager.CaliperMode.VERTICAL);
				}
			}
		});
		horizontalRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					caliperManager.setMode(CaliperManager.CaliperMode.HORIZONTAL);
				}
			}
		});
		
		panel.add(verticalRadio, "gapleft para, split 2");
		panel.add(horizontalRadio, "wrap para");
		
		// Distance and unit in a colored border panel
		Color caliperColor = GUIUtil.getUITheme().getCaliperColor();
		JPanel distancePanel = new JPanel(new MigLayout("ins 0"));
		distancePanel.setOpaque(false);
		Border caliperBorder = new LineBorder(caliperColor, 1);
		distancePanel.setBorder(new CompoundBorder(caliperBorder, new EmptyBorder(5, 5, 5, 5)));
		
		JSpinner distanceSpinner = caliperManager.getDistanceSpinner();
		JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) distanceSpinner.getEditor();
		javax.swing.JTextField textField = editor.getTextField();
		textField.setForeground(caliperColor);
		distancePanel.add(distanceSpinner, "split 2, aligny center");
		distancePanel.add(caliperManager.getUnitSelector(), "gapright unrel");
		
		panel.add(new JLabel(trans.get("CaliperDialog.lbl.distance")));
		panel.add(distancePanel, "growx, wrap para");
		
		// Caliper 1 position row
		panel.add(new JLabel(String.format(trans.get("CaliperDialog.lbl.caliperPosition"), 1)));
		JPanel caliper1Panel = new JPanel(new MigLayout("ins 0, fillx", "[][grow][]", ""));
		caliper1Panel.setOpaque(false);
		caliper1Panel.add(caliperManager.getCaliper1PositionSpinner(), "growx");
		
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
		caliper2Panel.add(caliperManager.getCaliper2PositionSpinner(), "growx");
		
		// Snap button with text - use the actual toggle button but make it show text
		JToggleButton snap2Button = caliperManager.getCaliper2SnapButton();
		// Set text on the toggle button
		snap2Button.setText(trans.get("CaliperDialog.btn.snap"));
		snap2Button.setToolTipText(String.format(trans.get("CaliperDialog.btn.snap.ttip"), 2));
		caliper2Panel.add(snap2Button, "gapleft rel");
		panel.add(caliper2Panel, "growx, wrap para");
		
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
