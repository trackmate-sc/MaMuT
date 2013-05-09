package fiji.plugin.mamut.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import viewer.render.Source;

import com.mxgraph.util.mxBase64;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;

public class SourceSpotImageUpdater <T extends RealType<T>> extends SpotImageUpdater {

	/** How much extra we capture around spot radius. */
	private static final double RADIUS_FACTOR = 1.1;
	private Source<T> source;
	private Integer previousFrame = -1;
	private final double[] calibration;
	private RandomAccessibleInterval<T> img;

	public SourceSpotImageUpdater(TrackMateModel model, Source<T> source) {
		super(model);
		this.source = source;
		this.calibration = new double[] { model.getSettings().dx, model.getSettings().dy, model.getSettings().dz }; 
	}

	
	/**
	 * @return the image string of the given spot, based on the raw images contained in the given model.
	 * For performance, the image at target frame is stored for subsequent calls of this method. 
	 * So it is a good idea to group calls to this method for spots that belong to the
	 * same frame.
	 */
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
		
		// Extract central slice
		long z = Math.round(spot.getFeature(Spot.POSITION_Z).doubleValue() / calibration[2]);
		IntervalView<T> slice = Views.hyperSlice(img, 2, z);
		
		// Get spot coords
		long x = Math.round(spot.getFeature(Spot.POSITION_X).doubleValue() / calibration[0]);
		long y = Math.round(spot.getFeature(Spot.POSITION_Y).doubleValue() / calibration[1]);
		long r = (long) Math.ceil(RADIUS_FACTOR * spot.getFeature(Spot.RADIUS).doubleValue() / calibration[0]);
		
		// Crop
		long[] min = new long[] { x - r, y - r };
		long[] max = new long[] { x + r, y + r };
		IntervalView<T> crop = Views.interval(slice, min, max);
			
		// Convert to string
		byte[] bytes = create8BitImage(crop);
		int width = (int) crop.dimension(0);
		int height = (int) crop.dimension(1);
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
	    byte[] imgData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	    System.arraycopy(bytes, 0, imgData, 0, bytes.length);     
		
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
	
	


	private static final <T extends RealType<T>> byte[] create8BitImage(RandomAccessibleInterval<T> rai) {
		
		/*
		 * Determine min & max
		 */
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (T pixel : Views.iterable(rai)) {
			double val = pixel.getRealDouble();
			if (val > max) {
				max = val;
			}
			if (val < min) {
				min = val;
			}
		}
		double scale = 256.0/(max-min+1);

		/*
		 * Scale
		 */
		
		int size = (int) (rai.dimension(0)*rai.dimension(1));
		byte[] pixels8 = new byte[size];
		
		int index = 0;
		for (T pixel : Views.iterable(rai)) {
			double val = pixel.getRealDouble();
			int value = (int) ( val * scale + 0.5);
			if (value>255) value = 255;
			pixels8[index++] = (byte)value;
		}
		return pixels8;
	}
}
