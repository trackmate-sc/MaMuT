package fiji.plugin.mamut.gui;

import java.awt.event.KeyEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.viewer.MamutViewer;

public class MamutKeyboardHandler {

	private final MamutViewer viewer;
	private final MaMuT mamut;

	public MamutKeyboardHandler(final MaMuT mamut, final MamutViewer viewer) {
		this.mamut = mamut;
		this.viewer = viewer;
		installKeyboardActions(viewer.getDisplay());
	}

	protected void installKeyboardActions(final JComponent graphComponent) {
		InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		SwingUtilities.replaceUIInputMap(graphComponent, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);

		inputMap = getInputMap(JComponent.WHEN_FOCUSED);
		SwingUtilities.replaceUIInputMap(graphComponent, JComponent.WHEN_FOCUSED, inputMap);
		SwingUtilities.replaceUIActionMap(graphComponent, createActionMap());
	}

	protected InputMap getInputMap(final int condition) {
		final InputMap map = new InputMap();

		map.put(KeyStroke.getKeyStroke("A"), "add spot");
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "add spot");
		map.put(KeyStroke.getKeyStroke("D"), "delete spot");
		map.put(KeyStroke.getKeyStroke("E"), "increase spot radius");
		map.put(KeyStroke.getKeyStroke("Q"), "decrease spot radius");
		map.put(KeyStroke.getKeyStroke("shift E"), "increase spot radius a lot");
		map.put(KeyStroke.getKeyStroke("shift Q"), "decrease spot radius a lot");
		map.put(KeyStroke.getKeyStroke("control E"), "increase spot radius a bit");
		map.put(KeyStroke.getKeyStroke("control Q"), "decrease spot radius a bit");

		map.put(KeyStroke.getKeyStroke("shift A"), "semi-auto tracking");

		map.put(KeyStroke.getKeyStroke("shift L"), "toggle linking mode");

		map.put(KeyStroke.getKeyStroke("F1"), "show help");

		map.put(KeyStroke.getKeyStroke("S"), "toggle brightness dialog");

		return map;
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
