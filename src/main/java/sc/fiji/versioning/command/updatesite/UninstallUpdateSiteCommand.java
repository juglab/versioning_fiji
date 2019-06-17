package sc.fiji.versioning.command.updatesite;

import net.imagej.ui.swing.updater.SwingTools;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;

//TODO move swing code to better place

@Plugin(type= Command.class, label = "Uninstall update site")
public class UninstallUpdateSiteCommand extends AbstractUpdateSiteCommand {

	Frame parent = null;

	@Override
	public void run() {

		System.out.println("uninstalling " + site.getName());
		files.deactivateUpdateSite(site);
		if(applyChanges()) {
			SwingTools.showMessageBox(parent, "Successfully deinstalled update site " + site.getName() + ". Please restart ImageJ!", JOptionPane.INFORMATION_MESSAGE);
		} else {
			SwingTools.showMessageBox(parent, "Deinstallation of update site " + site.getName() + " canceled.", JOptionPane.INFORMATION_MESSAGE);
		}
	}

}
