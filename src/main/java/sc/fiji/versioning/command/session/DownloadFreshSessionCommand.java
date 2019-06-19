package sc.fiji.versioning.command.session;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.versioning.service.VersioningService;

@Plugin(type= Command.class, label = "Sessions > Start new Session > Download fresh session")
public class DownloadFreshSessionCommand implements Command {

	@Parameter(label = "Name of the session")
	String name;

	@Parameter
	VersioningService versioningService;

	@Override
	public void run() {
		System.out.println("command started");
		try {
			versioningService.downloadFreshSession(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("command done");
	}

}
