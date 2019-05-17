/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package sc.fiji.versioning.command.util;

import net.imagej.ImageJ;
import net.imagej.updater.UpdaterUI;
import org.scijava.Initializable;
import org.scijava.Priority;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.versioning.notification.NotificationService;

import java.util.List;

@Plugin(type = Command.class, label="Throw error")
public class ErrorThrowingCommand implements Command {

    @Parameter
    LogService logService;
    @Override
    public void run() {
        logService.error("This is an error.");
    }

}
