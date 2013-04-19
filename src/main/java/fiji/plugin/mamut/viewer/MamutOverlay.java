package fiji.plugin.mamut.viewer;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import net.imglib2.realtransform.AffineTransform3D;
import viewer.render.ViewerState;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;

public class MamutOverlay {

	private static final Font DEFAULT_FONT = new Font( "Monospaced", Font.PLAIN, 10 );
	/** The viewer state. */
	private ViewerState state;
	/** The transform for the viewer current viewpoint. */
	private final AffineTransform3D transform = new AffineTransform3D();
	/** The model to point on this overlay. */
	private final TrackMateModel model;
	/** The viewer in which this overlay is painted. */
	private final MamutViewer viewer;
	/** The font use to paint spot name. */
	private Font textFont = DEFAULT_FONT;


	public MamutOverlay(final TrackMateModel model, final MamutViewer viewer) {
		this.model = model;
		this.viewer = viewer;
	}

	public synchronized void paint( final Graphics2D g ) {
		
		if (! (Boolean) viewer.displaySettings.get(KEY_SPOTS_VISIBLE)) {
			return;
		}
		final float radiusRatio = (Float) viewer.displaySettings.get(KEY_SPOT_RADIUS_RATIO);
		final boolean doDisplayNames = (Boolean) viewer.displaySettings.get(KEY_DISPLAY_SPOT_NAMES);
		
		/*
		 * Collect current view
		 */
		state.getViewerTransform(transform);

		/*
		 * Setup painter object
		 */
		g.setColor(Color.MAGENTA);
		g.setFont(textFont );
		
		/*
		 * Compute scale
		 */
		final double vx = transform.get( 0, 0 );
		final double vy = transform.get( 1, 0 );
		final double vz = transform.get( 2, 0 );
		final double transformScale = Math.sqrt( vx*vx + vy*vy + vz*vz );

		Iterable<Spot> spots = model.getFilteredSpots().get(state.getCurrentTimepoint());

		for (Spot spot : spots) {
			
			Color color;
			if (null == viewer.colorProvider || null == (color = viewer.colorProvider.get(spot))) {
				color = AbstractTrackMateModelView.DEFAULT_COLOR;
			}
			g.setColor(color);
			
			double x = spot.getFeature(Spot.POSITION_X);
			double y = spot.getFeature(Spot.POSITION_Y);
			double z = spot.getFeature(Spot.POSITION_Z);
			double radius = spot.getFeature(Spot.RADIUS);

			double[] globalCoords = new double[] { x, y, z };
			double[] viewerCoords = new double[3];
			transform.apply(globalCoords, viewerCoords);

			double rad = radius * transformScale * radiusRatio;
			double zv = viewerCoords[2];
			double dz2 = zv * zv;

			if (dz2 < rad*rad ) {
				
				double arad = Math.sqrt(rad * rad - dz2);
				g.drawOval(
						(int) (viewerCoords[0] - arad), 
						(int) (viewerCoords[1] - arad), 
						(int) (2*arad),
						(int) (2*arad));
				
				if (doDisplayNames) {
					int tx = (int) (viewerCoords[0] + arad + 5);
					int ty = (int) viewerCoords[1];
					g.drawString(spot.getName(), tx, ty);
				}
				

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
