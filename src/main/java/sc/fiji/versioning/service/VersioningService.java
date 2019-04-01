package sc.fiji.versioning.service;

import net.imagej.ImageJService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.scijava.command.CommandInfo;
import org.scijava.plugin.PluginInfo;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Service for getting class dependencies via asm.
 *
 * @author Deborah Schmidt
 */
public interface VersioningService extends ImageJService {

	void commitCurrentStatus() throws Exception;
	List<AppCommit> getCommits() throws Exception;
	void restoreStatus(String id) throws Exception;

	/**
	 * @param id The id of the commit that will be merged with
	 * @throws Exception Exception in case version management tool fails
	 */
	void deleteStatus(String id) throws Exception;

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
}
