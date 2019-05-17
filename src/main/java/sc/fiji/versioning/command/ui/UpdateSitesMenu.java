package sc.fiji.versioning.command.ui;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.UpdaterUI;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import sc.fiji.versioning.command.InstallUpdateSiteCommand;
import sc.fiji.versioning.command.ManageUpdateSitesCommand;
import sc.fiji.versioning.command.ModifyUpdateSiteCommand;
import sc.fiji.versioning.command.UninstallUpdateSiteCommand;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class UpdateSitesMenu extends JMenu {

	ModuleService moduleService;
	CommandService commandService;


	public UpdateSitesMenu(FilesCollection files, CommandService commandService, ModuleService moduleService, CommandInfo updater) {
		super("Update Sites");
		this.commandService = commandService;
		this.moduleService = moduleService;
		List<UpdateSite> installableUpdateSites = new ArrayList<>();
		List<UpdateSite> uninstallableUpdateSites = new ArrayList<>();
		List<UpdateSite> modifiableUpdateSites = new ArrayList<>();

		for(UpdateSite site : files.getUpdateSites(true)) {
			if(site.isActive()) {
				if(!site.isOfficial()) {
					uninstallableUpdateSites.add(site);
				}
				if(site.isUploadable()) {
					modifiableUpdateSites.add(site);
				}
			}
			else {
				installableUpdateSites.add(site);
			}
		}
		add(new UpdateSitesSubMenu("Install", files, installableUpdateSites, InstallUpdateSiteCommand.class, commandService, moduleService));
		add(new UpdateSitesSubMenu("Uninstall", files, uninstallableUpdateSites, UninstallUpdateSiteCommand.class, commandService, moduleService));
		add(new UpdateSitesSubMenu("Modify", files, modifiableUpdateSites, ModifyUpdateSiteCommand.class, commandService, moduleService));
		add(getActionForCommand("Manage Update Sites", ManageUpdateSitesCommand.class, "files", files, "updater", updater));
	}

	private Action getActionForCommand(String name, Class commandClass, Object... args) {
		return new AbstractAction(name) {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(commandClass, true, args);
			}
		};
	}

}
