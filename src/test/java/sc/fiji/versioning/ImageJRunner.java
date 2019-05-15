package sc.fiji.versioning;

import net.imagej.ImageJ;
import sc.fiji.versioning.command.Versioning;

public class ImageJRunner {
	public static void main(final String... args) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
