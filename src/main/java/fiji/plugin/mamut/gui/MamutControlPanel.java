package fiji.plugin.mamut.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;

public class MamutControlPanel extends ConfigureViewsPanel {

	private static final long serialVersionUID = 1L;
	private static final ImageIcon MAMUT_ICON = new ImageIcon(MaMuT.class.getResource("mammouth-16x16.png"));
	private static final Icon SAVE_ICON = new ImageIcon(MaMuT.class.getResource("page_save.png"));
	private JButton jButtonMamutViewer;
	private JButton jButtonSaveButton;
	public final ActionEvent MAMUT_VIEWER_BUTTON_PRESSED = new ActionEvent(this, 2, "MamutViewerButtonPushed");
	public final ActionEvent MAMUT_SAVE_BUTTON_PRESSED = new ActionEvent(this, 3, "MamutSaveButtonPushed");

	public MamutControlPanel(Model model) {
		super(model);

		// Move trackscheme button
		Point btp = jButtonShowTrackScheme.getLocation();
		jButtonShowTrackScheme.setLocation(btp.x, btp.y + 50);
		Dimension btd = jButtonShowTrackScheme.getSize();

		// New Mamut viewer button
		jButtonMamutViewer = new JButton("Viewer", MAMUT_ICON);
		jButtonMamutViewer.setFont(SMALL_FONT);
		jButtonMamutViewer.setLocation(btp);
		jButtonMamutViewer.setSize(btd);
		jButtonMamutViewer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				fireAction(MAMUT_VIEWER_BUTTON_PRESSED);
			}
		});
		add(jButtonMamutViewer);

		// Save button
		Point atp = jButtonDoAnalysis.getLocation();
		jButtonSaveButton = new JButton("Save", SAVE_ICON);
		jButtonSaveButton.setFont(SMALL_FONT);
		jButtonSaveButton.setLocation(atp.x, atp.y + 50);
		jButtonSaveButton.setSize(btd);
		jButtonSaveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				fireAction(MAMUT_SAVE_BUTTON_PRESSED);
			}
		});
		add(jButtonSaveButton);
	}
}
