package sc.fiji.versioning.notification;

import net.imagej.ImageJService;
import org.scijava.log.LogMessage;
import org.scijava.ui.DialogPrompt;

public interface NotificationService extends ImageJService {
	default void addNotification(String text, Runnable action) {
		addNotification(text, action, 0);
	}
	void addNotification(String text, Runnable action, int disposeTimeout);
	void removeNotification(String text);
	default void addMessage(String message, DialogPrompt.MessageType messageType) {
		addMessage(message, messageType, 0);
	}
	void addMessage(String message, DialogPrompt.MessageType warningMessage, int disposeTimeout);
	default void addMessage(LogMessage message) {
		addMessage(message, 0);
	}
	void addMessage(LogMessage message, int disposeTimeout);
}
