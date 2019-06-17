/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package sc.fiji.versioning.command.session;

import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.versioning.service.VersioningUIService;

@Plugin(type= Command.class, label = "Help>Current session>Show history")
public class ShowHistoryCommand implements Command {

    @Parameter
    VersioningUIService versioningUIService;

    @Override
    public void run() {
        versioningUIService.showSessionHistory();
    }

    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(ShowHistoryCommand.class, true);
    }

}
