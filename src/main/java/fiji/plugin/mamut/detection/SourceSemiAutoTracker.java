/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2022 MaMuT development team.
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
package fiji.plugin.mamut.detection;

import java.util.List;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.semiauto.AbstractSemiAutoTracker;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * A class made to perform semi-automated tracking of spots in MaMuT.
 * <p>
 * The user has to select one spot, one a meaningful location of a source. The
 * spot location and its radius are then used to extract a small rectangular
 * neighborhood in the next frame around the spot. The neighborhood is then
 * passed to a {@link fiji.plugin.trackmate.detection.SpotDetector} that returns
 * the spot it found. If a spot of {@link Spot#QUALITY} high enough is found
 * near enough to the first spot center, then it is added to the model and
 * linked with the first spot.
 * <p>
 * The process is then repeated, taking the newly found spot as a source for the
 * next neighborhood. The model is updated live for every spot found.
 * <p>
 * The process halts when:
 * <ul>
 * <li>no spots of quality high enough are found;
 * <li>spots of high quality are found, but too far from the initial spot;
 * <li>the source has no time-point left.
 * </ul>
 *
 * @param <T>
 *            the type of the source. Must extend {@link RealType} and
 *            {@link NativeType} to use with most TrackMate
 *            {@link fiji.plugin.trackmate.detection.SpotDetector}s.
 *
 * @author Jean-Yves Tinevez - 2013
 */
public class SourceSemiAutoTracker< T extends RealType< T > & NativeType< T >> extends AbstractSemiAutoTracker< T >
{

	/** The minimal diameter size, in pixel, under which we stop down-sampling. */
	private static final double MIN_SPOT_PIXEL_SIZE = 5d;

	private final List< SourceAndConverter< T >> sources;

	/*
	 * CONSTRUCTOR
	 */

	public SourceSemiAutoTracker( final Model model, final SelectionModel selectionModel, final List< SourceAndConverter< T >> sources, final Logger logger )
	{
		super( model, selectionModel, logger );
		this.sources = sources;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( !super.checkInput() ) { return false; }
		if ( sources == null )
		{
			errorMessage = BASE_ERROR_MESSAGE + "source is null.";
			return false;
		}
		return true;
	}

	@Override
	protected SearchRegion< T > getNeighborhood( final Spot spot, final int frame )
	{

		final double radius = spot.getFeature( Spot.RADIUS );

		/*
		 * Source, rai and transform
		 */

		final Double so = spot.getFeature( SpotSourceIdAnalyzerFactory.SOURCE_ID );
		if ( null == so )
		{
			logger.log( "Spot: " + spot + ": The source index of given spot is not set." );
			return null;
		}
		final int sourceIndex = so.intValue();
		final Source< T > source = sources.get( sourceIndex ).getSpimSource();

		if ( !source.isPresent( frame ) )
		{
			logger.log( "Spot: " + spot + ": Target source has exhausted its time points." );
			return null;
		}

		/*
		 * Determine optimal level to operate on. We want to exploit the
		 * possible multi-levels that exist in the source, to go faster. For
		 * instance, we do not want to detect spot that are larger than 10
		 * pixels (then we move up by one level), but we do not want to detect
		 * spots that are smaller than 5 pixels in diameter
		 */

		int level = 0;
		while ( level < source.getNumMipmapLevels() - 1 )
		{

			/*
			 * Scan all axes. The "worst" one is the one with the largest scale.
			 * If at this scale the spot is too small, then we stop.
			 */

			final AffineTransform3D sourceToGlobal = new AffineTransform3D();
			source.getSourceTransform( frame, level, sourceToGlobal );
			double scale = Affine3DHelpers.extractScale( sourceToGlobal, 0 );
			for ( int axis = 1; axis < sourceToGlobal.numDimensions(); axis++ )
			{
				final double sc = Affine3DHelpers.extractScale( sourceToGlobal, axis );
				if ( sc > scale )
				{
					scale = sc;
				}
			}

			final double diameterInPix = 2 * radius / scale;
			if ( diameterInPix < MIN_SPOT_PIXEL_SIZE )
			{
				break;
			}
			level++;
		}

		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		source.getSourceTransform( frame, level, sourceToGlobal );
		final RandomAccessibleInterval< T > rai = source.getSource( frame, level );

		// Protection against missing data.
		final long size = rai.dimension( 0 ) * rai.dimension( 1 ) * rai.dimension( 2 );
		if ( size < 10 )
		{
			return null;
		}

		/*
		 * Extract scales
		 */

		final double dx = Affine3DHelpers.extractScale( sourceToGlobal, 0 );
		final double dy = Affine3DHelpers.extractScale( sourceToGlobal, 1 );
		final double dz = Affine3DHelpers.extractScale( sourceToGlobal, 2 );
		final double[] calibration = new double[] { dx, dy, dz };

		/*
		 * Extract source coords
		 */

		final double neighborhoodFactor = Math.max( NEIGHBORHOOD_FACTOR, distanceTolerance + 1 );

		final Point roundedSourcePos = new Point( 3 );
		sourceToGlobal.applyInverse( new Round<>( roundedSourcePos ), spot );
		final long x = roundedSourcePos.getLongPosition( 0 );
		final long y = roundedSourcePos.getLongPosition( 1 );
		final long z = roundedSourcePos.getLongPosition( 2 );
		final long r = ( long ) Math.ceil( neighborhoodFactor * radius / dx );
		final long rz = ( long ) Math.ceil( neighborhoodFactor * radius / dz );

		/*
		 * Ensure quality
		 */

		final Double qf = spot.getFeature( Spot.QUALITY );
		if ( null == qf )
		{
			ok = false;
			logger.error( "Spot: " + spot + " Bad spot: has a null QUALITY feature." );
			return null;
		}
		double quality = qf.doubleValue();
		if ( quality < 0 )
		{
			final RandomAccess< T > ra = rai.randomAccess();
			ra.setPosition( roundedSourcePos );
			quality = ra.get().getRealDouble();
		}

		/*
		 * Extract crop cube
		 */

		final long width = rai.dimension( 0 );
		final long height = rai.dimension( 1 );
		final long depth = rai.dimension( 2 );

		final long x0 = Math.max( 0, x - r );
		final long y0 = Math.max( 0, y - r );
		final long z0 = Math.max( 0, z - rz );

		final long x1 = Math.min( width - 1, x + r );
		final long y1 = Math.min( height - 1, y + r );
		final long z1 = Math.min( depth - 1, z + rz );

		final long[] min = new long[] { x0, y0, z0 };
		final long[] max = new long[] { x1, y1, z1 };

		final Interval interval = new FinalInterval( min, max );

		final double[] cal = new double[] { dx, dy, dz };

		/*
		 * Build the transformation that will put back the found spot in the
		 * global coordinate system.
		 */

		final AffineTransform3D scale = new AffineTransform3D();
		for ( int i = 0; i < 3; i++ )
		{
			scale.set( 1 / cal[ i ], i, i );
		}
		final AffineTransform3D translate = new AffineTransform3D();
		for ( int i = 0; i < 3; i++ )
		{
			translate.set( min[ i ], i, 3 );
		}
		final AffineTransform3D transform = sourceToGlobal.copy().concatenate( scale );

		final SearchRegion< T > sn = new SearchRegion<>();
		sn.source = Views.dropSingletonDimensions( rai );
		sn.interval = interval;
		sn.transform = transform;
		sn.calibration = calibration;
		return sn;
	}

	@Override
	protected void exposeSpot( final Spot newSpot, final Spot previousSpot )
	{
		// We just copy the SOURCE_INDEX value from the old spot to the new
		// spot.
		final Double sourceIndex = previousSpot.getFeature( SpotSourceIdAnalyzerFactory.SOURCE_ID );
		newSpot.putFeature( SpotSourceIdAnalyzerFactory.SOURCE_ID, sourceIndex );
	}
}
