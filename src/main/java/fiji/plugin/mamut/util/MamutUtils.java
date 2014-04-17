package fiji.plugin.mamut.util;

import java.util.List;

import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.img.Img;
import net.imglib2.img.constant.ConstantImg;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.trackmate.Spot;

public class MamutUtils
{
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static final Img< ? > getImgAround( final Spot spot, final int width, final int height, final List< SourceAndConverter< ? > > list )
	{
		/*
		 * Here be generics massacre...
		 */

		final Type type = ( Type ) list.iterator().next().getSpimSource().getType();
		final RealType rtype = ( RealType ) type.createVariable();
		rtype.setZero();
		final NativeType ntype = ( NativeType ) rtype;
		final long[] size = new long[] { width, height };

		// Retrieve frame
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		// Retrieve source ID
		final Double si = spot.getFeature( SpotSourceIdAnalyzerFactory.SOURCE_ID );
		if ( null == si )
		{
			final Img ret = new ConstantImg( size, rtype );
			return ret;
		}

		final int sourceID = si.intValue();
		final Source source = list.get( sourceID ).getSpimSource();
		final RandomAccessibleInterval img = source.getSource( frame, 0 );

		// Get spot coords
		final AffineTransform3D sourceToGlobal = source.getSourceTransform( frame, 0 );
		final Point roundedSourcePos = new Point( 3 );
		sourceToGlobal.applyInverse( new Round< Point >( roundedSourcePos ), spot );
		final long x = roundedSourcePos.getLongPosition( 0 );
		final long y = roundedSourcePos.getLongPosition( 1 );
		final long z = Math.max( img.min( 2 ), Math.min( img.max( 2 ), roundedSourcePos.getLongPosition( 2 ) ) );

		final int xp = ( int ) Math.ceil( width / 2d );
		final int xm = ( int ) Math.floor( width / 2d );
		final int yp = ( int ) Math.ceil( height / 2d );
		final int ym = ( int ) Math.floor( height / 2d );

		// Extract central slice
		final IntervalView slice = Views.hyperSlice( img, 2, z );

		// Crop
		final Interval cropInterval = Intervals.createMinMax( x - xm, y - ym, x + xp, y + yp );

		if ( isEmpty( cropInterval ) )
		{
			final Img ret = new ConstantImg( size, type );
			return ret;
		}
		else
		{
			final ExtendedRandomAccessibleInterval extendZero = Views.extendZero( slice );
			final IntervalView crop = Views.zeroMin( Views.interval( extendZero, cropInterval ) );
			final Img target = Util.getArrayOrCellImgFactory( crop, ntype ).create( size, ntype );
			new IterableIntervalProjector2D( 0, 1, crop, target, new TypeIdentity() ).map();
			return target;
		}
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
