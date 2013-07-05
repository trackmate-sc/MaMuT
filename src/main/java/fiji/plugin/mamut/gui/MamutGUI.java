package fiji.plugin.mamut.gui;

import static fiji.plugin.mamut.MaMuT.MAMUT_ICON;
import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class MamutGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private final static ImageIcon ANNOTATION_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/TrackIcon_small.png"));
	private MamutControlPanel viewPanel;
	private AnnotationPanel annotationPanel;

	public MamutGUI(Model model, MamutGUIModel guiModel) {
		setTitle(PLUGIN_NAME + " v" + PLUGIN_VERSION);
		setIconImage(MAMUT_ICON.getImage());
		setSize(320, 560);
		setResizable(false);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		setVisible(true);

		viewPanel = new MamutControlPanel(model);
		tabbedPane.addTab("Views", MAMUT_ICON, viewPanel, "The control panel for views");

		annotationPanel = new AnnotationPanel(guiModel);
		tabbedPane.addTab("Annotation", ANNOTATION_ICON, annotationPanel, "Annotation tools");

	}

	public MamutControlPanel getViewPanel() {
		return viewPanel;
	}

	public AnnotationPanel getAnnotationPanel() {
		return annotationPanel;
	}
}
