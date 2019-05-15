package sc.fiji.versioning.command;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type= Command.class, label = "Install update site")
public class InstallUpdateSiteCommand extends UpdateSiteCommand {

	@Override
	public void run() {
		System.out.println("installing " + site.getName());
//		files.addUpdateSite(site);
	}
}
