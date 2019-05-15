package sc.fiji.versioning.command.action;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class UpdateAction extends AbstractAction {

	@Parameter
	CommandService commandService;
	private final CommandInfo updater;

	public UpdateAction(CommandInfo oldUpdater) {
		super("Update..");
		this.updater = oldUpdater;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		commandService.run(updater, true);
	}
}
