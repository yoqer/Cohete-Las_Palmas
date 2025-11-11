package info.openrocket.swing.gui.components;

import java.awt.event.ActionEvent;

import javax.swing.JComboBox;

import info.openrocket.core.appearance.DecalImage;
import info.openrocket.swing.gui.adaptors.DecalModel;

/**
 * Dedicated combo box for texture selection that encapsulates the required
 * DecalModel behaviour and keeps the UI wiring in one place.
 */
public class TextureComboBox extends JComboBox<DecalImage> {
	private static final long serialVersionUID = 2493299948337060422L;

	private DecalImage previousSelection;

	public TextureComboBox(DecalModel decalModel) {
		super(decalModel);
		setMaximumRowCount(20);
		previousSelection = (DecalImage) decalModel.getSelectedItem();
		addActionListener(this::handleSelectionChange);
	}

	private void handleSelectionChange(ActionEvent e) {
		DecalModel model = (DecalModel) getModel();
		DecalImage decal = (DecalImage) getSelectedItem();

		// Trigger refreshes even if the same item is re-selected
		if (decal == previousSelection) {
			model.setSelectedItem(decal);
		}
		previousSelection = decal;
	}
}
