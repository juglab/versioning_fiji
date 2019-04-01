package sc.fiji.versioning.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IllegalTodoFileModification;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.FileChange;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;

import static sc.fiji.versioning.model.FileChange.versionPattern;

public class GitCommands {

	public static void commitCurrentStatus(Git git) throws GitAPIException {
		System.out.println("git add .");
		git.add().addFilepattern(".").call();
		System.out.println("git add -u .");
		git.add().setUpdate(true).addFilepattern(".").call();

		String date = new SimpleDateFormat().format(new Date());
		System.out.println("git commit -m \'" + date + "\'");
		git.commit().setMessage(date).call();
		git.close();
	}

	public static List<AppCommit> getCommits(Git git) throws IOException, GitAPIException {
		List<AppCommit> commits = new ArrayList<>();
		if(git.getRepository().resolve(Constants.HEAD) == null) return commits;
		RevCommit lastCom = null;
		System.out.println("git log");
		Iterable<RevCommit> iterator = git.log().call();
		List<RevCommit> result = new ArrayList();
		iterator.forEach(c -> result.add(c));
		Collections.reverse(result);
		result.stream().map(commit -> "   " + commit).forEach(System.out::println);
		for(RevCommit commit : result) {
			AppCommit c = new AppCommit();
			c.id = commit.getId().getName();
			c.changes = getChanges(git, commit, lastCom);
			PersonIdent authorIdent = commit.getAuthorIdent();
			c.date = authorIdent.getWhen();
			c.commitMsg = new SimpleDateFormat().format(c.date);
			if(c.changes.size() > 0) {
				lastCom = commit;
				commits.add(c);
			}
		}
		return commits;
	}

	public static Git initOrLoad(File localPath) throws GitAPIException, IOException {
		try {
			return Git.open(localPath);
		}
		catch(RepositoryNotFoundException e) {
			System.out.println("git init " + localPath);
			return Git.init().setDirectory(localPath).call();
		}
	}

	public static List<FileChange> getChanges(Git git, RevCommit commit1, RevCommit commit2) throws GitAPIException, IOException {
		AbstractTreeIterator tree2;
		if(commit2 != null)
			tree2 = getTree(git, commit2);
		else
			tree2 = new EmptyTreeIterator();
		List<FileChange> changes = new ArrayList<>();
		AbstractTreeIterator tree1 = getTree(git, commit1);
		System.out.println("git diff " + tree2.hashCode() + " " + tree1.hashCode());
		git.diff().setOldTree(tree2).setNewTree(tree1).call().forEach(entry -> {
			FileChange change = new FileChange();
			change.status = toStatus(entry.getChangeType());
			change.oldPath = entry.getOldPath();
			change.newPath = entry.getNewPath();
			changes.add(change);
		});
		detectVersionChanges(changes);
		return changes;
	}

	private static void detectVersionChanges(List<FileChange> changes) {
		Map<String, FileChange> deleted = new HashMap<>();
		Map<String, FileChange> added = new HashMap<>();
		for(FileChange change : changes) {
			if(change.status.equals(FileChange.Status.DELETE)) {
				final Matcher matcher = versionPattern.matcher(change.oldPath);
				if (matcher.matches()) {
					deleted.put(matcher.group(1), change);
				}
			}
			if(change.status.equals(FileChange.Status.ADD)) {
				final Matcher matcher = versionPattern.matcher(change.newPath);
				if (matcher.matches()) {
					added.put(matcher.group(1), change);
				}
			}
		}
		for (Map.Entry<String,FileChange> del : deleted.entrySet()) {
			FileChange add = added.get(del.getKey());
			if(add!=null) {
				changes.remove(del.getValue());
				changes.remove(add);
				FileChange c = new FileChange();
				c.oldPath = del.getValue().oldPath;
				c.newPath = add.newPath;
				c.status = FileChange.Status.VERSION_CHANGE;
				changes.add(c);
			}
		}
	}

	public static boolean changedFiles(Git git) throws GitAPIException {
		System.out.println("git status");
		return git.status().call().hasUncommittedChanges();
	}

	private static AbstractTreeIterator getTree(Git git, RevCommit commit1) throws IOException {
		CanonicalTreeParser parser;
		try( ObjectReader reader = git.getRepository().newObjectReader() ) {
			parser = new CanonicalTreeParser(null, reader, commit1.getTree().getId());
		}
		return parser;
	}

	private static FileChange.Status toStatus(DiffEntry.ChangeType changeType) {
		switch(changeType) {
			case ADD: return FileChange.Status.ADD;
			case DELETE: return FileChange.Status.DELETE;
			case MODIFY: return FileChange.Status.MODIFY;
			case RENAME: return FileChange.Status.RENAME;
			case COPY: return FileChange.Status.COPY;
		}
		return null;
	}

	public static void restoreStatus(Git git, String id) throws GitAPIException {
		System.out.println("git reset --hard " + id);
		git.reset().setMode(ResetCommand.ResetType.HARD).setRef(id).call();
		System.out.println("git clean -f -d");
		git.clean().setForce(true).setCleanDirectories(true).call();
	}

	public static void deleteStatus(Git git, String id) throws GitAPIException, IOException {
		commitCurrentStatus(git);
		RevWalk walk = new RevWalk(git.getRepository());
		RevCommit commit = walk.parseCommit(ObjectId.fromString(id));
		RebaseCommand.InteractiveHandler handler = new RebaseCommand.InteractiveHandler() {
			@Override
			public void prepareSteps(List<RebaseTodoLine> steps) {
				boolean foundStep = false;
				for(RebaseTodoLine step : steps) {
					try {
						if(commit.getName().startsWith(step.getCommit().name())) {
							foundStep = true;
							System.out.println("   pick " + step.getCommit());
						} else {
							if(foundStep) {
								System.out.println("   squash " + step.getCommit());
								step.setAction(RebaseTodoLine.Action.SQUASH);
//								RevCommit c = walk.parseCommit(step.getCommit().toObjectId());
//								System.out.println("Found step " + c.getFullMessage());
//								name = c.getFullMessage();
								foundStep = false;
							} else {
								System.out.println("   pick " + step.getCommit());
								step.setAction(RebaseTodoLine.Action.PICK);
							}
						}
					} catch (IllegalTodoFileModification e) {
						e.printStackTrace();
					}
				}
			}
			@Override
			public String modifyCommitMessage(String oldMessage) {
				return oldMessage;
			}
		};
		System.out.println("git rebase -i " + commit.getParent(0).hashCode());
		git.rebase().setUpstream(commit.getParent(0)).runInteractively(handler).call();
	}

	public static List<FileChange> getCurrentChanges(Git git) throws GitAPIException {
		List<FileChange> changes = new ArrayList<>();
		System.out.println("git status");
		Status status = git.status().call();

		//added
		status.getAdded().forEach(file -> {
			FileChange change = new FileChange();
			change.status = FileChange.Status.ADD;
			change.newPath = file;
			changes.add(change);
		});
		status.getUntrackedFolders().forEach(file -> {
			FileChange change = new FileChange();
			change.status = FileChange.Status.ADD;
			change.newPath = file;
			changes.add(change);
		});
		status.getUntracked().forEach(file -> {
			FileChange change = new FileChange();
			change.status = FileChange.Status.ADD;
			change.newPath = file;
			changes.add(change);
		});

		//modified
		status.getModified().forEach(file -> {
			FileChange change = new FileChange();
			change.status = FileChange.Status.MODIFY;
			change.oldPath = file;
			change.newPath = file;
			changes.add(change);
		});

		//deleted
		status.getRemoved().forEach(file -> {
			FileChange change = new FileChange();
			change.status = FileChange.Status.DELETE;
			change.oldPath = file;
			changes.add(change);
		});
		status.getMissing().forEach(file -> {
			FileChange change = new FileChange();
			change.status = FileChange.Status.DELETE;
			change.oldPath = file;
			changes.add(change);
		});
		return changes;
	}

	public static void discardChange(Git git, FileChange fileChange) throws GitAPIException {
		if(fileChange.status.equals(FileChange.Status.DELETE)
			|| fileChange.status.equals(FileChange.Status.MODIFY)) {
			System.out.println("git checkout HEAD~1 " + fileChange.oldPath);
			git.checkout().addPath(fileChange.oldPath).setStartPoint("HEAD~1").call();
//			git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").addPath(fileChange.oldPath).call();
		}
		else if(fileChange.status.equals(FileChange.Status.ADD)) {
			System.out.println("git rm " + fileChange.newPath);
			git.rm().addFilepattern(fileChange.newPath).call();
		}
		else if(fileChange.status.equals(FileChange.Status.VERSION_CHANGE)) {
			System.out.println("git rm " + fileChange.newPath);
			System.out.println("git checkout HEAD~1 " + fileChange.oldPath);
			git.rm().addFilepattern(fileChange.newPath).call();
			git.checkout().addPath(fileChange.oldPath).setStartPoint("HEAD~1").call();
		}
	}

	public static void commitAmend(Git git) throws GitAPIException {
		System.out.println("git commit --amend");
		git.commit().setAmend(true).setMessage("").call();
	}

	public static void undoLastCommit(Git git) throws GitAPIException {
		restoreStatus(git, "HEAD~1");
	}
}
