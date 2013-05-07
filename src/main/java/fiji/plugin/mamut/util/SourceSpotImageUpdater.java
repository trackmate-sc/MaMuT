package fiji.plugin.mamut.util;

import net.imglib2.RandomAccessibleInterval;
import viewer.render.Source;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.trackscheme.SpotIconGrabber;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;

public class SourceSpotImageUpdater extends SpotImageUpdater {

	private Source<?> source;
	private Integer previousFrame = -1;
	private final double[] calibration;
	private SpotIconGrabber<?> grabber;

	public SourceSpotImageUpdater(TrackMateModel model, Source<?> source) {
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getImageString(final Spot spot) {

		Integer frame = spot.getFeature(Spot.FRAME).intValue();
		if (null == frame)
			return "";
		if (frame == previousFrame) {
			// Keep the same image than in memory
		} else {
			RandomAccessibleInterval img = source.getSource(frame, 0);
			grabber = new SpotIconGrabber(img, calibration);
			previousFrame = frame;
		}
		return grabber.getImageString(spot);
	}
}
