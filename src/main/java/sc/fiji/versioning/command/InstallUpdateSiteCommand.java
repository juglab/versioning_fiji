package sc.fiji.versioning.command;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import net.imagej.ui.swing.updater.ProgressDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.xml.sax.SAXException;
import sc.fiji.versioning.command.ui.FileChangesConfirmationDialog;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@Plugin(type= Command.class, label = "Install update site")
public class InstallUpdateSiteCommand extends UpdateSiteCommand {

	@Override
	public void run() {
		System.out.println("installing " + site.getName());
		try {
			files.activateUpdateSite(site, new ProgressDialog(null));
		} catch (ParserConfigurationException | IOException | SAXException e) {
			e.printStackTrace();
		}
		if(files.changes().iterator().hasNext()) {
//			files.markForUpdate(false);
			FileChangesConfirmationDialog main = new FileChangesConfirmationDialog(files);
			main.setLocationRelativeTo(null);
			main.setVisible(true);
			main.requestFocus();
		}
	}
}
