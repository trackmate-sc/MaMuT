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

	public static final ImageIcon THREEDVIEWER_ICON = new ImageIcon( MaMuT.class.getResource( "sport_8ball.png" ) );
	private final JButton jButtonMamutViewer;
	private final JButton jButtonSaveButton;
	public final ActionEvent MAMUT_VIEWER_BUTTON_PRESSED = new ActionEvent(this, 2, "MamutViewerButtonPushed");
	public final ActionEvent MAMUT_SAVE_BUTTON_PRESSED = new ActionEvent(this, 3, "MamutSaveButtonPushed");

	public MamutControlPanel(final Model model) {
		super(model);

		// Move trackscheme button
		final Point btp = jButtonShowTrackScheme.getLocation();
		jButtonShowTrackScheme.setLocation(btp.x, btp.y + 50);
		final Dimension btd = jButtonShowTrackScheme.getSize();

		// Hijack the do analysis bytton
		jButtonDoAnalysis.setText( "3D Viewer" );
		jButtonDoAnalysis.setIcon( THREEDVIEWER_ICON );
		jButtonDoAnalysis.setToolTipText( "Launch a 3D view of the annotation data only." );

		// New Mamut viewer button
		jButtonMamutViewer = new JButton( "Mamut Viewer", MAMUT_ICON );
		jButtonMamutViewer.setFont(SMALL_FONT);
		jButtonMamutViewer.setLocation(btp);
		jButtonMamutViewer.setSize(btd);
		jButtonMamutViewer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				fireAction(MAMUT_VIEWER_BUTTON_PRESSED);
			}
		});
		add(jButtonMamutViewer);

		// Save button
		final Point atp = jButtonDoAnalysis.getLocation();
		jButtonSaveButton = new JButton("Save", SAVE_ICON);
		jButtonSaveButton.setFont(SMALL_FONT);
		jButtonSaveButton.setLocation(atp.x, atp.y + 50);
		jButtonSaveButton.setSize(btd);
		jButtonSaveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				fireAction(MAMUT_SAVE_BUTTON_PRESSED);
			}
		});
		add(jButtonSaveButton);
	}
}
