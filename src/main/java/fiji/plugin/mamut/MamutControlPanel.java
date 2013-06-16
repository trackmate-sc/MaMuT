package fiji.plugin.mamut;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;

public class MamutControlPanel extends ConfigureViewsPanel {

	private static final long serialVersionUID = 1L;
	private static final ImageIcon MAMUT_ICON = new ImageIcon(MamutControlPanel.class.getResource("mammouth-16x16.png"));
	private JButton jButtonMamutViewer;
	public final ActionEvent MAMUT_VIEWER_BUTTON_PRESSED = new ActionEvent(this, 2, "MamutViewerButtonPushed");

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
		
		
	}

}
