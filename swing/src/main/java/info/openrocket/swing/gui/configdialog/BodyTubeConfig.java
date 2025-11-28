package info.openrocket.swing.gui.configdialog;


import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.material.Material;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.SymmetricComponent;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import info.openrocket.swing.gui.adaptors.BooleanModel;
import info.openrocket.swing.gui.adaptors.CustomFocusTraversalPolicy;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.components.SpinnerWithSlider;

@SuppressWarnings("serial")
public class BodyTubeConfig extends RocketComponentConfig {

	private DoubleModel maxLength;
	private final JCheckBox checkAutoOuterRadius;
	private static final Translator trans = Application.getTranslator();

	public BodyTubeConfig(OpenRocketDocument d, RocketComponent c, JDialog parent) {
		super(d, c, parent);

		JPanel panel = new JPanel(new MigLayout("gap rel unrel", "[][65lp::][]", ""));

		////  Body tube length
		panel.add(new JLabel(trans.get("BodyTubecfg.lbl.Bodytubelength")));

		maxLength = new DoubleModel(2.0);
		DoubleModel length = new DoubleModel(component, "Length", UnitGroup.UNITS_LENGTH, 0);
		register(length);

		SpinnerWithSlider spinnerWithSlider = new SpinnerWithSlider(length, 0, 0.5, maxLength);
		focusElement = spinnerWithSlider.getSpinner();
		panel.add(spinnerWithSlider, "growx, wrap");
		order.add(spinnerWithSlider.getTextField());

		//// Body tube diameter
		panel.add(new JLabel(trans.get("BodyTubecfg.lbl.Outerdiameter")));

		// Diameter = 2*Radius
		final DoubleModel od = new DoubleModel(component, "OuterRadius", 2, UnitGroup.UNITS_LENGTH, 0);
		register(od);
		spinnerWithSlider = new SpinnerWithSlider(od, 0, 0.04, 0.2);
		panel.add(spinnerWithSlider, "growx, wrap 0px");
		order.add(spinnerWithSlider.getTextField());

		//// Automatic
		javax.swing.Action outerAutoAction = od.getAutomaticAction();
		checkAutoOuterRadius = new JCheckBox(outerAutoAction);
		checkAutoOuterRadius.setText(trans.get("BodyTubecfg.checkbox.Automatic"));
		panel.add(checkAutoOuterRadius, "skip, span 2, wrap");
		order.add(checkAutoOuterRadius);
		updateCheckboxAutoAftRadius();

		////  Inner diameter
		panel.add(new JLabel(trans.get("BodyTubecfg.lbl.Innerdiameter")));

		// Diameter = 2*Radius
		final DoubleModel innerRadiusModel = new DoubleModel(component, "InnerRadius", 2, UnitGroup.UNITS_LENGTH, 0);
		register(innerRadiusModel);
		spinnerWithSlider = new SpinnerWithSlider(innerRadiusModel, new DoubleModel(0), od);
		panel.add(spinnerWithSlider, "growx, wrap");
		order.add(spinnerWithSlider.getTextField());


		////  Wall thickness
		panel.add(new JLabel(trans.get("BodyTubecfg.lbl.Wallthickness")));

		final DoubleModel thicknessModel = new DoubleModel(component, "Thickness", UnitGroup.UNITS_LENGTH, 0);
		register(thicknessModel);
		spinnerWithSlider = new SpinnerWithSlider(thicknessModel, 0, 0.01);
		panel.add(spinnerWithSlider, "growx, wrap 0px");
		order.add(spinnerWithSlider.getTextField());

		//// Filled
		BooleanModel bm = new BooleanModel(component, "Filled");
		register(bm);
		JCheckBox check = new JCheckBox(bm);
		check.setText(trans.get("BodyTubecfg.checkbox.Filled"));
		check.setToolTipText(trans.get("BodyTubecfg.checkbox.Filled.ttip"));
		panel.add(check, "skip, span 2, wrap");
		order.add(check);

		//// Material
		MaterialPanel materialPanel = new MaterialPanel(component, document, Material.Type.BULK, order);
		register(materialPanel);
		panel.add(materialPanel, "cell 4 0, gapleft 20lp, aligny 0%, spany");

		//// General and General properties
		tabbedPane.insertTab(trans.get("BodyTubecfg.tab.General"), null, panel,
				trans.get("BodyTubecfg.tab.Generalproperties"), 0);

		tabbedPane.setSelectedIndex(0);

		MotorConfig motorConfig = new MotorConfig((MotorMount)c, order);
		register(motorConfig);

		tabbedPane.insertTab(trans.get("BodyTubecfg.tab.Motor"), null, motorConfig,
				trans.get("BodyTubecfg.tab.Motormountconf"), 1);

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

	/**
	 * Sets the checkAutoOuterRadius checkbox's enabled state and tooltip text, based on the state of its previous
	 * component. If there is no next and previous symmetric component, the checkAutoOuterRadius checkbox is disabled.
	 * If there is still a next or previous component which does not have its auto state enabled, meaning it can still
	 * serve as a reference component for this component, the auto checkbox is enabled.
	 */
	private void updateCheckboxAutoAftRadius() {
		if (component == null || checkAutoOuterRadius == null) return;

		// Disable check button if there is no component to get the diameter from
		SymmetricComponent prevComp = ((BodyTube) component).getPreviousSymmetricComponent();
		SymmetricComponent nextComp = ((BodyTube) component).getNextSymmetricComponent();
		if (prevComp == null && nextComp == null) {
			checkAutoOuterRadius.setEnabled(false);
			((BodyTube) component).setOuterRadiusAutomatic(false);
			checkAutoOuterRadius.setToolTipText(trans.get("BodyTubecfg.checkbox.ttip.Automatic_noReferenceComponent"));
			return;
		}
		if (!(prevComp != null && nextComp == null && prevComp.usesNextCompAutomatic()) &&
				!(nextComp != null && prevComp == null && nextComp.usesPreviousCompAutomatic()) &&
				!(nextComp != null && prevComp != null && prevComp.usesNextCompAutomatic() && nextComp.usesPreviousCompAutomatic())) {
			checkAutoOuterRadius.setEnabled(true);
			checkAutoOuterRadius.setToolTipText(trans.get("BodyTubecfg.checkbox.ttip.Automatic"));
		} else {
			checkAutoOuterRadius.setEnabled(false);
			((BodyTube) component).setOuterRadiusAutomatic(false);
			checkAutoOuterRadius.setToolTipText(trans.get("BodyTubecfg.checkbox.ttip.Automatic_alreadyAuto"));
		}
	}
}
