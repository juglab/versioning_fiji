package sc.fiji.versioning.command.session;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.versioning.service.VersioningService;
import sc.fiji.versioning.service.VersioningUIService;

@Plugin(type= Command.class, label = "Sessions > Start new Session > Copy current session")
public class CopyCurrentSessionCommand implements Command {

	@Parameter
	VersioningService versioningService;

	@Parameter
	VersioningUIService versioningUIService;

	@Override
	public void run() {
		String newName = versioningUIService.askFor("Name of new session", String.class);
		try {
			versioningService.copyCurrentSession(newName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
