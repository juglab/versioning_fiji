package sc.fiji.notification;

import net.imagej.legacy.LegacyService;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdaterUI;
import net.imagej.updater.util.AvailableSites;
import net.miginfocom.swing.MigLayout;
import org.scijava.Initializable;
import org.scijava.app.AppService;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.log.LogMessage;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.swing.search.SwingSearchBar;
import org.xml.sax.SAXException;
import sc.fiji.notification.ui.DialogActionExecutedEvent;
import sc.fiji.notification.ui.NotificationDialog;
import sc.fiji.versioning.command.session.*;
import sc.fiji.versioning.service.SessionChangedEvent;
import sc.fiji.versioning.service.VersioningService;
import sc.fiji.versioning.ui.updatesite.UpdateSitesMenu;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Plugin(type = Service.class)
public class SwingNotificationService extends AbstractService implements NotificationService, Initializable, ActionListener {

	static final int DIALOG_MARGIN = 5;
	@Parameter
	private LegacyService legacy;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private AppService appService;

	@Parameter
	private VersioningService versioningService;

	@Parameter
	private EventService eventService;

	Map<String, NotificationDialog> notifications = new LinkedHashMap<>();
	Map<String, JMenuItem> notificationMenuItems = new LinkedHashMap<>();
	private Icon notificationIcon, notificationNewIcon;
	JButton eventBtn;
	private JPopupMenu popup;
	private JMenuItem noNotificationsItem;
	private JMenuItem sessionIndicator;
	private int sessionsListIndex;
	private JMenu sessionsMenu;
	private List<JMenuItem> sessionsMenuEntries = new ArrayList();


	@Override
	public void initialize() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				addEventButton((SwingSearchBar) legacy.getIJ1Helper().getSearchBar());
	//		Panel statusBar = legacy.getIJ1Helper().getStatusBar();
	//		ProgressBar ij1progress = (ProgressBar) statusBar.getComponent(1);
				createPopupMenu();
			});
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
		eventService.subscribe(this);
	}

	@EventHandler
	private void onSessionChanged(final SessionChangedEvent e) {
		updateSessionIndicator();
		updateSessionsList();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getClass().equals(DialogActionExecutedEvent.class)) {
			removeNotification((NotificationDialog)e.getSource());
		}
	}

	private void removeNotification(NotificationDialog dialog) {
		removeNotification(dialog.getTitle());
	}

	private class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			popup.show(e.getComponent(),
					e.getX(), e.getY());
		}
	}

	private void createPopupMenu() {
		popup = new JPopupMenu();
		UpdateSitesMenu updateSites = new UpdateSitesMenu(getFilesCollection(), commandService, moduleService, getOldUpdater());
		noNotificationsItem = new JMenuItem("No new notifications");
		noNotificationsItem.setEnabled(false);
		popup.add(noNotificationsItem);
		popup.addSeparator();
		popup.add(createCurrentSessionIndicator());
		popup.addSeparator();
		popup.add(updateSites);
		popup.add(createSessionsMenu());
		popup.add(createCurrentSessionMenu());
		MouseListener popupListener = new PopupListener();
		eventBtn.addMouseListener(popupListener);
	}

	private Component createCurrentSessionIndicator() {
		sessionIndicator = new JMenuItem();
		updateSessionIndicator();
		return sessionIndicator;
	}

	private void updateSessionIndicator() {
		try {
			sessionIndicator.setText("Session: " + versioningService.getCurrentSession().toString());
			popup.invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Component createCurrentSessionMenu() {
		JMenu menu = new JMenu("Current session");
		menu.add(new AbstractAction("Show version history") {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(ShowHistoryCommand.class, true);
			}
		});
		menu.add(new AbstractAction("Revert latest change") {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(RevertLatestSessionChangeCommand.class, true);
			}
		});
		menu.add(new AbstractAction("Restore initial state") {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(RestoreInitialSessionStateCommand.class, true);
			}
		});
		return menu;
	}

	private Component createSessionsMenu() {
		sessionsMenu = new JMenu("Sessions");
		sessionsMenu.add(new AbstractAction("Manage sessions") {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(ManageSessionsCommand.class, true);
			}
		});
		sessionsMenu.add(createStartSessionMenu());
		sessionsMenu.addSeparator();
		sessionsListIndex = sessionsMenu.getComponentCount();
		updateSessionsList();
		return sessionsMenu;
	}

	private void updateSessionsList() {
		sessionsMenuEntries.forEach(entry -> sessionsMenu.remove(entry));
		sessionsMenuEntries.clear();
		try {
			String currentSession = versioningService.getCurrentSession().name;
			versioningService.getSessions().forEach(session -> {
				JMenuItem item = new JMenuItem(new AbstractAction(session.toString()) {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							versioningService.openSession(session.name);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				});
				item.setEnabled(!currentSession.equals(session.name));
				sessionsMenuEntries.add(item);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		sessionsMenuEntries.forEach(entry -> sessionsMenu.add(entry));
	}

	private Component createStartSessionMenu() {
		JMenu menu = new JMenu("Start new session");
		menu.add(new AbstractAction("Copy current session") {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(CopyCurrentSessionCommand.class, true);
			}
		});
		menu.add(new AbstractAction("Import session from Fiji.app folder") {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(ImportSessionFromFolderCommand.class, true);
			}
		});
		menu.add(new AbstractAction("Download fresh session") {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(DownloadFreshSessionCommand.class, true);
			}
		});
		return menu;
	}

	private void addEventButton(SwingSearchBar searchbar) {
		eventBtn = new JButton();
		eventBtn.setBackground(null);
		Container parent = searchbar.getParent();
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new MigLayout("fillx"));
		parent.remove(searchbar);
		bottomPanel.add(searchbar, "pushx, growx");
		double size = searchbar.getPreferredSize().getHeight();
		bottomPanel.add(eventBtn, "east, hmin 0, hmax " + size + ", wmax " + size);
		createIcons((int)size);
		eventBtn.setIcon(notificationIcon);
		parent.add(bottomPanel, "south");
	}

	private void createIcons(int size) {
		Image img = new ImageIcon(getClass().getResource("/notification.png")).getImage();
		Image newimg = img.getScaledInstance(size, size,  java.awt.Image.SCALE_SMOOTH);
		notificationIcon = new ImageIcon(newimg);
		img = new ImageIcon(getClass().getResource("/notification-new.png")).getImage();
		newimg = img.getScaledInstance(size, size,  java.awt.Image.SCALE_SMOOTH);
		notificationNewIcon = new ImageIcon(newimg);
	}

	@Override
	public void addNotification(String text, Runnable runnable, int disposeTimeout) {
		addNotification(text, getAction(text, runnable), disposeTimeout);
	}

	private void addNotification(String text, Action action, int popupVisibleTimeout) {

		//in case there is already a notification with the same name, remove it
		removeNotification(text);
		try {
			SwingUtilities.invokeAndWait(() -> {

				//create notification dialog
				NotificationDialog dialog = new NotificationDialog(text, action);
				handleDialogEvents(text, dialog);
				notifications.put(text, dialog);
				initTimer(dialog, popupVisibleTimeout);
				updateNotificationPlacement();

				//create notification menu item
				JMenuItem item = popup.add(action);
				popup.add(item, 0);
				notificationMenuItems.put(text, item);

				//update event btn
				eventBtn.setIcon(notificationNewIcon);
				//remove no notifications item if present
				popup.remove(noNotificationsItem);
			});
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	private void handleDialogEvents(String text, NotificationDialog dialog) {
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);
				notifications.remove(text);
				updateNotificationPlacement();
			}
		});
		dialog.addActionListener(this);
	}

	private void initTimer(NotificationDialog dialog, int popupVisibleTimeout) {
		//start timer if popup should vanish after timeout
		if (popupVisibleTimeout > 0) {
			ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
			exec.schedule(() -> dialog.dispose(), popupVisibleTimeout, TimeUnit.MILLISECONDS);
		}
	}

	private Action getAction(String text, Runnable action) {
		return new AbstractAction(text) {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		};
	}

	private Action getAction(String text, DialogPrompt.MessageType messageType) {
		return new AbstractAction(text) {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch(messageType) {
					case WARNING_MESSAGE: commandService.run("Console", true);
					case ERROR_MESSAGE: commandService.run("Console", true);
				}
			}
		};
	}

	@Override
	public void removeNotification(String text) {
		if (SwingUtilities.isEventDispatchThread()) {
			_removeNotification(text);
		} else {
			try {
				SwingUtilities.invokeAndWait(() -> _removeNotification(text));
			} catch (InterruptedException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	private void _removeNotification(String text) {
		if(notifications.containsKey(text)) {
			notifications.get(text).dispose();
			notifications.remove(text);
		}
		if(notificationMenuItems.containsKey(text)) {
			popup.remove(notificationMenuItems.get(text));
			notificationMenuItems.remove(text);
		}
	}

	private void updateNotificationPlacement() {
		if (SwingUtilities.isEventDispatchThread()) {
			_updateNotificationPlacement();
		} else {
			try {
				SwingUtilities.invokeAndWait(() -> {
					_updateNotificationPlacement();
				});
			} catch (InterruptedException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	private void _updateNotificationPlacement() {
		Component parent = eventBtn;
		int x = parent.getLocationOnScreen().x + parent.getWidth();
		int y = parent.getLocationOnScreen().y + parent.getHeight() + DIALOG_MARGIN;
		for (NotificationDialog dialog : notifications.values()) {
			int _x = x - dialog.getWidth();
			dialog.setLocation(_x, y);
			System.out.println("Placing " + dialog.getTitle() + " to " + _x + ", " + y);
			y += dialog.getHeight() + DIALOG_MARGIN;
		}
	}

	@Override
	public void addMessage(String message, DialogPrompt.MessageType messageType, int disposeTimeout) {
		addNotification(message, getAction(message, messageType), disposeTimeout);
	}

	@Override
	public void addMessage(LogMessage message, int disposeTimeout) {
		addNotification(message.text(), getAction(message), disposeTimeout);
	}


	private CommandInfo getOldUpdater() {
		final List<CommandInfo> updaters =
				commandService.getCommandsOfType(UpdaterUI.class);
		if (updaters.size() > 0) {
			for(CommandInfo updater : updaters) {
				if(!updater.getClassName().equals(this.getClass().getName())) {
					return updater;
				}
			}
		}
		else {
			log().error("No updater plugins found!");
		}
		return null;
	}

	private Action getAction(LogMessage message) {
		return new AbstractAction(message.text()) {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run("Console", true);
			}
		};
	}

	private FilesCollection getFilesCollection() {
		FilesCollection files = new FilesCollection(appService.getApp().getBaseDirectory());
		try {
			files.read();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		 }
		AvailableSites.initializeAndAddSites(files, log());
		try {
			files.write();
		} catch (IOException | SAXException | TransformerConfigurationException e) {
			e.printStackTrace();
		}
		return files;
	}

}
