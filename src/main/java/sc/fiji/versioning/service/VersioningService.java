package sc.fiji.versioning.service;

import net.imagej.ImageJService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.scijava.command.CommandInfo;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Service for versioning an ImageJ installation.
 *
 * @author Deborah Schmidt
 */
public interface VersioningService extends ImageJService {

	/**
	 * Creates a commit including all changes made to the ImageJ
	 * installation since the last commit
	 * @throws Exception
	 */
	void commitCurrentChanges() throws Exception;

	/**
	 * Returns a list of all commits representing saved states of
	 * the current ImageJ installation.
	 * @return List of all commits
	 * @throws Exception in case version management tool fails
	 */
	List<AppCommit> getCommits() throws Exception;

	/**
	 * Restores a commit and discards all saved ImageJ states
	 * saved since this commit
	 * @param id The id of the commit that will be restored
	 * @throws Exception in case version management tool fails
	 */
	void restoreCommit(String id) throws Exception;

	/**
	 * Merges a commit with the next recent commit
	 * @param id The id of the commit that will be merged
	 * @throws Exception Exception in case version management tool fails
	 */
	void mergeCommitWithNext(String id) throws Exception;

	/**
	 * Returns all changes made to the current ImageJ installation
	 * which are not yet tracked by the version management tool
	 * @return list of file changes
	 * @throws Exception in case version management tool fails
	 */
	List<FileChange> getCurrentChanges() throws Exception;

	/**
	 * Checks whether the ImageJ installation contains changes
	 * which are not yet tracked by the version management tool
	 * @return true if installation has unsaved changes, false otherwise
	 * @throws Exception in case version management tool fails
	 */
	boolean hasUnsavedChanges() throws Exception;

	/**
	 * Discards specific file change of the most recent commit
	 * @param fileChange file change that will be discarded
	 * @throws Exception in case version management tool fails
	 */
	void discardChange(FileChange fileChange) throws Exception;

	/**
	 * Discards specific file changes of the most recent commit
	 * @param fileChanges that will be discarded
	 * @throws Exception in case version management tool fails
	 */
	void discardChange(List<FileChange> fileChanges) throws Exception;

	/**
	 * Undo the changes of the commit that was created most recently
	 * @throws Exception in case version management tool fails
	 */
	void undoLastCommit() throws Exception;

	/**
	 * Restores the very first saved state of the current ImageJ installation
	 * @throws Exception in case version management tool fails
	 */
	default void restoreInitialCommit() throws Exception {
		restoreCommit(getCommits().get(0).id);
	}

	/**
	 * Returns a list of changes between two commit versions.
	 * @param id1 The id of the first commit
	 * @param id2 The id of the second commit
	 * @return
	 */
	List<FileChange> getChanges(String id1, String id2) throws Exception;

	File getBaseDirectory();

	void setBaseDirectory(File dir);
}
