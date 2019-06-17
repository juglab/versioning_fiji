package sc.fiji.versioning.command.updatesite;

import net.imagej.ui.swing.updater.ProgressDialog;
import net.imagej.ui.swing.updater.SwingTools;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;

//TODO move swing code to better place

@Plugin(type= Command.class, label = "Install update site")
public class InstallUpdateSiteCommand extends AbstractUpdateSiteCommand {

	Frame parent = null;

	@Override
	public void run() {
		System.out.println("installing " + site.getName());
		try {
			files.activateUpdateSite(site, new ProgressDialog(parent));
		} catch (ParserConfigurationException | IOException | SAXException e) {
			e.printStackTrace();
		}
		if(applyChanges()) {
			SwingTools.showMessageBox(parent, "Successfully installed update site " + site.getName() + ". Please restart ImageJ!", JOptionPane.INFORMATION_MESSAGE);
		} else {
			SwingTools.showMessageBox(parent, "Installation of update site " + site.getName() + " canceled.", JOptionPane.INFORMATION_MESSAGE);
		}
	}

}
