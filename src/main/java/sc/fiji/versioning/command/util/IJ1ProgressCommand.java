package sc.fiji.versioning.command.util;

import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type=Command.class, label="Command using IJ1 Progressbar")
public class IJ1ProgressCommand implements Command {
	private int progress;
	private int total = 10000;

	@Override
	public void run() {
		progress = 0;
		Thread t1 = new Thread(() -> {
			while(progress < total) {
				IJ.showProgress(progress, total);
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
		ij.command().run(IJ1ProgressCommand.class, true);
	}

}
