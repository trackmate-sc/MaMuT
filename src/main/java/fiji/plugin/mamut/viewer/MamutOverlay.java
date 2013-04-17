package fiji.plugin.mamut.viewer;

import java.awt.Color;
import java.awt.Graphics2D;

import net.imglib2.realtransform.AffineTransform3D;
import viewer.render.ViewerState;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class MamutOverlay {

	private ViewerState state;
	private final AffineTransform3D transform = new AffineTransform3D();
	private final TrackMateModel model;


	public MamutOverlay(TrackMateModel model) {
		this.model = model;
	}

	public synchronized void paint( final Graphics2D g ) {
		state.getViewerTransform(transform);
		
		/*
		 * Compute scale
		 */
		final double vx = transform.get( 0, 0 );
		final double vy = transform.get( 1, 0 );
		final double vz = transform.get( 2, 0 );
		final double transformScale = Math.sqrt( vx*vx + vy*vy + vz*vz );

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

			double rad = radius * transformScale;
			double zv = viewerCoords[2];
			double dz2 = zv * zv;

			if (dz2 < rad*rad ) {
				
				double arad = Math.sqrt(rad * rad - dz2);
				g.drawOval(
						(int) (viewerCoords[0] - arad), 
						(int) (viewerCoords[1] - arad), 
						(int) (2*arad),
						(int) (2*arad)); 

			} else {
				g.fillOval(
						(int) viewerCoords[0] - 2, 
						(int) viewerCoords[1] - 2, 
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
