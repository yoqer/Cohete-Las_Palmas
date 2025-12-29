package info.openrocket.swing.startup;

import info.openrocket.core.arch.SystemInfo;
import info.openrocket.core.database.MotorDatabaseMetadataIO;
import info.openrocket.core.database.MotorDatabaseMetadata;
import info.openrocket.core.database.MotorDatabaseRemoteUpdater;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.preferences.ApplicationPreferences;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.gui.util.GUIUtil;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.awt.Component;

/**
 * Startup-time motor database update checker that can optionally install the latest remote motor database.
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public abstract class MotorDatabaseUpdateChecker {
	private static final Logger log = LoggerFactory.getLogger(MotorDatabaseUpdateChecker.class);
	private static final Translator trans = Application.getTranslator();
	private static final ApplicationPreferences prefs = Application.getPreferences();

	/**
	 * Check for motor database updates, and if an update is available,
	 * prompt the user to install it. If the user agrees, download and
	 * install the latest motor database.
	 */
	public static void checkForUpdatesAndInstallIfRequested() {
		if (!prefs.getCheckMotorDatabaseUpdates()) {
			return;
		}
		checkForUpdatesAndInstallIfRequested(null, false);
	}

	/**
	 * Check for motor database updates immediately (ignores the "check at startup" preference).
	 */
	public static void checkForUpdatesNowAndInstallIfRequested(Component parent) {
		checkForUpdatesAndInstallIfRequested(parent, true);
	}

	private static void checkForUpdatesAndInstallIfRequested(Component parent, boolean userInitiated) {
		File motorLibraryDir = SystemInfo.getOpenRocketMotorLibraryDirectory();
		if (motorLibraryDir == null) {
			if (userInitiated) {
				JOptionPane.showMessageDialog(parent,
						trans.get("MotorDbUpdate.Failed.message"),
						trans.get("MotorDbUpdate.Failed.title"),
						JOptionPane.WARNING_MESSAGE);
			}
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
			if (userInitiated) {
				String failureMessage = trans.get("MotorDbUpdate.Failed.message");
				if (remoteResult.error != null) {
					failureMessage = failureMessage + "\n\n" + formatThrowable(remoteResult.error);
				}
				JOptionPane.showMessageDialog(parent, failureMessage,
						trans.get("MotorDbUpdate.Failed.title"),
						JOptionPane.WARNING_MESSAGE);
			}
			return;
		}

		String localVersion = trans.get("MotorDbUpdate.Available.yourVersion.Unknown");
		try {
			long v = MotorDatabaseMetadataIO.readDatabaseVersion(new File(motorLibraryDir, "metadata.json"));
			if (v > 0) {
				localVersion = Long.toString(v);
			}
		} catch (Exception ignore) {
		}

		if (!updater.isRemoteNewer(motorLibraryDir, remote)) {
			if (userInitiated) {
				String message = trans.get("MotorDbUpdate.Latest.message") + "\n\n" +
						trans.get("MotorDbUpdate.Available.yourVersion") + " " + localVersion + "\n" +
						trans.get("MotorDbUpdate.Available.remoteVersion") + " " + remote.getDatabaseVersion();
				JOptionPane.showMessageDialog(parent, message,
						trans.get("MotorDbUpdate.Latest.title"),
						JOptionPane.INFORMATION_MESSAGE);
			}
			return;
		}

		String remoteVersion = Long.toString(remote.getDatabaseVersion());
		if (prefs.getIgnoreMotorDatabaseUpdateVersions().contains(remoteVersion)) {
			if (userInitiated) {
				String message = trans.get("MotorDbUpdate.Skipped.message") + "\n\n" +
						trans.get("MotorDbUpdate.Available.remoteVersion") + " " + remoteVersion;
				JOptionPane.showMessageDialog(parent, message,
						trans.get("MotorDbUpdate.Skipped.title"),
						JOptionPane.INFORMATION_MESSAGE);
			}
			return;
		}

		JPanel prompt = new JPanel(new MigLayout("fillx, ins 0", "[grow]"));
		String message = "<html>" +
				trans.get("MotorDbUpdate.Available.message") + "<br><br>" +
				trans.get("MotorDbUpdate.Available.yourVersion") + " <b>" + localVersion + "</b><br>" +
				trans.get("MotorDbUpdate.Available.remoteVersion") + " <b>" + remoteVersion + "</b><br>" +
				trans.get("MotorDbUpdate.Available.question") +
				"</html>";
		prompt.add(new JLabel(message), "growx, wrap para");

		JCheckBox dontAskAgain = new JCheckBox(trans.get("MotorDbUpdate.Available.dontAskAgain"));
		dontAskAgain.setToolTipText(trans.get("MotorDbUpdate.Available.dontAskAgain.ttip"));
		prompt.add(dontAskAgain, "growx, wrap");

		Object[] options = new Object[]{
				trans.get("MotorDbUpdate.Available.btn.install"),
				trans.get("MotorDbUpdate.Available.btn.later"),
				trans.get("MotorDbUpdate.Available.btn.skipVersion"),
		};

		int res = JOptionPane.showOptionDialog(
				parent,
				prompt,
				trans.get("MotorDbUpdate.Available.title"),
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null,
				options,
				options[0]);

		if (dontAskAgain.isSelected()) {
			prefs.setCheckMotorDatabaseUpdates(false);
		}

		if (res == 2) {
			List<String> ignored = new ArrayList<>(prefs.getIgnoreMotorDatabaseUpdateVersions());
			if (!ignored.contains(remoteVersion)) {
				ignored.add(remoteVersion);
				prefs.setIgnoreMotorDatabaseUpdateVersions(ignored);
			}
			return;
		}
		if (res != 0) {
			return;
		}

		// Downloading motor database
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
			JOptionPane.showMessageDialog(parent, failureMessage,
					trans.get("MotorDbUpdate.Failed.title"),
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Motor database updated successfully
		JOptionPane.showMessageDialog(parent,
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
