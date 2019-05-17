package sc.fiji.versioning.command.util;

import net.imagej.ImageJ;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type=Command.class, label="Command using IJ2 Progressbar")
public class IJ2ProgressCommand implements Command {

	@Parameter
	StatusService statusService;

	private int progress;
	private int total = 10000;

	@Override
	public void run() {
		progress = 0;
		Thread t1 = new Thread(() -> {
			while(progress < total) {
				statusService.showProgress(progress, total);
				progress++;
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();
	}

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(IJ2ProgressCommand.class, true);
	}

}
