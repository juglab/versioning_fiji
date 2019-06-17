package sc.fiji.versioning.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.scijava.Initializable;
import org.scijava.app.AppService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Deborah Schmidt
 */
@Plugin(type = Service.class)
public class GitVersioningService extends AbstractService implements VersioningService, Initializable, AutoCloseable {

	@Parameter
	AppService appService;

	private File base;
	private Git git;

	@Override
	public void initialize() {
		base = appService.getApp().getBaseDirectory();
	}

	@Override
	public void commitCurrentChanges() throws IOException, GitAPIException {
		loadGit();
		GitCommands.commitCurrentStatus(git);
	}

	private void loadGit() throws GitAPIException, IOException {
		if(git != null && !git.getRepository().getDirectory().getParentFile().equals(base)) git = null;
		if(git == null) {
			git = GitCommands.initOrLoad(getBaseDirectory());
		}
	}

	@Override
	public List<AppCommit> getCommits() throws GitAPIException, IOException {
		loadGit();
		return GitCommands.getCommits(git);
	}

	@Override
	public void restoreCommit(String id) throws GitAPIException, IOException {
		loadGit();
		GitCommands.restoreStatus(git, id);
	}

	@Override
	public void mergeCommitWithNext(String id) throws GitAPIException, IOException {
		loadGit();
		GitCommands.deleteStatus(git, id);
	}

	@Override
	public List<FileChange> getCurrentChanges() throws GitAPIException, IOException {
		loadGit();
		return GitCommands.getCurrentChanges(git);
	}

	@Override
	public boolean hasUnsavedChanges() throws GitAPIException, IOException {
		loadGit();
		return GitCommands.changedFiles(git);
	}

	@Override
	public void discardChange(FileChange fileChange) throws GitAPIException, IOException {
		loadGit();
		GitCommands.discardChange(git, fileChange);
		GitCommands.commitAmend(git);
	}

	@Override
	public void discardChange(List<FileChange> fileChanges) throws GitAPIException, IOException {
		loadGit();
		for(FileChange fileChange : fileChanges) {
			GitCommands.discardChange(git, fileChange);
		}
		GitCommands.commitAmend(git);
	}

	@Override
	public void undoLastCommit() throws GitAPIException, IOException {
		loadGit();
		GitCommands.undoLastCommit(git);
	}

	@Override
	public List<FileChange> getChanges(String id1, String id2) throws GitAPIException, IOException {
		return GitCommands.getChanges(git, id1, id2);
	}

	@Override
	public File getBaseDirectory() {
		return base;
	}

	@Override
	public void setBaseDirectory(File dir) {
		base = dir;
	}

	@Override
	public void close() {
		git.close();
	}
}
