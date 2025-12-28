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
import java.util.concurrent.ExecutionException;

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
		TaskResult<MotorDatabaseMetadata> remoteResult = runWithProgressDialog(
				trans.get("MotorDbUpdate.Checking.title"),
				trans.get("MotorDbUpdate.Checking.message"),
				updater::fetchRemoteMetadata);

		MotorDatabaseMetadata remote = remoteResult.value;
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
		TaskResult<Boolean> installedResult = runWithProgressDialog(
				trans.get("MotorDbUpdate.Downloading.title"),
				trans.get("MotorDbUpdate.Downloading.message"),
				() -> {
					updater.installRemoteDatabase(motorLibraryDir, remote);
					return Boolean.TRUE;
				});

		if (installedResult.value == null || !installedResult.value) {
			// Unable to download and install the motor database update
			String failureMessage = trans.get("MotorDbUpdate.Failed.message");
			if (installedResult.error != null) {
				failureMessage = failureMessage + "\n\n" + formatThrowable(installedResult.error);
			}
			JOptionPane.showMessageDialog(null, failureMessage,
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

	private static final class TaskResult<T> {
		private final T value;
		private final Throwable error;

		private TaskResult(T value, Throwable error) {
			this.value = value;
			this.error = error;
		}
	}

	/**
	 * Run a task in the background while showing a modal progress dialog.
	 *
	 * @param title   the dialog title
	 * @param message the dialog message
	 * @param task    the task to run
	 * @return the result of the task, or null if an error occurred
	 */
	private static <T> TaskResult<T> runWithProgressDialog(String title, String message, WorkerTask<T> task) {
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
			return new TaskResult<>(worker.get(), null);
		} catch (Exception e) {
			Throwable t = unwrapWorkerException(e);
			log.info("Motor database update check/download failed: {}", t.getMessage());
			log.debug("Motor database update check/download failed", t);
			return new TaskResult<>(null, t);
		} finally {
			dialog.dispose();
		}
	}

	private static Throwable unwrapWorkerException(Exception e) {
		if (e instanceof ExecutionException && e.getCause() != null) {
			return e.getCause();
		}
		return e;
	}

	private static String formatThrowable(Throwable t) {
		if (t == null) {
			return "";
		}
		String msg = t.getMessage();
		if (msg == null || msg.trim().isEmpty()) {
			return t.getClass().getName();
		}
		return msg;
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
