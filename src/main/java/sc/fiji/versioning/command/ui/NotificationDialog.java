package sc.fiji.versioning.command.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class NotificationDialog extends JDialog {

	private JButton closeBtn;
	private List<ActionListener> listeners = new ArrayList<>();

	public NotificationDialog(String title, Action action) {
		setUndecorated(true);
		setSize(180, 60);
		setTitle(title);
		setContentPane(createContent(title));
		pack();

		NotificationDialog dialog = this;

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				//check if dialog got disposed, if not, call action
				if(isActive()) {
					listeners.forEach(actionListener -> actionListener.actionPerformed(new DialogActionExecutedEvent(dialog)));
					action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				super.mouseEntered(e);
				displayCloseBtn(true);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				super.mouseMoved(e);
				displayCloseBtn(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				super.mouseExited(e);
				displayCloseBtn(true);
			}
		});
		displayCloseBtn(false);
		setVisible(true);
	}

	private Container createContent(String title) {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		panel.setLayout(new MigLayout("fillx"));
		closeBtn = new JButton(new AbstractAction(" x ") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		closeBtn.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		closeBtn.setBackground(null);
		closeBtn.setOpaque(false);
		panel.add(closeBtn, "alignx right, wrap");
		panel.add(new JLabel(title), "alignx right");
		return panel;
	}

	public void displayCloseBtn(boolean visible) {
		closeBtn.setVisible(visible);
	}

	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}

	public void removeActionListener(ActionListener listener) {
		if(listeners.contains(listener))
			listeners.remove(listener);
	}
}
