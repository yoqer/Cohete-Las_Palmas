package info.openrocket.swing.gui.main;

import info.openrocket.core.appearance.DecalImage;
import info.openrocket.core.startup.Application;
import info.openrocket.core.util.DecalNotFoundException;
import info.openrocket.core.util.StateChangeListener;
import info.openrocket.swing.gui.util.OverwritePrompter;
import info.openrocket.swing.gui.util.SwingPreferences;
import info.openrocket.swing.gui.widgets.SaveFileChooser;
import info.openrocket.swing.util.BaseTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Window;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class ExportDecalActionTest extends BaseTestCase {

	@TempDir
	Path tempDir;

	private SwingPreferences preferences;
	private OverwritePromptController promptController;

	@BeforeEach
	void setUpPrefs() {
		preferences = (SwingPreferences) Application.getPreferences();
		preferences.setDefaultDirectory(tempDir.toFile());
		promptController = new OverwritePromptController();
		ExportDecalAction.setOverwritePrompt(promptController);
	}

	@AfterEach
	void resetPrompt() {
		ExportDecalAction.setOverwritePrompt(null);
	}

	@Test
	void openChooserDialogSingleDecalConfiguresSaveMode() {
		TestFileChooser chooser = new TestFileChooser(JFileChooser.APPROVE_OPTION);
		List<DecalImage> decals = List.of(new RecordingDecal("decals/alpha.png"));

		int result = ExportDecalAction.openChooserDialog(null, chooser, decals);

		assertThat(result).isEqualTo(JFileChooser.APPROVE_OPTION);
		assertThat(chooser.saveDialogCalls).isEqualTo(1);
		assertThat(chooser.openDialogCalls).isZero();
		assertThat(chooser.getDialogType()).isEqualTo(JFileChooser.SAVE_DIALOG);
		assertThat(chooser.getFileSelectionMode()).isEqualTo(JFileChooser.FILES_ONLY);
		assertThat(chooser.getSelectedFile()).isEqualTo(new File(tempDir.toFile(), "alpha.png"));
	}

	@Test
	void openChooserDialogMultipleDecalsConfiguresDirectoryMode() {
		TestFileChooser chooser = new TestFileChooser(JFileChooser.APPROVE_OPTION);
		List<DecalImage> decals = List.of(
				new RecordingDecal("decals/alpha.png"),
				new RecordingDecal("decals/beta.png"));

		int result = ExportDecalAction.openChooserDialog(null, chooser, decals);

		assertThat(result).isEqualTo(JFileChooser.APPROVE_OPTION);
		assertThat(chooser.openDialogCalls).isEqualTo(1);
		assertThat(chooser.saveDialogCalls).isZero();
		assertThat(chooser.getDialogType()).isEqualTo(JFileChooser.OPEN_DIALOG);
		assertThat(chooser.getFileSelectionMode()).isEqualTo(JFileChooser.DIRECTORIES_ONLY);
	}

	@Test
	void handleApprovalSingleDecalExportsChosenFileAndUpdatesPreferences() throws Exception {
		TestFileChooser chooser = new TestFileChooser(JFileChooser.APPROVE_OPTION);
		File selected = tempDir.resolve("custom.png").toFile();
		chooser.setCurrentDirectory(tempDir.toFile());
		chooser.setSelectedFile(selected);

		RecordingDecal decal = new RecordingDecal("decals/source.png");
		List<DecalImage> decals = List.of(decal);
		chooser.forceSelection(selected);
		ExportDecalAction.openChooserDialog(null, chooser, decals);
		chooser.setCurrentDirectory(tempDir.toFile());
		chooser.setSelectedFile(selected);

		boolean exported = ExportDecalAction.handleApproval(null, chooser, decals);

		assertThat(exported).isTrue();
		assertThat(decal.getExportTargets()).containsExactly(selected);
		assertThat(preferences.getDefaultDirectory()).isEqualTo(tempDir.toFile());
		assertThat(selected).exists();
	}

	@Test
	void handleApprovalMultipleDecalsExportsEachAndUsesSelectedDirectory() throws Exception {
		TestFileChooser chooser = new TestFileChooser(JFileChooser.APPROVE_OPTION);
		File exportDir = tempDir.resolve("exports").toFile();
		assertThat(exportDir.mkdir()).isTrue();
		List<DecalImage> decals = List.of(new RecordingDecal("tex/first.png"),
				new RecordingDecal("tex/second.png"));
		chooser.forceSelection(exportDir);
		ExportDecalAction.openChooserDialog(null, chooser, decals);
		chooser.setCurrentDirectory(tempDir.toFile());
		chooser.setSelectedFile(exportDir);
		createFile(exportDir, "first.png");
		createFile(exportDir, "second.png");
		promptController.setResponses(OverwritePrompter.Choice.OVERWRITE_ALL);

		RecordingDecal first = (RecordingDecal) decals.get(0);
		RecordingDecal second = (RecordingDecal) decals.get(1);

		boolean exported = ExportDecalAction.handleApproval(null, chooser, decals);

		assertThat(exported).isTrue();
		assertThat(first.getExportTargets()).containsExactly(new File(exportDir, "first.png"));
		assertThat(second.getExportTargets()).containsExactly(new File(exportDir, "second.png"));
		assertThat(preferences.getDefaultDirectory()).isEqualTo(exportDir);
		assertThat(new File(exportDir, "first.png")).exists();
		assertThat(new File(exportDir, "second.png")).exists();
	}

	@Test
	void handleApprovalMultipleDecalsStopsWhenUserCancelsOverwrite() throws Exception {
		TestFileChooser chooser = new TestFileChooser(JFileChooser.APPROVE_OPTION);
		File exportDir = tempDir.resolve("exports").toFile();
		assertThat(exportDir.mkdir()).isTrue();
		RecordingDecal first = new RecordingDecal("tex/first.png");
		List<DecalImage> decals = List.of(first);
		chooser.forceSelection(exportDir);
		ExportDecalAction.openChooserDialog(null, chooser, decals);
		chooser.setCurrentDirectory(tempDir.toFile());
		chooser.setSelectedFile(exportDir);
		createFile(exportDir, "first.png");
		promptController.setResponses(OverwritePrompter.Choice.CANCEL);

		boolean exported = ExportDecalAction.handleApproval(null, chooser, decals);

		assertThat(exported).isFalse();
		assertThat(preferences.getDefaultDirectory()).isEqualTo(tempDir.toFile());
	}

	@Test
	void overwriteSingleFileDoesNotAffectSubsequentNonConflictingExports() throws Exception {
		TestFileChooser chooser = new TestFileChooser(JFileChooser.APPROVE_OPTION);
		File exportDir = tempDir.resolve("exports").toFile();
		assertThat(exportDir.mkdir()).isTrue();
		List<DecalImage> decals = List.of(new RecordingDecal("tex/first.png"),
				new RecordingDecal("tex/second.png"));
		chooser.forceSelection(exportDir);
		ExportDecalAction.openChooserDialog(null, chooser, decals);
		chooser.setCurrentDirectory(tempDir.toFile());
		chooser.setSelectedFile(exportDir);
		createFile(exportDir, "first.png");
		promptController.setResponses(OverwritePrompter.Choice.OVERWRITE);

		RecordingDecal first = (RecordingDecal) decals.get(0);
		RecordingDecal second = (RecordingDecal) decals.get(1);

		boolean exported = ExportDecalAction.handleApproval(null, chooser, decals);

		assertThat(exported).isTrue();
		assertThat(first.getExportTargets()).containsExactly(new File(exportDir, "first.png"));
		assertThat(second.getExportTargets()).containsExactly(new File(exportDir, "second.png"));
	}

	private void createFile(File directory, String name) throws IOException {
		File target = new File(directory, name);
		try (FileOutputStream stream = new FileOutputStream(target)) {
			stream.write(new byte[] { 0x1 });
		}
	}

	private static final class TestFileChooser extends SaveFileChooser {
		private final int returnValue;
		private int saveDialogCalls;
		private int openDialogCalls;
		private File forcedSelection;

		private TestFileChooser(int returnValue) {
			this.returnValue = returnValue;
		}
		
		void forceSelection(File file) {
			this.forcedSelection = file;
		}

		@Override
		public int showSaveDialog(Component parent) throws HeadlessException {
			if (forcedSelection != null) {
				super.setSelectedFile(forcedSelection);
			}
			saveDialogCalls++;
			return returnValue;
		}

		@Override
		public int showOpenDialog(Component parent) throws HeadlessException {
			if (forcedSelection != null) {
				super.setSelectedFile(forcedSelection);
			}
			openDialogCalls++;
			return returnValue;
		}
	}

	private static final class RecordingDecal implements DecalImage {
		private final String name;
		private final List<File> exportTargets = new ArrayList<>();

		private RecordingDecal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public InputStream getBytes() throws IOException, DecalNotFoundException {
			return new ByteArrayInputStream(new byte[] { 0x1 });
		}

		@Override
		public void exportImage(File file) throws IOException, DecalNotFoundException {
			exportTargets.add(file);
			try (FileOutputStream outputStream = new FileOutputStream(file)) {
				outputStream.write(new byte[] { 0x5 });
			}
		}

		@Override
		public void fireChangeEvent(Object source) {
			// not needed
		}

		@Override
		public File getDecalFile() {
			return null;
		}

		@Override
		public void setDecalFile(File file) {
			// not needed
		}

		@Override
		public boolean isIgnored() {
			return false;
		}

		@Override
		public void setIgnored(boolean ignored) {
			// not needed
		}

		@Override
		public int compareTo(DecalImage o) {
			return this.getName().compareTo(o.getName());
		}

		@Override
		public void addChangeListener(StateChangeListener listener) {
			// not needed
		}

		@Override
		public void removeChangeListener(StateChangeListener listener) {
			// not needed
		}
		List<File> getExportTargets() {
			return exportTargets;
		}
	}

	private static final class OverwritePromptController implements OverwritePrompter.Prompt {
		private Queue<OverwritePrompter.Choice> responses = new ArrayDeque<>();

		void setResponses(OverwritePrompter.Choice... choices) {
			this.responses = new ArrayDeque<>(Arrays.asList(choices));
		}

		@Override
		public OverwritePrompter.Choice request(Window parent, File target) {
			if (responses.isEmpty()) {
				throw new AssertionError("Unexpected overwrite prompt for file " + target);
			}
			return responses.remove();
		}
	}
}
