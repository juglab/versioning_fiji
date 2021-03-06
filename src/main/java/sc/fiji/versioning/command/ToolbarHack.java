/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package sc.fiji.versioning.command;

import net.imagej.ImageJ;
import net.imagej.updater.UpdaterUI;
import org.scijava.Initializable;
import org.scijava.Priority;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.log.LogListener;
import org.scijava.log.LogMessage;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.notification.NotificationService;
import sc.fiji.versioning.service.VersioningService;

import java.util.List;

@Plugin(type = UpdaterUI.class, priority = Priority.HIGH)
public class ToolbarHack implements UpdaterUI, Initializable, LogListener {

    @Parameter
    LogService logService;

    @Parameter
    CommandService commandService;

    @Parameter
    NotificationService notificationService;

    @Parameter
    VersioningService versioningService;

    @Override
    public void initialize() {
//        logService.addLogListener(this);
    }

    @Override
    public void run() {
        notificationService.addNotification("Updates available!", () -> commandService.run(getOldUpdater(), true));
        try {
            versioningService.commitCurrentChanges();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CommandInfo getOldUpdater() {
        final List<CommandInfo> updaters =
                commandService.getCommandsOfType(UpdaterUI.class);
        if (updaters.size() > 0) {
            for(CommandInfo updater : updaters) {
                if(!updater.getClassName().equals(this.getClass().getName())) {
                    return updater;
                }
            }
        }
        else {
            logService.error("No updater plugins found!");
        }
        return null;
    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // invoke the plugin
        ij.command().run(ToolbarHack.class, true);
    }

    @Override
    public void messageLogged(LogMessage message) {
        notificationService.addMessage(message);
    }
}
