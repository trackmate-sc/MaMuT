package fiji.plugin.mamut.gui;

import static fiji.plugin.mamut.MaMuT.MAMUT_ICON;
import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import fiji.plugin.trackmate.Model;

public class MamutGUI extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private MamutControlPanel viewPanel;

	public MamutGUI(Model model) {
		setTitle(PLUGIN_NAME + " v" + PLUGIN_VERSION);
		setIconImage(MAMUT_ICON.getImage());
		setSize(320, 560);
		setResizable(false);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		setVisible(true);
		
		viewPanel = new MamutControlPanel(model);
		tabbedPane.addTab("Views", MAMUT_ICON, viewPanel, "The control panel for views");
		
	}
	
	public MamutControlPanel getViewPanel() {
		return viewPanel;
	}
}
