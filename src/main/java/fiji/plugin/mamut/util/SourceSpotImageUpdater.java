package fiji.plugin.mamut.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Max;
import net.imglib2.algorithm.stats.Min;
import net.imglib2.display.RealUnsignedByteConverter;
import net.imglib2.display.XYProjector;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import viewer.render.Source;

import com.mxgraph.util.mxBase64;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;

public class SourceSpotImageUpdater <T extends RealType<T>> extends SpotImageUpdater {

	/** How much extra we capture around spot radius. */
	private static final double RADIUS_FACTOR = 1.1;
	private Source<T> source;
	private Integer previousFrame = -1; // TODO: remove and make super.previousFrame protected?
	private RandomAccessibleInterval<T> img;

	public SourceSpotImageUpdater(Settings settings, Source<T> source) {
		super(settings);
		this.source = source;
	}

	/**
	 * @return the image string of the given spot, based on the raw images contained in the given model.
	 * For performance, the image at target frame is stored for subsequent calls of this method.
	 * So it is a good idea to group calls to this method for spots that belong to the
	 * same frame.
	 */
	@Override
	public String getImageString(final Spot spot) {

		Integer frame = spot.getFeature(Spot.FRAME).intValue();
		if (null == frame)
			return "";
		if (frame == previousFrame) {
			// Keep the same image than in memory
		} else {
			img = source.getSource(frame, 0);
			previousFrame = frame;
		}

		// TODO?: Currently calibration[] is not used

		// Get spot coords
		AffineTransform3D sourceToGlobal = source.getSourceTransform(frame, 0);
		Point roundedSourcePos = new Point(3);
		sourceToGlobal.applyInverse(new Round<Point>(roundedSourcePos), spot);
		long x = roundedSourcePos.getLongPosition(0);
		long y = roundedSourcePos.getLongPosition(1);
		long z = roundedSourcePos.getLongPosition(2);
		long r = (long) Math.ceil(RADIUS_FACTOR * spot.getFeature(Spot.RADIUS).doubleValue() / 
				Utils.extractScale(sourceToGlobal, 0));

		// Extract central slice
		IntervalView<T> slice = Views.hyperSlice(img, 2, z);

		// Crop
		
		long w = img.dimension(0);
		long h = img.dimension(1);

		long x0 = Math.max(0, x - r);
		long y0 = Math.max(0, y - r);

		long x1 = Math.min(w-1, x + r);
		long y1 = Math.min(h-1, y + r);

		long[] min = new long[] { x0, y0 };
		long[] max = new long[] { x1, y1 };

		
		IntervalView<T> crop = Views.zeroMin( Views.interval(slice, min, max) );

		int width = (int) crop.dimension(0);
		int height = (int) crop.dimension(1);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
	    byte[] imgData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	    double minValue = Min.findMin(Views.iterable(crop)).get().getRealDouble();
	    double maxValue = Max.findMax(Views.iterable(crop)).get().getRealDouble();
		new XYProjector<T,UnsignedByteType>(crop,ArrayImgs.unsignedBytes(imgData, width, height), new RealUnsignedByteConverter<T>(minValue, maxValue)).map();

		// Convert to string
	    String str;
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "png", bos);
			str = mxBase64.encodeToString(bos.toByteArray(), false);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

		return str;
	}
}
