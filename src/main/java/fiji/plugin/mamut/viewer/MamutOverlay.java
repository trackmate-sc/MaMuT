package fiji.plugin.mamut.viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

import mpicbg.imglib.util.Util;
import net.imglib2.realtransform.AffineTransform3D;
import viewer.render.ViewerState;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class MamutOverlay {

	private static final boolean DEBUG = false;
	private ViewerState state;
	private final AffineTransform3D transform = new AffineTransform3D();
	private final TrackMateModel model;


	public MamutOverlay(TrackMateModel model) {
		this.model = model;
	}

	public synchronized void paint( final Graphics2D g ) {
		state.getViewerTransform(transform);

		g.setColor(Color.MAGENTA);

		Iterable<Spot> spots = model.getFilteredSpots().get(state.getCurrentTimepoint());

		for (Spot spot : spots) {
			double x = spot.getFeature(Spot.POSITION_X);
			double y = spot.getFeature(Spot.POSITION_Y);
			double z = spot.getFeature(Spot.POSITION_Z);
			double radius = spot.getFeature(Spot.RADIUS);

			double[] globalCoords = new double[] { x, y, z };
			double[] viewerCoords = new double[3];
			transform.apply(globalCoords, viewerCoords);

			double[] gCorner1 = new double[] { x-radius, y-radius, z-radius };
			double[] vCorner1 = new double[3];
			transform.apply(gCorner1, vCorner1);

			double[] gCorner2 = new double[] { x+radius, y+radius, z+radius };
			double[] vCorner2 = new double[3];
			transform.apply(gCorner2, vCorner2);

			double rad = Math.abs(vCorner1[0] - vCorner2[0]);
			for (int i = 1; i < vCorner2.length; i++) {
				double d = Math.abs(vCorner1[i] - vCorner2[i]);
				if (rad <  d) {
					rad = d;
				}
			}
			double xs = Math.min(vCorner1[0], vCorner2[0]);
			double ys = Math.min(vCorner1[1], vCorner2[1]);

			double zv = viewerCoords[2];
			double dz2 = (zv-z)*(zv-z);

			if (dz2 < rad*rad ) {
				
				double arad = Math.sqrt(rad * rad - dz2);


				g.drawOval(
						(int) (xs - arad), 
						(int) (ys - arad), 
						(int) (2*arad),
						(int) (2*arad)); 

			} else {
				g.fillOval(
						(int) xs, 
						(int) ys, 
						4, 4);
			}
		}


	}

	/**
	 * Update data to show in the overlay.
	 */
	public void setViewerState( final ViewerState state ) {
		this.state = state;
	}

}
