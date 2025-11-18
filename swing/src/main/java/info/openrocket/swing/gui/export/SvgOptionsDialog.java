package info.openrocket.swing.gui.export;

import info.openrocket.core.file.svg.export.SVGExportOptions;
import info.openrocket.core.preferences.ApplicationPreferences;
import info.openrocket.swing.gui.components.SVGOptionPanel;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;

/**
 * Modal dialog wrapping the SVGOptionPanel with OK/Cancel actions.
 */
public class SvgOptionsDialog extends JDialog {
	private static final Translator trans = Application.getTranslator();
	private final SVGOptionPanel optionsPanel;
	private boolean confirmed = false;

	public SvgOptionsDialog(Frame owner) {
		super(owner, "SVG Options", true);
		optionsPanel = new SVGOptionPanel(true);
		initialize();
	}

	public SvgOptionsDialog(Dialog owner) {
		super(owner, "SVG Options", true);
		optionsPanel = new SVGOptionPanel(true);
		initialize();
	}

	private void initialize() {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(optionsPanel, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancel = new JButton("Cancel");
		JButton ok = new JButton("OK");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				confirmed = false;
				dispose();
			}
		});
				ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Store preferences before closing
				optionsPanel.storePreferences();
				confirmed = true;
				dispose();
			}
		});
		buttons.add(cancel);
		buttons.add(ok);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(getOwner());
	}

	public boolean showDialog(JComponent relativeTo) {
		setLocationRelativeTo(relativeTo);
		setVisible(true);
		return confirmed;
	}

	public boolean showDialog() {
		setVisible(true);
		return confirmed;
	}

	public Color getStrokeColor() { return optionsPanel.getStrokeColor(); }
	public double getStrokeWidth() { return optionsPanel.getStrokeWidth(); }
	public boolean isDrawCrosshair() { return optionsPanel.isDrawCrosshair(); }
	public Color getCrosshairColor() { return optionsPanel.getCrosshairColor(); }

	public SVGExportOptions getExportOptions() {
		double partSpacing = optionsPanel.getPartSpacing();
		
		return new SVGExportOptions(
			getStrokeColor(),
			getStrokeWidth(),
			isDrawCrosshair(),
			getCrosshairColor(),
			isShowLabels(),
			getLabelColor(),
			partSpacing
		);
	}

	public boolean isShowLabels() {
		return optionsPanel.isShowLabels();
	}

	public void setShowLabels(boolean showLabels) {
		optionsPanel.setShowLabels(showLabels);
	}

	public Color getLabelColor() {
		return optionsPanel.getLabelColor();
	}

	public void setLabelColor(Color color) {
		optionsPanel.setLabelColor(color);
	}

	public void setFromPreferences(ApplicationPreferences prefs) {
		optionsPanel.setStrokeColor(prefs.getSVGStrokeColor());
		optionsPanel.setStrokeWidth(prefs.getSVGStrokeWidth());
		optionsPanel.setDrawCrosshair(prefs.isSVGDrawCrosshair());
		optionsPanel.setCrosshairColor(prefs.getSVGCrosshairColor());
		optionsPanel.setShowLabels(prefs.isSVGShowLabels());
		optionsPanel.setLabelColor(prefs.getSVGLabelColor());
	}

	public SVGOptionPanel getOptionsPanel() {
		return optionsPanel;
	}
}

