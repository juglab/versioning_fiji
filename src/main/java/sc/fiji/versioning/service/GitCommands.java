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

	static boolean debug = true;

	public static void commitCurrentStatus(Git git) throws GitAPIException {
		//NOTE: not doing `git add .` because it's incredibly slow
		if(debug) System.out.println("git status");
		Status status = git.status().call();
		Set<String> toAdd = new HashSet<>();
		Set<String> toRemove = new HashSet<>();
		status.getUncommittedChanges().forEach(toAdd::add);
//		status.getUntrackedFolders().forEach(toAdd::add);
		status.getUntracked().forEach(toAdd::add);
		status.getModified().forEach(toAdd::add);
		status.getMissing().forEach(toRemove::add);
		if(toAdd.size() + toRemove.size() == 0) {
			System.out.println("Nothing to commit");
			return;
		}
		for (String s : toAdd) {
			if(debug) System.out.println("git add " + s);
			git.add().addFilepattern(s).call();
		}
		for (String s : toRemove) {
			if(debug) System.out.println("git rm " + s);
			git.rm().addFilepattern(s).call();
		}
		String date = new SimpleDateFormat().format(new Date());
		if(debug) System.out.println("git commit -m \'" + date + "\'");
		git.commit().setMessage(date).call();
	}

	public static List<AppCommit> getCommits(Git git) throws IOException, GitAPIException {
		List<AppCommit> commits = new ArrayList<>();
		if(git.getRepository().resolve(Constants.HEAD) == null) return commits;
		RevCommit lastCom = null;
		if(debug) System.out.println("git log");
		Iterable<RevCommit> iterator = git.log().call();
		List<RevCommit> result = new ArrayList();
		iterator.forEach(c -> result.add(c));
		Collections.reverse(result);
		if(debug) result.stream().map(commit -> "     " + commit).forEach(System.out::println);
		for(RevCommit commit : result) {
			AppCommit c = new AppCommit();
			c.id = commit.getId().getName();
			c.changes = getChanges(git, commit, lastCom);
			PersonIdent authorIdent = commit.getAuthorIdent();
			c.date = authorIdent.getWhen();
			c.commitMsg = new SimpleDateFormat().format(c.date);
//			if(c.changes.size() > 0) {
				lastCom = commit;
				commits.add(c);
//			}
		}
		return commits;
	}

	public static Git initOrLoad(File localPath) throws GitAPIException, IOException {
		try {
			Git git = Git.open(localPath);
			if(!git.getRepository().getDirectory().getParentFile().equals(localPath)) {
				System.out.println("Preventing git from loading existing git repository in "
						+ git.getRepository().getDirectory().getParentFile()
						+ ", creating new repository in " + localPath);
				throw new RepositoryNotFoundException(localPath);
			}
			return git;
		}
		catch(RepositoryNotFoundException e) {
			if(debug) System.out.println("git init " + localPath);
			return Git.init().setDirectory(localPath).call();
		}
	}

	public static List<FileChange> getChanges(Git git, String commit1ID, String commit2ID) throws GitAPIException, IOException {
		RevWalk walk = new RevWalk(git.getRepository());
		RevCommit commit1 = walk.parseCommit(ObjectId.fromString(commit1ID));
		RevCommit commit2 = walk.parseCommit(ObjectId.fromString(commit2ID));
		return getChanges(git, commit1, commit2);
	}

	public static List<FileChange> getChanges(Git git, RevCommit commit1, RevCommit commit2) throws GitAPIException, IOException {
		AbstractTreeIterator tree2;
		if(commit2 != null)
			tree2 = getTree(git, commit2);
		else
			tree2 = new EmptyTreeIterator();
		List<FileChange> changes = new ArrayList<>();
		AbstractTreeIterator tree1 = getTree(git, commit1);
		if(debug) System.out.println("git diff " + (commit2 == null ? "NULL" : commit2.getName()) + " " + commit1.getName());
		git.diff().setOldTree(tree2).setNewTree(tree1).call().forEach(entry -> {
			FileChange change = new FileChange();
			change.status = toStatus(entry.getChangeType());
			change.oldPath = entry.getOldPath();
			change.newPath = entry.getNewPath();
			changes.add(change);
		});
		detectVersionChanges(changes);
		if(debug) changes.stream().map(change -> "     " + change).forEach(System.out::println);
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
		if(debug) System.out.println("git status");
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
		if(debug) System.out.println("git reset --hard " + id);
		git.reset().setMode(ResetCommand.ResetType.HARD).setRef(id).call();
		if(debug) System.out.println("git clean -f -d");
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
							if(debug) System.out.println("     pick " + step.getCommit());
						} else {
							if(foundStep) {
								if(debug) System.out.println("     squash " + step.getCommit());
								step.setAction(RebaseTodoLine.Action.SQUASH);
//								RevCommit c = walk.parseCommit(step.getCommit().toObjectId());
//								System.out.println("Found step " + c.getFullMessage());
//								name = c.getFullMessage();
								foundStep = false;
							} else {
								if(debug) System.out.println("     pick " + step.getCommit());
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
		if(debug) System.out.println("git rebase -i " + commit.getParent(0).getName());
		git.rebase().setUpstream(commit.getParent(0)).runInteractively(handler).call();
	}

	public static List<FileChange> getCurrentChanges(Git git) throws GitAPIException {
		List<FileChange> changes = new ArrayList<>();
		if(debug) System.out.println("git status");
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
		status.getChanged().forEach(file -> {
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
		if(debug) changes.stream().map(change -> "     " + change).forEach(System.out::println);
		return changes;
	}

	public static void discardChange(Git git, FileChange fileChange) throws GitAPIException {
		if(fileChange.status.equals(FileChange.Status.DELETE)
			|| fileChange.status.equals(FileChange.Status.MODIFY)) {
			if(debug) System.out.println("git checkout HEAD~1 " + fileChange.oldPath);
			git.checkout().addPath(fileChange.oldPath).setStartPoint("HEAD~1").call();
//			git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").addPath(fileChange.oldPath).call();
		}
		else if(fileChange.status.equals(FileChange.Status.ADD)) {
			if(debug) System.out.println("git rm " + fileChange.newPath);
			git.rm().addFilepattern(fileChange.newPath).call();
		}
		else if(fileChange.status.equals(FileChange.Status.VERSION_CHANGE)) {
			if(debug) System.out.println("git rm " + fileChange.newPath);
			if(debug) System.out.println("git checkout HEAD~1 " + fileChange.oldPath);
			git.rm().addFilepattern(fileChange.newPath).call();
			git.checkout().addPath(fileChange.oldPath).setStartPoint("HEAD~1").call();
		}
	}

	public static void commitAmend(Git git) throws GitAPIException {
		if(debug) System.out.println("git commit --amend");
		git.commit().setAmend(true).setMessage("").call();
	}

	public static void undoLastCommit(Git git) throws GitAPIException {
		restoreStatus(git, "HEAD~1");
	}

	public static void createAndCheckoutBranch(Git git, String branchName) throws GitAPIException {
		if(debug) System.out.println("git checkout -b " + branchName);
		git.checkout().setCreateBranch(true).setName(branchName).call();
	}

	public static void createAndCheckoutEmptyBranch(Git git, String branchName) throws GitAPIException, IOException {
		if(debug) System.out.println("git checkout --orphan " + branchName);
		git.checkout().setOrphan(true).setName(branchName).call();
		if(debug) System.out.println("git reset --hard");
		git.reset().setMode(ResetCommand.ResetType.HARD).call();
	}

	public static void checkoutBranch(Git git, String name) throws GitAPIException {
		if(debug) System.out.println("git checkout " + name);
		git.checkout().setName(name).call();
	}

	public static void renameBranch(Git git, String oldName, String newName) throws GitAPIException {
		if(debug) System.out.println("git branch -m " + oldName + " " + newName);
		git.branchRename().setOldName(oldName).setNewName(newName).call();
	}

	public static void deleteBranch(Git git, String name) throws GitAPIException {
		if(debug) System.out.println("git branch -d " + name);
		git.branchDelete().setForce(true).setBranchNames(name).call();
	}

	public static List<Ref> getBranches(Git git) throws GitAPIException {
		if(debug) System.out.println("git branch");
		return git.branchList().call();
	}

	public static String getCurrentBranch(Git git) throws IOException {
		return git.getRepository().getFullBranch();
	}
}
