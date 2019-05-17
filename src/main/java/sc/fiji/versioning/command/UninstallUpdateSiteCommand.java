package sc.fiji.versioning.command;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import sc.fiji.versioning.command.ui.FileChangesConfirmationDialog;

@Plugin(type= Command.class, label = "Uninstall update site")
public class UninstallUpdateSiteCommand extends UpdateSiteCommand {
	@Override
	public void run() {
		System.out.println("uninstalling " + site.getName());
		files.deactivateUpdateSite(site);
		if(files.changes().iterator().hasNext()) {
//			files.markForUpdate(false);
			FileChangesConfirmationDialog main = new FileChangesConfirmationDialog(files);
			main.setLocationRelativeTo(null);
			main.setVisible(true);
			main.requestFocus();

		}
	}
}
