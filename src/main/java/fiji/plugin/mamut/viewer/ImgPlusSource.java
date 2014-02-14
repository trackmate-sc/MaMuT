package fiji.plugin.mamut.viewer;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import fiji.plugin.mamut.util.TransformUtils;

public class ImgPlusSource< T extends NumericType< T >> implements Source< T >
{

	private final ImgPlus< T > img;

	public ImgPlusSource( final ImgPlus< T > img )
	{
		this.img = img;
	}

	@Override
	public boolean isPresent( final int t )
	{
		return t >= 0 && t < img.dimension( 3 );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return Views.hyperSlice( img, 3, t );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		InterpolatorFactory< T, RandomAccessible< T >> factory;
		switch ( method )
		{
		default:
		case NEARESTNEIGHBOR:
			factory = new NearestNeighborInterpolatorFactory< T >();
			break;
		case NLINEAR:
			factory = new NLinearInterpolatorFactory< T >();
			break;
		}
		final T zero = img.firstElement().createVariable();
		zero.setZero();
		return Views.interpolate( Views.extendValue( getSource( t, level ), zero ), factory );
	}

	@Override
	public AffineTransform3D getSourceTransform( final int t, final int level )
	{
		final AffineTransform3D identity = TransformUtils.getTransformFromCalibration( img ).inverse();
		return identity;
	}

	@Override
	public T getType()
	{
		return img.firstElement().copy();
	}

	@Override
	public String getName()
	{
		return img.getName();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}

}
