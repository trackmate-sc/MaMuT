package fiji.plugin.mamut.viewer;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.ImgPlus;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;
import fiji.plugin.mamut.util.TransformUtils;
import viewer.render.Interpolation;
import viewer.render.Source;

public class ImgPlusSource<T extends NumericType<T>> implements Source<T> {
	
	private final ImgPlus<T> img;

	public ImgPlusSource(ImgPlus<T> img) {
		this.img = img;
	}

	@Override
	public boolean isPresent(int t) {
		return t>=0 && t < img.dimension(3) ;
	}

	@Override
	public RandomAccessibleInterval<T> getSource(int t, int level) {
		return Views.hyperSlice(img, 3, t);
	}

	@Override
	public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method) {
		InterpolatorFactory<T, RandomAccessible< T >> factory;
		switch (method) {
		default:
		case NEARESTNEIGHBOR:
			factory = new NearestNeighborInterpolatorFactory<T>();
			break;
		case NLINEAR:
			factory = new NLinearInterpolatorFactory<T>();
			break;
		}
		return Views.interpolate(Views.extendZero(getSource(t, level)), factory);
	}

	@Override
	public AffineTransform3D getSourceTransform(int t, int level) {
		AffineTransform3D identity = TransformUtils.getTransformFromCalibration(img).inverse();
		return identity;
	}

	@Override
	public T getType() {
		return img.firstElement().copy();
	}

	@Override
	public String getName() {
		return img.getName();
	}

	@Override
	public int getNumMipmapLevels() {
		return 1;
	}
}
