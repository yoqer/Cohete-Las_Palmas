package info.openrocket.swing.gui.configdialog;


import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.material.Material;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.CustomFocusTraversalPolicy;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.adaptors.IntegerModel;
import info.openrocket.swing.gui.components.SpinnerWithSlider;

public class TubeFinSetConfig extends RocketComponentConfig {
	private static final long serialVersionUID = 508482875624928676L;
	private static final Translator trans = Application.getTranslator();
	
	public TubeFinSetConfig(OpenRocketDocument d, RocketComponent c, JDialog parent) {
		super(d, c, parent);
		
		JPanel primary = new JPanel(new MigLayout());
		
		
		JPanel panel = new JPanel(new MigLayout("gap rel unrel, ins 0", "[][65lp::][30lp::][]", ""));
		
		////  Number of fins
		panel.add(new JLabel(trans.get("TubeFinSetCfg.lbl.Nbroffins")));
		
		IntegerModel im = new IntegerModel(component, "FinCount", 1, 8);
		register(im);
		
		JSpinner spin = new JSpinner(im.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		panel.add(spin, "growx, wrap");
		order.add(((SpinnerEditor) spin.getEditor()).getTextField());

		//// Length:
		panel.add(new JLabel(trans.get("TubeFinSetCfg.lbl.Length")));
		
		DoubleModel m = new DoubleModel(component, "Length", UnitGroup.UNITS_LENGTH, 0);
		register(m);
		
		SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(m, 0, 0.02, 0.1);
		focusElement = spinnerWithSlider.getSpinner();
		panel.add(spinnerWithSlider, "growx, wrap para");
		order.add(spinnerWithSlider.getTextField());
		
		
		//// Outer diameter:
		panel.add(new JLabel(trans.get("TubeFinSetCfg.lbl.Outerdiam")));
		
		DoubleModel od = new DoubleModel(component, "OuterRadius", 2, UnitGroup.UNITS_LENGTH, 0);
		register(od);

		spinnerWithSlider = new SpinnerWithSlider(od, 0, 0.04, 0.2);
		panel.add(spinnerWithSlider, "growx, wrap rel");
		order.add(spinnerWithSlider.getTextField());
		
		JCheckBox check = new JCheckBox(od.getAutomaticAction());
		//// Automatic
		check.setText(trans.get("TubeFinSetCfg.checkbox.Automatic"));
		panel.add(check, "skip, span 2, wrap");
		order.add(check);

		////  Inner diameter:
		panel.add(new JLabel(trans.get("TubeFinSetCfg.lbl.Innerdiam")));

		m = new DoubleModel(component, "InnerRadius", 2, UnitGroup.UNITS_LENGTH, 0);
		register(m);
		spinnerWithSlider = new SpinnerWithSlider(m, new DoubleModel(0), od);
		panel.add(spinnerWithSlider, "growx, wrap rel");
		order.add(spinnerWithSlider.getTextField());
		
		
		//// Thickness:
		panel.add(new JLabel(trans.get("TubeFinSetCfg.lbl.Thickness")));
		
		m = new DoubleModel(component, "Thickness", UnitGroup.UNITS_LENGTH, 0);
		register(m);
		
		spinnerWithSlider = new SpinnerWithSlider(m, 0, 0.01);
		panel.add(spinnerWithSlider, "growx, wrap 20lp");
		order.add(spinnerWithSlider.getTextField());
		
		primary.add(panel, "grow, gapright 40lp");

		// Right side panel
		panel = new JPanel(new MigLayout("gap rel unrel, ins 0"));

		{ //// Placement
			//// Position relative to:
			PlacementPanel placementPanel = new PlacementPanel(component, order);
			register(placementPanel);
			panel.add(placementPanel, "span, grow, wrap");

			//// Fin rotation:
			JLabel label = new JLabel(trans.get("TubeFinSetCfg.lbl.Finrotation"));
			//// The angle of the first fin in the fin set.
			label.setToolTipText(trans.get("TubeFinSetCfg.lbl.ttip.Finrotation"));
			placementPanel.add(label, "newline");

			m = new DoubleModel(component, "BaseRotation", UnitGroup.UNITS_ANGLE);
			register(m);

			spinnerWithSlider = new SpinnerWithSlider(m, -Math.PI, Math.PI);
			placementPanel.add(spinnerWithSlider, "growx");
			order.add(spinnerWithSlider.getTextField());
		}

		{//// Material
			MaterialPanel materialPanel = new MaterialPanel(component, document, Material.Type.BULK, order);
			register(materialPanel);
			panel.add(materialPanel, "span, grow, wrap");
		}

		primary.add(panel, "grow");


		//// General and General properties
		tabbedPane.insertTab(trans.get("LaunchLugCfg.tab.General"), null, primary,
				trans.get("LaunchLugCfg.tab.Generalprop"), 0);
		tabbedPane.setSelectedIndex(0);

		// Apply the custom focus travel policy to this config dialog
		//// Make sure the cancel & ok button is the last component
		order.add(cancelButton);
		order.add(okButton);
		CustomFocusTraversalPolicy policy = new CustomFocusTraversalPolicy(order);
		parent.setFocusTraversalPolicy(policy);
	}
	
	@Override
	public void updateFields() {
		super.updateFields();
	}
	
}
