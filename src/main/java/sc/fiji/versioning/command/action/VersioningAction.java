package sc.fiji.versioning.command.action;

import net.imagej.ImageJ;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import sc.fiji.versioning.command.Versioning;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class VersioningAction extends AbstractAction {
	@Parameter
	CommandService commandService;

	public VersioningAction() {
		super("Versioning");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		commandService.run(Versioning.class, true);
	}
}
