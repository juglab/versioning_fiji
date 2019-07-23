package sc.fiji.versioning.command.session;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.versioning.service.VersioningUIService;

@Plugin(type= Command.class, label = "Help>Sessions>Manage sessions")
public class CompareSessionsCommand implements Command{

	@Parameter
	private VersioningUIService versioningUIService;

	@Override
	public void run() {
		versioningUIService.showSessions();
	}
}
