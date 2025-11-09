package info.openrocket.swing.gui.main;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import info.openrocket.core.appearance.DecalImage;
import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import info.openrocket.core.util.DecalNotFoundException;

import info.openrocket.swing.gui.dialogs.DecalNotFoundDialog;
import info.openrocket.swing.gui.util.FileHelper;
import info.openrocket.swing.gui.util.SwingPreferences;

public abstract class ExportDecalAction {
	
	private final static Translator trans = Application.getTranslator();

	public static void export(Window parent, OpenRocketDocument doc) {
		final JFileChooser chooser = new JFileChooser();
		final List<DecalImage> selectedDecals;
		
		Collection<DecalImage> exportableDecals = doc.getDecalList();
		if (exportableDecals.isEmpty()) {
			JOptionPane.showMessageDialog(parent, trans.get("ExportDecalDialog.selection.required"));
			return;
		}
		
		ExportDecalSelectionDialog selectionDialog = new ExportDecalSelectionDialog(parent, exportableDecals);
		selectionDialog.setLocationRelativeTo(parent);
		selectionDialog.setVisible(true);
		
		if (!selectionDialog.isApproved()) {
			return;
		}
		
		List<DecalImage> decals = selectionDialog.getSelectedDecals();
		if (decals.isEmpty()) {
			return;
		}
		selectedDecals = new ArrayList<>(decals);

		int returnVal = openChooserDialog(parent, chooser, selectedDecals);

		// Cancelled file chooser
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}

		// Approved file chooser
		handleApproval(parent, chooser, selectedDecals);
	}
	
	private static int openChooserDialog(Window parent, JFileChooser chooser, List<DecalImage> selectedDecals) {
		File dir = ((SwingPreferences) Application.getPreferences()).getDefaultDirectory();
		if (selectedDecals.size() == 1) {
			chooser.setDialogType(JFileChooser.SAVE_DIALOG);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			File fullName = new File(selectedDecals.get(0).getName());
			chooser.setCurrentDirectory(dir);
			chooser.setSelectedFile(new File(dir, fullName.getName()));
			return chooser.showSaveDialog(parent);
		} else {
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setCurrentDirectory(dir);
			return chooser.showOpenDialog(parent);
		}
	}
	
	private static boolean handleApproval(Window parent, JFileChooser chooser, List<DecalImage> selectedDecals) {
		File selectedFile = chooser.getSelectedFile();
		if (selectedDecals.size() == 1) {
			DecalImage decal = selectedDecals.get(0);
			File file = selectedFile;
			if (file == null) {
				file = new File(chooser.getCurrentDirectory(), new File(decal.getName()).getName());
			}
			((SwingPreferences) Application.getPreferences()).setDefaultDirectory(chooser.getCurrentDirectory());
			return export(parent, decal, file);
		} else {
			File targetDirectory = selectedFile;
			if (targetDirectory == null) {
				targetDirectory = chooser.getCurrentDirectory();
			}
			if (ensureDirectory(parent, targetDirectory)) {
				((SwingPreferences) Application.getPreferences()).setDefaultDirectory(targetDirectory);
				return exportMultiple(parent, selectedDecals, targetDirectory);
			}
		}

		return false;
	}
	
	private static boolean ensureDirectory(Window parent, File directory) {
		if (directory == null) {
			JOptionPane.showMessageDialog(parent, trans.get("ExportDecalDialog.directory.required"));
			return false;
		}
		if (!directory.exists() && !directory.mkdirs()) {
			JOptionPane.showMessageDialog(parent, trans.get("ExportDecalDialog.directory.required"));
			return false;
		}
		if (!directory.isDirectory()) {
			JOptionPane.showMessageDialog(parent, trans.get("ExportDecalDialog.directory.required"));
			return false;
		}
		return true;
	}
	
	private static boolean export(Window parent, DecalImage decal, File selectedFile) {
		
		if (selectedFile == null) {
			return false;
		}
		
		if (!FileHelper.confirmWrite(selectedFile, parent)) {
			return false;
		}
		
		try {
			decal.exportImage(selectedFile);
			return true;
		} catch (DecalNotFoundException e) {
			DecalNotFoundDialog.showDialog(parent, e);
		} catch (IOException iex) {
			String message = MessageFormat.format(trans.get("ExportDecalDialog.exception"), selectedFile.getAbsoluteFile());
			JOptionPane.showMessageDialog(parent, message, "", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
	
	private static boolean exportMultiple(Window parent, List<DecalImage> decals, File targetDirectory) {
		boolean exported = false;
		for (DecalImage decal : decals) {
			File sourceName = new File(decal.getName());
			File exportTarget = new File(targetDirectory, sourceName.getName());
			if (!export(parent, decal, exportTarget)) {
				return false;
			}
			exported = true;
		}
		return exported;
	}
}
