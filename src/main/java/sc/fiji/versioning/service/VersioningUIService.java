package sc.fiji.versioning.service;

import net.imagej.ImageJService;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.util.Progress;
import sc.fiji.versioning.model.FileChange;

import java.awt.*;
import java.util.List;

/**
 * UI Service for versioning an ImageJ installation.
 *
 * @author Deborah Schmidt
 */
public interface VersioningUIService extends ImageJService {

	/**
	 * Open dialog and wait for response to approve a list of changes.
	 * @param changes the list of changes to be approved
	 * @return true if approved, false if not
	 */
	default boolean approveChanges(List<FileChange> changes) {
		return approveChanges(changes, "");
	}

	/**
	 * Open dialog including the message and wait for response to approve a list of changes.
	 * @param changes the list of changes to be approved
	 * @return true if approved, false if not
	 */
	boolean approveChanges(List<FileChange> changes, String message);

	/**
	 * Open dialog and wait for response to approve suggested changes stored in files.
	 * @param files The FilesCollection containing the changes which need to be approved
	 * @return true if approved, false if not
	 */
	boolean approveChanges(FilesCollection files);

	/**
	 * @return a dialog indicating progress
	 */
	Progress getProgressDialog();

	/**
	 * Shows the history (all recorded changes) of the current session.
	 */
	void showSessionHistory();

	<T> T askFor(String question, Class<T> returnType);

	void showSessions();
}
