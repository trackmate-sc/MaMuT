package fiji.plugin.mamut;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.realtransform.AffineTransform3D;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

/**
 * The overlay class in charge of drawing the tracks on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class TransformedTrackOverlay implements Overlay {

	protected final ImagePlus imp;
	protected Map<String, Object> displaySettings;
	protected final TrackMateModel model;
	protected final List<AffineTransform3D> transforms;
	private TrackColorGenerator colorGenerator;
	private Collection<DefaultWeightedEdge> highlight = new HashSet<DefaultWeightedEdge>();
	
	/*
	 * CONSTRUCTOR
	 */

	public TransformedTrackOverlay(final TrackMateModel model, final ImagePlus imp, final List<AffineTransform3D> transformList, final Map<String, Object> displaySettings) {
		this.model = model;
		this.imp = imp;
		this.transforms = transformList;
		this.displaySettings = displaySettings;
		this.colorGenerator = new PerTrackFeatureColorGenerator(model, TrackIndexAnalyzer.TRACK_INDEX);
	}

	/*
	 * PUBLIC METHODS
	 */
	
	public void setHighlight(Collection<DefaultWeightedEdge> edges) {
		this.highlight = edges;
	}

	@Override
	public final void paint(final Graphics g, final int xcorner, final int ycorner, final double magnification) {
		boolean tracksVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_TRACKS_VISIBLE);
		if (!tracksVisible  || model.getTrackModel().getNFilteredTracks() == 0)
			return;

		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();	
		final double dt = model.getSettings().dt;
		final float mag = (float) magnification;
		Spot source, target;

		// Deal with highlighted edges first: brute and thick display
		g2d.setStroke(new BasicStroke(4.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
		for (DefaultWeightedEdge edge : highlight) {
			source = model.getTrackModel().getEdgeSource(edge);
			target = model.getTrackModel().getEdgeTarget(edge);
			drawEdge(g2d, source, target, xcorner, ycorner, mag);
		}

		// The rest
		final int currentFrame = imp.getFrame() - 1;
		final int trackDisplayMode = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_MODE);
		final int trackDisplayDepth = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH);
		final Map<Integer, Set<DefaultWeightedEdge>> trackEdges = model.getTrackModel().getTrackEdges(); 
		final Set<Integer> filteredTrackIDs = model.getTrackModel().getFilteredTrackIDs();

		g2d.setStroke(new BasicStroke(2.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if (trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL || trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK) 
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

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
				colorGenerator.setCurrentTrackID(trackID);
				final Set<DefaultWeightedEdge> track = trackEdges.get(trackID);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getTrackModel().getEdgeSource(edge);
					target = model.getTrackModel().getEdgeTarget(edge);
					g2d.setColor(colorGenerator.color(edge));
					drawEdge(g2d, source, target, xcorner, ycorner, mag);
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK: 
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK: 
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK: {

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			for (int trackID : filteredTrackIDs) {
				colorGenerator.setCurrentTrackID(trackID);
				final Set<DefaultWeightedEdge> track= trackEdges.get(trackID);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getTrackModel().getEdgeSource(edge);
					sourceFrame = source.getFeature(Spot.POSITION_T) / dt;
					if (sourceFrame < minT || sourceFrame >= maxT)
						continue;

					target = model.getTrackModel().getEdgeTarget(edge);
					g2d.setColor(colorGenerator.color(edge));
					drawEdge(g2d, source, target, xcorner, ycorner, mag);
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD: {

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			for (int trackID : filteredTrackIDs) {
				colorGenerator.setCurrentTrackID(trackID);
				final Set<DefaultWeightedEdge> track= trackEdges.get(trackID);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getTrackModel().getEdgeSource(edge);
					sourceFrame = source.getFeature(Spot.POSITION_T) / dt;
					if (sourceFrame < minT || sourceFrame >= maxT)
						continue;

					transparency = (float) (1 - Math.abs(sourceFrame-currentFrame) / trackDisplayDepth);
					target = model.getTrackModel().getEdgeTarget(edge);
					g2d.setColor(colorGenerator.color(edge));
					drawEdge(g2d, source, target, xcorner, ycorner, mag, transparency);
				}
			}
			break;

		}


		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);


	}

	/* 
	 * PROTECTED METHODS
	 */

	protected void drawEdge(final Graphics2D g2d, final Spot source, final Spot target,
			final int xcorner, final int ycorner, final double magnification, final float transparency) {

		// Find x & y & z in physical coordinates
		final double x0i = source.getFeature(Spot.POSITION_X);
		final double y0i = source.getFeature(Spot.POSITION_Y);
		final double z0i = source.getFeature(Spot.POSITION_Z);
		final double[] physicalPositionSource = new double[] { x0i, y0i, z0i };
		
		final double x1i = target.getFeature(Spot.POSITION_X);
		final double y1i = target.getFeature(Spot.POSITION_Y);
		final double z1i = target.getFeature(Spot.POSITION_Z);
		final double[] physicalPositionTarget = new double[] { x1i, y1i, z1i };
		
		// Find frame
		int sourceFrame = source.getFeature(Spot.FRAME).intValue();
		int targetFrame = target.getFeature(Spot.FRAME).intValue();

		// In pixel units
		final double[] pixelPositionSource = new double[3];
		transforms.get(sourceFrame).apply(physicalPositionSource, pixelPositionSource);
		final double[] pixelPositionTarget = new double[3];
		transforms.get(targetFrame).apply(physicalPositionTarget, pixelPositionTarget);

		// Scale to image zoom
		final double x0s = (pixelPositionSource[0] - xcorner) * magnification ;
		final double y0s = (pixelPositionSource[1] - ycorner) * magnification ;
		final double x1s = (pixelPositionTarget[0] - xcorner) * magnification ;
		final double y1s = (pixelPositionTarget[1] - ycorner) * magnification ;

		// Round
		final int x0 = (int) Math.round(x0s);
		final int y0 = (int) Math.round(y0s);
		final int x1 = (int) Math.round(x1s);
		final int y1 = (int) Math.round(y1s);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
		g2d.drawLine(x0, y0, x1, y1);

	}

	protected void drawEdge(final Graphics2D g2d, final Spot source, final Spot target,
			final int xcorner, final int ycorner, final float magnification) {

		// Find x & y & z in physical coordinates
		final double x0i = source.getFeature(Spot.POSITION_X);
		final double y0i = source.getFeature(Spot.POSITION_Y);
		final double z0i = source.getFeature(Spot.POSITION_Z);
		final double[] physicalPositionSource = new double[] { x0i, y0i, z0i };
		
		final double x1i = target.getFeature(Spot.POSITION_X);
		final double y1i = target.getFeature(Spot.POSITION_Y);
		final double z1i = target.getFeature(Spot.POSITION_Z);
		final double[] physicalPositionTarget = new double[] { x1i, y1i, z1i };

		// Find frame
		int sourceFrame = source.getFeature(Spot.FRAME).intValue();
		int targetFrame = target.getFeature(Spot.FRAME).intValue();
				
		// In pixel units
		final double[] pixelPositionSource = new double[3];
		transforms.get(sourceFrame).apply(physicalPositionSource, pixelPositionSource);
		final double[] pixelPositionTarget = new double[3];
		transforms.get(targetFrame).apply(physicalPositionTarget, pixelPositionTarget);

		// Scale to image zoom
		final double x0s = (pixelPositionSource[0] - xcorner) * magnification ;
		final double y0s = (pixelPositionSource[1] - ycorner) * magnification ;
		final double x1s = (pixelPositionTarget[0] - xcorner) * magnification ;
		final double y1s = (pixelPositionTarget[1] - ycorner) * magnification ;

		// Round
		final int x0 = (int) Math.round(x0s);
		final int y0 = (int) Math.round(y0s);
		final int x1 = (int) Math.round(x1s);
		final int y1 = (int) Math.round(y1s);

		g2d.drawLine(x0, y0, x1, y1);
	}

	public void setTrackColorGenerator(TrackColorGenerator colorGenerator) {
		this.colorGenerator = colorGenerator;
	}
	
	/**
	 * Ignored.
	 */
	@Override
	public void setComposite(Composite composite) {	}

}