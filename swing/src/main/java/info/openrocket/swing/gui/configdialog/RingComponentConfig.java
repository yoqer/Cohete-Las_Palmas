package info.openrocket.swing.gui.configdialog;


import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.material.Material;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.ThicknessRingComponent;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.components.SpinnerWithSlider;

@SuppressWarnings("serial")
public class RingComponentConfig extends RocketComponentConfig {
	private static final Translator trans = Application.getTranslator();
	
	public RingComponentConfig(OpenRocketDocument d, RocketComponent component, JDialog parent) {
		super(d, component, parent);
	}
	

	protected JPanel generalTab(String length, String outer, String inner, String thickness) {
		JPanel primary = new JPanel(new MigLayout());

		JPanel panel = new JPanel(new MigLayout("gap rel unrel, ins 0", "[][65lp::][30lp::]", ""));
		DoubleModel m;
		SpinnerWithSlider spinnerWithSlider;
		DoubleModel od = null;

		//// Attributes ----

		//// Length
		if (length != null) {
			panel.add(new JLabel(length));

			m = new DoubleModel(component, "Length", UnitGroup.UNITS_LENGTH, 0);
			register(m);

			spinnerWithSlider = new SpinnerWithSlider(m, 0, 0.1, 1.0);
			if (component instanceof ThicknessRingComponent) {
				focusElement = spinnerWithSlider.getSpinner();
			}
			panel.add(spinnerWithSlider, "growx, spanx 2, wrap");
			order.add(spinnerWithSlider.getTextField());
		}

		//// Outer diameter
		if (outer != null) {
			panel.add(new JLabel(outer));
			
			//// OuterRadius
			od = new DoubleModel(component, "OuterRadius", 2, UnitGroup.UNITS_LENGTH, 0);
			register(od);

			spinnerWithSlider = new SpinnerWithSlider(od, 0, 0.04, 0.2);
			panel.add(spinnerWithSlider, "growx, spanx 2, wrap");
			order.add(spinnerWithSlider.getTextField());
			
			if (od.isAutomaticAvailable()) {
				JCheckBox check = new JCheckBox(od.getAutomaticAction());
				//// Automatic
				check.setText(trans.get("ringcompcfg.Automatic"));
				check.setToolTipText(trans.get("ringcompcfg.AutomaticOuter.ttip"));
				panel.add(check, "skip, spanx 2, wrap");
				order.add(check);
			}
		}
		
		
		////  Inner diameter
		if (inner != null) {
			panel.add(new JLabel(inner));
			
			//// InnerRadius
			m = new DoubleModel(component, "InnerRadius", 2, UnitGroup.UNITS_LENGTH, 0);
			register(m);
			
			if (od == null)
				spinnerWithSlider = new SpinnerWithSlider(m, 0, 0.04, 0.2);
			else
				spinnerWithSlider = new SpinnerWithSlider(m, new DoubleModel(0), od);
			panel.add(spinnerWithSlider, "growx, spanx 2, wrap");
			order.add(spinnerWithSlider.getTextField());
			
			if (m.isAutomaticAvailable()) {
				JCheckBox check = new JCheckBox(m.getAutomaticAction());
				//// Automatic
				check.setText(trans.get("ringcompcfg.Automatic"));
				check.setToolTipText(trans.get("ringcompcfg.AutomaticInner.ttip"));
				panel.add(check, "skip, span 2, wrap");
				order.add(check);
			}
		}
		
		
		////  Wall thickness
		if (thickness != null) {
			panel.add(new JLabel(thickness));
			
			//// Thickness
			m = new DoubleModel(component, "Thickness", UnitGroup.UNITS_LENGTH, 0);
			register(m);
			
			spinnerWithSlider = new SpinnerWithSlider(m, 0, 0.01);
			panel.add(spinnerWithSlider, "growx, spanx 2, wrap");
			order.add(spinnerWithSlider.getTextField());
		}

		primary.add(panel, "grow, gapright 20lp");

		// Right side panel
		JPanel rightSide = new JPanel(new MigLayout("gap rel unrel, ins 0", "[][65lp::][30lp::]", ""));
		primary.add(rightSide, "cell 4 0, aligny 0, spany");

		//// Position
		PlacementPanel pp = new PlacementPanel(component, order);
		register(pp);
		rightSide.add(pp, "span, grow");

		//// Material
		MaterialPanel materialPanel = new MaterialPanel(component, document, Material.Type.BULK, order);
		register(materialPanel);
		rightSide.add(materialPanel, "span, grow, wrap");

		return primary;
	}
	
	
}
