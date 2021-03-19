/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2021 MaMuT development team.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.mamut.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_WIDTH;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_X;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_Y;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_ELEMENT_KEY;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;

import org.jdom2.Element;

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.brightness.SetupAssignments;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.mamut.viewer.MamutViewerFactory;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class MamutXmlWriter extends TmXmlWriter
{

	public MamutXmlWriter( final File file, final Logger logger )
	{
		super( file, logger );
	}

	/**
	 * Appends the content of a {@link Settings} object to the document.
	 *
	 * @param settings
	 *            the {@link Settings} to write. It must be a
	 *            {@link SourceSettings} instance, otherwise an exception is
	 *            thrown.
	 */
	@Override
	public void appendSettings( final Settings settings )
	{

		if ( !( settings instanceof SourceSettings ) ) { throw new IllegalArgumentException( "The settings must be a SourceSettings instance." ); }

		final SourceSettings ss = ( SourceSettings ) settings;

		final Element settingsElement = new Element( SETTINGS_ELEMENT_KEY );

		final Element imageInfoElement = echoImageInfo( ss );
		settingsElement.addContent( imageInfoElement );

		if ( settings.detectorFactory != null )
		{
			final Element detectorElement = echoDetectorSettings( settings );
			settingsElement.addContent( detectorElement );
		}

		final Element initFilter = echoInitialSpotFilter( settings );
		settingsElement.addContent( initFilter );

		final Element spotFiltersElement = echoSpotFilters( settings );
		settingsElement.addContent( spotFiltersElement );

		if ( settings.trackerFactory != null )
		{
			final Element trackerElement = echoTrackerSettings( settings );
			settingsElement.addContent( trackerElement );
		}

		final Element trackFiltersElement = echoTrackFilters( settings );
		settingsElement.addContent( trackFiltersElement );

		final Element analyzersElement = echoAnalyzers( settings );
		settingsElement.addContent( analyzersElement );

		root.addContent( settingsElement );
	}

	public void appendMamutState( final TrackMateGUIModel guimodel,
			final SetupAssignments setupAssignments, final Bookmarks bookmarks )
	{
		final Element guiel = new Element( GUI_STATE_ELEMENT_KEY );
		// views
		for ( final TrackMateModelView view : guimodel.getViews() )
		{
			final Element viewel = new Element( GUI_VIEW_ELEMENT_KEY );
			viewel.setAttribute( GUI_VIEW_ATTRIBUTE, view.getKey() );
			guiel.addContent( viewel );

			if ( view.getKey().equals( MamutViewerFactory.KEY ) )
			{
				final MamutViewer mv = ( MamutViewer ) view;
				final Point location = mv.getLocation();
				final Dimension size = mv.getSize();
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_X, "" + location.x );
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_Y, "" + location.y );
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_WIDTH, "" + size.width );
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT, "" + size.height );

			}
			else if ( view.getKey().equals( "TRACKSCHEME" ) )
			{
				final TrackScheme ts = ( TrackScheme ) view;
				final Point location = ts.getGUI().getLocation();
				final Dimension size = ts.getGUI().getSize();
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_X, "" + location.x );
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_Y, "" + location.y );
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_WIDTH, "" + size.width );
				viewel.setAttribute( GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT, "" + size.height );
			}
		}

		// BDV & MaMuT viewer context

		if ( null != setupAssignments )
			guiel.addContent( setupAssignments.toXml() );

		if ( null != bookmarks )
			guiel.addContent( bookmarks.toXml() );

		root.addContent( guiel );
		logger.log( "  Added GUI current state.\n" );
	}
}
