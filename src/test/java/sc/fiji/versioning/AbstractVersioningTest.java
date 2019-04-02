package sc.fiji.versioning;

import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import sc.fiji.versioning.service.VersioningService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class AbstractVersioningTest {

	public static ImageJ ij;
	public static VersioningService versioning;

	@ClassRule
	public static TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void setup() throws Exception {
		if(ij == null) {
			ij = new ImageJ();
			// test if versioning service exists
			versioning = ij.get(VersioningService.class);
			assertNotNull(versioning);
			versioning.setBaseDirectory(folder.getRoot());

			// test if initially no commits are present
			assertEquals(0, versioning.getCommits().size());

			folder.newFile();

			// test initial commit
			versioning.commitCurrentChanges();
			assertEquals(1, versioning.getCommits().size());
		}
	}

	@After
	public void revertToInitialCommit() throws Exception {
		versioning.restoreInitialCommit();
		assertEquals(1, versioning.getCommits().size());
	}

	File getTestFile() {
		return new File(folder.getRoot() + "/gittestfile");
	}

	String getContent(File newFile) throws IOException {
		String res = "";
		for(String line : Files.readAllLines(newFile.toPath(), StandardCharsets.UTF_8)) {
			res += line;
		}
		return res;
	}

	void writeTestString(File newFile) throws IOException {
		FileWriter fileWriter = new FileWriter(newFile);
		fileWriter.write(getTestString());
		fileWriter.close();
	}

	String getTestString() {
		return "Test";
	}

	String getRelativePath(File testFile) {
		return testFile.getAbsolutePath().replace(folder.getRoot()+"/", "");
	}

}
