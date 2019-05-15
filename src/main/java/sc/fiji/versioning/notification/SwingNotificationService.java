package sc.fiji.versioning.notification;

import net.imagej.legacy.LegacyService;
import net.imagej.updater.FilesCollection;
import net.miginfocom.swing.MigLayout;
import org.scijava.Initializable;
import org.scijava.app.AppService;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.ui.swing.search.SwingSearchBar;
import org.xml.sax.SAXException;
import sc.fiji.versioning.command.action.VersioningAction;
import sc.fiji.versioning.command.ui.NotificationDialog;
import sc.fiji.versioning.command.ui.UpdateSitesMenu;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Plugin(type = Service.class)
public class SwingNotificationService extends AbstractService implements NotificationService, Initializable {

	@Parameter
	LegacyService legacy;

	@Parameter
	ModuleService moduleService;

	@Parameter
	CommandService commandService;

	@Parameter
	AppService appService;

	List<NotificationDialog> notifications = new ArrayList<>();
	private Icon notificationIcon, notificationNewIcon;
	private JButton eventBtn;
	private JPopupMenu popup;
	private JMenuItem noNotificationsItem;

	@Override
	public void initialize() {
		addEventButton((SwingSearchBar) legacy.getIJ1Helper().getSearchBar());
		createPopupMenu();
	}

	private class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			popup.show(e.getComponent(),
					e.getX(), e.getY());
		}

		public void mouseReleased(MouseEvent e) {}
	}

	private void createPopupMenu() {
		popup = new JPopupMenu();
		UpdateSitesMenu updateSites = new UpdateSitesMenu(getFilesCollection(), commandService, moduleService);
		noNotificationsItem = new JMenuItem("No new notifications");
		noNotificationsItem.setEnabled(false);
		popup.add(noNotificationsItem);
		popup.addSeparator();
		popup.add(updateSites);
		VersioningAction versioningAction = new VersioningAction();
		getContext().inject(versioningAction);
		popup.add(versioningAction);
		MouseListener popupListener = new PopupListener();
		eventBtn.addMouseListener(popupListener);
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
	public void addNotification(String text, Runnable runnable, int popupVisibleTimeout) {
		Component parent = eventBtn;
		if(notifications.size() > 0) parent = notifications.get(notifications.size()-1);
		NotificationDialog dialog = new NotificationDialog(parent, text, getAction(text, runnable));
		notifications.add(dialog);
		eventBtn.setIcon(notificationNewIcon);
		JMenuItem item = popup.add(getAction(text, runnable));
		popup.add(item, 0);
		popup.remove(noNotificationsItem);
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

	@Override
	public void removeNotification(String text) {

	}

	private FilesCollection getFilesCollection() {
		FilesCollection files = new FilesCollection(appService.getApp().getBaseDirectory());
		try {
			files.read();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return files;
	}

}
