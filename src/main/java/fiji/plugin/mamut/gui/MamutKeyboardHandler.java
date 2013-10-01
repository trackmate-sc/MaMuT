package fiji.plugin.mamut.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import fiji.FijiTools;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.viewer.MamutViewer;

public class MamutKeyboardHandler {

	private static final Properties DEFAULT_KEYBINGS = new Properties();
	static {
		DEFAULT_KEYBINGS.setProperty("A", "add spot");
		DEFAULT_KEYBINGS.setProperty("ENTER", "add spot");
		DEFAULT_KEYBINGS.setProperty("D", "delete spot");
		DEFAULT_KEYBINGS.setProperty("shift A", "semi-auto tracking");
		DEFAULT_KEYBINGS.setProperty("shift L", "toggle linking mode");
		DEFAULT_KEYBINGS.setProperty("E", "increase spot radius");
		DEFAULT_KEYBINGS.setProperty("Q", "decrease spot radius");
		DEFAULT_KEYBINGS.setProperty("shift E", "increase spot radius a lot");
		DEFAULT_KEYBINGS.setProperty("shift Q", "decrease spot radius a lot");
		DEFAULT_KEYBINGS.setProperty("control E", "increase spot radius a bit");
		DEFAULT_KEYBINGS.setProperty("control Q", "decrease spot radius a bit");
		DEFAULT_KEYBINGS.setProperty("F1", "show help");
		DEFAULT_KEYBINGS.setProperty("S", "toggle brightness dialog");

		DEFAULT_KEYBINGS.setProperty("I", "toggle interpolation");
		DEFAULT_KEYBINGS.setProperty("F", "toggle fused mode");
		DEFAULT_KEYBINGS.setProperty("G", "toggle grouping");

		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		for (int i = 0; i < numkeys.length; ++i) {
			DEFAULT_KEYBINGS.setProperty(numkeys[i], "set current source " + i);
			DEFAULT_KEYBINGS.setProperty("shift " + numkeys[i], "toggle source visibility " + i);
		}

		DEFAULT_KEYBINGS.setProperty("shift Z", "align XY plane");
		DEFAULT_KEYBINGS.setProperty("shift X", "align ZY plane");
		DEFAULT_KEYBINGS.setProperty("shift Y", "align XZ plane");
		DEFAULT_KEYBINGS.setProperty("shift C", "align XZ plane");

		DEFAULT_KEYBINGS.setProperty("CLOSE_BRACKET", "next timepoint");
		DEFAULT_KEYBINGS.setProperty("M", "next timepoint");
		DEFAULT_KEYBINGS.setProperty("OPEN_BRACKET", "previous timepoint");
		DEFAULT_KEYBINGS.setProperty("N", "previous timepoint");
	}
	private final MamutViewer viewer;
	private final MaMuT mamut;

	public MamutKeyboardHandler(final MaMuT mamut, final MamutViewer viewer) {
		this.mamut = mamut;
		this.viewer = viewer;

		installKeyboardActions(viewer.getDisplay());
	}

	protected InputMap readPropertyFile() {
		Properties config = new Properties();
		try {
			final String fijiDir = FijiTools.getFijiDir();
			final InputStream stream = new FileInputStream(new File(fijiDir, "mamut.properties"));
			config.load(stream);
		} catch (final IOException e) {
			System.out.println("MaMuT: cannot find the config file. Using default key bindings.");
			System.out.println(e.getMessage());
			config = DEFAULT_KEYBINGS;
		}

		return generateMapFrom(config);
	}

	private InputMap generateMapFrom(final Properties config) {
		final InputMap map = new InputMap();
		for (final Object obj : config.keySet()) {
			final String key = (String) obj;
			final String command = config.getProperty(key);
			map.put(KeyStroke.getKeyStroke(key), command);
		}
		return map;
	}

	protected void installKeyboardActions(final JComponent graphComponent) {
		// Remove old input map for navigation and put new one.
		viewer.getKeybindings().removeInputMap("navigation");

		final InputMap inputMap = readPropertyFile();
		final ActionMap actionMap = createActionMap();

		viewer.getKeybindings().addActionMap("all", actionMap);
		viewer.getKeybindings().addInputMap("all", inputMap);

	}

	/**
	 * Return the mapping between JTree's input map and MaMuT's actions.
	 */
	protected ActionMap createActionMap() {
		final ActionMap map = new ActionMap();

		map.put("add spot", MamutActions.getAddSpotAction(mamut, viewer));
		map.put("delete spot", MamutActions.getDeleteSpotAction(mamut, viewer));
		map.put("increase spot radius", MamutActions.getIncreaseRadiusAction(mamut, viewer));
		map.put("decrease spot radius", MamutActions.getDecreaseRadiusAction(mamut, viewer));
		map.put("increase spot radius a lot", MamutActions.getIncreaseRadiusALotAction(mamut, viewer));
		map.put("decrease spot radius a lot", MamutActions.getDecreaseRadiusALotAction(mamut, viewer));
		map.put("increase spot radius a bit", MamutActions.getIncreaseRadiusABitAction(mamut, viewer));
		map.put("decrease spot radius a bit", MamutActions.getDecreaseRadiusABitAction(mamut, viewer));

		map.put("semi-auto tracking", MamutActions.getSemiAutoTrackingAction(mamut));

		map.put("toggle linking mode", MamutActions.getToggleLinkingModeAction(mamut, viewer.getLogger()));

		map.put("show help", MamutActions.getShowHelpAction());

		map.put("toggle brightness dialog", MamutActions.getToggleBrightnessDialogAction(mamut));

		return map;
	}

}
