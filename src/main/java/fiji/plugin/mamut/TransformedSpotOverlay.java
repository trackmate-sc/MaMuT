package fiji.plugin.mamut;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

/**
 * The overlay class in charge of drawing the spot images on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class TransformedSpotOverlay implements Overlay {

	private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 12);
	private static final boolean DEBUG = false;

	/** The color mapping of the target collection. */
	protected Map<Spot, Color> targetColor;
	protected final ImagePlus imp;
	protected Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	protected FontMetrics fm;
	protected Map<String, Object> displaySettings;
	protected final TrackMateModel model;
	protected final List<AffineTransform3D> transforms;
	private final double scalingY;
	private final double scalingX;

	/*
	 * CONSTRUCTOR
	 */

	public TransformedSpotOverlay(final TrackMateModel model, final ImagePlus imp, final List<AffineTransform3D> transformList, final Map<String, Object> displaySettings) {
		this.model = model;
		this.imp = imp;
		this.transforms = transformList;
		this.displaySettings = displaySettings;
		// Here we cheat: to get the scaling of the curcke radius, we sue the imp physical calibration, and
		// not the transform
		this.scalingX = 1 / imp.getCalibration().pixelWidth;
		this.scalingY = 1 / imp.getCalibration().pixelHeight;
		computeSpotColors();
	}

	/*
	 * METHODS
	 */

	
	@Override
	public void paint(Graphics g, int xcorner, int ycorner, double magnification) {

		boolean spotVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_SPOTS_VISIBLE);
		if (!spotVisible  || null == model.getFilteredSpots())
			return;

		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();
		final Font originalFont = g2d.getFont();
		
		g2d.setComposite(composite);
		g2d.setFont(LABEL_FONT);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		fm = g2d.getFontMetrics();
		
		final int frame = imp.getFrame()-1;
		final double mag = (double) magnification;
		final Set<Spot> spotSelection = model.getSelectionModel().getSpotSelection();
		
		// Transform back to get physical coordinates of the currently viewed z-slice
		final int pixelCoordsZ = imp.getSlice()-1;

		// Deal with normal spots.
		g2d.setStroke(new BasicStroke(1.0f));
		Color color;
		final SpotCollection target = model.getFilteredSpots();
		List<Spot> spots = target.get(frame);
		if (null != spots) { 
			for (Spot spot : spots) {

				if (spotSelection  != null && spotSelection.contains(spot))
					continue;

				color = targetColor.get(spot);
				if (null == color)
					color = AbstractTrackMateModelView.DEFAULT_COLOR;
				g2d.setColor(color);
				drawSpot(g2d, spot, pixelCoordsZ, xcorner, ycorner, mag, frame);

			}
		}

		// Deal with spot selection
		if (null != spotSelection) {
			g2d.setStroke(new BasicStroke(2.0f));
			g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
			Integer sFrame;
			for(Spot spot : spotSelection) {
				sFrame = target.getFrame(spot);
				if (DEBUG)
					System.out.println("[TransformedSpotOverlay] For spot "+spot+" in selection, found frame "+sFrame);
				if (null == sFrame || sFrame != frame)
					continue;
				drawSpot(g2d, spot, pixelCoordsZ, xcorner, ycorner, mag, frame);
			}
		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);
		g2d.setFont(originalFont);
	}
	
	public void computeSpotColors() {
		final String feature = (String) displaySettings.get(TrackMateModelView.KEY_SPOT_COLOR_FEATURE);
		targetColor = new HashMap<Spot, Color>( model.getSpots().getNSpots());
		
		// Simple case
		if (null == feature) {
			for(Spot spot : model.getSpots()) {
				targetColor.put(spot, TrackMateModelView.DEFAULT_COLOR);
			}
			return;
		}
		
		// Feature based. Get min & max
		double min = Float.POSITIVE_INFINITY;
		double max = Float.NEGATIVE_INFINITY;
		Double val;
		for (int ikey : model.getSpots().keySet()) {
			for (Spot spot : model.getSpots().get(ikey)) {
				val = spot.getFeature(feature);
				if (null == val)
					continue;
				if (val > max) max = val;
				if (val < min) min = val;
			}
		}
		for(Spot spot : model.getSpots()) {
			val = spot.getFeature(feature);
			InterpolatePaintScale  colorMap = (InterpolatePaintScale) displaySettings.get(TrackMateModelView.KEY_COLORMAP);
			if (null == val)
				targetColor.put(spot, TrackMateModelView.DEFAULT_COLOR);
			else
				targetColor.put(spot, colorMap .getPaint((val-min)/(max-min)) );
		}
	}
	
	@Override
	public void setComposite(Composite composite) {
		this.composite = composite;
	}

	protected void drawSpot(final Graphics2D g2d, final Spot spot, final int pixelZSlice, final int xcorner, final int ycorner, final double magnification, final int frame) {
		
		// Absolute location
		final double xp = spot.getFeature(Spot.POSITION_X);
		final double yp = spot.getFeature(Spot.POSITION_Y);
		final double zp = spot.getFeature(Spot.POSITION_Z);
		final double[] physicalCoords = new double[] { xp, yp, zp };
		
		// Transform location
		final double[] pixelCoords = new double[3];
		transforms.get(frame).apply(physicalCoords , pixelCoords);
		final double x = pixelCoords[0];
		final double y = pixelCoords[1];
		
		// Transform current view
		final double[] physicalViewCoords = new double[3];
		transforms.get(frame).applyInverse(physicalViewCoords, new double[] { x, y, pixelZSlice });
		Spot viewSpot = new Spot(physicalViewCoords);
		final double physicalDz2 = viewSpot.squareDistanceTo(spot);

		// Physical radius, stretched according to display settings
		final float radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
		final float physicalRadius = spot.getFeature(Spot.RADIUS).floatValue() * radiusRatio ;

		// Scale to image zoom
		final double xs = (x - xcorner) * magnification ;
		final double ys = (y - ycorner) * magnification ;
		
		if (physicalDz2 >= physicalRadius * physicalRadius) {
			
			g2d.fillOval((int) Math.round(xs - 2*magnification), (int) Math.round(ys - 2*magnification), 
						 (int) Math.round(4*magnification), 	(int) Math.round(4*magnification));
		
		} else {
			
			// Transform radius
			final double apparentRadius = Math.sqrt(physicalRadius * physicalRadius - physicalDz2);
			final double pixelRadiusX = apparentRadius * scalingX * magnification; 
			final double pixelRadiusY = apparentRadius * scalingY * magnification;  

			// Debug
			if (DEBUG) {
				System.out.println("[TransformedSpotOverlay] In "+imp.getShortTitle()+", drawing spot "+spot.getName()+" with phys. coords. " + 
						Util.printCoordinates(physicalCoords) + " at pixel "+Util.printCoordinates(pixelCoords)+" with radius = "+pixelRadiusX+"x"+pixelRadiusY);
			}

			// Draw circle
			g2d.drawOval(
					(int) Math.round(xs - pixelRadiusX), 
					(int) Math.round(ys - pixelRadiusY), 
					(int) Math.round(2 * pixelRadiusX), 
					(int) Math.round(2 * pixelRadiusY));
			
			// Draw name
			boolean spotNameVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES);
			if (spotNameVisible ) {
				String str = spot.toString();
				int xindent = fm.stringWidth(str) / 2;
				int yindent = fm.getAscent() / 2;
				g2d.drawString(spot.toString(), (int) xs-xindent, (int) ys+yindent);
			}
		}
	}
	

	


}