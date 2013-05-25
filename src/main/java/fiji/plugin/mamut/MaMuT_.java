package fiji.plugin.mamut;

import ij.IJ;
import ij.ImagePlus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import loci.formats.FormatException;
import net.imglib2.RealPoint;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.io.ImgIOException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jfree.chart.renderer.InterpolatePaintScale;

import viewer.BrightnessDialog;
import viewer.HelpFrame;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import fiji.plugin.mamut.viewer.ImgPlusSource;
import fiji.plugin.mamut.viewer.MamutOverlay;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class MaMuT_ <T extends RealType<T> & NativeType<T>> implements BrightnessDialog.MinMaxListener, ModelChangeListener {

	public static final String PLUGIN_NAME = "MaMuT";
	public static final String PLUGIN_VERSION = "v0.5.0";
	private static final double DEFAULT_RADIUS = 1;
	/** By how portion of the current radius we change this radius for every
	 * change request.	 */
	private static final double RADIUS_CHANGE_FACTOR = 0.1;

	private static final int CHANGE_A_LOT_KEY = KeyEvent.SHIFT_DOWN_MASK;
	private static final int CHANGE_A_BIT_KEY = KeyEvent.CTRL_DOWN_MASK;

	private KeyStroke brightnessKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_C, 0 );
	private KeyStroke helpKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );
	private KeyStroke addSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_A, 0 );
	private KeyStroke deleteSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_D, 0 );
	private KeyStroke moveSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_SPACE, 0 );

	private int increaseRadiusKey = KeyEvent.VK_E;
	private int decreaseRadiusKey = KeyEvent.VK_Q;
	private KeyStroke increaseRadiusKeystroke = KeyStroke.getKeyStroke( increaseRadiusKey, 0 );
	private KeyStroke decreaseRadiusKeystroke = KeyStroke.getKeyStroke( decreaseRadiusKey, 0 );
	private KeyStroke increaseRadiusALotKeystroke = KeyStroke.getKeyStroke( increaseRadiusKey, CHANGE_A_LOT_KEY );
	private KeyStroke decreaseRadiusALotKeystroke = KeyStroke.getKeyStroke( decreaseRadiusKey, CHANGE_A_LOT_KEY );
	private KeyStroke increaseRadiusABitKeystroke = KeyStroke.getKeyStroke( increaseRadiusKey, CHANGE_A_BIT_KEY );
	private KeyStroke decreaseRadiusABitKeystroke = KeyStroke.getKeyStroke( decreaseRadiusKey, CHANGE_A_BIT_KEY );
	private KeyStroke toggleLinkingModeKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_L, 0);

	private final ArrayList< AbstractLinearRange > displayRanges;
	private BrightnessDialog brightnessDialog;
	/** The {@link MamutViewer}s managed by this plugin. */
	private Collection<MamutViewer> viewers = new ArrayList<MamutViewer>();
	/** The model shown and edited by this plugin. */
	private TrackMateModel model;
	/** The next created spot will be set with this radius. */
	private double radius = DEFAULT_RADIUS;
	/** The radius below which a spot cannot go. */
	private final double minRadius;
	/** The spot currently moved under the mouse. */
	private Spot movedSpot = null;
	/** The image data sources to be displayed in the views. */
	private final List<SourceAndConverter<?>> sources;
	/** The number of timepoints in the image sources. */
	private final int nTimepoints;
	/**  If true, the next added spot will be automatically linked to the previously created one, given that 
	 * the new spot is created in a subsequent frame. */
	private boolean isLinkingMode = false;
	/** The color map for painting the spots. It is centralized here and is used in the 
	 * {@link MamutOverlay}s.  */
	private final Map<Spot, Color> spotColorProvider;
	private TrackColorGenerator trackColorProvider;
	private Settings settings;
	private SelectionModel selectionModel;

	public MaMuT_() throws ImgIOException, FormatException, IOException {

		/*
		 * Load image
		 */

		final String id = "E:/Users/JeanYves/Desktop/Data/Celegans.tif";// "/Users/tinevez/Desktop/Data/Celegans-XY.tif";
		ImagePlus imp = IJ.openImage(id);
		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(imp);
		nTimepoints = (int) img.dimension(3);

		/*
		 * Find adequate rough scales
		 */

		minRadius = 2 * Math.min(img.calibration(0), img.calibration(1));

		/*
		 * Instantiate model
		 */

		model = new TrackMateModel();
		model.addTrackMateModelChangeListener(this);

		/*
		 * Settings
		 */

		settings = new Settings();
		settings.setFrom(imp);
		settings.addTrackAnalyzer(new TrackIndexAnalyzer(model)); // we need at least this one

		/*
		 * Autoupdate features
		 */

		new ModelFeatureUpdater(model, settings);

		/*
		 * Selection model
		 */

		selectionModel = new SelectionModel(model);

		/*
		 * Create image source
		 */

		final Source<T> source = new ImgPlusSource<T>(img);
		final RealARGBConverter< T > converter = new RealARGBConverter< T >( 0, img.firstElement().getMaxValue() );
		sources = new ArrayList< SourceAndConverter< ? > >(1);
		sources.add( new SourceAndConverter< T >(source, converter ));

		/*
		 * Create display range
		 */

		displayRanges = new ArrayList< AbstractLinearRange >();
		displayRanges.add( converter );

		/*
		 * Color provider
		 */

		spotColorProvider = new HashMap<Spot, Color>();
		trackColorProvider = new PerTrackFeatureColorGenerator(model, TrackIndexAnalyzer.TRACK_ID);

		/*
		 * Create views
		 */

		newViewer();
		//		newViewer();

	}		


	/*
	 * PUBLIC METHODS
	 */

	public MamutViewer newViewer() {
		final MamutViewer viewer = new MamutViewer(800, 600, sources, nTimepoints, model, selectionModel, 
				spotColorProvider, trackColorProvider);
		installKeyBindings(viewer);
		installMouseListeners(viewer);
		//		viewer.addHandler(viewer);
		viewer.render();
		viewers.add(viewer);

		viewer.getFrame().addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent arg0) { }
			@Override
			public void windowIconified(WindowEvent arg0) { }
			@Override
			public void windowDeiconified(WindowEvent arg0) { }

			@Override
			public void windowDeactivated(WindowEvent arg0) { }

			@Override
			public void windowClosing(WindowEvent arg0) { }

			@Override
			public void windowClosed(WindowEvent arg0) {
				viewers.remove(viewer);
			}

			@Override
			public void windowActivated(WindowEvent arg0) { }
		});

		return viewer;
	}


	@Override
	public void modelChanged(ModelChangeEvent event) {
		refresh();
	}


	@Override
	public void setMinMax( final int min, final int max ) {
		for ( final AbstractLinearRange r : displayRanges ) {
			r.setMin( min );
			r.setMax( max );
		}
		refresh();
	}


	public void toggleBrightnessDialog() {
		brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
	}


	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Configures the specified {@link MamutViewer} with key bindings.
	 * @param the {@link MamutViewer} to configure.
	 */
	private void installKeyBindings(final MamutViewer viewer) {

		/*
		 *  Help window
		 */
		viewer.addKeyAction( helpKeystroke, new AbstractAction( "help" ) {
			@Override
			public void actionPerformed( final ActionEvent arg0 ) {
				showHelp();
			}

			private static final long serialVersionUID = 1L;
		} );

		/*
		 *  Brightness dialog
		 */
		viewer.addKeyAction( brightnessKeystroke, new AbstractAction( "brightness settings" ) {
			@Override
			public void actionPerformed( final ActionEvent arg0 ) {
				toggleBrightnessDialog();
			}

			private static final long serialVersionUID = 1L;
		} );
		brightnessDialog = new BrightnessDialog( viewer.getFrame() );
		viewer.installKeyActions( brightnessDialog );
		brightnessDialog.setListener( this );

		/*
		 *  Add spot
		 */
		viewer.addKeyAction(addSpotKeystroke, new AbstractAction( "add spot" ) {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				addSpot(viewer);
			}
			private static final long serialVersionUID = 1L;
		});

		/*
		 * Delete spot
		 */
		viewer.addKeyAction(deleteSpotKeystroke, new AbstractAction( "delete spot" ) {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				deleteSpot(viewer);
			}
			private static final long serialVersionUID = 1L;
		});

		/*
		 * Change radius
		 */

		viewer.addKeyAction(increaseRadiusKeystroke, new AbstractAction( "increase spot radius" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, 1d); }
			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(increaseRadiusALotKeystroke, new AbstractAction( "increase spot radius a lot" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, 10d); }
			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(increaseRadiusABitKeystroke, new AbstractAction( "increase spot radius a bit" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, 0.1d); }
			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(decreaseRadiusKeystroke, new AbstractAction( "decrease spot radius" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, -1d); }
			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(decreaseRadiusALotKeystroke, new AbstractAction( "decrease spot radius a lot" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, -5d); }
			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(decreaseRadiusABitKeystroke, new AbstractAction( "decrease spot radius a bit" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, -0.1d); }
			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(decreaseRadiusABitKeystroke, new AbstractAction( "decrease spot radius a bit" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, -0.1d); }
			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(toggleLinkingModeKeystroke, new AbstractAction( "toggle linking mode" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { toggleLinkingMode(viewer); }
			private static final long serialVersionUID = 1L;
		});


		/*
		 * Custom key presses
		 */

		viewer.addHandler(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent event) { }

			@Override
			public void keyReleased(KeyEvent event) {
				if (event.getKeyCode() == moveSpotKeystroke.getKeyCode()) {
					if (null != movedSpot) {
						model.beginUpdate();
						try {
							model.updateFeatures(movedSpot);
						} finally {
							model.endUpdate();
							String str = String.format(
									"Moved spot " + movedSpot + " to location X = %.1f, Y = %.1f, Z = %.1f.", 
									movedSpot.getFeature(Spot.POSITION_X), movedSpot.getFeature(Spot.POSITION_Y), 
									movedSpot.getFeature(Spot.POSITION_Z));
							viewer.getLogger().log(str);
							movedSpot = null;
						}
						refresh();
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == moveSpotKeystroke.getKeyCode()) {
					movedSpot = getSpotWithinRadius(viewer);
				}

			}
		});


	}



	/**
	 * Configures the specified {@link MamutViewer} with mouse listeners. 
	 * @param viewer  the {@link MamutViewer} to configure.
	 */
	private void installMouseListeners(final MamutViewer viewer) {
		viewer.addHandler(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent arg0) {
				if (null != movedSpot) {
					final RealPoint gPos = new RealPoint( 3 );
					viewer.getGlobalMouseCoordinates(gPos);
					double[] coordinates = new double[3];
					gPos.localize(coordinates);
					movedSpot.putFeature(Spot.POSITION_X, coordinates[0]);
					movedSpot.putFeature(Spot.POSITION_Y, coordinates[1]);
					movedSpot.putFeature(Spot.POSITION_Z, coordinates[2]);
				}

			}

			@Override
			public void mouseDragged(MouseEvent arg0) { }
		});


		viewer.addHandler(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent arg0) { }

			@Override
			public void mousePressed(MouseEvent arg0) { }

			@Override
			public void mouseExited(MouseEvent arg0) {}

			@Override
			public void mouseEntered(MouseEvent arg0) { }

			@Override
			public void mouseClicked(MouseEvent event) {

				if (event.getClickCount() < 2) {

					
					Spot spot = getSpotWithinRadius(viewer);
					int currentTimePoint = viewer.getCurrentTimepoint();
					if (null != spot) {
						// Center view on it
						centerOnSpot(spot, currentTimePoint);
						if (!event.isShiftDown()) {
							// Replace selection
							selectionModel.clearSpotSelection();
						}
						// Toggle it to selection
						if (selectionModel.getSpotSelection().contains(spot)) {
							selectionModel.removeSpotFromSelection(spot);
						} else {
							selectionModel.addSpotToSelection(spot);
						}
						
					} else {
						// Clear selection
						selectionModel.clearSelection();
					}

				} else {

					
				}
				refresh();
			}
		});

	}



	private void refresh() {
		// Just ask to repaint the TrackMate overlay
		for (MamutViewer viewer : viewers) {
			viewer.refresh();
		}
	}

	private void centerOnSpot(Spot spot, int currentTimePoint) {
		for (MamutViewer otherView : viewers) {
			otherView.centerViewOn(spot);
		}
	}

	/**
	 * Adds a new spot at the mouse current location.
	 * @param viewer  the viewer in which the add spot request was made.
	 */
	private void addSpot(final MamutViewer viewer) {

		// Check if the mouse is not off-screen
		Point mouseScreenLocation = MouseInfo.getPointerInfo().getLocation();
		Point viewerPosition = viewer.getFrame().getLocationOnScreen();
		Dimension viewerSize = viewer.getFrame().getSize();
		if (mouseScreenLocation.x < viewerPosition.x ||
				mouseScreenLocation.y < viewerPosition.y ||
				mouseScreenLocation.x > viewerPosition.x + viewerSize.width ||
				mouseScreenLocation.y > viewerPosition.y + viewerSize.height ) {
			return;
		}

		int frame = viewer.getCurrentTimepoint();

		// Ok, then create this spot, wherever it is.
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates(gPos);
		double[] coordinates = new double[3];
		gPos.localize(coordinates);
		Spot spot = new Spot(coordinates);
		spot.putFeature(Spot.RADIUS, radius );
		spot.putFeature(Spot.POSITION_T, Double.valueOf(frame) );
		model.beginUpdate();
		try {
			model.addSpotTo(spot, frame);
		} finally {
			model.endUpdate();
		}

		String message = String.format(
				"Added spot " + spot + " at location X = %.1f, Y = %.1f, Z = %.1f, T = %.0f", 
				spot.getFeature(Spot.POSITION_X), spot.getFeature(Spot.POSITION_Y), 
				spot.getFeature(Spot.POSITION_Z), spot.getFeature(Spot.FRAME));

		// Then, possibly, the edge. We must do it in a subsequent update, otherwise the model gets confused.
		final Set<Spot> spotSelection = selectionModel.getSpotSelection();
		if (isLinkingMode && spotSelection.size() == 1) { // if we are in the right mode & if there is only one spot in selection
			Spot targetSpot = spotSelection.iterator().next();
			if (targetSpot.getFeature(Spot.FRAME).intValue() < spot.getFeature(Spot.FRAME).intValue()) { // & if they are on different frames
				model.beginUpdate();
				try {

					// Create link
					model.addEdge(targetSpot, spot, -1);
				} finally {
					model.endUpdate();
				}
				message += ", linked to spot " + targetSpot + ".";
			} else {
				message += ".";
			}
		} else {
			message += ".";
		}
		viewer.getLogger().log(message);

		// Store new spot as the sole selection for this model
		selectionModel.clearSpotSelection();
		selectionModel.addSpotToSelection(spot);
	}

	/**
	 * Adds a new spot at the mouse current location.
	 * @param viewer  the viewer in which the delete spot request was made.
	 */
	private void deleteSpot(final MamutViewer viewer) {
		Spot spot = getSpotWithinRadius(viewer); 
		if (null != spot) {
			// We can delete it
			model.beginUpdate();
			try {
				model.removeSpot(spot);
			} finally {
				model.endUpdate();
				String str = "Removed spot " + spot + "."; 
				viewer.getLogger().log(str);
			}
		}

	}

	/**
	 * Increases (or decreases) the neighbor spot radius. 
	 * @param viewer  the viewer in which the change radius was made. 
	 * @param factor  the factor by which to change the radius. Negative value are used
	 * to decrease the radius.
	 */
	private void increaseSpotRadius(final MamutViewer viewer, double factor) {
		Spot spot = getSpotWithinRadius(viewer);
		if (null != spot) {
			// Change the spot radius
			double rad = spot.getFeature(Spot.RADIUS);
			rad += factor * RADIUS_CHANGE_FACTOR * rad;

			if (rad < minRadius) {
				return;
			}

			radius = rad;
			spot.putFeature(Spot.RADIUS, rad);
			// Mark the spot for model update;
			model.beginUpdate();
			try {
				model.updateFeatures(spot);
			} finally {
				model.endUpdate();
				String str = String.format(
						"Changed spot " + spot + " radius to R = %.1f.", 
						spot.getFeature(Spot.RADIUS));
				viewer.getLogger().log(str);
			}
			refresh();
		}
	}


	private void showHelp() {
		new HelpFrame(MaMuT_.class.getResource("Help.html"));
	}


	/**
	 * Returns the closest {@link Spot} with respect to the current mouse location, and
	 * for which the current location is within its radius, or <code>null</code> if there is no such spot.
	 * In other words: returns the spot in which the mouse pointer is.
	 * @param viewer  the viewer to inspect. 
	 * @return  the closest spot within radius.
	 */
	private Spot getSpotWithinRadius(final MamutViewer viewer) {
		/*
		 * Get the closest spot
		 */
		int frame = viewer.getCurrentTimepoint();
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates(gPos);
		double[] coordinates = new double[3];
		gPos.localize(coordinates);
		Spot location = new Spot(coordinates);
		Spot closestSpot = model.getSpots().getClosestSpot(location, frame, true);
		if (null == closestSpot) {
			return null;
		}
		/*
		 * Determine if we are inside the spot
		 */
		double d2 = closestSpot.squareDistanceTo(location);
		double r = closestSpot.getFeature(Spot.RADIUS);
		if (d2 < r*r) {
			return closestSpot;
		} else {
			return null;
		}

	}


	private void computeSpotColors(final String feature) {
		spotColorProvider.clear();
		// Check null
		if (null == feature) {
			for(Spot spot : model.getSpots().iterable(false)) {
				spotColorProvider.put(spot, TrackMateModelView.DEFAULT_COLOR);
			}
			return;
		}

		// Get min & max
		double min = Float.POSITIVE_INFINITY;
		double max = Float.NEGATIVE_INFINITY;
		Double val;
		for (int ikey : model.getSpots().keySet()) {
			for (Spot spot : model.getSpots().iterable(ikey, false)) {
				val = spot.getFeature(feature);
				if (null == val)
					continue;
				if (val > max) max = val;
				if (val < min) min = val;
			}
		}

		for(Spot spot : model.getSpots().iterable(false)) {
			val = spot.getFeature(feature);
			InterpolatePaintScale  colorMap = InterpolatePaintScale.Jet;
			if (null == feature || null == val)
				spotColorProvider.put(spot, TrackMateModelView.DEFAULT_COLOR);
			else
				spotColorProvider.put(spot, colorMap .getPaint((val-min)/(max-min)) );
		}
	}

	private void toggleLinkingMode(MamutViewer viewer) {
		this.isLinkingMode = !isLinkingMode;
		String str = "Switched auto-linking mode " +  (isLinkingMode ? "on." : "off.");
		viewer.getLogger().log(str);
	}


	/*
	 * MAIN METHOD
	 */

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgIOException, FormatException, IOException {
		new MaMuT_<T>();
	}

}
