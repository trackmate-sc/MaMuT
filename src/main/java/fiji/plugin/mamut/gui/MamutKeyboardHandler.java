package fiji.plugin.mamut.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
		InputMap map = null;

		if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
			map = (InputMap) UIManager.get("ScrollPane.ancestorInputMap");
		} else if (condition == JComponent.WHEN_FOCUSED) {
			map = new InputMap();
		}

		map.put(KeyStroke.getKeyStroke("A"), "add spot");
		map.put(KeyStroke.getKeyStroke("shift A"), "semi-auto tracking");

		return map;
	}

	/**
	 * Return the mapping between JTree's input map and MaMuT's actions.
	 */
	protected ActionMap createActionMap() {
		final ActionMap map = (ActionMap) UIManager.get("ScrollPane.actionMap");

		map.put("add spot", new AbstractAction("add spot") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				//				mamut.addSpot(viewer);
				System.out.println("Hey guys!");// DEBUG
			}
		});

		return map;
	}
}
