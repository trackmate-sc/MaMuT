package fiji.plugin.mamut.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.*;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;

import org.jdom2.Element;

import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class MamutXmlWriter extends TmXmlWriter {

	public MamutXmlWriter(File file) {
		super(file);
	}

	/**
	 * Appends the content of a {@link Settings} object to the document.
	 * 
	 * @param settings
	 *        the {@link Settings} to write. It must be a {@link SourceSettings}
	 *        instance, otherwise an exception is thrown.
	 * @param detectorProvider
	 *        the {@link DetectorProvider}, required to marshall the selected
	 *        detector and its settings. If <code>null</code>, they won't be
	 *        appended.
	 * @param trackerProvider
	 *        the {@link TrackerProvider}, required to marshall the selected
	 *        tracker and its settings. If <code>null</code>, they won't be
	 *        appended.
	 */
	@Override
	public void appendSettings(Settings settings, DetectorProvider detectorProvider, TrackerProvider trackerProvider) {

		if (!(settings instanceof SourceSettings)) {
			throw new IllegalArgumentException("The settings must be a SourceSettings instance.");
		}

		SourceSettings ss = (SourceSettings) settings;

		Element settingsElement = new Element(SETTINGS_ELEMENT_KEY);

		Element imageInfoElement = echoImageInfo(ss);
		settingsElement.addContent(imageInfoElement);

		if (null != detectorProvider) {
			Element detectorElement = echoDetectorSettings(settings, detectorProvider);
			settingsElement.addContent(detectorElement);
		}

		Element initFilter = echoInitialSpotFilter(settings);
		settingsElement.addContent(initFilter);

		Element spotFiltersElement = echoSpotFilters(settings);
		settingsElement.addContent(spotFiltersElement);

		if (null != trackerProvider) {
			Element trackerElement = echoTrackerSettings(settings, trackerProvider);
			settingsElement.addContent(trackerElement);
		}

		Element trackFiltersElement = echoTrackFilters(settings);
		settingsElement.addContent(trackFiltersElement);

		Element analyzersElement = echoAnalyzers(settings);
		settingsElement.addContent(analyzersElement);

		root.addContent(settingsElement);
	}

	public void appendMamutState(TrackMateGUIModel guimodel) {
		Element guiel = new Element(GUI_STATE_ELEMENT_KEY);
		// views
		for (TrackMateModelView view : guimodel.getViews()) {
			Element viewel = new Element(GUI_VIEW_ELEMENT_KEY);
			viewel.setAttribute(GUI_VIEW_ATTRIBUTE, view.getKey());
			guiel.addContent(viewel);

			if (view.getKey().equals(MamutViewer.KEY)) {
				MamutViewer mv = (MamutViewer) view;
				Point location = mv.getFrame().getLocation();
				Dimension size = mv.getFrame().getSize();
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_X, "" + location.x);
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_Y, "" + location.y);
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_WIDTH, "" + size.width);
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT, "" + size.height);

			} else if (view.getKey().equals(TrackScheme.KEY)) {
				TrackScheme ts = (TrackScheme) view;
				Point location = ts.getGUI().getLocation();
				Dimension size = ts.getGUI().getSize();
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_X, "" + location.x);
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_Y, "" + location.y);
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_WIDTH, "" + size.width);
				viewel.setAttribute(GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT, "" + size.height);
			}
		}

		root.addContent(guiel);
		logger.log("  Added GUI current state.\n");
	}

}
