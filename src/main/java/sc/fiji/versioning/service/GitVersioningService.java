package sc.fiji.versioning.service;

import net.imagej.updater.util.UpdaterUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.scijava.Initializable;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;
import org.scijava.download.DownloadService;
import org.scijava.event.EventService;
import org.scijava.io.location.BytesLocation;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.io.location.URLLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.task.Task;
import org.scijava.util.ByteArray;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.model.Session;
import sc.fiji.versioning.ui.updatesite.NewSitesDialog;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Deborah Schmidt
 */
@Plugin(type = Service.class)
public class GitVersioningService extends AbstractService implements VersioningService, Initializable, AutoCloseable {

	@Parameter
	AppService appService;

	@Parameter
	private EventService eventService;

	@Parameter
	private DownloadService downloadService;

	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;

	private File base;
	private Git git;


	@Override
	public void initialize() {
		base = appService.getApp().getBaseDirectory();
	}

	@Override
	public void commitCurrentChanges() throws IOException, GitAPIException {
		loadGit();
		GitCommands.commitCurrentStatus(git);
	}

	private void loadGit() throws GitAPIException, IOException {
		if(git != null && !git.getRepository().getDirectory().getParentFile().equals(base)) git = null;
		if(git == null) {
			git = GitCommands.initOrLoad(getBaseDirectory());
		}
	}

	@Override
	public List<AppCommit> getCommits() throws GitAPIException, IOException {
		loadGit();
		return GitCommands.getCommits(git);
	}

	@Override
	public void restoreCommit(String id) throws GitAPIException, IOException {
		loadGit();
		GitCommands.restoreStatus(git, id);
	}

	@Override
	public void mergeCommitWithNext(String id) throws GitAPIException, IOException {
		loadGit();
		GitCommands.deleteStatus(git, id);
	}

	@Override
	public List<FileChange> getCurrentChanges() throws GitAPIException, IOException {
		loadGit();
		return GitCommands.getCurrentChanges(git);
	}

	@Override
	public boolean hasUnsavedChanges() throws GitAPIException, IOException {
		loadGit();
		return GitCommands.changedFiles(git);
	}

	@Override
	public void discardChange(FileChange fileChange) throws GitAPIException, IOException {
		loadGit();
		GitCommands.discardChange(git, fileChange);
		GitCommands.commitAmend(git);
	}

	@Override
	public void discardChange(List<FileChange> fileChanges) throws GitAPIException, IOException {
		loadGit();
		for(FileChange fileChange : fileChanges) {
			GitCommands.discardChange(git, fileChange);
		}
		GitCommands.commitAmend(git);
	}

	@Override
	public void undoLastCommit() throws GitAPIException, IOException {
		loadGit();
		GitCommands.undoLastCommit(git);
	}

	@Override
	public List<FileChange> getChanges(String id1, String id2) throws GitAPIException, IOException {
		loadGit();
		return GitCommands.getChanges(git, id1, id2);
	}

	@Override
	public List<Session> getSessions() throws GitAPIException, IOException {
		loadGit();
		List<Ref> branches = GitCommands.getBranches(git);
		List<Session> result = new ArrayList<>();
		branches.forEach(branch -> {
			Session session = new Session();
			session.name = branch.getName();
			result.add(session);
		});
		return result;
	}

	@Override
	public Session getCurrentSession() throws GitAPIException, IOException {
		loadGit();
		Session session = new Session();
		session.name = GitCommands.getCurrentBranch(git);
		return session;
	}

	@Override
	public void importSessionFromFolder(File dir, String name) throws GitAPIException, IOException {
		loadGit();
		commitCurrentChanges();
		GitCommands.createAndCheckoutEmptyBranch(git, name);
		IOFileFilter gitFilter = FileFilterUtils.prefixFileFilter(".git");
		IOFileFilter gitFiles = FileFilterUtils.notFileFilter(gitFilter);
		FileUtils.copyDirectory(dir, getBaseDirectory(), gitFiles);
		commitCurrentChanges();
		eventService.publish(new SessionChangedEvent());
	}

	@Override
	public void downloadFreshSession(String name) throws GitAPIException, IOException, ExecutionException, InterruptedException {
		loadGit();
		commitCurrentChanges();
		GitCommands.createAndCheckoutEmptyBranch(git, name);
		String platform = UpdaterUtil.getPlatform();
		URL url = new URL("https://downloads.imagej.net/fiji/latest/fiji-" + platform + ".zip");
		String downloadDir = Files.createTempDirectory("fiji") + "/fiji-" + platform + ".zip";
		InputStream in = url.openStream();
		Files.copy(in, Paths.get(downloadDir), StandardCopyOption.REPLACE_EXISTING);
		unZipIt(downloadDir, getBaseDirectory().getAbsolutePath());
//		downloadAndUnpackResource(url, getBaseDirectory());
		commitCurrentChanges();
		eventService.publish(new SessionChangedEvent());
	}


	/**
	 * Unzip it
	 * @param zipFile input zip file
	 * @param outputFolder zip file output folder
	 */
	public void unZipIt(String zipFile, String outputFolder){

		System.out.println("unzip " + zipFile + " to " + outputFolder);

		byte[] buffer = new byte[1024];

		try{

			//create output directory is not exists
			File folder = new File(outputFolder);
			if(!folder.exists()){
				folder.mkdir();
			}

			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze = zis.getNextEntry();

			while(ze!=null){
				boolean executable = false;
				if(ze.getName().contains("Fiji.app/ImageJ-")) {
					executable = true;
				}

				String fileName = ze.getName().replace("Fiji.app/", "");
				File newFile = new File(outputFolder + File.separator + fileName);

//				System.out.println("file unzip : "+ newFile.getAbsoluteFile());

				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();

				if(executable) {
					newFile.setExecutable(true);
				}

			}

			zis.closeEntry();
			zis.close();

			System.out.println("Done");

		}catch(IOException ex){
			ex.printStackTrace();
		}
	}

	private void downloadAndUnpackResource(Location source, File destDir) throws InterruptedException, ExecutionException, IOException {
		System.out.println("downloading and unpacking " + source + " to " + destDir);
		ByteArray byteArray = new ByteArray(1048576);
		BytesLocation bytes = new BytesLocation(byteArray);
		Task task = downloadService.download(source, bytes).task();
		task.waitFor();
		byte[] buf = new byte[65536];
		ByteArrayInputStream bais = new ByteArrayInputStream(byteArray.getArray(), 0, byteArray.size());
		destDir.mkdirs();
		ZipInputStream zis = new ZipInputStream(bais);
		Throwable var10 = null;

		try {
			while(true) {
				ZipEntry entry = zis.getNextEntry();
				if (entry == null) {
					break;
				}

				String name = entry.getName();
				statusService.showStatus("Unpacking " + name);
				File outFile = new File(destDir, name);
				if (entry.isDirectory()) {
					outFile.mkdirs();
				} else {
					int size = (int)entry.getSize();
					int len = 0;
					FileOutputStream out = new FileOutputStream(outFile);
					Throwable var17 = null;

					try {
						while(true) {
							statusService.showStatus(len, size, "Unpacking " + name);
							int r = zis.read(buf);
							if (r < 0) {
								break;
							}

							len += r;
							out.write(buf, 0, r);
						}
					} catch (Throwable var40) {
						var17 = var40;
						throw var40;
					} finally {
						if (out != null) {
							if (var17 != null) {
								try {
									out.close();
								} catch (Throwable var39) {
									var17.addSuppressed(var39);
								}
							} else {
								out.close();
							}
						}

					}
				}
			}
		} catch (Throwable var42) {
			var10 = var42;
			throw var42;
		} finally {
			if (zis != null) {
				if (var10 != null) {
					try {
						zis.close();
					} catch (Throwable var38) {
						var10.addSuppressed(var38);
					}
				} else {
					zis.close();
				}
			}

		}

		statusService.clearStatus();
	}

	@Override
	public File getBaseDirectory() {
		return base;
	}

	@Override
	public void setBaseDirectory(File dir) {
		base = dir;
	}

	@Override
	public void copyCurrentSession(String newSessionName) throws GitAPIException, IOException {
		loadGit();
		GitCommands.createAndCheckoutBranch(git, newSessionName);
		eventService.publish(new SessionChangedEvent());
	}

	@Override
	public void openSession(String name) throws GitAPIException, IOException {
		loadGit();
		if(name == "default") name = "master";
		GitCommands.checkoutBranch(git, name);
		eventService.publish(new SessionChangedEvent());
	}

	@Override
	public void renameSession(String oldSessionName, String newSessionName) throws Exception {
		loadGit();
		GitCommands.renameBranch(git, oldSessionName, newSessionName);
	}

	@Override
	public void deleteSession(String sessionName) throws Exception {
		loadGit();
		GitCommands.deleteBranch(git, sessionName);
	}

	@Override
	public void close() {
		git.close();
	}
}
