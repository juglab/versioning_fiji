package sc.fiji.versioning.command;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type= Command.class, label = "Uninstall update site")
public class UninstallUpdateSiteCommand extends UpdateSiteCommand {
	@Override
	public void run() {
		System.out.println("uninstalling " + site.getName());
//		files.removeUpdateSite(site.getName());
	}
}
