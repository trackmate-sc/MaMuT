package fiji.plugin.mamut.gui;

import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class MamutGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private final static ImageIcon ANNOTATION_ICON = new ImageIcon(
			TrackMateWizard.class.getResource("images/TrackIcon_small.png"));
	private static final ImageIcon MAMUT_ICON = new ImageIcon(
			MaMuT.class.getResource("mammouth-32x32.png"));

	private final MamutControlPanel viewPanel;
	private final AnnotationPanel annotationPanel;

	public MamutGUI(final Model model, final MamutGUIModel guiModel) {
		setTitle(PLUGIN_NAME + " v" + PLUGIN_VERSION);
		setIconImage(MAMUT_ICON.getImage());
		setSize(320, 576);
		setResizable(false);

		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		setVisible(true);

		viewPanel = new MamutControlPanel(model);
		tabbedPane.addTab("Views", MAMUT_ICON, viewPanel,
				"The control panel for views");

		annotationPanel = new AnnotationPanel(guiModel);
		tabbedPane.addTab("Annotation", ANNOTATION_ICON, annotationPanel,
				"Annotation tools");

	}

	public MamutControlPanel getViewPanel() {
		return viewPanel;
	}

	public AnnotationPanel getAnnotationPanel() {
		return annotationPanel;
	}
}
