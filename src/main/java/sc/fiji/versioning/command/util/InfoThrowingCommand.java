/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package sc.fiji.versioning.command.util;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

@Plugin(type = Command.class, label="Throw info")
public class InfoThrowingCommand implements Command {

    @Parameter
    LogService logService;
    @Parameter
    UIService uiService;
    @Override
    public void run() {
        logService.info("This is some information.");
    }

}
