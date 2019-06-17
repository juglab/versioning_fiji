package sc.fiji.versioning.command.updatesite;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.imagej.updater.UpdateSite;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.xml.sax.SAXException;
import sc.fiji.versioning.service.VersioningUIService;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;

public abstract class AbstractUpdateSiteCommand implements Command {

	@Parameter
	VersioningUIService versioningUIService;

	@Parameter
	protected UpdateSite site;

	@Parameter
	protected FilesCollection files;


	protected boolean applyChanges() {
		if(versioningUIService.approveChanges(files)) {
			new Thread(() -> {
				System.out.println("Changes:");
				files.changes().forEach(change -> System.out.println("   " + change));
				final Installer installer =
						new Installer(files.clone(files.changes()), versioningUIService.getProgressDialog());
				try {
					installer.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}).run();
			try {
				files.write();
				return true;
			} catch (IOException | SAXException | TransformerConfigurationException e1) {
				e1.printStackTrace();
			}
		}
		return false;
	}

}
