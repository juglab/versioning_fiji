package sc.fiji.versioning.command;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

public abstract class UpdateSiteCommand implements Command {

	@Parameter
	protected UpdateSite site;

	@Parameter
	protected FilesCollection files;

}
