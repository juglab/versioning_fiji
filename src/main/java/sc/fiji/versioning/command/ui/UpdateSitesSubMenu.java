package sc.fiji.versioning.command.ui;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import sc.fiji.versioning.command.UpdateSiteCommand;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

public class UpdateSitesSubMenu extends JMenu {

	ModuleService moduleService;
	CommandService commandService;

	private static final int MAX_SIZE = 20;
	private final Class<? extends UpdateSiteCommand> action;
	private final FilesCollection files;

	public UpdateSitesSubMenu(String name, FilesCollection files, List<UpdateSite> sites, Class action, CommandService commandService, ModuleService moduleService) {

		super(name);

		this.commandService = commandService;
		this.moduleService = moduleService;
		this.files = files;
		this.action = action;

		if(sites.size() > MAX_SIZE) {
			addAlphanumeric(sites);
		}else {
			add(sites);
		}
	}

	private void addAlphanumeric(List<UpdateSite> sites) {
		sites.sort(Comparator.comparing(UpdateSite::getName));
		List<UpdateSite> smallerChunk = new ArrayList<>();
		UpdateSite last = null, next = null;
		for(UpdateSite site : sites) {
			if(smallerChunk.size() == MAX_SIZE) {
				next = site;
				add(new UpdateSitesSubMenu(getGroupName(smallerChunk, last, next), files, smallerChunk, action, commandService, moduleService));
				last = smallerChunk.get(smallerChunk.size()-1);
				smallerChunk = new ArrayList<>();
			}
			smallerChunk.add(site);
		}
		if(smallerChunk.size() > 0) {
			add(new UpdateSitesSubMenu(getGroupName(smallerChunk, last, next), files, smallerChunk, action, commandService, moduleService));
		}
	}

	private String getGroupName(List<UpdateSite> smallerChunk, UpdateSite previous, UpdateSite next) {
		String part1, part2;
		String nameFirst = smallerChunk.get(0).getName();
		String nameLast = smallerChunk.get(smallerChunk.size()-1).getName();
		if(previous == null) {
			part1 = nameFirst.substring(0, 0);
		} else {
			int i = 0;
			for(; i < nameFirst.length(); i++) {
				if(!previous.getName().substring(0, i).toLowerCase().equals(nameFirst.substring(0,i).toLowerCase())) {
					break;
				}
			}
			part1 = nameFirst.substring(0, i+1);
		}
		if(next == null) {
			part2 = nameLast.substring(0, 0);
		} else {
			int i = 0;
			for(; i < nameLast.length(); i++) {
				if(!next.getName().substring(0, i).toLowerCase().equals(nameLast.substring(0,i).toLowerCase())) {
					break;
				}
			}
			part2 = nameLast.substring(0, i);
		}
		return part1 + " - " + part2;
	}

	private void add(List<UpdateSite> sites) {
		sites.forEach(site -> add(site));
	}

	private void add(UpdateSite site) {
		ModuleInfo info = new CommandInfo(action);
		info.setLabel(info.getLabel() + " " + site.getName());
		Map<String, Object> presets = new HashMap<>();
		presets.put("files", files);
		presets.put("site", site);
		((CommandInfo) info).setPresets(presets);
		moduleService.addModule(info);
		add(new AbstractAction(site.getName()) {
			@Override
			public void actionPerformed(ActionEvent e) {
				commandService.run(action, true, "files", files, "site", site);
			}
		});
	}
}
