package info.openrocket.swing.startup;

import info.openrocket.core.arch.SystemInfo;
import info.openrocket.core.database.MotorDatabaseMetadata;
import info.openrocket.core.database.MotorDatabaseRemoteUpdater;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.gui.util.GUIUtil;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.io.File;

/**
 * Startup-time motor database update checker that can optionally install the latest remote motor database.
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public final class MotorDatabaseUpdateChecker {
	private static final Logger log = LoggerFactory.getLogger(MotorDatabaseUpdateChecker.class);
	private static final Translator trans = Application.getTranslator();

	private MotorDatabaseUpdateChecker() {
	}

	/**
	 * Check for motor database updates, and if an update is available,
	 * prompt the user to install it. If the user agrees, download and
	 * install the latest motor database.
	 */
	public static void checkForUpdatesAndInstallIfRequested() {
		File motorLibraryDir = SystemInfo.getOpenRocketMotorLibraryDirectory();
		if (motorLibraryDir == null) {
			return;
		}

		MotorDatabaseRemoteUpdater updater = new MotorDatabaseRemoteUpdater();

		// Checking for motor database updates
		MotorDatabaseMetadata remote = runWithProgressDialog(
				trans.get("MotorDbUpdate.Checking.title"),
				trans.get("MotorDbUpdate.Checking.message"),
				updater::fetchRemoteMetadata);

		if (remote == null) {
			return;
		}
		if (!updater.isRemoteNewer(motorLibraryDir, remote)) {
			return;
		}

		String message = trans.get("MotorDbUpdate.Available.message") + "\n\n" +
				trans.get("MotorDbUpdate.Available.version") + " " + remote.getDatabaseVersion() + "\n" +
				trans.get("MotorDbUpdate.Available.question");

		// A newer motor database is available, do you want to install it?
		int res = JOptionPane.showConfirmDialog(null, message,
				trans.get("MotorDbUpdate.Available.title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.INFORMATION_MESSAGE);
		if (res != JOptionPane.YES_OPTION) {
			return;
		}

		// Downloading motor databas
		Boolean installed = runWithProgressDialog(
				trans.get("MotorDbUpdate.Downloading.title"),
				trans.get("MotorDbUpdate.Downloading.message"),
				() -> {
					updater.installRemoteDatabase(motorLibraryDir, remote);
					return Boolean.TRUE;
				});

		if (installed == null || !installed) {
			// Unable to download and install the motor database update
			JOptionPane.showMessageDialog(null,
					trans.get("MotorDbUpdate.Failed.message"),
					trans.get("MotorDbUpdate.Failed.title"),
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Motor database updated successfully
		JOptionPane.showMessageDialog(null,
				trans.get("MotorDbUpdate.Installed.message"),
				trans.get("MotorDbUpdate.Installed.title"),
				JOptionPane.INFORMATION_MESSAGE);
	}

	private interface WorkerTask<T> {
		T run() throws Exception;
	}

	/**
	 * Run a task in the background while showing a modal progress dialog.
	 *
	 * @param title   the dialog title
	 * @param message the dialog message
	 * @param task    the task to run
	 * @return the result of the task, or null if an error occurred
	 */
	private static <T> T runWithProgressDialog(String title, String message, WorkerTask<T> task) {
		final ProgressDialog dialog = new ProgressDialog(title, message);
		final SwingWorker<T, Void> worker = new SwingWorker<>() {
			@Override
			protected T doInBackground() throws Exception {
				return task.run();
			}

			@Override
			protected void done() {
				dialog.setVisible(false);
			}
		};

		worker.execute();
		dialog.setVisible(true);

		try {
			return worker.get();
		} catch (Exception e) {
			log.info("Motor database update check/download failed: {}", e.getMessage());
			return null;
		} finally {
			dialog.dispose();
		}
	}

	private static class ProgressDialog extends JDialog {
		ProgressDialog(String title, String message) {
			super(null, title, ModalityType.DOCUMENT_MODAL);

			JPanel panel = new JPanel(new MigLayout("fill"));
			panel.add(new JLabel(message), "wrap para");

			JProgressBar progress = new JProgressBar();
			progress.setIndeterminate(true);
			panel.add(progress, "growx");

			this.add(panel);
			this.pack();
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			this.setLocationByPlatform(true);
			GUIUtil.setWindowIcons(this);
		}
	}
}
