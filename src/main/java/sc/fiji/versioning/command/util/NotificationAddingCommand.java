/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package sc.fiji.versioning.command.util;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.notification.NotificationService;

import javax.swing.*;
import java.util.Date;

@Plugin(type = Command.class, label="Add notification")
public class NotificationAddingCommand implements Command {

    @Parameter
    NotificationService notificationService;
    @Override
    public void run() {
        String text = new Date().toString();
        notificationService.addNotification(text, () -> {
            JOptionPane.showMessageDialog(null,
                    text);
        });
    }

}
