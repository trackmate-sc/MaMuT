package fiji.plugin.mamut.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import bdv.BigDataViewerActions;
import bdv.util.KeyProperties;
import bdv.viewer.NavigationActions;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.viewer.MamutViewer;
import ij.IJ;

public class MamutKeyboardHandler {

	private static final Properties DEFAULT_KEYBINGS = new Properties();
	static {
		DEFAULT_KEYBINGS.setProperty( "A", "add spot" );
		DEFAULT_KEYBINGS.setProperty("ENTER", "add spot");
		DEFAULT_KEYBINGS.setProperty("D", "delete spot");
		DEFAULT_KEYBINGS.setProperty("shift A", "semi-auto tracking");
		DEFAULT_KEYBINGS.setProperty("shift L", "toggle linking mode");
		DEFAULT_KEYBINGS.setProperty( "L", "toggle link" );
		DEFAULT_KEYBINGS.setProperty("E", "increase spot radius");
		DEFAULT_KEYBINGS.setProperty("Q", "decrease spot radius");
		DEFAULT_KEYBINGS.setProperty("shift E", "increase spot radius a lot");
		DEFAULT_KEYBINGS.setProperty("shift Q", "decrease spot radius a lot");
		DEFAULT_KEYBINGS.setProperty("control E", "increase spot radius a bit");
		DEFAULT_KEYBINGS.setProperty("control Q", "decrease spot radius a bit");
		DEFAULT_KEYBINGS.setProperty( "F1", "show help" );
		DEFAULT_KEYBINGS.setProperty("S", "toggle brightness dialog");

		DEFAULT_KEYBINGS.setProperty("I", "toggle interpolation");
		DEFAULT_KEYBINGS.setProperty("F", "toggle fused mode");
		DEFAULT_KEYBINGS.setProperty("G", "toggle grouping");
		DEFAULT_KEYBINGS.setProperty( "T", BigDataViewerActions.MANUAL_TRANSFORM );

		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		for (int i = 0; i < numkeys.length; ++i) {
			DEFAULT_KEYBINGS.setProperty(numkeys[i], "set current source " + i);
			DEFAULT_KEYBINGS.setProperty("shift " + numkeys[i], "toggle source visibility " + i);
		}

		DEFAULT_KEYBINGS.setProperty( "shift Z", "align XY plane" );
		DEFAULT_KEYBINGS.setProperty( "shift X", "align ZY plane" );
		DEFAULT_KEYBINGS.setProperty( "shift Y", "align XZ plane" );
		DEFAULT_KEYBINGS.setProperty( "shift C", "align XZ plane" );

		DEFAULT_KEYBINGS.setProperty( "CLOSE_BRACKET", "step time forward" );
		DEFAULT_KEYBINGS.setProperty("M", "next timepoint");
		DEFAULT_KEYBINGS.setProperty( "OPEN_BRACKET", "step time backward" );
		DEFAULT_KEYBINGS.setProperty("N", "previous timepoint");

		DEFAULT_KEYBINGS.setProperty( "shift CLOSE_BRACKET", "step time forward" );
		DEFAULT_KEYBINGS.setProperty( "shift M", "next timepoint" );
		DEFAULT_KEYBINGS.setProperty( "shift OPEN_BRACKET", "step time backward" );
		DEFAULT_KEYBINGS.setProperty( "shift N", "previous timepoint" );

	}
	private final MamutViewer viewer;
	private final MaMuT mamut;

	public MamutKeyboardHandler(final MaMuT mamut, final MamutViewer viewer) {
		this.mamut = mamut;
		this.viewer = viewer;

		installKeyboardActions( viewer.getViewerPanel() );
	}

	protected InputMap readPropertyFile() {
		Properties config = new Properties();

		final String fijiDir = IJ.getDirectory("imagej");
		File file = new File( fijiDir, "mamut.properties" );

		try {
			if ( !file.exists() )
			{
				// Look in build folder.
				file = new File( MaMuT.class.getResource( "../../../mamut.properties" ).getPath() );
			}
			final InputStream stream = new FileInputStream( file );
			config.load(stream);
		}
		catch ( final Exception e )
		{
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
		NavigationActions.installActionBindings( viewer.getKeybindings(), viewer.getViewerPanel(), KeyProperties.readPropertyFile() );

		final InputMap inputMap = readPropertyFile();
		final ActionMap actionMap = createActionMap();

		viewer.getKeybindings().addActionMap( "all", actionMap );
		viewer.getKeybindings().addInputMap( "all", inputMap );
	}

	/**
	 * Return2 the mapping between JTree's input map and MaMuT's actions.
	 * 
	 * @return the mapping between JTree's input map and MaMuT's actions.
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
		map.put( "toggle link", MamutActions.getToggleLinkAction( mamut, viewer.getLogger() ) );

		map.put( "show help", MamutActions.getShowHelpAction( viewer ) );

		map.put("toggle brightness dialog", MamutActions.getToggleBrightnessDialogAction(mamut));

		map.put( "step time forward", MamutActions.getStepWiseTimeBrowsingAction( mamut, viewer, true ) );
		map.put( "step time backward", MamutActions.getStepWiseTimeBrowsingAction( mamut, viewer, false ) );

		map.put( BigDataViewerActions.MANUAL_TRANSFORM, MamutActions.getToggleManualTransformAction( mamut, viewer ) );
		return map;
	}

}
