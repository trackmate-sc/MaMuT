package fiji.plugin.mamut.detection;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import fiji.plugin.mamut.util.Utils;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.CropImgView;

public class SemiAutoTracker<T extends RealType<T>  & NativeType<T>> implements Algorithm {

	private static final String BASE_ERROR_MESSAGE = "[SemiAutoTracker] ";
	/** The size of the local neighborhood to inspect, in units of the source spot diameter. */
	private static final double NEIGHBORHOOD_FACTOR = 3d;
	/** The quality drop we tolerate before aborting detection. The highest, the more intolerant. */
	private static final double QUALITY_THRESHOLD = 0.01d;
	/** How close must be the new spot found to be accepted, in radius units. */
	private static final double DISTANCE_TOLERANCE = 1.5;
	private final Model model;
	private final SelectionModel selectionModel;
	private final List<SourceAndConverter<T>> sources;
	private String errorMessage;

	/*
	 * CONSTRUCTOR 
	 */

	public SemiAutoTracker(Model model, SelectionModel selectionModel, List<SourceAndConverter<T>> sources) {
		this.model = model;
		this.selectionModel = selectionModel;
		this.sources = sources;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean process() {

		/*
		 * Initial spot
		 */

		Spot spot = selectionModel.getSpotSelection().iterator().next();

		while (true) {

			/*
			 * Extract spot & features
			 */

			int frame = spot.getFeature(Spot.FRAME).intValue() + 1; // We want to segment in the next frame
			double radius = spot.getFeature(Spot.RADIUS);
			double[] position = new double[3];
			spot.localize(position);


			/*
			 * Source, rai and transform
			 */

			int sourceIndex = 0; // TODO when Tobias will have saved the source index origin as a spot feature, use it.
			int level = 0; // TODO is there a way to exploit this better? Use different level for large spots?
			Source<T> source = sources.get(sourceIndex).getSpimSource();
			
			if (!source.isPresent(frame)) {
				errorMessage = "Target source has exhausted its time points";
				return true;
			}
			
			AffineTransform3D sourceToGlobal = source.getSourceTransform(frame, 0);
			RandomAccessibleInterval<T> rai = source.getSource(frame, level);

			/*
			 * Extract scales
			 */

			double dx = Utils.extractScale(sourceToGlobal, 0);
			double dy = dx;
			double dz = Utils.extractScale(sourceToGlobal, 2);

			/*
			 * Extract source coords
			 */

			Point roundedSourcePos = new Point(3);
			sourceToGlobal.applyInverse(new Round<Point>(roundedSourcePos), spot);
			long x = roundedSourcePos.getLongPosition(0);
			long y = roundedSourcePos.getLongPosition(1);
			long z = roundedSourcePos.getLongPosition(2);
			long r = (long) Math.ceil(NEIGHBORHOOD_FACTOR * radius / dx);
			long rz = (long) Math.ceil(NEIGHBORHOOD_FACTOR * radius / dz);

			/*
			 * Ensure quality
			 */

			Double qf = spot.getFeature(Spot.QUALITY);
			if (null == qf) {
				errorMessage = BASE_ERROR_MESSAGE + "Target spot has a null QUALITY feature.";
				return false;
			}
			double quality = qf.doubleValue(); 
			if (quality < 0) {
				RandomAccess<T> ra = rai.randomAccess();
				ra.setPosition(roundedSourcePos);
				quality = ra.get().getRealDouble();
			}

			/*
			 * Extract crop cube
			 */

			long width = rai.dimension(0);
			long height = rai.dimension(1);
			long depth = rai.dimension(2);

			long x0 = Math.max(0, x - r);
			long y0 = Math.max(0, y - r);
			long z0 = Math.max(0, z - rz);

			long x1 = Math.min(width, x + r);
			long y1 = Math.min(height, y + r);
			long z1 = Math.min(depth, z + rz);

			long[] min = new long[] { x0, y0, z0 };
			long[] max = new long[] { x1, y1, z1 };
			ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> erai = Views.extendBorder(rai);
			Img<T> cropimg =new CropImgView<T>(erai, min, max, new ArrayImgFactory<T>());

			/*
			 * Detect in crop cube
			 */

			AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
			double[] cal = new double[] { dx, dy, dz };
			ImgPlus<T> imgplus = new ImgPlus<T>(cropimg, "crop", axes, cal);
			SpotDetector<T> detector = createDetector(imgplus, radius, quality * QUALITY_THRESHOLD);

			if (!detector.checkInput() || !detector.process()) {
				errorMessage = detector.getErrorMessage();
				return false;
			}

			/*
			 * Get results
			 */

			List<Spot> detectedSpots = detector.getResult();
			if (detectedSpots.isEmpty()) {
				errorMessage = "No suitable spot found.";
				return true;
			}
			
			/*
			 * Translate spots
			 */
			
			String[] features = new String[] { Spot.POSITION_X, Spot.POSITION_Y, Spot.POSITION_Z }; 
			for (Spot ds : detectedSpots) {
				for (int i = 0; i < features.length; i++) {
					Double val = ds.getFeature(features[i]);
					ds.putFeature(features[i], val + (double) min[i] * cal[i]);
				}
			}
			
			// Sort then by ascending quality
			TreeSet<Spot> sortedSpots = new TreeSet<Spot>(Spot.featureComparator(Spot.QUALITY));
			sortedSpots.addAll(detectedSpots);

			boolean found = false;
			Spot target = null;
			for (Iterator<Spot> iterator = sortedSpots.descendingIterator(); iterator.hasNext();) {
				Spot candidate = iterator.next();
				if (candidate.squareDistanceTo(spot) < DISTANCE_TOLERANCE * DISTANCE_TOLERANCE * radius * radius) {
					found = true;
					target = candidate;
					break;
				}
			}

			if (!found) {
				errorMessage = "Suitable spot found, but outside the target radius.";
				return true;
			}
			
			/*
			 * Update model
			 */

			// spot
			target.putFeature(Spot.RADIUS, radius );
			target.putFeature(Spot.POSITION_T, Double.valueOf(frame) );
			model.beginUpdate();
			try {
				model.addSpotTo(target, frame);
			} finally { 
				model.endUpdate(); 
			}

			// link
			model.beginUpdate();
			try {
				model.addEdge(spot, target, spot.squareDistanceTo(target));
			} finally {
				model.endUpdate();
			}

			/*
			 * Loop
			 */

			spot = target;

		}
	}

	/**
	 * Returns a new instance of a {@link SpotDetector} that will inspect the neighborhood.
	 * @param img  the neighborhood to inspect.
	 * @param radius  the expected spot radius. 
	 * @param quality  the quality threshold below which found spots will be discarded.
	 * @return  a new {@link SpotTracker}.
	 */
	protected SpotDetector<T> createDetector(ImgPlus<T> img, double radius, double quality) {
		LogDetector<T> detector = new LogDetector<T>(img, radius, quality, true, false);
		detector.setNumThreads(1); // KISS TODO test if it would be better to shoot a few threads more
		return detector;
	}

	@Override
	public boolean checkInput() {
		if (null == model) {
			errorMessage = BASE_ERROR_MESSAGE + "model is null.";
			return false;
		}
		if (null == selectionModel) {
			errorMessage = BASE_ERROR_MESSAGE + "selectionModel is null.";
			return false;
		}
		if (selectionModel.getSpotSelection().size() != 1) {
			errorMessage = BASE_ERROR_MESSAGE + "Expected to have 1 spot in selection, but got " + 
					selectionModel.getSpotSelection().size() + "."; 
			return false;
		}
		if (sources == null) {
			errorMessage = BASE_ERROR_MESSAGE + "source is null.";
			return false;
		}
		return true;
	}


	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

}
