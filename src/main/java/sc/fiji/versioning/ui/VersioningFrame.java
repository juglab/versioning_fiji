package sc.fiji.versioning.ui;

import net.imagej.updater.util.Progress;
import net.miginfocom.swing.MigLayout;
import sc.fiji.versioning.model.AppCommit;
import sc.fiji.versioning.model.AppCommitInProgress;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.service.VersioningService;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
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

	private VisualStatus checkForChanges, checkForCrash;

	private static final String frameTitle = "Versioning";
	private static final String panelTitle = "Managing your local installation";
	private static final String checkForChangesStr = "Checking for changes..";
	private static final String savingChangesStr = "Saving changes ..";
	private static final String uncommittedChangesStr = "Uncommitted changes";
	private static final String restoreBtnTitle = "Restore commit";
	private static final String deleteBtnTitle = "Merge with next commit";
	private static final String restartFrameTitle = "Please restart the application.";
	private static final String restartFrameText = "Restart after restoring application status";
	private static final String discardSelectedBtnTitle = "Discard selected";
	private static final String undoLastCommitBtnTitle = "Undo last commit";
	private static final String changesPresentStr = "Unsaved changes in Fiji installation.";
	private static final String noChangesPresentStr = "No unsaved changes in Fiji installation.";

	public VersioningFrame(VersioningService versioningService) {
		super(frameTitle);
		this.versioningService = versioningService;
		setMinimumSize(new Dimension(800, 400));
		setContentPane(createContent());
	}

	public void checkForChanges() {
		checkForChanges.setStatus(VisualStatus.StatusType.RUNNING, checkForChangesStr);
		try {
			if(versioningService.hasUnsavedChanges()) {
				checkForChanges.setStatus(VisualStatus.StatusType.WARNING, changesPresentStr);
				System.out.println("     unsaved changes");
				checkForChanges.setStatus(VisualStatus.StatusType.RUNNING, savingChangesStr);
				saveChanges();
			} else {
				checkForChanges.setStatus(VisualStatus.StatusType.DONE, noChangesPresentStr);
				System.out.println("     no unsaved changes");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveChanges() {
		Vector<AppCommit> commits = this.commits;
		commits.add(new AppCommitInProgress());
		commitList.setListData(commits);
		commitList.setSelectedIndex(commits.size()-1);
		new Thread(() -> {
			try {
				versioningService.commitCurrentChanges();
			} catch (Exception e) {
				e.printStackTrace();
			}
			updateCommits();
			checkForChanges.setStatus(VisualStatus.StatusType.DONE, noChangesPresentStr);
		}).start();
	}

	private void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private Container createContent() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
		panel.setLayout(new MigLayout("gap 15px, wmin 400px, hmin 60px, fill", "[][grow][]", "[][][grow][]"));
		panel.add(createTitle(), "span, wrap");
		panel.add(createChangeCheck(), "span, wrap");
		panel.add(createCommitView(), "span, wrap, grow");
		panel.add(createFooter(), "span");
		return panel;
	}

	private static Component createTitle() {
		return createHTMLText("<html><h2>" + panelTitle + "</h2></html>");
	}

	private static JEditorPane createHTMLText(String text) {
		JEditorPane component =
				new JEditorPane(new HTMLEditorKit().getContentType(), text);
		component.setEditable(false);
		component.setOpaque(false);
		Font font = UIManager.getFont("Label.font");
		String bodyRule = "body { font-family: " + font.getFamily() + "; " +
				"font-size: " + font.getSize() + "pt; }";
		((HTMLDocument)component.getDocument()).getStyleSheet().addRule(bodyRule);
		return component;
	}

	private Component createChangeCheck() {
		checkForChanges = new VisualStatus();
		checkForChanges.setStatus(VisualStatus.StatusType.IDLE, checkForChangesStr);
		return checkForChanges;
	}

	private Component createCommitView() {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("fill"));
		panel.add(scroll(createCommitList()), "grow");
		panel.add(scroll(createCommitDetailView()), "grow, span");
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
				commitDetails.setListData(new Vector());
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
			progress = new JLabel(uncommittedChangesStr);
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

	private Component createFooter() {
		JPanel panel = new JPanel();
		panel.add(createRestoreBtn());
		panel.add(createDeleteBtn());
		panel.add(createUndoLastCommitBtn());
		panel.add(createDiscardSelectedBtn());
		return panel;
	}

	private Component createRestoreBtn() {
		Action action = new AbstractAction(restoreBtnTitle) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					versioningService.restoreCommit(data.get(commitList.getSelectedIndex()).id);
					JOptionPane.showMessageDialog(null,
							restartFrameTitle,
							restartFrameText,
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
		Action action = new AbstractAction(deleteBtnTitle) {
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
		Action action = new AbstractAction(discardSelectedBtnTitle) {
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
		Action action = new AbstractAction(undoLastCommitBtnTitle) {
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
