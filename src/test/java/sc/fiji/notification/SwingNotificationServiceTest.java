package sc.fiji.notification;

import net.imagej.ImageJ;
import org.junit.Test;
import sc.fiji.notification.ui.NotificationDialog;

import javax.swing.*;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

public class SwingNotificationServiceTest {

	@Test
	public void addNotification() throws InvocationTargetException, InterruptedException {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		SwingNotificationService notifyService = ij.get(SwingNotificationService.class);
		assertEquals(0, notifyService.notifications.size());
		notifyService.addNotification("n1", () -> {}, 0);
		assertEquals(1, notifyService.notifications.size());
		notifyService.addNotification("n2", () -> {}, 0);
		notifyService.addNotification("n3", () -> {}, 0);
		assertEquals(3, notifyService.notifications.size());

		NotificationDialog dialog1 = null, dialog2 = null, dialog3 = null;

		int i = 0;
		for(NotificationDialog dialog : notifyService.notifications.values()) {
			if(i == 0) dialog1 = dialog;
			if(i == 1) dialog2 = dialog;
			if(i == 2) dialog3 = dialog;
			i++;
		}
		assertNotNull(dialog1);
		assertNotNull(dialog2);
		assertNotNull(dialog3);
		assert(dialog1.getTitle().equals("n1"));
		assert(dialog2.getTitle().equals("n2"));
		assert(dialog3.getTitle().equals("n3"));
		Point btnBottomRight = new Point(notifyService.eventBtn.getLocationOnScreen().x + notifyService.eventBtn.getWidth(),
				notifyService.eventBtn.getLocationOnScreen().y + notifyService.eventBtn.getHeight());
		int margin = notifyService.DIALOG_MARGIN;
		assertEquals(btnBottomRight.y + margin, dialog1.getLocationOnScreen().y);
		assertEquals(btnBottomRight.y + dialog1.getHeight() + margin*2, dialog2.getLocationOnScreen().y);
		assertEquals(btnBottomRight.y + dialog1.getHeight() + dialog2.getHeight() + margin*3, dialog3.getLocationOnScreen().y);

		System.out.println("removing notification");
		notifyService.removeNotification("n1");
		NotificationDialog finalDialog1 = dialog1;
		NotificationDialog finalDialog2 = dialog2;
		NotificationDialog finalDialog3 = dialog3;
		SwingUtilities.invokeAndWait(() -> {
			assertFalse(finalDialog1.isDisplayable());
			assertEquals(btnBottomRight.y + margin, finalDialog2.getLocationOnScreen().y);
			assertEquals(btnBottomRight.y + finalDialog2.getHeight() + margin*2, finalDialog3.getLocationOnScreen().y);
		});

	}
}