package sc.fiji.versioning.command.session;

import net.imagej.ImageJ;
import net.imagej.updater.FilesCollection;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.xml.sax.SAXException;
import sc.fiji.versioning.service.VersioningService;
import sc.fiji.versioning.service.VersioningUIService;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Plugin(type= Command.class, label = "Sessions > Start new Session > Import session from folder")
public class ImportSessionFromFolderCommand implements Command {

	@Parameter(label = "Name of the session")
	String name;

	@Parameter(style = "directory", label = "Fiji.app folder to import from")
	File dir;

	@Parameter
	LogService logService;

	@Parameter
	VersioningService versioningService;

	@Override
	public void run() {
		FilesCollection test = new FilesCollection(dir);
		try {
			test.read();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			logService.error("Cannot cannot read directory as ImageJ / Fiji installation: " + dir);
			return;
		}
		try {
			versioningService.importSessionFromFolder(dir, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(ImportSessionFromFolderCommand.class, true);
	}
}
