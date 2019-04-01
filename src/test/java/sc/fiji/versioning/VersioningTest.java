package sc.fiji.versioning;

import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Test;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.service.VersioningService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;
import static junit.framework.TestCase.*;

public class VersioningTest {

	private ImageJ ij;

	@Test
	public void testAddingFile() throws Exception {
		ij = new ImageJ();

		// test if versioning service exists
		VersioningService versioning = ij.get(VersioningService.class);
		assertNotNull(versioning);

		// test if initially no commits are present
		assertEquals(0, versioning.getCommits().size());

		// test initial commit
		versioning.commitCurrentStatus();
		assertEquals(1, versioning.getCommits().size());

		// adding a file and committing it
		File newFile = getTestFile();
		assertFalse(newFile.exists());
		newFile.createNewFile();
		assertTrue(newFile.exists());
		assertEquals(1, versioning.getCurrentChanges().size());
		versioning.commitCurrentStatus();
		assertEquals(0, versioning.getCurrentChanges().size());
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(2, commits.size());
		assertEquals(1, commits.get(1).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(1).changes.get(0).newPath);
		assertEquals(FileChange.Status.ADD, commits.get(1).changes.get(0).status);

		// going back to initial state of IJ
		versioning.restoreStatus(commits.get(0).id);
		assertEquals(0, versioning.getCurrentChanges().size());
		assertFalse(newFile.exists());
		assertEquals(1, versioning.getCommits().size());

	}

	@Test
	public void testModifyingFile() throws Exception {
		ij = new ImageJ();

		// test if versioning service exists
		VersioningService versioning = ij.get(VersioningService.class);
		assertNotNull(versioning);

		// test if initially no commits are present
		assertEquals(0, versioning.getCommits().size());

		// test initial commit
		versioning.commitCurrentStatus();
		assertEquals(1, versioning.getCommits().size());

		// adding a file
		File newFile = getTestFile();
		assertFalse(newFile.exists());
		newFile.createNewFile();
		assertTrue(newFile.exists());
		assertEquals(1, versioning.getCurrentChanges().size());

		// committing changes
		versioning.commitCurrentStatus();
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
		versioning.commitCurrentStatus();
		assertEquals(0, versioning.getCurrentChanges().size());
		commits = versioning.getCommits();
		assertEquals(3, commits.size());
		assertEquals(1, commits.get(2).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(2).changes.get(0).newPath);
		assertEquals(FileChange.Status.MODIFY, commits.get(2).changes.get(0).status);

		// going back to empty file state
		versioning.restoreStatus(commits.get(1).id);
		assertEquals(0, versioning.getCurrentChanges().size());
		assertTrue(newFile.exists());
		assertEquals("", getContent(newFile));
		assertEquals(2, versioning.getCommits().size());

	}

	@Test
	public void testDeleteFile() throws Exception {
		ij = new ImageJ();

		// test if versioning service exists
		VersioningService versioning = ij.get(VersioningService.class);
		assertNotNull(versioning);

		// test if initially no commits are present
		assertEquals(0, versioning.getCommits().size());

		// test initial commit
		versioning.commitCurrentStatus();
		assertEquals(1, versioning.getCommits().size());

		// adding a file
		File newFile = getTestFile();
		assertFalse(newFile.exists());
		newFile.createNewFile();
		assertTrue(newFile.exists());
		assertEquals(1, versioning.getCurrentChanges().size());

		// committing changes
		versioning.commitCurrentStatus();
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
		versioning.commitCurrentStatus();
		assertEquals(0, versioning.getCurrentChanges().size());
		commits = versioning.getCommits();
		assertEquals(3, commits.size());
		assertEquals(1, commits.get(2).changes.size());
		assertEquals(getRelativePath(getTestFile()), commits.get(2).changes.get(0).oldPath);
		assertEquals(FileChange.Status.DELETE, commits.get(2).changes.get(0).status);

		// going back to empty file state
		versioning.restoreStatus(commits.get(1).id);
		assertEquals(0, versioning.getCurrentChanges().size());
		assertTrue(newFile.exists());
		assertEquals(2, versioning.getCommits().size());

	}

	@Test
	public void testDeleteCommit() throws Exception {
		ij = new ImageJ();
		VersioningService versioning = ij.get(VersioningService.class);

		// initial commit
		versioning.commitCurrentStatus();
		assertEquals(1, versioning.getCommits().size());

		// adding a file
		File newFile = getTestFile();
		newFile.createNewFile();
		// committing changes
		versioning.commitCurrentStatus();

		// changing a file
		writeTestString(newFile);
		// committing changes
		versioning.commitCurrentStatus();
		List<AppCommit> commits = versioning.getCommits();
		assertEquals(3, commits.size());
		String msg = commits.get(2).commitMsg;
		assertNotNull(msg);
		assertFalse(msg.isEmpty());

		// deleting second commit
		versioning.deleteStatus(commits.get(1).id);
		commits = versioning.getCommits();
		assertEquals(0, versioning.getCurrentChanges().size());
		assertEquals(getTestString(), getContent(newFile));
		assertEquals(2, commits.size());
//		assertTrue(commits.get(1).commitMsg.contains(msg));

	}

	private String getContent(File newFile) throws IOException {
		String res = "";
		for(String line : Files.readAllLines(newFile.toPath(), StandardCharsets.UTF_8)) {
			res += line;
		}
		return res;
	}

	private void writeTestString(File newFile) throws IOException {
		FileWriter fileWriter = new FileWriter(newFile);
		fileWriter.write(getTestString());
		fileWriter.close();
	}

	private String getTestString() {
		return "Test";
	}

	private String getRelativePath(File testFile) {
		return testFile.getAbsolutePath().replace(ij.app().getApp().getBaseDirectory().getAbsolutePath()+"/", "");
	}

	private File getTestFile() {
		return new File(ij.app().getApp().getBaseDirectory().getAbsolutePath() + "/gittestfile");
	}

	@After
	public void cleanup() throws IOException {
		System.out.println("Cleaning up after test..");
		System.out.println("   ..deleting test file");
		getTestFile().delete();
		System.out.println("   ..deleting git folder");
		if(ij == null) return;
		File gitFolder = new File(ij.app().getApp().getBaseDirectory().getAbsolutePath() + "/.git");
		if(gitFolder.exists()) {

			walkFileTree(gitFolder.toPath(), new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file,
				                                 BasicFileAttributes attrs) throws IOException {

//					System.out.println("       Deleting file: " + file);
					delete(file);
					return CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
				                                          IOException exc) throws IOException {

//					System.out.println("       Deleting dir: " + dir);
					if (exc == null) {
						delete(dir);
						return CONTINUE;
					} else {
						throw exc;
					}
				}

			});

			gitFolder.delete();
			if(gitFolder.exists()) {
				System.out.println("     Failed to delete git folder " + gitFolder);
			} else {
				System.out.println("     Successfully deleted git folder " + gitFolder);
			}
		}
	}

}
