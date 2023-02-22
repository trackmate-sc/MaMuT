/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2023 MaMuT development team.
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

import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntAttribute;
import static fiji.plugin.trackmate.io.TmXmlKeys.ANALYSER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.ANALYSER_KEY_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.ANALYZER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.EDGE_ANALYSERS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_WIDTH;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_X;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_Y;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_FILENAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_FOLDER_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_NFRAMES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_NSLICES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ANALYSERS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_ANALYSERS_ELEMENT_KEY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdom2.DataConversionException;
import org.jdom2.Element;

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.brightness.SetupAssignments;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.providers.MamutSpotAnalyzerProvider;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImagePlus;

@SuppressWarnings( "deprecation" )
public class MamutXmlReader extends TmXmlReader
{

	public MamutXmlReader( final File file )
	{
		super( file );
	}

	@Override
	public Settings readSettings( final ImagePlus imp )
	{
		throw new UnsupportedOperationException( "MaMuT cannot load a XML file that requires specifying an ImagePlus." );
	}

	@Override
	public Settings readSettings(
			final ImagePlus imp,
			final DetectorProvider detectorProvider,
			final TrackerProvider trackerProvider,
			final SpotAnalyzerProvider spotAnalyzerProvider,
			final EdgeAnalyzerProvider edgeAnalyzerProvider,
			final TrackAnalyzerProvider trackAnalyzerProvider,
			final SpotMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider )
	{
		throw new UnsupportedOperationException( "MaMuT cannot load a XML file that requires specifying an ImagePlus." );
	}

	@Override
	public ImagePlus readImage()
	{
		throw new UnsupportedOperationException( "MaMuT cannot load a XML file that requires specifying an ImagePlus." );
	}

	public SourceSettings readSourceSettings()
	{
		final Element settingsElement = root.getChild( SETTINGS_ELEMENT_KEY );
		if ( null == settingsElement )
			return null;

		/*
		 * Read image path.
		 */

		final Element imageInfoElement = settingsElement.getChild( IMAGE_ELEMENT_KEY );
		String filename = imageInfoElement.getAttributeValue( IMAGE_FILENAME_ATTRIBUTE_NAME );
		String folder = imageInfoElement.getAttributeValue( IMAGE_FOLDER_ATTRIBUTE_NAME );
		if ( null == filename || filename.isEmpty() )
		{
			logger.error( "Cannot find image file name in xml file.\n" );
			ok = false;
			return null;
		}
		if ( null == folder || folder.isEmpty() )
			folder = file.getParent(); // it is a relative path, then

		File imageFile = new File( folder, filename );
		if ( !imageFile.exists() || !imageFile.canRead() )
		{
			/*
			 * Could not find it to the absolute path. Then we look for the same
			 * path of the xml file
			 */
			folder = file.getParent();
			imageFile = new File( folder, filename );
			if ( !imageFile.exists() || !imageFile.canRead() )
			{
				logger.error( "Cannot read image file: " + imageFile + ". Using an empty placeholder.\n" );
				ok = false;

				final double scalex = readDoubleAttribute( imageInfoElement, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, logger );
				final double scaley = readDoubleAttribute( imageInfoElement, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, logger );
				final double scalez = readDoubleAttribute( imageInfoElement, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, logger );
				final int sizex = readIntAttribute( imageInfoElement, IMAGE_WIDTH_ATTRIBUTE_NAME, logger, 512 );
				final int sizey = readIntAttribute( imageInfoElement, IMAGE_HEIGHT_ATTRIBUTE_NAME, logger, 512 );
				final int sizez = readIntAttribute( imageInfoElement, IMAGE_NSLICES_ATTRIBUTE_NAME, logger, 1 );
				final int ntimepoints = readIntAttribute( imageInfoElement, IMAGE_NFRAMES_ATTRIBUTE_NAME, logger, 1 );;
				filename = String.format( "x=%d y=%d z=%d sx=%f sy=%f sz=%f t=%d.dummy",
						sizex, sizey, sizez,
						scalex, scaley, scalez,
						ntimepoints );
				// This will signal we need to create a dummy BDV.
				folder = null;
			}
		}

		final SourceSettings settings = new SourceSettings( folder, filename );

		// Detector
		getDetectorSettings( settingsElement, settings, new DetectorProvider() );

		// Tracker
		getTrackerSettings( settingsElement, settings, new TrackerProvider() );

		// Spot Filters
		final FeatureFilter initialFilter = getInitialFilter( settingsElement );
		if ( null != initialFilter )
			settings.initialSpotFilterValue = initialFilter.value;

		final List< FeatureFilter > spotFilters = getSpotFeatureFilters( settingsElement );
		settings.setSpotFilters( spotFilters );

		// Track Filters
		final List< FeatureFilter > trackFilters = getTrackFeatureFilters( settingsElement );
		settings.setTrackFilters( trackFilters );

		// Features analyzers

		final Element analyzersEl = settingsElement.getChild( ANALYZER_COLLECTION_ELEMENT_KEY );
		if ( null == analyzersEl )
		{
			logger.error( "Could not find the feature analyzer element.\n" );
			ok = false;
		}
		else
		{

			// Spot analyzers
			final Element spotAnalyzerEl = analyzersEl.getChild( SPOT_ANALYSERS_ELEMENT_KEY );
			if ( null == spotAnalyzerEl )
			{
				logger.error( "Could not find the spot analyzer element.\n" );
				ok = false;

			}
			else
			{

				final List< Element > children = spotAnalyzerEl.getChildren( ANALYSER_ELEMENT_KEY );
				final int nSetups = settings.getSources().size();
				final SpotAnalyzerProvider spotAnalyzerProvider = new SpotAnalyzerProvider( nSetups );
				final SpotMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider = new SpotMorphologyAnalyzerProvider( nSetups );
				final MamutSpotAnalyzerProvider mamutSpotAnalyzerProvider = new MamutSpotAnalyzerProvider( nSetups );
				for ( final Element child : children )
				{

					final String key = child.getAttributeValue( ANALYSER_KEY_ATTRIBUTE );
					if ( null == key )
					{
						logger.error( "Could not find analyzer name for element " + child + ".\n" );
						ok = false;
						continue;
					}

					SpotAnalyzerFactoryBase< ? > spotAnalyzer = spotAnalyzerProvider.getFactory( key );
					if ( null == spotAnalyzer )
					{
						/*
						 * Special case: if we cannot find a matching analyzer
						 * for a declared factory, then we will try to see
						 * whether it is a morphology spot analyzer, that are
						 * treated separately.
						 */
						spotAnalyzer = spotMorphologyAnalyzerProvider.getFactory( key );
					}
					if ( null == spotAnalyzer )
					{
						/*
						 * SECOND special case: now we will try if it is a mamut
						 * analyzer, that is also treated separately.
						 */
						spotAnalyzer = mamutSpotAnalyzerProvider.getFactory( key );
					}

					if ( null == spotAnalyzer )
					{
						// We finally give up.
						logger.error( "Unknown spot analyzer key: " + key + ".\n" );
						ok = false;
					}
					else
					{
						settings.addSpotAnalyzerFactory( spotAnalyzer );
					}
				}
			}

			// Edge analyzers
			final Element edgeAnalyzerEl = analyzersEl.getChild( EDGE_ANALYSERS_ELEMENT_KEY );
			if ( null == edgeAnalyzerEl )
			{
				logger.error( "Could not find the edge analyzer element.\n" );
				ok = false;

			}
			else
			{

				final List< Element > children = edgeAnalyzerEl.getChildren( ANALYSER_ELEMENT_KEY );
				final EdgeAnalyzerProvider edgeAnalyzerProvider = new EdgeAnalyzerProvider();
				for ( final Element child : children )
				{

					final String key = child.getAttributeValue( ANALYSER_KEY_ATTRIBUTE );
					if ( null == key )
					{
						logger.error( "Could not find analyzer name for element " + child + ".\n" );
						ok = false;
						continue;
					}

					final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getFactory( key );
					if ( null == edgeAnalyzer )
					{
						logger.error( "Unknown edge analyzer key: " + key + ".\n" );
						ok = false;
					}
					else
					{
						settings.addEdgeAnalyzer( edgeAnalyzer );
					}
				}
			}

			// Track analyzers
			final Element trackAnalyzerEl = analyzersEl.getChild( TRACK_ANALYSERS_ELEMENT_KEY );
			if ( null == trackAnalyzerEl )
			{
				logger.error( "Could not find the track analyzer element.\n" );
				ok = false;

			}
			else
			{

				final List< Element > children = trackAnalyzerEl.getChildren( ANALYSER_ELEMENT_KEY );
				final TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();
				for ( final Element child : children )
				{

					final String key = child.getAttributeValue( ANALYSER_KEY_ATTRIBUTE );
					if ( null == key )
					{
						logger.error( "Could not find analyzer name for element " + child + ".\n" );
						ok = false;
						continue;
					}

					final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getFactory( key );
					if ( null == trackAnalyzer )
					{
						logger.error( "Unknown track analyzer key: " + key + ".\n" );
						ok = false;
					}
					else
					{
						settings.addTrackAnalyzer( trackAnalyzer );
					}
				}
			}
		}

		return settings;
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
	public Collection< TrackMateModelView > getViews(
			final MaMuT mamut,
			final ViewProvider provider,
			final Model model,
			final Settings settings,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings )
	{
		final Element guiel = root.getChild( GUI_STATE_ELEMENT_KEY );
		if ( null != guiel )
		{
			final List< Element > children = guiel.getChildren( GUI_VIEW_ELEMENT_KEY );
			final Collection< TrackMateModelView > views = new ArrayList<>( children.size() );

			for ( final Element child : children )
			{
				final String viewKey = child.getAttributeValue( GUI_VIEW_ATTRIBUTE );
				if ( null == viewKey )
				{
					logger.error( "Could not find view key attribute for element "
							+ child + ".\n" );
					ok = false;
				}
				else
				{
					if ( viewKey.equals( MamutViewer.KEY ) )
					{
						final MamutViewer mv = mamut.newViewer();
						try
						{
							final int mvx = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_X ).getIntValue();
							final int mvy = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_Y ).getIntValue();
							final int mvwidth = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_WIDTH ).getIntValue();
							final int mvheight = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT ).getIntValue();
							mv.setLocation( mvx, mvy );
							mv.setSize( mvwidth, mvheight );
						}
						catch ( final DataConversionException e )
						{
							e.printStackTrace();
						}
						continue;
					}

					final ViewFactory viewFactory = provider.getFactory( viewKey );
					if ( null == viewFactory )
					{
						logger.error( "Unknown view for key " + viewKey + ".\n" );
						ok = false;
					}
					else
					{

						final TrackMateModelView view = viewFactory.create( model, settings, selectionModel, displaySettings );
						views.add( view );

						new Thread( "MaMuT view rendering thread" )
						{
							@Override
							public void run()
							{
								if ( viewKey.equals( "TRACKSCHEME" ) )
								{
									final TrackScheme ts = ( TrackScheme ) view;
									// ts.render();

									try
									{
										final int mvx = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_X ).getIntValue();
										final int mvy = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_Y ).getIntValue();
										final int mvwidth = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_WIDTH ).getIntValue();
										final int mvheight = child.getAttribute( GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT ).getIntValue();
										ts.getGUI().setLocation( mvx, mvy );
										ts.getGUI().setSize( mvwidth, mvheight );
									}
									catch ( final DataConversionException e )
									{
										e.printStackTrace();
									}
								}
							}
						}.start();
					}
				}
			}
			return views;

		}

		logger.error( "Could not find GUI state element.\n" );
		ok = false;
		return null;
	}

	public void readSetupAssignments( final SetupAssignments setupAssignments )
	{
		final Element guiel = root.getChild( GUI_STATE_ELEMENT_KEY );
		if ( null != guiel )
		{
			try
			{
				// brightness & color settings
				if ( guiel.getChild( "SetupAssignments" ) != null )
				{
					setupAssignments.restoreFromXml( guiel );
				}
				else
				{
					logger.error( "Could not find SetupAssignments element.\n" );
					ok = false;
				}
			}
			catch ( final IllegalArgumentException e )
			{
				logger.error( "Saved SetupAssignments do not match current image.\n" );
				ok = false;
			}
		}
		else
		{
			logger.error( "Could not find GUI state element.\n" );
			ok = false;
		}
	}

	public void readBookmarks( final Bookmarks bookmarks )
	{
		final Element guiel = root.getChild( GUI_STATE_ELEMENT_KEY );
		if ( null != guiel )
		{
			bookmarks.restoreFromXml( guiel );
		}
		else
		{
			logger.error( "Could not find GUI state element.\n" );
			ok = false;
		}
	}
}
