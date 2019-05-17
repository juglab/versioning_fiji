package sc.fiji.versioning.command.ui;

import net.imagej.ui.swing.updater.ProgressDialog;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.miginfocom.swing.MigLayout;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.transform.TransformerConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class FileChangesConfirmationDialog extends JDialog {

	private final FilesCollection files;

	public FileChangesConfirmationDialog(FilesCollection files) {
		this.files = files;
		this.setContentPane(createContent());
		pack();
	}

	private Container createContent() {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.add(createChangesPanel(), "span, grow");
		panel.add(createFooter(), "south");
		return panel;
	}

	private Component createChangesPanel() {
		FileTable table = new FileTable(files);
		table.setFiles(files.changes());
		return table;
	}

	private Component createFooter() {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.add(new JButton(new AbstractAction("Apply") {
			@Override
			public void actionPerformed(ActionEvent e) {
				final Installer installer =
						new Installer(files, new ProgressDialog(null));
				try {
					installer.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					files.write();
				} catch (IOException | SAXException | TransformerConfigurationException e1) {
					e1.printStackTrace();
				}
				dispose();
			}
		}));
		panel.add(new JButton(new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				//reset changes?
				dispose();
			}
		}));
		return panel;
	}
}
