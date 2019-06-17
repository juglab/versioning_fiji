package sc.fiji.notification.ui;

import sc.fiji.notification.ui.NotificationDialog;

import java.awt.event.ActionEvent;

public class DialogActionExecutedEvent extends ActionEvent {
	public DialogActionExecutedEvent(NotificationDialog source) {
		super(source, 0, "");
	}
}
