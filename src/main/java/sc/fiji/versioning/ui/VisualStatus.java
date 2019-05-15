package sc.fiji.versioning.ui;

import javax.swing.*;
import java.awt.*;

public class VisualStatus extends JPanel {

	enum StatusType {
		IDLE, RUNNING, DONE, FAIL, WARNING;
	}

	private final JLabel label, title;

	public VisualStatus() {
		super();
		label = new JLabel("\u2013", SwingConstants.CENTER);
		final Font font = label.getFont();
		label.setFont(new Font(font.getName(), Font.BOLD, font.getSize() *
				2));
		label.setPreferredSize(new Dimension(50, 30));
		label.setMinimumSize(new Dimension(50, 30));
		label.setMaximumSize(new Dimension(50, 30));
		title = new JLabel();
		add(label);
		add(title);
	}

	public void setStatus(final StatusType status, String name) {
		title.setText(name);
		switch (status) {
			case IDLE:
				label.setText("\u2013");
				label.setForeground(Color.getHSBColor(0.6f, 0.f, 0.3f));
				break;
			case RUNNING:
				label.setText("\u2794");
				label.setForeground(Color.getHSBColor(0.6f, 0.f, 0.3f));
				break;
			case DONE:
				label.setText("\u2713");
				label.setForeground(Color.getHSBColor(0.3f, 1, 0.6f));
				break;
			case FAIL:
				label.setText("\u2013");
				label.setForeground(Color.red);
				break;
			case WARNING:
				label.setText("\u2013");
				label.setForeground(Color.orange);
				break;
		}
		revalidate();
	}

}
