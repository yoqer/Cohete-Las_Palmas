package info.openrocket.swing.gui.configdialog;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.material.Material;
import info.openrocket.core.rocketcomponent.RailButton;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import info.openrocket.swing.gui.adaptors.CustomFocusTraversalPolicy;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.components.SpinnerWithSlider;


@SuppressWarnings("serial")
public class RailButtonConfig extends RocketComponentConfig {
	
	private static final Translator trans = Application.getTranslator();
	
	public RailButtonConfig( OpenRocketDocument document, RocketComponent component, JDialog parent) {
		super(document, component, parent);

		//// General and General properties
		tabbedPane.insertTab( trans.get("RailBtnCfg.tab.General"), null, buttonTab( (RailButton)component ), trans.get("RailBtnCfg.tab.GeneralProp"), 0);
		tabbedPane.setSelectedIndex(0);

		// Apply the custom focus travel policy to this panel
		//// Make sure the cancel & ok button is the last component
		order.add(cancelButton);
		order.add(okButton);
		CustomFocusTraversalPolicy policy = new CustomFocusTraversalPolicy(order);
		parent.setFocusTraversalPolicy(policy);
	}
	
	private JPanel buttonTab( final RailButton rbc ){
		
		JPanel primary = new JPanel(new MigLayout());
		
		JPanel panel = new JPanel( new MigLayout("gap rel unrel, ins 0"));
		SpinnerWithSlider spinnerWithSlider;
		
			
		{ //// Outer Diameter
			panel.add(new JLabel(trans.get("RailBtnCfg.lbl.OuterDiam")));
			DoubleModel odModel = new DoubleModel(component, "OuterDiameter", UnitGroup.UNITS_LENGTH, 0);
			register(odModel);
			spinnerWithSlider = new SpinnerWithSlider(odModel, 0, 0.02);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}
		{ //// Inner Diameter
			panel.add(new JLabel(trans.get("RailBtnCfg.lbl.InnerDiam")));
			DoubleModel idModel = new DoubleModel(component, "InnerDiameter", UnitGroup.UNITS_LENGTH, 0);
			register(idModel);
			spinnerWithSlider = new SpinnerWithSlider(idModel, 0, 0.02);
			panel.add(spinnerWithSlider, "growx, wrap 20lp");
			order.add(spinnerWithSlider.getTextField());
		}
		{ //// Base Height
			panel.add(new JLabel(trans.get("RailBtnCfg.lbl.BaseHeight")));
			DoubleModel heightModel = new DoubleModel(component, "BaseHeight", UnitGroup.UNITS_LENGTH, 0);
			register(heightModel);
			DoubleModel maxBaseHeight = new DoubleModel(component, "MaxBaseHeight", UnitGroup.UNITS_LENGTH);
			register(maxBaseHeight);
			DoubleModel zeroModel = new DoubleModel(0, UnitGroup.UNITS_LENGTH);
			register(zeroModel);
			spinnerWithSlider = new SpinnerWithSlider(heightModel, zeroModel, maxBaseHeight);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}
		{ //// Flange Height
			panel.add(new JLabel(trans.get("RailBtnCfg.lbl.FlangeHeight")));
			DoubleModel heightModel = new DoubleModel(component, "FlangeHeight", UnitGroup.UNITS_LENGTH, 0);
			register(heightModel);
			DoubleModel maxFlangeHeight = new DoubleModel(component, "MaxFlangeHeight", UnitGroup.UNITS_LENGTH);
			register(maxFlangeHeight);
			DoubleModel zeroModel = new DoubleModel(0, UnitGroup.UNITS_LENGTH);
			register(zeroModel);
			spinnerWithSlider = new SpinnerWithSlider(heightModel, zeroModel, maxFlangeHeight);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}
		{ //// Total Height
			panel.add(new JLabel(trans.get("RailBtnCfg.lbl.TotalHeight")));
			DoubleModel heightModel = new DoubleModel(component, "TotalHeight", UnitGroup.UNITS_LENGTH, 0);
			register(heightModel);
			DoubleModel minTotalHeight = new DoubleModel(component, "MinTotalHeight", UnitGroup.UNITS_LENGTH);
			register(minTotalHeight);
			DoubleModel maxTotalHeight = new DoubleModel(0.02, UnitGroup.UNITS_LENGTH);
			register(maxTotalHeight);
			spinnerWithSlider = new SpinnerWithSlider(heightModel, minTotalHeight, maxTotalHeight);
			panel.add(spinnerWithSlider, "growx, wrap 20lp");
			order.add(spinnerWithSlider.getTextField());
		}
		{ //// Screw height
			panel.add(new JLabel(trans.get("RailBtnCfg.lbl.ScrewHeight")));
			DoubleModel heightModel = new DoubleModel(component, "ScrewHeight", UnitGroup.UNITS_LENGTH, 0);
			register(heightModel);
			spinnerWithSlider = new SpinnerWithSlider(heightModel, 0, 0.02);
			panel.add(spinnerWithSlider, "growx, wrap 30lp");
			order.add(spinnerWithSlider.getTextField());
		}
  
		// -------- Instances ------
		InstancesPanel ip = new InstancesPanel(component, order);
		register(ip);
		panel.add(ip, "span, grow, wrap para");


		primary.add(panel, "grow, gapright 20lp");

		// Right side panel
		panel = new JPanel(new MigLayout("gap rel unrel, ins 0", "[][65lp::][30lp::][]", ""));

		{// -------- Placement ------
			//// Position relative to:
			PlacementPanel placementPanel = new PlacementPanel(component, order);
			register(placementPanel);
			panel.add(placementPanel, "span, grow, wrap");

			{ //// Rotation:
				placementPanel.add(new JLabel(trans.get("RailBtnCfg.lbl.Angle")), "newline");
				DoubleModel angleModel = new DoubleModel(component, "AngleOffset", UnitGroup.UNITS_ANGLE, -180, +180);
				register(angleModel);
				SpinnerWithSlider angleSpinner = new SpinnerWithSlider(angleModel, -Math.PI, Math.PI);
				placementPanel.add(angleSpinner, "growx");
				order.add(angleSpinner.getTextField());
			}
		}

		//// Material
		MaterialPanel materialPanel = new MaterialPanel(component, document, Material.Type.BULK, order);
		register(materialPanel);
		panel.add(materialPanel,"span, grow, wrap");

		primary.add(panel, "grow");

		return primary;
	}
	
	@Override
	public void updateFields() {
		super.updateFields();
	}
	
}
