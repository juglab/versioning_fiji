package sc.fiji.versioning.notification;

import net.imagej.ImageJService;

public interface NotificationService extends ImageJService {
	default void addNotification(String text, Runnable action) {
		addNotification(text, action, 0);
	}
	void addNotification(String text, Runnable action, int popupVisibleTimeout);
	void removeNotification(String text);
}
