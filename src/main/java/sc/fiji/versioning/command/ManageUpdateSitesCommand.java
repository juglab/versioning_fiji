package sc.fiji.versioning.command;

import net.imagej.ui.swing.updater.SitesDialog;
import net.imagej.updater.FilesCollection;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

public class ManageUpdateSitesCommand implements Command {

	@Parameter
	private FilesCollection files;

	@Override
	public void run() {
		new SitesDialog(null, files).setVisible(true);
	}
}
