package fiji.plugin.mamut.detection;

import java.util.List;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import fiji.plugin.mamut.util.Utils;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.semiauto.AbstractSemiAutoTracker;
import fiji.plugin.trackmate.util.CropImgView;

/**
 * A class made to perform semi-automated tracking of spots in MaMuT.
 * <p>
 * The user has to select one spot, one a meaningful location of a source. The
 * spot location and its radius are then used to extract a small rectangular
 * neighborhood in the next frame around the spot. The neighborhood is then
 * passed to a {@link SpotDetector} that returns the spot it found. If a spot of
 * {@link Spot#QUALITY} high enough is found near enough to the first spot
 * center, then it is added to the model and linked with the first spot.
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
 * @author Jean-Yves Tinevez - 2013
 * @param <T>
 *        the type of the source. Must extend {@link RealType} and
 *        {@link NativeType} to use with most TrackMate {@link SpotDetector}s.
 */
public class SourceSemiAutoTracker<T extends RealType<T> & NativeType<T>> extends AbstractSemiAutoTracker<T> {

	/** The minimal diameter size, in pixel, under which we stop down-sampling. */
	private static final double MIN_SPOT_PIXEL_SIZE = 5d;
	private List<SourceAndConverter<T>> sources;

	/* CONSTRUCTOR */

	public SourceSemiAutoTracker(Model model, SelectionModel selectionModel, List<SourceAndConverter<T>> sources, Logger logger) {
		super(model, selectionModel, logger);
		this.sources = sources;
	}

	/* METHODS */

	@Override
	public boolean checkInput() {
		if (!super.checkInput()) {
			return false;
		}
		if (sources == null) {
			errorMessage = BASE_ERROR_MESSAGE + "source is null.";
			return false;
		}
		return true;
	}

	@Override
	protected SpotNeighborhood<T> getNeighborhood(Spot spot, int frame) {

		double radius = spot.getFeature(Spot.RADIUS);

		/* Source, rai and transform */

		int sourceIndex = 0; // TODO when Tobias will have saved the source
								// index origin as a spot feature, use it.
		Source<T> source = sources.get(sourceIndex).getSpimSource();

		if (!source.isPresent(frame)) {
			logger.log("Spot: " + spot + ": Target source has exhausted its time points");
			return null;
		}

		/* Determine optimal level to operate on. We want to exploit the
		 * possible multi-levels that exist in the source, to go faster. For
		 * instance, we do not want to detect spot that are larger than 10
		 * pixels (then we move up by one level), but we do not want to detect
		 * spots that are smaller than 5 pixels in diameter */

		int level = 0;
		while (level < source.getNumMipmapLevels() - 1) {

			/* Scan all axes. The "worst" one is the one with the largest scale.
			 * If at this scale the spot is too small, then we stop. */

			AffineTransform3D sourceToGlobal = source.getSourceTransform(frame, level);
			double scale = Utils.extractScale(sourceToGlobal, 0);
			for (int axis = 1; axis < sourceToGlobal.numDimensions(); axis++) {
				double sc = Utils.extractScale(sourceToGlobal, axis);
				if (sc > scale) {
					scale = sc;
				}
			}

			double diameterInPix = 2 * radius / scale;
			if (diameterInPix < MIN_SPOT_PIXEL_SIZE) {
				break;
			}
			level++;
		}

		AffineTransform3D sourceToGlobal = source.getSourceTransform(frame, level);
		RandomAccessibleInterval<T> rai = source.getSource(frame, level);

		/* Extract scales */

		double dx = Utils.extractScale(sourceToGlobal, 0);
		double dy = Utils.extractScale(sourceToGlobal, 1);
		double dz = Utils.extractScale(sourceToGlobal, 2);

		/* Extract source coords */

		double neighborhoodFactor = Math.max(NEIGHBORHOOD_FACTOR, distanceTolerance + 1);

		Point roundedSourcePos = new Point(3);
		sourceToGlobal.applyInverse(new Round<Point>(roundedSourcePos), spot);
		long x = roundedSourcePos.getLongPosition(0);
		long y = roundedSourcePos.getLongPosition(1);
		long z = roundedSourcePos.getLongPosition(2);
		long r = (long) Math.ceil(neighborhoodFactor * radius / dx);
		long rz = (long) Math.ceil(neighborhoodFactor * radius / dz);

		/* Ensure quality */

		Double qf = spot.getFeature(Spot.QUALITY);
		if (null == qf) {
			ok = false;
			logger.error("Spot: " + spot + " Bad spot: has a null QUALITY feature.");
			return null;
		}
		double quality = qf.doubleValue();
		if (quality < 0) {
			RandomAccess<T> ra = rai.randomAccess();
			ra.setPosition(roundedSourcePos);
			quality = ra.get().getRealDouble();
		}

		/* Extract crop cube */

		long width = rai.dimension(0);
		long height = rai.dimension(1);
		long depth = rai.dimension(2);

		long x0 = Math.max(0, x - r);
		long y0 = Math.max(0, y - r);
		long z0 = Math.max(0, z - rz);

		long x1 = Math.min(width - 1, x + r);
		long y1 = Math.min(height - 1, y + r);
		long z1 = Math.min(depth - 1, z + rz);

		long[] min = new long[] { x0, y0, z0 };
		long[] max = new long[] { x1, y1, z1 };
		Img<T> cropimg = new CropImgView<T>(rai, min, max, new ArrayImgFactory<T>());

		AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		double[] cal = new double[] { dx, dy, dz };
		ImgPlus<T> imgplus = new ImgPlus<T>(cropimg, "crop", axes, cal);

		/* Build the transformation that will put back the found spot in the
		 * global coordinate system */

		AffineTransform3D scale = new AffineTransform3D();
		for (int i = 0; i < 3; i++) {
			scale.set(1 / cal[i], i, i);
		}
		AffineTransform3D translate = new AffineTransform3D();
		for (int i = 0; i < 3; i++) {
			translate.set(min[i], i, 3);
		}

		AffineTransform3D transform = sourceToGlobal.copy().concatenate(translate).concatenate(scale);
		SpotNeighborhood<T> sn = new SpotNeighborhood<T>();
		sn.neighborhood = imgplus;
		sn.transform = transform;
		return sn;
	}
}
