package sc.fiji.versioning.command.ui;

import java.awt.event.ActionEvent;

public class DialogActionExecutedEvent extends ActionEvent {
	public DialogActionExecutedEvent(NotificationDialog source) {
		super(source, 0, "");
	}
}
