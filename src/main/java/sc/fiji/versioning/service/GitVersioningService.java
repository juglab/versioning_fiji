package sc.fiji.versioning.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.scijava.app.AppService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Deborah Schmidt
 */
@Plugin(type = Service.class)
public class GitVersioningService extends AbstractService implements VersioningService {

	@Parameter
	AppService appService;

	private Git git;

	@Override
	public void commitCurrentStatus() throws IOException, GitAPIException {
		loadGit();
		GitCommands.commitCurrentStatus(git);
	}

	private void loadGit() throws GitAPIException, IOException {
		if(git == null) {
			git = GitCommands.initOrLoad(getBase());
		}
	}

	@Override
	public List<AppCommit> getCommits() throws GitAPIException, IOException {
		loadGit();
		return GitCommands.getCommits(git);
	}

	@Override
	public void restoreStatus(String id) throws GitAPIException, IOException {
		loadGit();
		GitCommands.restoreStatus(git, id);
	}

	@Override
	public void deleteStatus(String id) throws GitAPIException, IOException {
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

	private File getBase() {
		return appService.getApp().getBaseDirectory();
	}
}
