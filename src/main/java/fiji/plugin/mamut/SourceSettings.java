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
package fiji.plugin.mamut;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bdv.BigDataViewer;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.mamut.providers.MamutSpotAnalyzerProvider;
import fiji.plugin.mamut.util.DummySpimData;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.RandomAccessibleInterval;

public class SourceSettings extends Settings
{

	private final List< SourceAndConverter< ? > > sources;

	private final CacheControl cache;

	private final ArrayList< ConverterSetup > converterSetups;

	private final List< SpotAnalyzerFactoryBase< ? > > mamutSpotAnalyzerFactories;

	/**
	 * Loads and prepares the image sources from the specified files.
	 * <p>
	 * This instantiates and sets the following fields:
	 * <ul>
	 * <li>{@link #sources}
	 * <li>{@link #cache}
	 * </ul>
	 */
	public SourceSettings( final String imageFolder, final String imageFileName )
	{
		this.imageFileName = imageFileName;
		this.imageFolder = imageFolder;
		this.sources = new ArrayList<>();
		this.converterSetups = new ArrayList<>();
		this.mamutSpotAnalyzerFactories = new ArrayList<>();


		final File bdvFile = new File( imageFolder, imageFileName );
		AbstractSequenceDescription< ?, ?, ? > seq = null;
		try
		{
			SpimDataMinimal spimData;
			if ( imageFolder == null )
			{
				/*
				 * We could not find an image, we create a dummy BDV data source
				 * instead.
				 */
				spimData = DummySpimData.tryCreate( imageFileName );
			}
			else
			{
				spimData = new XmlIoSpimDataMinimal().load( bdvFile.getAbsolutePath() );
			}

			if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
				System.err.println( "WARNING:\n"
						+ "Opening <SpimData> dataset that is not suited for suited for interactive browsing.\n"
						+ "Consider resaving as HDF5 for better performance." );

			seq = spimData.getSequenceDescription();
			BigDataViewer.initSetups( spimData, converterSetups, sources );
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}
		this.cache = ( ( ViewerImgLoader ) seq.getImgLoader() ).getCacheControl();
		final List< TimePoint > timepoints = seq.getTimePoints().getTimePointsOrdered();
		this.nframes = timepoints.size();

		// Image size
		final SourceAndConverter< ? > firstSource = sources.get( 0 );
		final RandomAccessibleInterval< ? > firstStack = firstSource.getSpimSource().getSource( 0, 0 );
		this.width = ( int ) firstStack.dimension( 0 );
		this.height = ( int ) firstStack.dimension( 1 );
		this.nslices = ( int ) firstStack.dimension( 2 );
		this.dx = 1f;
		this.dy = 1f;
		this.dz = 1f;
		this.dt = 1f;

		// Crop cube
		this.setRoi( null );
	}

	@Override
	public void addAllAnalyzers()
	{
		clearSpotAnalyzerFactories();

		// Analyzers specific to MaMuT.
		final MamutSpotAnalyzerProvider mamutSpotAnalyzerProvider = new MamutSpotAnalyzerProvider( sources.size() );
		final List< String > mamutSpotAnalyzerKeys = mamutSpotAnalyzerProvider.getKeys();
		for ( final String key : mamutSpotAnalyzerKeys )
			addSpotAnalyzerFactory( mamutSpotAnalyzerProvider.getFactory( key ) );

		// TrackMate analyzers.
		final SpotAnalyzerProvider spotAnalyzerProvider = new SpotAnalyzerProvider( sources.size() );
		final List< String > spotAnalyzerKeys = spotAnalyzerProvider.getKeys();
		for ( final String key : spotAnalyzerKeys )
			addSpotAnalyzerFactory( spotAnalyzerProvider.getFactory( key ) );

		clearEdgeAnalyzers();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = new EdgeAnalyzerProvider();
		final List< String > edgeAnalyzerKeys = edgeAnalyzerProvider.getKeys();
		for ( final String key : edgeAnalyzerKeys )
			addEdgeAnalyzer( edgeAnalyzerProvider.getFactory( key ) );

		clearTrackAnalyzers();
		final TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();
		final List< String > trackAnalyzerKeys = trackAnalyzerProvider.getKeys();
		for ( final String key : trackAnalyzerKeys )
			addTrackAnalyzer( trackAnalyzerProvider.getFactory( key ) );
	}

	public List< SourceAndConverter< ? > > getSources()
	{
		return sources;
	}

	public CacheControl getCacheControl()
	{
		return cache;
	}

	public ArrayList< ConverterSetup > getConverterSetups()
	{
		return converterSetups;
	}

	@Override
	public String toStringFeatureAnalyzersInfo()
	{
		final StringBuilder str = new StringBuilder();

		if ( mamutSpotAnalyzerFactories.isEmpty() )
		{
			str.append( "No spot feature analyzers.\n" );
		}
		else
		{
			str.append( "Mamut spot feature analyzers:\n" );
			prettyPrintFeatureAnalyzer( mamutSpotAnalyzerFactories, str );
		}

		if ( edgeAnalyzers.isEmpty() )
		{
			str.append( "No edge feature analyzers.\n" );
		}
		else
		{
			str.append( "Edge feature analyzers:\n" );
			prettyPrintFeatureAnalyzer( edgeAnalyzers, str );
		}

		if ( trackAnalyzers.isEmpty() )
		{
			str.append( "No track feature analyzers.\n" );
		}
		else
		{
			str.append( "Track feature analyzers:\n" );
			prettyPrintFeatureAnalyzer( trackAnalyzers, str );
		}

		return str.toString();
	}
}
