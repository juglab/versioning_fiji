package sc.fiji.versioning.ui;

import net.imagej.updater.util.Progress;
import net.miginfocom.swing.MigLayout;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.model.Session;
import sc.fiji.versioning.service.VersioningService;
import sc.fiji.versioning.service.VersioningUIService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SessionsFrame extends JFrame implements Progress {

	@Parameter
	private VersioningService versioningService;

	@Parameter
	private VersioningUIService versioningUIService;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private EventService eventService;

	private JList sessionsList;
	private List<Session> data = new ArrayList<>();
	private JButton deleteBtn, restoreBtn, renameBtn;

	private static final String frameTitle = "Session manager";
	private static final String restoreBtnTitle = "Open session";
	private static final String deleteBtnTitle = "Delete session";
	private static final String renameBtnTitle = "Rename session";
	private static final String restartFrameTitle = "Please restart the application.";
	private static final String restartFrameText = "Restart after session change";
	private JComboBox<Session> referenceSession;
	private JComboBox<Session> compareSession;
	private JList compareList;
	private Session rSession, cSession;
	private Session currentSession;

	public SessionsFrame() {
		super(frameTitle);
		setMinimumSize(new Dimension(800, 500));
		setContentPane(createContent());
	}

	public void init() {
		updateCurrentSession();
		updateList();
	}

	private void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private Container createContent() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
		panel.setLayout(new MigLayout("gap 15px, wmin 400px, hmin 60px, fill", "[][grow][]", "[][][grow][]"));
		panel.add(createSessionsView(), "span, wrap, grow");
		panel.add(createCompareChoice(), "span, wrap");
		panel.add(createCompareView(), "span, wrap, grow, push");
		panel.add(createFooter(), "span");
		return panel;
	}

	private Component createCompareView() {
		compareList = new JList();
		return scroll(compareList);
	}

	private Component createCompareChoice() {
		JPanel panel = new JPanel();
		referenceSession = new JComboBox<>();
		referenceSession.addActionListener(e -> {
			rSession = data.get(referenceSession.getSelectedIndex());
			threadService.run(() -> updateCompareList());
		});
		compareSession = new JComboBox<>();
		compareSession.addActionListener(e -> {
			cSession = data.get(compareSession.getSelectedIndex());
			threadService.run(() -> updateCompareList());
		});
		panel.add(new JLabel("Compare"));
		panel.add(referenceSession);
		panel.add(new JLabel("to"));
		panel.add(compareSession);
		panel.add(new JLabel(":"));
		return panel;
	}

	private Component createSessionsView() {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("fill"));
		panel.add(scroll(createSessionsList()), "grow");
		return panel;
	}

	private Component scroll(Component component) {
		return new JScrollPane(component);
	}

	private Component createSessionsList() {
		sessionsList = new JList();
		sessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sessionsList.setCellRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(
					JList<?> list,
					Object value,
					int index,
					boolean isSelected,
					boolean cellHasFocus)
			{
				Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(currentSession != null && currentSession.name.equals(((Session)value).name)) {
					comp.setForeground(Color.blue);
				}
				return comp;
			}
		});
		sessionsList.addListSelectionListener(e -> {
			int selected = sessionsList.getSelectedIndex();
			if (selected < 0 || selected >= data.size()) {
				restoreBtn.setEnabled(false);
				deleteBtn.setEnabled(false);
				renameBtn.setEnabled(false);
				return;
			}
			updateCurrentSession();
			Session selectedSession = data.get(selected);
			if (currentSession.name.equals(selectedSession.name)) {
				restoreBtn.setEnabled(false);
				deleteBtn.setEnabled(false);
			} else {
				restoreBtn.setEnabled(true);
				deleteBtn.setEnabled(true);
			}
			renameBtn.setEnabled(true);
		});
//		sessionsList.setCellRenderer(new SessionsRenderer());
		return sessionsList;
	}

	private void updateCurrentSession() {
		try {
			currentSession = versioningService.getCurrentSession();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private Component createFooter() {
		JPanel panel = new JPanel();
		panel.add(createLoadBtn());
		panel.add(createRenameBtn());
		panel.add(createDeleteBtn());
		return panel;
	}

	private Component createLoadBtn() {
		Action action = new AbstractAction(restoreBtnTitle) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					versioningService.openSession(data.get(sessionsList.getSelectedIndex()).name);
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
					versioningService.deleteSession(data.get(sessionsList.getSelectedIndex()).name);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				updateList();
			}
		};
		deleteBtn = new JButton(action);
		deleteBtn.setEnabled(false);
		return deleteBtn;
	}

	private Component createRenameBtn() {
		Action action = new AbstractAction(renameBtnTitle) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String newName = versioningUIService.askFor("New name", String.class);
					versioningService.renameSession(data.get(sessionsList.getSelectedIndex()).name, newName);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				updateList();
			}
		};
		renameBtn = new JButton(action);
		renameBtn.setEnabled(false);
		return renameBtn;
	}

	public void updateList() {
		try {
			int selected = sessionsList.getSelectedIndex();
			data = versioningService.getSessions();
			sessionsList.setListData(asVector(data));
			sessionsList.setSelectedIndex(selected);
			data.forEach(session -> {
				referenceSession.addItem(session);
				compareSession.addItem(session);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateCompareList() {
		try {
			List<FileChange> changes = versioningService.getChanges(cSession, rSession);
			if(changes != null) compareList.setListData(asVector(changes));
		} catch (Exception e) {
			e.printStackTrace();
		}
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
