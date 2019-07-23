package sc.fiji.versioning;

import net.imagej.ImageJ;
import net.imagej.ui.swing.updater.ProgressDialog;
import net.imagej.updater.CommandLine;
import net.imagej.updater.FileObject;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class CrashTest {

	@Test
	@Ignore
	public void test() {
		ImageJ ij = new ImageJ();
		UpdateSite crashSite = ij.update().getUpdateSite("crash");
		if(crashSite == null)
			CommandLine.main(ij.app().getApp().getBaseDirectory(), -1, new ProgressDialog(null), "add-update-site", "crash", "https://sites.imagej.net/Crash");
		crashSite = ij.update().getUpdateSite("crash");
		assertNotNull(crashSite);
		assertTrue(crashSite.isActive());

		FilesCollection files = new FilesCollection(ij.getApp().getBaseDirectory());
		FileObject crashFile = files.get("jars/scijava-common.jar");
		assertNotNull(crashFile);
//		assertTrue(crashFile.getFilename(false).endsWith("2.10.0.jar"));
		ij.context().dispose();
		ij = new ImageJ();
		ij.ui().showUI();
	}
}
