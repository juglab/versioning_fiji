package sc.fiji.versioning.command.session;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.service.VersioningService;
import sc.fiji.versioning.service.VersioningUIService;

import java.util.List;

@Plugin(type= Command.class, label = "Help>Current session>Revert latest change")
public class RevertLatestSessionChangeCommand implements Command {

	@Parameter
	VersioningService versioningService;

	@Parameter
	VersioningUIService versioningUIService;

	@Override
	public void run() {
		List<AppCommit> commits = null;
		try {
			if (versioningService.hasUnsavedChanges()) {
				versioningService.commitCurrentChanges();
			}
			commits = versioningService.getCommits();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(commits == null || commits.size() < 2) return;
		String currentID = commits.get(commits.size() - 1).id;
		String prevID = commits.get(commits.size() - 2).id;
		try {
			List<FileChange> changes = versioningService.getChanges(prevID, currentID);
			if (versioningUIService.approveChanges(changes)) {
				try {
					versioningService.undoLastCommit();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
