/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package sc.fiji.versioning.command;

import net.imagej.ImageJ;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import sc.fiji.versioning.service.VersioningService;
import sc.fiji.versioning.ui.VersioningFrame;

import javax.swing.*;

@Plugin(type = Command.class, menuPath = "Help>Versioning")
public class Versioning implements Command, Initializable {

    VersioningFrame frame;

    @Parameter
    VersioningService versioningService;

    @Parameter
    ThreadService threadService;

    @Override
    public void initialize() {

    }

    private void createGui() {
        threadService.queue(() -> {
            frame = new VersioningFrame(versioningService);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
            frame.checkForChanges();
        });
    }

    @Override
    public void run() {
        createGui();
    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // invoke the plugin
        ij.command().run(Versioning.class, true);
    }

}
