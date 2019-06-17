package sc.fiji.versioning.command.updatesite;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Plugin(type= Command.class, label = "Modify update site")
public class ModifyUpdateSiteCommand extends AbstractUpdateSiteCommand {

	@Override
	public void run() {
		System.out.println("modifying " + site.getName());
		// TODO
		throw new NotImplementedException();
	}
}
