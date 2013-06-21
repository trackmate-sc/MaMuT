package fiji.plugin.mamut.util;

import net.imglib2.realtransform.AffineTransform3D;


public class Utils {

	// TODO: move this to a helper class together with the extract... methods in RotationAnimator
	/**
	 * Get the scale factor along the X, Y, or Z axis. For example, a points
	 * A=(x,0,0) and B=(x+1,0,0) on the X axis will be transformed to points
	 * A'=T*A and B'=T*B by the transform T. The distance between A' and B'
	 * gives the X scale factor.
	 *
	 * @param transform
	 *            an affine transform.
	 * @param axis
	 *            index of the axis for which the scale factor should be
	 *            computed.
	 * @return scale factor.
	 */
	public static double extractScale(final AffineTransform3D transform, final int axis)
	{
		double sqSum = 0;
		final int c = axis;
		for ( int r = 0; r < 3; ++r )
		{
			final double x = transform.get( r, c );
			sqSum += x * x;
		}
		return Math.sqrt( sqSum );
	}

}
