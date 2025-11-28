package info.openrocket.swing.gui.configdialog;


import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.material.Material;
import info.openrocket.core.rocketcomponent.FinSet;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.TrapezoidFinSet;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.CustomFocusTraversalPolicy;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.adaptors.EnumModel;
import info.openrocket.swing.gui.adaptors.IntegerModel;
import info.openrocket.swing.gui.components.SpinnerWithSlider;


public class TrapezoidFinSetConfig extends FinSetConfig {
	private static final long serialVersionUID = -4870745241749769842L;
	private static final Translator trans = Application.getTranslator();
	
	public TrapezoidFinSetConfig(OpenRocketDocument d, final RocketComponent component, JDialog parent) {
		super(d, component, parent);

		JPanel mainPanel = new JPanel(new MigLayout());

		// Left side
		JPanel panel = new JPanel(new MigLayout("gap rel unrel, ins 0", "[][65lp::][30lp::]", ""));

		////  Number of fins:
		JLabel label = new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Nbroffins"));
		//// The number of fins in the fin set.
		label.setToolTipText(trans.get("TrapezoidFinSetCfg.lbl.ttip.Nbroffins"));
		panel.add(label);

		final IntegerModel finCountModel = new IntegerModel(component, "FinCount", 1, 8);
		register(finCountModel);

		final JSpinner finCountSpinner = new JSpinner(finCountModel.getSpinnerModel());
		finCountSpinner.setEditor(new SpinnerEditor(finCountSpinner));
		//// The number of fins in the fin set.
		finCountSpinner.setToolTipText(trans.get("TrapezoidFinSetCfg.lbl.ttip.Nbroffins"));
		panel.add(finCountSpinner, "growx, wrap");
		order.add(((SpinnerEditor) finCountSpinner.getEditor()).getTextField());

		{////  Fin cant:
			label = new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Fincant"));
			//// The angle that the fins are canted with respect to the rocket
			label.setToolTipText(trans.get("TrapezoidFinSetCfg.lbl.ttip.Fincant"));
			panel.add(label);

			final DoubleModel cantModel = new DoubleModel(component, "CantAngle", UnitGroup.UNITS_ANGLE, -FinSet.MAX_CANT_RADIANS, FinSet.MAX_CANT_RADIANS);
			register(cantModel);

			SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(cantModel, -FinSet.MAX_CANT_RADIANS, FinSet.MAX_CANT_RADIANS);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}

		{////  Root chord:
			panel.add(new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Rootchord")));

			final DoubleModel rootChordModel = new DoubleModel(component, "RootChord", UnitGroup.UNITS_LENGTH, 0);
			register(rootChordModel);

			SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(rootChordModel, 0, 0.05, 0.2);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}

		{////  Tip chord:
			panel.add(new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Tipchord")));

			final DoubleModel tipChordModel = new DoubleModel(component, "TipChord", UnitGroup.UNITS_LENGTH, 0);
			register(tipChordModel);

			SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(tipChordModel, 0, 0.05, 0.2);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}

		{////  Height:
			panel.add(new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Height")));

			final DoubleModel heightModel = new DoubleModel(component, "Height", UnitGroup.UNITS_LENGTH, 0);
			register(heightModel);

			SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(heightModel, 0, 0.05, 0.2);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}

		{////  Sweep length:
			panel.add(new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Sweeplength")));

			final DoubleModel sweepDistanceModel = new DoubleModel(component, "Sweep", UnitGroup.UNITS_LENGTH);
			register(sweepDistanceModel);
			component.addChangeListener(sweepDistanceModel);
			
			// sweep slider from -1.1*TipChord to 1.1*RootChord
			DoubleModel tc = new DoubleModel(component, "TipChord", -1.1, UnitGroup.UNITS_LENGTH);
			DoubleModel rc = new DoubleModel(component, "RootChord", 1.1, UnitGroup.UNITS_LENGTH);
			register(tc);
			register(rc);
			SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(sweepDistanceModel, tc, rc);
			panel.add(spinnerWithSlider, "growx, wrap");
			order.add(spinnerWithSlider.getTextField());
		}

		{////  Sweep angle:
			panel.add(new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Sweepangle")));

			final DoubleModel sweepAngleModel = new DoubleModel(component, "SweepAngle", UnitGroup.UNITS_ANGLE,
					-TrapezoidFinSet.MAX_SWEEP_ANGLE, TrapezoidFinSet.MAX_SWEEP_ANGLE);
			register(sweepAngleModel);
			component.addChangeListener(sweepAngleModel);

			SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(sweepAngleModel, -Math.PI / 4, Math.PI / 4);
			panel.add(spinnerWithSlider, "growx, wrap 30lp");
			order.add(spinnerWithSlider.getTextField());
		}

		{////  Fin cross section:
			panel.add(new JLabel(trans.get("TrapezoidFinSetCfg.lbl.FincrossSection")));
			EnumModel<FinSet.CrossSection> em = new EnumModel<>(component, "CrossSection");
			register(em);
			JComboBox<FinSet.CrossSection> sectionCombo = new JComboBox<>(em);
			panel.add(sectionCombo, "span, growx, wrap");
			order.add(sectionCombo);
		}

		{ ////  Thickness:
			panel.add(new JLabel(trans.get("TrapezoidFinSetCfg.lbl.Thickness")));

			final DoubleModel thicknessModel = new DoubleModel(component, "Thickness", UnitGroup.UNITS_LENGTH, 0);
			register(thicknessModel);

			final JSpinner thicknessSpinner = new JSpinner(thicknessModel.getSpinnerModel());
			thicknessSpinner.setEditor(new SpinnerEditor(thicknessSpinner));
			panel.add(thicknessSpinner, "growx");
			order.add(((SpinnerEditor) thicknessSpinner.getEditor()).getTextField());

		}

		mainPanel.add(panel, "aligny 0, gapright 40lp");

		// Right side panel
		panel = new JPanel(new MigLayout("gap rel unrel, ins 0", "[][65lp::][30lp::]", ""));

		{//// -------- Placement -------
			// Position relative to:
			PlacementPanel placementPanel = new PlacementPanel(component, order);
			register(placementPanel);
			panel.add(placementPanel, "span, grow");

			{ ///  Fin rotation
				label = new JLabel(trans.get("FinSetCfg.lbl.FinRotation"));
				label.setToolTipText(trans.get("FinSetCfg.lbl.FinRotation.ttip"));
				placementPanel.add(label, "newline");

				final DoubleModel baseRotationModel = new DoubleModel(component, "BaseRotation",
						UnitGroup.UNITS_ANGLE, -Math.PI, Math.PI);
				register(baseRotationModel);

				SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(baseRotationModel);
				placementPanel.add(spinnerWithSlider, "growx");
				order.add(spinnerWithSlider.getTextField());
			}
		}

		{//// Material
			MaterialPanel materialPanel = new MaterialPanel(component, document, Material.Type.BULK, order);
			register(materialPanel);
			panel.add(materialPanel, "span, grow, wrap");
		}

		{// Root fillets
			panel.add(filletMaterialPanel(), "span, grow, wrap");
		}

		mainPanel.add(panel, "aligny 0");

		//// General and General properties
		tabbedPane.insertTab(trans.get("TrapezoidFinSetCfg.tab.General"), null, mainPanel,
				trans.get("TrapezoidFinSetCfg.tab.Generalproperties"), 0);
		tabbedPane.setSelectedIndex(0);

		addFinSetButtons();

		// Apply the custom focus travel policy to this config dialog
		//// Make sure the cancel & ok button is the last component
		order.add(cancelButton);
		order.add(okButton);
		CustomFocusTraversalPolicy policy = new CustomFocusTraversalPolicy(order);
		parent.setFocusTraversalPolicy(policy);
	}
}
