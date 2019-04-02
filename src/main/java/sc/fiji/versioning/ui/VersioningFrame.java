package sc.fiji.versioning.ui;

import net.imagej.updater.util.Progress;
import net.miginfocom.swing.MigLayout;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.AppCommitInProgress;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.service.VersioningService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class VersioningFrame extends JFrame implements Progress {

	private VersioningService versioningService;

	private JList commitList, commitDetails;
	private List<AppCommit> data;
	private JButton deleteBtn, restoreBtn, discardSelectedBtn, undoLastCommitBtn, renameBtn;
	private Vector<AppCommit> commits;
	private List<FileChange> changes;

	public VersioningFrame(VersioningService versioningService) {
		super("Versioning");
		this.versioningService = versioningService;
		setMinimumSize(new Dimension(800, 400));
		setContentPane(createContent());
	}

	public void saveCurrentState() {
		try {
			if(versioningService.hasUnsavedChanges()) {
				System.out.println("   unsaved changes");
				Vector<AppCommit> commits = this.commits;
				commits.add(new AppCommitInProgress());
				commitList.setListData(commits);
				new Thread(() -> {
					try {
						versioningService.commitCurrentChanges();
					} catch (Exception e) {
						e.printStackTrace();
					}
					updateCommits();
				}).start();
			} else {
				System.out.println("   no unsaved changes");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private Container createContent() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
		panel.setLayout(new MigLayout("gap 15px, wmin 400px, hmin 60px, fill", "[][grow][]", "[grow][]"));
		panel.add(scroll(createCommitList()), "grow");
		panel.add(scroll(createCommitDetailView()), "grow, span, wrap");
		panel.add(createRestoreBtn());
		panel.add(createDeleteBtn());
		panel.add(createUndoLastCommitBtn());
		panel.add(createDiscardSelectedBtn());
		return panel;
	}

	private Component scroll(Component component) {
		return new JScrollPane(component);
	}

	private Component createCommitList() {
		commitList = new JList();
		updateCommits();
		commitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		commitList.addListSelectionListener(e -> {
			int selected = commitList.getSelectedIndex();
			if(selected < 0 || selected >= data.size()) {
				restoreBtn.setEnabled(false);
				deleteBtn.setEnabled(false);
				return;
			}
			changes = data.get(selected).changes;
			if(changes != null) {
				commitDetails.setListData(asVector(changes));
				commitDetails.revalidate();
				restoreBtn.setEnabled(true);
				deleteBtn.setEnabled(true);
			} else {
				restoreBtn.setEnabled(false);
				deleteBtn.setEnabled(true);
			}
			if(selected == data.size()-1) {
				deleteBtn.setEnabled(false);
			}
		});
		commitList.setCellRenderer(new MyDefaultListCellRenderer());
		return commitList;
	}

	private class MyDefaultListCellRenderer extends DefaultListCellRenderer {

		private final JLabel progress;

		public MyDefaultListCellRenderer() {
			super();
			progress = new JLabel("Saving changes ..");
			progress.setBackground(Color.red);
		}

		@Override
		public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
		{
			if(AppCommitInProgress.class.equals(value.getClass())) {
				return progress;
			} else {
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		}


	}
	private Component createCommitDetailView() {
		commitDetails = new JList();
		commitDetails.addListSelectionListener(e -> {
			int selected = commitDetails.getSelectedIndex();
			if(selected < 0 || selected >= changes.size()) {
				discardSelectedBtn.setEnabled(false);
				return;
			}
			discardSelectedBtn.setEnabled(true);
		});
		return commitDetails;
	}
	private Component createRestoreBtn() {
		Action action = new AbstractAction("Restore commit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					versioningService.restoreCommit(data.get(commitList.getSelectedIndex()).id);
					JOptionPane.showMessageDialog(null,
							"Please restart the application.",
							"Restart after restoring application status",
							JOptionPane.PLAIN_MESSAGE);
					dispose();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		};
		restoreBtn = new JButton(action);
		restoreBtn.setEnabled(false);
		return restoreBtn;
	}

	private Component createDeleteBtn() {
		Action action = new AbstractAction("Merge with next commit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					versioningService.mergeCommitWithNext(data.get(commitList.getSelectedIndex()).id);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				updateCommits();
			}
		};
		deleteBtn = new JButton(action);
		deleteBtn.setEnabled(false);
		return deleteBtn;
	}

	private Component createDiscardSelectedBtn() {
		Action action = new AbstractAction("Discard selected") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<FileChange> selectedChanges = new ArrayList<>();
					for(int index : commitDetails.getSelectedIndices()) {
						selectedChanges.add(changes.get(index));
					}
					versioningService.discardChange(selectedChanges);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				updateCommits();
			}
		};
		discardSelectedBtn = new JButton(action);
		discardSelectedBtn.setEnabled(false);
		return discardSelectedBtn;
	}

	private Component createUndoLastCommitBtn() {
		Action action = new AbstractAction("Undo last commit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					versioningService.undoLastCommit();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				updateCommits();
			}
		};
		undoLastCommitBtn = new JButton(action);
		undoLastCommitBtn.setEnabled(data.size() > 1);
		return undoLastCommitBtn;
	}

	public void updateCommits() {
		try {
			int selected = commitList.getSelectedIndex();
			commits = getCommits();
			commitList.setListData(commits);
			commitList.setSelectedIndex(selected);
			if(undoLastCommitBtn != null) undoLastCommitBtn.setEnabled(data.size() > 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Vector getCommits() throws Exception {
		data = versioningService.getCommits();
		return asVector(data);
	}

	private Vector asVector(List list) {
		Vector res = new Vector();
		res.addAll(list);
		return res;
	}

	@Override
	public void setCount(int count, int total) {

	}

	@Override
	public void addItem(Object item) {

	}

	@Override
	public void setItemCount(int count, int total) {

	}

	@Override
	public void itemDone(Object item) {

	}

	@Override
	public void done() {

	}
}
