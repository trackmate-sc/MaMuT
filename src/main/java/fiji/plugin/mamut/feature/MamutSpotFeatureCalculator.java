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
package fiji.plugin.mamut.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.ImgView;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * A class dedicated to centralizing the calculation of the numerical features
 * of spots, through {@link SpotAnalyzer}s, tuned for Mamut.
 * 
 * @author Jean-Yves Tinevez - 2020
 * 
 */
public class MamutSpotFeatureCalculator
{

	private final SourceSettings settings;

	private final ExecutorService executor;

	public MamutSpotFeatureCalculator( final SourceSettings settings )
	{
		this.settings = settings;
		this.executor = Executors.newCachedThreadPool();
	}

	/**
	 * Update the specified spot feature values.
	 * <p>
	 * Computation is done in another thread and this method returns
	 * immediately.
	 */
	public void updateSpotFeatures( final Iterable< Spot > toCompute )
	{
		executor.execute( () -> computeSpotFeatures( toCompute ) );
	}

	/**
	 * Update the specified spot feature values.
	 * <p>
	 * Computation is done in this thread, which blocks.
	 */
	public void computeSpotFeatures( final Iterable< Spot > toCompute )
	{
		// We always operate on the lowest resolution level.
		final int level = 0;
		final List< SpotAnalyzerFactoryBase< ? > > saf = settings.getSpotAnalyzerFactories();
		final List< SourceAndConverter< ? > > sources = settings.getSources();

		// Sort spots by frames.
		final SpotCollection sc = SpotCollection.fromCollection( toCompute );

		final NavigableSet< Integer > frames = sc.keySet();
		for ( final Integer iframe : frames )
		{
			final int frame = iframe.intValue();

			// Loop over each setup in this frame.
			for ( int c = 0; c < sources.size(); c++ )
			{
				final int channel = c;

				// The image for this setup, this frame (so 3D at max).
				final SourceAndConverter< ? > sourceAnConverter = sources.get( c );
				final Source< ? > source = sourceAnConverter.getSpimSource();

				// The transform for this setup, this frame.
				final AffineTransform3D sourceToGlobal = new AffineTransform3D();
				source.getSourceTransform( frame, level, sourceToGlobal );

				// The image.
				@SuppressWarnings( "rawtypes" )
				final RandomAccessibleInterval rai = source.getSource( frame, level );
				final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
				final double scaleX = Affine3DHelpers.extractScale( sourceToGlobal, 0 ) ;
				final double scaleY = Affine3DHelpers.extractScale( sourceToGlobal, 1 ) ;
				final double scaleZ = Affine3DHelpers.extractScale( sourceToGlobal, 2 ) ;
				final double[] cal = new double[] {
						1.,
						scaleY / scaleX,
						scaleZ / scaleX
				};
				final String[] units = new String[] { "globalpix", "globalpix", "globalpix" };
				@SuppressWarnings( "unchecked" )
				final ImgPlus< ? > imgPlus = new ImgPlus<>(
						ImgView.wrap( rai ),
						source.getName() + "C" + c + "_T" + frame,
						axes,
						cal,
						units );

				final List< Future< ? > > tasks = new ArrayList<>( sc.getNSpots( frame, false ) );
				for ( final Spot spot : sc.iterable( frame, false ) )
				{
					// Transform spot coordinates.
					final TransformedSpot transformedSpot = TransformedSpot.wrap( spot, sourceToGlobal, cal );

					// Execute update.
					final Future< ? > task = executor.submit( () -> {
						try
						{
							update( transformedSpot, saf, imgPlus, channel );
						}
						catch ( final Exception e )
						{
							e.printStackTrace();
						}
					} );
					tasks.add( task );
				}
				// Force computation before we move to next setup.
				try
				{
					for ( final Future< ? > task : tasks )
						task.get();
				}
				catch ( InterruptedException | ExecutionException e )
				{
					e.printStackTrace();
				}
			}
		}

	}

	private static final void update( final Spot spot, final List< SpotAnalyzerFactoryBase< ? > > analyzerFactories, final ImgPlus< ? > img, final int channel )
	{
		/*
		 * We expect to receive a single time-point image, so we point the
		 * analyzers to its only frame.
		 */
		final int frame = 0;

		// Process all analyzers.
		for ( @SuppressWarnings( "rawtypes" )
		final SpotAnalyzerFactoryBase factory : analyzerFactories )
		{
			@SuppressWarnings( "unchecked" )
			final SpotAnalyzer< ? > analyzer = factory.getAnalyzer( img, frame, channel );

			// We do multithread somewhere else.
			if ( analyzer instanceof MultiThreaded )
				( ( MultiThreaded ) analyzer ).setNumThreads( 1 );

			analyzer.process( Collections.singletonList( spot ) );
		}
	}
}
