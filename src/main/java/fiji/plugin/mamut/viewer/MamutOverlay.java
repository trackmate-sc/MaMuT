package fiji.plugin.mamut.viewer;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Set;

import net.imglib2.realtransform.AffineTransform3D;

import org.jgrapht.graph.DefaultWeightedEdge;

import viewer.render.ViewerState;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class MamutOverlay {

	private static final Font DEFAULT_FONT = new Font( "Monospaced", Font.PLAIN, 10 );
	private static final Stroke NORMAL_STROKE = new BasicStroke(1.0f);
	private static final Stroke HIGHLIGHT_STROKE = new BasicStroke(2.0f);
	/** The viewer state. */
	private ViewerState state;
	/** The transform for the viewer current viewpoint. */
	private final AffineTransform3D transform = new AffineTransform3D();
	/** The model to point on this overlay. */
	private final Model model;
	/** The viewer in which this overlay is painted. */
	private final MamutViewer viewer;
	/** The font use to paint spot name. */
	private Font textFont = DEFAULT_FONT;
	/** The selection model. Items belonging to it will be highlighted. */
	private final SelectionModel selectionModel;


	public MamutOverlay(Model model, SelectionModel selectionModel, MamutViewer viewer) {
		this.model = model;
		this.selectionModel = selectionModel;
		this.viewer = viewer;
	}

	public synchronized void paint( final Graphics2D g ) {

		/*
		 * Collect current view
		 */
		state.getViewerTransform(transform);

		/*
		 * Draw spots
		 */
		
		if ((Boolean) viewer.displaySettings.get(KEY_SPOTS_VISIBLE)) {


			final float radiusRatio = (Float) viewer.displaySettings.get(KEY_SPOT_RADIUS_RATIO);
			final boolean doDisplayNames = (Boolean) viewer.displaySettings.get(KEY_DISPLAY_SPOT_NAMES);

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

			Iterable<Spot> spots = model.getSpots().iterable(state.getCurrentTimepoint(), true);
			
			for (Spot spot : spots) {

				Color color;
				Stroke stroke;
				if (selectionModel.getSpotSelection().contains(spot)) {
					color = AbstractTrackMateModelView.DEFAULT_HIGHLIGHT_COLOR;
					stroke = HIGHLIGHT_STROKE;
				} else {
					if (null == viewer.spotColorProvider || null == (color = viewer.spotColorProvider.color(spot))) {
						color = AbstractTrackMateModelView.DEFAULT_COLOR;
					}
					stroke = NORMAL_STROKE;
				}
				g.setColor(color);
				g.setStroke(stroke);

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

		/*
		 * Draw edges
		 */

		boolean tracksVisible = (Boolean) viewer.displaySettings.get(TrackMateModelView.KEY_TRACKS_VISIBLE);

		if (tracksVisible  && model.getTrackModel().nTracks(false) > 0) {

			// Save graphic device original settings
			final Composite originalComposite = g.getComposite();
			final Stroke originalStroke = g.getStroke();
			final Color originalColor = g.getColor();	
			
			Spot source, target;

			// Deal with highlighted edges first: brute and thick display
			g.setStroke(new BasicStroke(4.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
			for (DefaultWeightedEdge edge : selectionModel.getEdgeSelection()) {
				source = model.getTrackModel().getEdgeSource(edge);
				target = model.getTrackModel().getEdgeTarget(edge);
				drawEdge(g, source, target, transform, 1f);
			}

			// The rest
			final int currentFrame = viewer.getCurrentTimepoint();
			final int trackDisplayMode = (Integer) viewer.displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_MODE);
			final int trackDisplayDepth = (Integer) viewer.displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH);
//			final Map<Integer, Set<DefaultWeightedEdge>> trackEdges = model.getTrackModel(). 
			final Set<Integer> filteredTrackIDs = model.getTrackModel().trackIDs(true);

			g.setStroke(new BasicStroke(2.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			if (trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL || trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK) 
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

			// Determine bounds for limited view modes
			int minT = 0;
			int maxT = 0;
			switch (trackDisplayMode) {
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
				minT = currentFrame - trackDisplayDepth;
				maxT = currentFrame + trackDisplayDepth;
				break;
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
				minT = currentFrame;
				maxT = currentFrame + trackDisplayDepth;
				break;
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
				minT = currentFrame - trackDisplayDepth;
				maxT = currentFrame;
				break;
			}

			double sourceFrame;
			float transparency;
			switch (trackDisplayMode) {

			case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE: {
				for (Integer trackID : filteredTrackIDs) {
					viewer.trackColorProvider.setCurrentTrackID(trackID);
					final Set<DefaultWeightedEdge> track = new HashSet<DefaultWeightedEdge>(model.getTrackModel().trackEdges(trackID)); // TODO TEST

					for (DefaultWeightedEdge edge : track) {
						if (selectionModel.getEdgeSelection().contains(edge))
							continue;

						source = model.getTrackModel().getEdgeSource(edge);
						target = model.getTrackModel().getEdgeTarget(edge);
						g.setColor(viewer.trackColorProvider.color(edge));
						drawEdge(g, source, target,transform, 1f);
					}
				}
				break;
			}

			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK: 
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK: 
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK: {

				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

				for (int trackID : filteredTrackIDs) {
					viewer.trackColorProvider.setCurrentTrackID(trackID);
					final Set<DefaultWeightedEdge> track = new HashSet<DefaultWeightedEdge>(model.getTrackModel().trackEdges(trackID)); // TODO TEST

					for (DefaultWeightedEdge edge : track) {
						if (selectionModel.getEdgeSelection().contains(edge))
							continue;

						source = model.getTrackModel().getEdgeSource(edge);
						sourceFrame = source.getFeature(Spot.FRAME).intValue();
						if (sourceFrame < minT || sourceFrame >= maxT)
							continue;

						target = model.getTrackModel().getEdgeTarget(edge);
						g.setColor(viewer.trackColorProvider.color(edge));
						drawEdge(g, source, target, transform, 1f);
					}
				}
				break;
			}

			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD: {

				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				for (int trackID : filteredTrackIDs) {
					viewer.trackColorProvider.setCurrentTrackID(trackID);
					final Set<DefaultWeightedEdge> track = model.getTrackModel().trackEdges(trackID);

					for (DefaultWeightedEdge edge : track) {
						if (selectionModel.getEdgeSelection().contains(edge))
							continue;

						source = model.getTrackModel().getEdgeSource(edge);
						sourceFrame = source.getFeature(Spot.FRAME).intValue();
						if (sourceFrame < minT || sourceFrame >= maxT)
							continue;

						transparency = (float) (1 - Math.abs(sourceFrame-currentFrame) / trackDisplayDepth);
						target = model.getTrackModel().getEdgeTarget(edge);
						g.setColor(viewer.trackColorProvider.color(edge));
						drawEdge(g, source, target, transform, transparency);
					}
				}
				break;

			}

			}
			
			// Restore graphic device original settings		
			g.setComposite(originalComposite);
			g.setStroke(originalStroke);
			g.setColor(originalColor);

		}


	}

	protected void drawEdge(final Graphics2D g2d, final Spot source, final Spot target, final AffineTransform3D transform, float transparency) {

		// Find x & y & z in physical coordinates
		final double x0i = source.getFeature(Spot.POSITION_X);
		final double y0i = source.getFeature(Spot.POSITION_Y);
		final double z0i = source.getFeature(Spot.POSITION_Z);
		final double[] physicalPositionSource = new double[] { x0i, y0i, z0i };

		final double x1i = target.getFeature(Spot.POSITION_X);
		final double y1i = target.getFeature(Spot.POSITION_Y);
		final double z1i = target.getFeature(Spot.POSITION_Z);
		final double[] physicalPositionTarget = new double[] { x1i, y1i, z1i };

		// In pixel units
		final double[] pixelPositionSource = new double[3];
		transform.apply(physicalPositionSource, pixelPositionSource);
		final double[] pixelPositionTarget = new double[3];
		transform.apply(physicalPositionTarget, pixelPositionTarget);

		// Round
		final int x0 = (int) Math.round(pixelPositionSource[0]);
		final int y0 = (int) Math.round(pixelPositionSource[1]);
		final int x1 = (int) Math.round(pixelPositionTarget[0]);
		final int y1 = (int) Math.round(pixelPositionTarget[1]);

		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
		g2d.drawLine(x0, y0, x1, y1);
	}


	/**
	 * Update data to show in the overlay.
	 */
	public void setViewerState( final ViewerState state ) {
		this.state = state;
	}

}
