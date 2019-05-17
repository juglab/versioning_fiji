package sc.fiji.versioning.command;

import net.imagej.ui.swing.updater.UpdaterFrame;
import net.imagej.ui.swing.updater.ViewOptions;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpToDate;
import net.imagej.updater.UpdaterUI;
import net.imagej.updater.UploaderService;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.xml.sax.SAXException;
import sc.fiji.versioning.command.ui.FileChangesConfirmationDialog;
import sc.fiji.versioning.command.ui.NewSitesDialog;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ManageUpdateSitesCommand implements Command {

	@Parameter
	private FilesCollection files;
	@Parameter
	private LogService log;
	@Parameter
	private UploaderService uploaderService;
	@Parameter
	private CommandService commandService;

	@Parameter
	private CommandInfo updater;

	@Override
	public void run() {
		Frame dialog = new NewSitesDialog(files, uploaderService, log);
		dialog.setVisible(true);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);
				System.out.println("CHANGES in manage update sites command:");
				files.changes().forEach(change -> System.out.println(change));
				if(files.changes().iterator().hasNext()) {
					files.markForUpdate(false);
					FileChangesConfirmationDialog main = new FileChangesConfirmationDialog(files);
						main.setLocationRelativeTo(null);
					main.setVisible(true);
					main.requestFocus();
				}
			}
		});
	}
}
