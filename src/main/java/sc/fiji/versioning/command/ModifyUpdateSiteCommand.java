package sc.fiji.versioning.command;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type= Command.class, label = "Modify update site")
public class ModifyUpdateSiteCommand extends UpdateSiteCommand {

	@Override
	public void run() {
		System.out.println("modifying " + site.getName());
	}
}
