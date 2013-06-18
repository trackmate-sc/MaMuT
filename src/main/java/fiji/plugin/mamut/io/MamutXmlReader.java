package fiji.plugin.mamut.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.*;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdom2.DataConversionException;
import org.jdom2.Element;

import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class MamutXmlReader extends TmXmlReader {

	public MamutXmlReader(File file) {
		super(file);
	}

	/**
	 * Returns the collection of views that were saved in this file. The views returned
	 * <b>will be rendered</b>.
	 * @param provider  the {@link ViewProvider} to instantiate the view. Each saved 
	 * view must be known by the specified provider.
	 * @return the collection of views.
	 * @see TrackMateModelView#render()
	 */
	public Collection<TrackMateModelView> getViews(ViewProvider provider) {
		Element guiel = root.getChild(GUI_STATE_ELEMENT_KEY);
		if (null != guiel) {

			List<Element> children = guiel.getChildren(GUI_VIEW_ELEMENT_KEY);
			Collection<TrackMateModelView> views = new ArrayList<TrackMateModelView>(children.size());

			for (final Element child : children) {
				final String viewKey = child.getAttributeValue(GUI_VIEW_ATTRIBUTE);
				if (null == viewKey) {
					logger.error("Could not find view key attribute for element " + child +".\n");
					ok = false;
				} else {
					final TrackMateModelView view = provider.getView(viewKey);
					if (null == view) {
						logger.error("Unknown view for key " + viewKey +".\n");
						ok = false;
					} else {
						views.add(view);

						new Thread("MaMuT view rendering thread") {
							public void run() {

								if (viewKey.equals(MamutViewer.KEY)) {
									MamutViewer mv = (MamutViewer) view;
//									mv.render();
									
									try {
										int mvx = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_X).getIntValue();
										int mvy = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_Y).getIntValue();
										int mvwidth = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_WIDTH).getIntValue();
										int mvheight = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT).getIntValue();
										mv.getFrame().setLocation(mvx, mvy);
										mv.getFrame().setSize(mvwidth, mvheight);
									} catch (DataConversionException e) {
										e.printStackTrace();
									}

								} else if (viewKey.equals(TrackScheme.KEY)) {
									TrackScheme ts = (TrackScheme) view;
//									ts.render();
									
									try {
										int mvx = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_X).getIntValue();
										int mvy = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_Y).getIntValue();
										int mvwidth = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_WIDTH).getIntValue();
										int mvheight = child.getAttribute(GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT).getIntValue();
										ts.getGUI().setLocation(mvx, mvy);
										ts.getGUI().setSize(mvwidth, mvheight);
									} catch (DataConversionException e) {
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

}
