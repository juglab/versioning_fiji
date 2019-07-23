package sc.fiji.versioning;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.Test;
import org.scijava.command.CommandModule;
import sc.fiji.versioning.command.session.DownloadFreshSessionCommand;
import sc.fiji.versioning.command.session.ImportSessionFromFolderCommand;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.service.GitCommands;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static junit.framework.TestCase.*;
import static net.imagej.util.AppUtils.getBaseDirectory;
import static org.junit.Assert.assertNotEquals;

public class VersioningTest extends AbstractVersioningTest {

	@Test
	public void testAddingFile() throws Exception {
		// adding a file and committing it
		File newFile = getTestFile();
		assertFalse(newFile.exists());
		newFile.createNewFile();
		assertTrue(newFile.exists());
		assertEquals(1, versioning.getCurrentChanges().size());
		versioning.commitCurrentChanges();
		assertEquals(0, versioning.getCurrentChanges().size());
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(2, commits.size());
		assertEquals(1, commits.get(1).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(1).changes.get(0).newPath);
		assertEquals(FileChange.Status.ADD, commits.get(1).changes.get(0).status);

		// going back to initial state of IJ
		versioning.restoreCommit(commits.get(0).id);
		assertEquals(0, versioning.getCurrentChanges().size());
		assertFalse(newFile.exists());
		assertEquals(1, versioning.getCommits().size());

	}

	@Test
	public void testModifyingFile() throws Exception {
		// adding a file
		File newFile = getTestFile();
		assertFalse(newFile.exists());
		newFile.createNewFile();
		assertTrue(newFile.exists());
		assertEquals(1, versioning.getCurrentChanges().size());

		// committing changes
		versioning.commitCurrentChanges();
		assertEquals(0, versioning.getCurrentChanges().size());
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(2, commits.size());
		assertEquals(1, commits.get(1).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(1).changes.get(0).newPath);
		assertEquals(FileChange.Status.ADD, commits.get(1).changes.get(0).status);

		// changing a file
		assertEquals("", getContent(newFile));
		writeTestString(newFile);
		assertEquals(getTestString(), getContent(newFile));
		assertEquals(1, versioning.getCurrentChanges().size());

		// committing changes
		versioning.commitCurrentChanges();
		assertEquals(0, versioning.getCurrentChanges().size());
		commits = versioning.getCommits();
		assertEquals(3, commits.size());
		assertEquals(1, commits.get(2).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(2).changes.get(0).newPath);
		assertEquals(FileChange.Status.MODIFY, commits.get(2).changes.get(0).status);

		// going back to empty file state
		versioning.restoreCommit(commits.get(1).id);
		assertEquals(0, versioning.getCurrentChanges().size());
		assertTrue(newFile.exists());
		assertEquals("", getContent(newFile));
		assertEquals(2, versioning.getCommits().size());

	}

	@Test
	public void testDeleteFile() throws Exception {
		// adding a file
		File newFile = getTestFile();
		assertFalse(newFile.exists());
		newFile.createNewFile();
		assertTrue(newFile.exists());
		assertEquals(1, versioning.getCurrentChanges().size());

		// committing changes
		versioning.commitCurrentChanges();
		assertEquals(0, versioning.getCurrentChanges().size());
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(2, commits.size());
		assertEquals(1, commits.get(1).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(1).changes.get(0).newPath);
		assertEquals(FileChange.Status.ADD, commits.get(1).changes.get(0).status);

		// delete file
		newFile.delete();
		assertFalse(newFile.exists());
		assertEquals(1, versioning.getCurrentChanges().size());

		// committing changes
		versioning.commitCurrentChanges();
		assertEquals(0, versioning.getCurrentChanges().size());
		commits = versioning.getCommits();
		assertEquals(3, commits.size());
		assertEquals(1, commits.get(2).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(2).changes.get(0).oldPath);
		assertEquals(FileChange.Status.DELETE, commits.get(2).changes.get(0).status);

		// going back to empty file state
		versioning.restoreCommit(commits.get(1).id);
		assertEquals(0, versioning.getCurrentChanges().size());
		assertTrue(newFile.exists());
		assertEquals(2, versioning.getCommits().size());

	}

	@Test
	public void testDeleteCommit() throws Exception {

		// adding a file
		File newFile = getTestFile();
		newFile.createNewFile();
		// committing changes
		versioning.commitCurrentChanges();

		// changing a file
		writeTestString(newFile);
		// committing changes
		versioning.commitCurrentChanges();
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(3, commits.size());
		assertNotNull(commits.get(2).commitMsg);
		assertFalse(commits.get(2).commitMsg.isEmpty());

		// deleting second commit
		versioning.mergeCommitWithNext(commits.get(1).id);
		commits = versioning.getCommits();
		assertEquals(0, versioning.getCurrentChanges().size());
		assertEquals(getTestString(), getContent(newFile));
		assertEquals(2, commits.size());
//		assertTrue(commits.get(1).commitMsg.contains(msg));

	}

	@Test
	public void testRevertLastCommit() throws Exception {

		// adding a file
		File newFile = getTestFile();
		newFile.createNewFile();
		// committing changes
		versioning.commitCurrentChanges();

		// changing a file
		writeTestString(newFile);
		// committing changes
		versioning.commitCurrentChanges();
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(3, commits.size());
		assertNotNull(commits.get(2).commitMsg);
		assertFalse(commits.get(2).commitMsg.isEmpty());

		// revert latest change
		versioning.undoLastCommit();
		commits = versioning.getCommits();
		assertEquals(0, versioning.getCurrentChanges().size());
		assertNotEquals(getTestString(), getContent(newFile));
		assertEquals(2, commits.size());
	}

	@Test
	public void testRevertLastCommitInNewSession() throws Exception {

		// adding a file
		File newFile = getTestFile();
		newFile.createNewFile();
		// committing changes
		versioning.commitCurrentChanges();

		String session1 = versioning.getCurrentSession().name;
		String session2 = "session2";
		// changing a file
		writeTestString(newFile);
		// committing changes
		versioning.commitCurrentChanges();
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(3, commits.size());
		assertNotNull(commits.get(2).commitMsg);
		assertFalse(commits.get(2).commitMsg.isEmpty());

		//create new session
		versioning.copyCurrentSession(session2);

		// revert latest change
		versioning.undoLastCommit();
		commits = versioning.getCommits();
		assertEquals(0, versioning.getCurrentChanges().size());
		assertNotEquals(getTestString(), getContent(newFile));
		assertEquals(2, commits.size());

		//checkout first session
		versioning.openSession(session1);
		assertEquals(getTestString(), getContent(newFile));

		//checkout second session
		versioning.openSession(session2);
		assertNotEquals(getTestString(), getContent(newFile));

		versioning.openSession("default");
	}

	@Test
	public void tetImportSessionFromFolder() throws Exception {
		System.out.println(versioning.getSessions());

		// adding a file
		File newFile = getTestFile();
		newFile.createNewFile();

		assertEquals("default", versioning.getCurrentSession().toString());

		versioning.commitCurrentChanges();

		assertTrue(newFile.exists());

		Git git = GitCommands.initOrLoad(folder.getRoot());
		GitCommands.createAndCheckoutEmptyBranch(git, "clij");

		assertFalse(newFile.exists());

		File toImport = new File("/home/random/Programs/Fiji.app.clij");
		FileUtils.copyDirectory(toImport, ij.app().getApp().getBaseDirectory());
		versioning.commitCurrentChanges();
		assertEquals("clij", versioning.getCurrentSession().toString());
		System.out.println(versioning.getSessions());
		versioning.openSession("default");
		assertTrue(newFile.exists());
	}

	@Test
	public void testDownloadNewSession() throws ExecutionException, InterruptedException {
		System.out.println("before call");
		Future<CommandModule> c = ij.command().run(DownloadFreshSessionCommand.class, true, "name", "new");
		c.get();
		System.out.println("after call");
	}

}
