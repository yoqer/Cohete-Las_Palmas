package info.openrocket.swing.gui.configdialog;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import net.miginfocom.swing.MigLayout;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.material.Material;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.Transition;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import info.openrocket.swing.gui.adaptors.BooleanModel;
import info.openrocket.swing.gui.adaptors.CustomFocusTraversalPolicy;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.adaptors.TransitionShapeModel;
import info.openrocket.swing.gui.components.DescriptionArea;
import info.openrocket.swing.gui.components.SpinnerWithSlider;

public class TransitionConfig extends RocketComponentConfig {
	private static final long serialVersionUID = -1851275950604625741L;
	
	private static final Translator trans = Application.getTranslator();
	private JComboBox<Transition.Shape> typeBox;

	private JLabel shapeLabel;
	private SpinnerWithSlider shapeSpinner;
	private final JCheckBox checkAutoAftRadius;
	private final JCheckBox checkAutoForeRadius;
	private DescriptionArea description;
	

	// Prepended to the description from Transition.DESCRIPTIONS
	private static final String PREDESC = "<html>";
	
	
	public TransitionConfig(OpenRocketDocument d, RocketComponent c, JDialog parent) {
		super(d, c, parent);
		
		final JPanel panel = new JPanel(new MigLayout("gap rel unrel, fillx", "[][65lp::][30lp::]", ""));
		SpinnerWithSlider spinnerWithSlider;

		////  Shape selection
		//// Transition shape:
		panel.add(new JLabel(trans.get("TransitionCfg.lbl.Transitionshape")));
		
		typeBox = new JComboBox<>(new TransitionShapeModel(c));
		typeBox.setEditable(false);
		typeBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Transition.Shape s = (Transition.Shape) typeBox.getSelectedItem();
				if (s != null) {
					description.setText(PREDESC + s.getTransitionDescription());
				}
				updateEnabled();
			}
		});
		panel.add(typeBox, "span 3, split 2");
		order.add(typeBox);

		{//// Clipped
			BooleanModel bm = new BooleanModel(component, "Clipped");
			register(bm);
			final JCheckBox checkbox = new JCheckBox(bm);
			checkbox.setText(trans.get("TransitionCfg.checkbox.Clipped"));
			checkbox.setToolTipText(trans.get("TransitionCfg.checkbox.Clipped.ttip"));
			panel.add(checkbox, "wrap");
			order.add(checkbox);
		}

		{
			////  Shape parameter:
			this.shapeLabel = new JLabel(trans.get("TransitionCfg.lbl.Shapeparam"));
			panel.add(shapeLabel);

			final DoubleModel shapeModel = new DoubleModel(component, "ShapeParameter", UnitGroup.UNITS_SHAPE_PARAMETER, 0, 1);
			register(shapeModel);

			DoubleModel min = new DoubleModel(component, "ShapeParameterMin");
			DoubleModel max = new DoubleModel(component, "ShapeParameterMax");
			register(min);
			register(max);
			this.shapeSpinner = new SpinnerWithSlider(shapeModel, min, max, false);
			panel.add(shapeSpinner, "growx, wrap");
			order.add(shapeSpinner.getTextField());

			updateEnabled();
		}

		{/// Transition length:

			panel.add(new JLabel(trans.get("TransitionCfg.lbl.Transitionlength")));

			final DoubleModel lengthModel = new DoubleModel(component, "Length", UnitGroup.UNITS_LENGTH, 0);
			register(lengthModel);

			SpinnerWithSlider lengthSpinner = new SpinnerWithSlider(lengthModel, 0, 0.05, 0.3);
			panel.add(lengthSpinner, "growx, spanx 2, wrap");
			order.add(lengthSpinner.getTextField());
		}

		{ /// Fore diameter:
			panel.add(new JLabel(trans.get("TransitionCfg.lbl.Forediam")));

	         // Diameter = 2*Radius
			final DoubleModel foreRadiusModel = new DoubleModel(component, "ForeRadius", 2, UnitGroup.UNITS_LENGTH, 0);
			register(foreRadiusModel);

			spinnerWithSlider = new SpinnerWithSlider(foreRadiusModel, 0, 0.04, 0.2);
			panel.add(spinnerWithSlider, "growx, spanx 2, wrap 0px");
			order.add(spinnerWithSlider.getTextField());

			checkAutoForeRadius = new JCheckBox(foreRadiusModel.getAutomaticAction());
			//// Automatic
			checkAutoForeRadius.setText(trans.get("TransitionCfg.checkbox.Automatic"));
			panel.add(checkAutoForeRadius, "skip, span 2, wrap");
			order.add(checkAutoForeRadius);
			updateCheckboxAutoForeRadius();
		}

		{	//// Aft diameter:
			panel.add(new JLabel(trans.get("TransitionCfg.lbl.Aftdiam")));

            // Diameter = 2*Radius
			final DoubleModel aftRadiusModel = new DoubleModel(component, "AftRadius", 2, UnitGroup.UNITS_LENGTH, 0);
			register(aftRadiusModel);

			spinnerWithSlider = new SpinnerWithSlider(aftRadiusModel, 0, 0.04, 0.2);
			panel.add(spinnerWithSlider, "growx, spanx 2, wrap 0px");
			order.add(spinnerWithSlider.getTextField());

			checkAutoAftRadius = new JCheckBox(aftRadiusModel.getAutomaticAction());
			//// Automatic
			checkAutoAftRadius.setText(trans.get("TransitionCfg.checkbox.Automatic"));
			panel.add(checkAutoAftRadius, "skip, span 2, wrap");
			order.add(checkAutoAftRadius);
			updateCheckboxAutoAftRadius();
		}

		{ ///  Wall thickness:
			panel.add(new JLabel(trans.get("TransitionCfg.lbl.Wallthickness")));

			final DoubleModel thicknessModel = new DoubleModel(component, "Thickness", UnitGroup.UNITS_LENGTH, 0);
			register(thicknessModel);
			DoubleModel maxRadius = new DoubleModel(component, "MaxRadius", UnitGroup.UNITS_LENGTH);
			register(maxRadius);
			DoubleModel zeroModel = new DoubleModel(0, UnitGroup.UNITS_LENGTH);
			register(zeroModel);
			spinnerWithSlider = new SpinnerWithSlider(thicknessModel, zeroModel, maxRadius);
			panel.add(spinnerWithSlider, "growx, spanx 2, wrap 0px");
			order.add(spinnerWithSlider.getTextField());

			//// Filled
			final JCheckBox thicknessCheckbox = new JCheckBox(new BooleanModel(component, "Filled"));
			//// Filled
			thicknessCheckbox.setText(trans.get("TransitionCfg.checkbox.Filled"));
			thicknessCheckbox.setToolTipText(trans.get("TransitionCfg.checkbox.Filled.ttip"));
			panel.add(thicknessCheckbox, "skip, span 2, wrap");
			order.add(thicknessCheckbox);
		}

		////  Description
		JPanel panel2 = new JPanel(new MigLayout("ins 0"));
		
		description = new DescriptionArea(5);
		description.setText(PREDESC + ((Transition) component).getShapeType().
				getTransitionDescription());
		panel2.add(description, "wmin 250lp, spanx, growx, wrap para");
		

		//// Material
		MaterialPanel materialPanel = new MaterialPanel(component, document, Material.Type.BULK, order);
		register(materialPanel);
		panel2.add(materialPanel, "span, wrap");
		panel.add(panel2, "cell 4 0, gapleft 40lp, aligny 0%, spany");
		
		//// General and General properties
		tabbedPane.insertTab(trans.get("TransitionCfg.tab.General"), null, panel,
				trans.get("TransitionCfg.tab.Generalproperties"), 0);
		//// Shoulder and Shoulder properties
		tabbedPane.insertTab(trans.get("TransitionCfg.tab.Shoulder"), null, shoulderTab(),
				trans.get("TransitionCfg.tab.Shoulderproperties"), 1);
		tabbedPane.setSelectedIndex(0);

		// Apply the custom focus travel policy to this config dialog
		//// Make sure the cancel & ok button is the last component
		order.add(cancelButton);
		order.add(okButton);
		CustomFocusTraversalPolicy policy = new CustomFocusTraversalPolicy(order);
		parent.setFocusTraversalPolicy(policy);
	}
	
	

	private void updateEnabled() {
		boolean e = ((Transition) component).getShapeType().usesParameter();
		shapeLabel.setEnabled(e);
		shapeSpinner.setEnabled(e);
	}

	/**
	 * Sets the checkAutoAftRadius checkbox's enabled state and tooltip text, based on the state of its next component.
	 * If there is no next symmetric component or if that component already has its auto checkbox checked, the
	 * checkAutoAftRadius checkbox is disabled.
	 */
	private void updateCheckboxAutoAftRadius() {
		if (component == null || checkAutoAftRadius == null) return;

		Transition transition = (Transition) component;
		boolean enabled = transition.canUseNextCompAutomatic();
		if (enabled) {														// Can use auto radius
			checkAutoAftRadius.setEnabled(true);
			checkAutoAftRadius.setToolTipText(trans.get("TransitionCfg.checkbox.ttip.Automatic"));
		} else if (transition.getNextSymmetricComponent() == null) {		// No next component to take the auto radius from
			checkAutoAftRadius.setEnabled(false);
			((Transition) component).setAftRadiusAutomatic(false);
			checkAutoAftRadius.setToolTipText(trans.get("TransitionCfg.checkbox.ttip.Automatic_noReferenceComponent"));
		} else {															// Next component already has its auto radius checked
			checkAutoAftRadius.setEnabled(false);
			((Transition) component).setAftRadiusAutomatic(false);
			checkAutoAftRadius.setToolTipText(trans.get("TransitionCfg.checkbox.ttip.Automatic_alreadyAuto"));
		}
	}

	/**
	 * Sets the checkAutoForeRadius checkbox's enabled state and tooltip text, based on the state of its next component.
	 * If there is no next symmetric component or if that component already has its auto checkbox checked, the
	 * checkAutoForeRadius checkbox is disabled.
	 */
	private void updateCheckboxAutoForeRadius() {
		if (component == null || checkAutoForeRadius == null) return;

		Transition transition = (Transition) component;
		boolean enabled = transition.canUsePreviousCompAutomatic();
		if (enabled) {														// Can use auto radius
			checkAutoForeRadius.setEnabled(true);
			checkAutoForeRadius.setToolTipText(trans.get("TransitionCfg.checkbox.ttip.Automatic"));
		} else if (transition.getPreviousSymmetricComponent() == null) {		// No next component to take the auto radius from
			checkAutoForeRadius.setEnabled(false);
			((Transition) component).setForeRadiusAutomatic(false);
			checkAutoForeRadius.setToolTipText(trans.get("TransitionCfg.checkbox.ttip.Automatic_noReferenceComponent"));
		} else {															// Next component already has its auto radius checked
			checkAutoForeRadius.setEnabled(false);
			((Transition) component).setForeRadiusAutomatic(false);
			checkAutoForeRadius.setToolTipText(trans.get("TransitionCfg.checkbox.ttip.Automatic_alreadyAuto"));
		}
	}
	
}
