package info.openrocket.swing.gui.configdialog;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.file.svg.export.SVGExportOptions;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.preferences.ApplicationPreferences;
import info.openrocket.core.rocketcomponent.Bulkhead;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.startup.Application;

import info.openrocket.swing.gui.adaptors.CustomFocusTraversalPolicy;
import info.openrocket.swing.gui.components.SVGOptionPanel;
import info.openrocket.swing.gui.export.ComponentSvgExportService;
import info.openrocket.swing.gui.util.FileHelper;
import info.openrocket.swing.gui.util.SwingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class BulkheadConfig extends RingComponentConfig {
	private static final Logger log = LoggerFactory.getLogger(BulkheadConfig.class);
	private static final Translator trans = Application.getTranslator();
	private static final ApplicationPreferences prefs = Application.getPreferences();
	
	public BulkheadConfig(OpenRocketDocument d, RocketComponent c, JDialog parent) {
		super(d, c, parent);
		
		JPanel tab;
		
		tab = generalTab(trans.get("BulkheadCfg.tab.Thickness"), trans.get("BulkheadCfg.tab.Diameter"), null, null);
		//// General and General properties
		tabbedPane.insertTab(trans.get("BulkheadCfg.tab.General"), null, tab,
				trans.get("BulkheadCfg.tab.Generalproperties"), 0);
		tabbedPane.setSelectedIndex(0);

		addSvgExportButton();

		// Apply the custom focus travel policy to this panel
		//// Make sure the cancel & ok button is the last component
		order.add(cancelButton);
		order.add(okButton);
		CustomFocusTraversalPolicy policy = new CustomFocusTraversalPolicy(order);
		parent.setFocusTraversalPolicy(policy);
	}

	private void addSvgExportButton() {
		JButton exportBtn = new JButton(trans.get("BulkheadConfig.lbl.exportSVG"));
		exportBtn.setToolTipText(trans.get("BulkheadConfig.lbl.exportSVG.ttip"));
		exportBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.info("Exporting bulkhead '{}' to SVG", component.getName());
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(FileHelper.SVG_FILTER);
				SwingPreferences swingPrefs = (SwingPreferences) Application.getPreferences();
				SVGOptionPanel optionPanel = new SVGOptionPanel(true);
				optionPanel.setStrokeColor(prefs.getSVGStrokeColor());
				optionPanel.setStrokeWidth(prefs.getSVGStrokeWidth());
				optionPanel.setDrawCrosshair(prefs.isSVGDrawCrosshair());
				optionPanel.setCrosshairColor(prefs.getSVGCrosshairColor());
				chooser.setAccessory(optionPanel);
				chooser.setCurrentDirectory(swingPrefs.getDefaultDirectory());
				chooser.setSelectedFile(suggestDefaultFile(swingPrefs));

				if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(BulkheadConfig.this)) {
					File selectedFile = chooser.getSelectedFile();
					selectedFile = FileHelper.forceExtension(selectedFile, "svg");
					if (!FileHelper.confirmWrite(selectedFile, buttonPanel)) {
						return;
					}
					swingPrefs.setDefaultDirectory(chooser.getCurrentDirectory());
					prefs.setSVGStrokeColor(optionPanel.getStrokeColor());
					prefs.setSVGStrokeWidth(optionPanel.getStrokeWidth());
					prefs.setSVGDrawCrosshair(optionPanel.isDrawCrosshair());
					prefs.setSVGCrosshairColor(optionPanel.getCrosshairColor());

					try {
						SVGExportOptions options = new SVGExportOptions(optionPanel.getStrokeColor(),
								optionPanel.getStrokeWidth(), optionPanel.isDrawCrosshair(), optionPanel.getCrosshairColor());
						ComponentSvgExportService.exportBulkhead((Bulkhead) component, selectedFile, options);
					}
					catch (Exception svgErr) {
						log.warn("Failed to export bulkhead SVG", svgErr);
						JOptionPane.showMessageDialog(BulkheadConfig.this,
								String.format(trans.get("BulkheadConfig.errorSVG.msg"), svgErr.getMessage()),
								trans.get("BulkheadConfig.errorSVG.title"), JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		addButtons(exportBtn);
		order.add(exportBtn);
	}

	private File suggestDefaultFile(SwingPreferences swingPrefs) {
		File directory = swingPrefs.getDefaultDirectory();
		if (directory == null) {
			directory = new File(System.getProperty("user.home", "."));
		}

		String baseName = component.getName();
		if (baseName == null || baseName.isBlank()) {
			baseName = component.getComponentName();
		}
		if (baseName == null || baseName.isBlank()) {
			baseName = "component";
		}
		String normalized = baseName.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "-");
		return new File(directory, normalized + ".svg");
	}
	
}