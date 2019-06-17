package sc.fiji.versioning.service;

import net.imagej.ui.swing.updater.ProgressDialog;
import net.imagej.ui.swing.updater.SwingTools;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.util.Progress;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import sc.fiji.versioning.model.FileChange;
import sc.fiji.versioning.ui.FileChangesConfirmationDialog;
import sc.fiji.versioning.ui.VersioningFrame;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Plugin(type = Service.class)
public class SwingVersioningService extends AbstractService implements VersioningUIService {

	@Parameter
	Context context;

	@Override
	public boolean approveChanges(List<FileChange> changes, String message) {
		Frame parent = null;
		if (changes.iterator().hasNext()) {
//			files.markForUpdate(false);
			return changesApproved(changes);
		} else {
			SwingTools.showMessageBox(parent, "Nothing to change on your installation.", JOptionPane.INFORMATION_MESSAGE);
		}
		return true;
	}

	private boolean changesApproved(List<FileChange> changes) {
		AtomicReference<FileChangesConfirmationDialog> dialog = new AtomicReference<>();
		if (SwingUtilities.isEventDispatchThread()) {
			dialog.set(new FileChangesConfirmationDialog(changes));
		} else {
			try {
				SwingUtilities.invokeAndWait(() -> {
					dialog.set(new FileChangesConfirmationDialog(changes));
				});
			} catch (InterruptedException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return dialog.get().fileChangesApproved();
	}

	@Override
	public boolean approveChanges(FilesCollection files) {
		Frame parent = null;
		if(files.changes().iterator().hasNext()) {
//			files.markForUpdate(false);
			return fileChangesApproved(files);
		} else {
			SwingTools.showMessageBox(parent, "Nothing to change on your installation.", JOptionPane.INFORMATION_MESSAGE);
		}
		return true;
	}

	@Override
	public Progress getProgressDialog() {
		return new ProgressDialog(null);
	}

	private boolean fileChangesApproved(FilesCollection files) {
		AtomicReference<FileChangesConfirmationDialog> dialog = new AtomicReference<>();
		if (SwingUtilities.isEventDispatchThread()) {
			dialog.set(new FileChangesConfirmationDialog(files));
		} else {
			try {
				SwingUtilities.invokeAndWait(() -> {
					dialog.set(new FileChangesConfirmationDialog(files));
				});
			} catch (InterruptedException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return dialog.get().fileChangesApproved();
	}

	@Override
	public void showSessionHistory() {
		VersioningFrame frame = new VersioningFrame();
		context.inject(frame);
		frame.init();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		frame.checkForChanges();
	}
}
