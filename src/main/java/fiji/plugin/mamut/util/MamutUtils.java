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
package fiji.plugin.mamut.util;

import bdv.viewer.Source;
import fiji.plugin.trackmate.Spot;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class MamutUtils
{

	@SuppressWarnings( "rawtypes" )
	public static final Img< ? > getSliceAround( final Spot spot, final int width, final int height, final Source source )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		// Get spot coords
		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		source.getSourceTransform( frame, 0, sourceToGlobal );
		final Point roundedSourcePos = new Point( 3 );
		sourceToGlobal.applyInverse( new Round<>( roundedSourcePos ), spot );
		final long x = roundedSourcePos.getLongPosition( 0 );
		final long y = roundedSourcePos.getLongPosition( 1 );
		final long z = roundedSourcePos.getLongPosition( 2 );
		// Grab patch
		return getImgPatch( new long[] { x, y, z }, frame, width, height, source );
	}

	@SuppressWarnings( "rawtypes" )
	public static final Img< ? > getStackAround( final Spot spot, final int width, final int height, final int depth, final Source source )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		// Get spot coords
		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		source.getSourceTransform( frame, 0, sourceToGlobal );
		final Point roundedSourcePos = new Point( 3 );
		sourceToGlobal.applyInverse( new Round<>( roundedSourcePos ), spot );
		final long x = roundedSourcePos.getLongPosition( 0 );
		final long y = roundedSourcePos.getLongPosition( 1 );
		final long z = roundedSourcePos.getLongPosition( 2 );
		return getImgPatch( new long[] { x, y, z }, frame, width, height, depth, source );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static final Img< ? > getImgPatch( final long[] pos, final int frame, final int width, final int height, final int depth, final Source source )
	{
		/*
		 * Here be generics massacre...
		 */

		final Type type = ( Type ) source.getType();
		final RealType rtype = ( RealType ) type.createVariable();
		rtype.setZero();
		final NativeType ntype = ( NativeType ) rtype;
		final long[] size = new long[] { width, height, depth };

		final RandomAccessibleInterval img = source.getSource( frame, 0 );

		// Get coords
		final long x = pos[ 0 ];
		final long y = pos[ 1 ];
		final long z = pos[ 2 ];

		final int xp = width / 2;
		final int xm = width - xp;
		final int yp = height / 2;
		final int ym = height - yp;
		final int zp = depth / 2;
		final int zm = depth - zp;

		// Crop
		final Interval cropInterval = Intervals.createMinMax( x - xm, y - ym, z - zm, x + xp, y + yp, z + zp );

		if ( isEmpty( cropInterval ) )
		{
			final Img ret = new ArrayImgFactory( ntype ).create( size );
			return ret;
		}

		final ExtendedRandomAccessibleInterval extendZero = Views.extendZero( img );
		final IntervalView crop = Views.zeroMin( Views.interval( extendZero, cropInterval ) );
		final Img target = Util.getArrayOrCellImgFactory( crop, ntype ).create( size );

		final RandomAccess randomAccess = crop.randomAccess();
		final Cursor cursor = target.localizingCursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			randomAccess.setPosition( cursor );
			( ( Type ) cursor.get() ).set( ( Type ) randomAccess.get() );
		}

		return target;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static final Img< ? > getImgPatch( final long[] pos, final int frame, final int width, final int height, final Source source )
	{
		/*
		 * Here be generics massacre...
		 */

		final Type type = ( Type ) source.getType();
		final RealType rtype = ( RealType ) type.createVariable();
		rtype.setZero();
		final NativeType ntype = ( NativeType ) rtype;
		final long[] size = new long[] { width, height };

		final RandomAccessibleInterval img = source.getSource( frame, 0 );

		// Get coords
		final long x = pos[ 0 ];
		final long y = pos[ 1 ];
		final long z = pos[ 2 ];

		final int xp = width / 2;
		final int xm = width - xp;
		final int yp = height / 2;
		final int ym = height - yp;

		// Extract central slice
		final IntervalView slice = Views.hyperSlice( img, 2, z );

		// Crop
		final Interval cropInterval = Intervals.createMinMax( x - xm, y - ym, x + xp, y + yp );

		if ( isEmpty( cropInterval ) )
		{
			final Img ret = new ArrayImgFactory( ntype ).create( size );
			return ret;
		}

		final ExtendedRandomAccessibleInterval extendZero = Views.extendZero( slice );
		final IntervalView crop = Views.zeroMin( Views.interval( extendZero, cropInterval ) );
		final Img target = Util.getArrayOrCellImgFactory( crop, ntype ).create( size );
		new IterableIntervalProjector2D( 0, 1, crop, target, new TypeIdentity() ).map();
		return target;
	}

	private static final boolean isEmpty( final Interval interval )
	{
		final int n = interval.numDimensions();
		for ( int d = 0; d < n; ++d )
			if ( interval.min( d ) > interval.max( d ) )
				return true;
		return false;
	}

}
