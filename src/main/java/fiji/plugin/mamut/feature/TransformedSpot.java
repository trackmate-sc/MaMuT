package fiji.plugin.mamut.feature;

import java.util.Map;

import bdv.util.Affine3DHelpers;
import fiji.plugin.trackmate.Spot;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Wrapper for a spot, that exposes different coordinates calculated from a
 * transform, but has still the same features.
 * <p>
 * 
 * @author Jean-Yves Tinevez
 */
public class TransformedSpot extends Spot
{
	// TODO If only Spot was an interface...

	private final Spot wrapped;

	private TransformedSpot( final RealPoint pos, final double radius, final Spot wrapped )
	{
		super( Spot.IDcounter.incrementAndGet() );
		super.putFeature( POSITION_X, Double.valueOf( pos.getDoublePosition( 0 ) ) );
		super.putFeature( POSITION_Y, Double.valueOf( pos.getDoublePosition( 1 ) ) );
		super.putFeature( POSITION_Z, Double.valueOf( pos.getDoublePosition( 2 ) ) );
		super.putFeature( RADIUS, Double.valueOf( radius ) );
		super.putFeature( QUALITY, Double.valueOf( wrapped.getFeature( Spot.QUALITY ) ) );
		this.wrapped = wrapped;
	}

	@Override
	public Map< String, Double > getFeatures()
	{
		return wrapped.getFeatures();
	}

	@Override
	public Double getFeature( final String feature )
	{
		if ( feature.equals( POSITION_X )
				|| feature.equals( POSITION_Y )
				|| feature.equals( POSITION_Z )
				|| feature.equals( RADIUS ) )
			return super.getFeature( feature );
		else
			return wrapped.getFeature( feature );
	}

	@Override
	public void putFeature( final String feature, final Double value )
	{
		if ( feature.equals( POSITION_X )
				|| feature.equals( POSITION_Y )
				|| feature.equals( POSITION_Z )
				|| feature.equals( RADIUS ) )
			super.putFeature( feature, value );
		else
			wrapped.putFeature( feature, value );
	}

	/**
	 * Returns a new spot that have its position and radius transformed with
	 * respect to the specified transform (inverse transform) and physical
	 * calibration, but that exposes the features of the wrapped spot.
	 * Modification made to the features of this spot are reflected on the
	 * wrapped spot, except for position features.
	 * 
	 * @param spot
	 *            the spot to wrap.
	 * @param transform
	 *            the transform.
	 * @param cal
	 *            the physical calibration, used to scale the pixel coordinates
	 *            returned by the inverse transform in physical coordinates.
	 * @return a new spot.
	 */
	public static TransformedSpot wrap( final Spot spot, final AffineTransform3D transform, final double[] cal )
	{
		final RealPoint pos = new RealPoint( 3 );
		transform.applyInverse( pos, spot );
		/*
		 * pos now contains the position in pixel coordinates. We need to scale
		 * them to the physical calibration.
		 */
		for ( int d = 0; d < 3; d++ )
			pos.setPosition( pos.getDoublePosition( d ) * cal[ d ], d );

		final double r = spot.getFeature( Spot.RADIUS ).doubleValue() / Affine3DHelpers.extractScale( transform, 0 );

		return new TransformedSpot( pos, r, spot );
	}
}
