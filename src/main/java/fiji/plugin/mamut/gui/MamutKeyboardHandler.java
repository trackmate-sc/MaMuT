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
import javax.swing.SwingUtilities;

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
			config = DEFAULT_KEYBINGS;
			e.printStackTrace();
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
		final InputMap inputMap = readPropertyFile();

		SwingUtilities.replaceUIInputMap(graphComponent, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
		SwingUtilities.replaceUIInputMap(graphComponent, JComponent.WHEN_FOCUSED, inputMap);
		SwingUtilities.replaceUIActionMap(graphComponent, createActionMap());
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

	public static void main(final String[] args) {
		new MamutKeyboardHandler(null, null);
	}
}
