package sc.fiji.versioning;

import net.imagej.ui.swing.updater.ProgressDialog;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.imagej.updater.UpdateSite;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;
import static sc.fiji.versioning.UpdaterTestUtils.initializeFromRoot;

public class UpdateSitesTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testActivateNotInstallUpdateSite() throws Exception {
		FilesCollection files = initializeFromRoot(folder.getRoot().getAbsolutePath());
		files.write();
		UpdateSite clij = clij();
		files.addUpdateSite(clij);
		files.activateUpdateSite(clij, null);
		assertTrue(files.changes().iterator().hasNext());
		FilesCollection files2 = new FilesCollection(folder.getRoot().getAbsoluteFile());
		files2.read();
		assertNull(files2.getUpdateSite("clij", true));
	}

	@Test
	public void testActivateAndInstallUpdateSite() throws Exception {
		FilesCollection files = initializeFromRoot(folder.getRoot().getAbsolutePath());
		files.write();
		UpdateSite clij = clij();
		files.addUpdateSite(clij);
		files.activateUpdateSite(clij, null);
		assertTrue(files.changes().iterator().hasNext());
		//install
		final Installer installer =
				new Installer(files, new ProgressDialog(null));
		installer.start();
		files.write();
		FilesCollection files2 = new FilesCollection(folder.getRoot().getAbsoluteFile());
		files2.read();
		assertNotNull(files2.getUpdateSite("clij", true));
	}

	@Test
	public void testDeactivateNotUninstallUpdateSite() throws Exception {
		FilesCollection files = initializeFromRoot(folder.getRoot().getAbsolutePath());
		files.write();
		UpdateSite clij = clij();
		files.addUpdateSite(clij);
		files.activateUpdateSite(clij, null);
		final Installer installer =	new Installer(files, null);
		installer.start();
		files.write();
		assertFalse(files.changes().iterator().hasNext());
		files.deactivateUpdateSite(clij);
		files.changes().forEach(fileObject -> System.out.println(fileObject.getAction()));
		assertTrue(files.changes().iterator().hasNext());
		FilesCollection files2 = new FilesCollection(folder.getRoot().getAbsoluteFile());
		files2.read();
		assertNotNull(files2.getUpdateSite("clij", true));
	}

	@Test
	public void testDeactivateAndUninstallUpdateSite() throws Exception {
		FilesCollection files = initializeFromRoot(folder.getRoot().getAbsolutePath());
		files.write();
		UpdateSite clij = clij();
		files.addUpdateSite(clij);
		files.activateUpdateSite(clij, null);
		Installer installer =	new Installer(files, null);
		installer.start();
		files.write();
		assertFalse(files.changes().iterator().hasNext());
		files.deactivateUpdateSite(clij);
		assertTrue(files.changes().iterator().hasNext());
		installer = new Installer(files.clone(files.changes()), new ProgressDialog(null));
		installer.start();
		files.write();
		assertFalse(files.changes().iterator().hasNext());
		FilesCollection files2 = new FilesCollection(folder.getRoot().getAbsoluteFile());
		files2.read();
		assertNotNull(files2.getUpdateSite("clij", true));
		assertNull(files2.getUpdateSite("clij", false));
	}

	private UpdateSite clij() {
		return new UpdateSite("clij", "https://update-sites.mpi-cbg.de/clij/", "", "", "", "", 0L);
	}

}
