package fiji.plugin.mamut.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_WIDTH;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_X;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_Y;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdom2.DataConversionException;
import org.jdom2.Element;

import viewer.gui.brightness.SetupAssignments;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class MamutXmlReader extends TmXmlReader {

	public MamutXmlReader(final File file) {
		super(file);
	}

	/**
	 * Returns the collection of views that were saved in this file. The views
	 * returned <b>will be rendered</b>.
	 *
	 * @param provider
	 *            the {@link ViewProvider} to instantiate the view. Each saved
	 *            view must be known by the specified provider.
	 * @return the collection of views.
	 * @see TrackMateModelView#render()
	 */
	@Override
	public Collection<TrackMateModelView> getViews(final ViewProvider provider, final Model model, final Settings settings, final SelectionModel selectionModel) {
		final Element guiel = root.getChild(GUI_STATE_ELEMENT_KEY);
		if (null != guiel) {

			final List<Element> children = guiel
					.getChildren(GUI_VIEW_ELEMENT_KEY);
			final Collection<TrackMateModelView> views = new ArrayList<TrackMateModelView>(
					children.size());

			for (final Element child : children) {
				final String viewKey = child
						.getAttributeValue(GUI_VIEW_ATTRIBUTE);
				if (null == viewKey) {
					logger.error("Could not find view key attribute for element "
							+ child + ".\n");
					ok = false;
				} else {
					final TrackMateModelView view = provider.getView(viewKey, model, settings, selectionModel);
					if (null == view) {
						logger.error("Unknown view for key " + viewKey + ".\n");
						ok = false;
					} else {
						views.add(view);

						new Thread("MaMuT view rendering thread") {
							@Override
							public void run() {

								if (viewKey.equals(MamutViewer.KEY)) {
									final MamutViewer mv = (MamutViewer) view;
									// mv.render();

									try {
										final int mvx = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_X)
												.getIntValue();
										final int mvy = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_Y)
												.getIntValue();
										final int mvwidth = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_WIDTH)
												.getIntValue();
										final int mvheight = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT)
												.getIntValue();
										mv.getFrame().setLocation(mvx, mvy);
										mv.getFrame()
												.setSize(mvwidth, mvheight);
									} catch (final DataConversionException e) {
										e.printStackTrace();
									}

								} else if (viewKey.equals(TrackScheme.KEY)) {
									final TrackScheme ts = (TrackScheme) view;
									// ts.render();

									try {
										final int mvx = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_X)
												.getIntValue();
										final int mvy = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_Y)
												.getIntValue();
										final int mvwidth = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_WIDTH)
												.getIntValue();
										final int mvheight = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT)
												.getIntValue();
										ts.getGUI().setLocation(mvx, mvy);
										ts.getGUI().setSize(mvwidth, mvheight);
									} catch (final DataConversionException e) {
										e.printStackTrace();
									}
								}
							};
						}.start();
					}
				}
			}
			return views;

		} else {
			logger.error("Could not find GUI state element.\n");
			ok = false;
			return null;
		}
	}

	public void getSetupAssignments(final SetupAssignments setupAssignments) {
		final Element guiel = root.getChild(GUI_STATE_ELEMENT_KEY);
		if (null != guiel) {
			// brightness & color settings
			if (guiel.getChild("SetupAssignments") != null) {
				setupAssignments.restoreFromXml( guiel );
			} else {
				logger.error("Could not find SetupAssignments element.\n");
			}
		} else {
			logger.error("Could not find GUI state element.\n");
			ok = false;
		}
	}
}
