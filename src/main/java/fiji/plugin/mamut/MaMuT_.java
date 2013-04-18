package fiji.plugin.mamut;

import ij.IJ;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;

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
import viewer.BrightnessDialog;
import viewer.HelpFrame;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import fiji.plugin.mamut.viewer.ImgPlusSource;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

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

	private final ArrayList< AbstractLinearRange > displayRanges;
	private BrightnessDialog brightnessDialog;
	private MamutViewer viewer;
	private TrackMateModel model;
	private double radius = DEFAULT_RADIUS;
	private final double minRadius;
	/** The spot currently moved under the mouse. */
	private Spot movedSpot = null;

	public MaMuT_() throws ImgIOException, FormatException, IOException {

		/*
		 * Load image
		 */
		
		final String id = "/Users/tinevez/Desktop/Data/Celegans-XY.tif";
		ImagePlus imp = IJ.openImage(id);
		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(imp);

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
		 * Create image source
		 */

		Source<T> source = new ImgPlusSource<T>(img);
		final RealARGBConverter< T > converter = new RealARGBConverter< T >( 0, img.firstElement().getMaxValue() );
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >(1);
		sources.add( new SourceAndConverter< T >(source, converter ));

		/*
		 * Create display range
		 */
		
		displayRanges = new ArrayList< AbstractLinearRange >();
		displayRanges.add( converter );
		
		/*
		 * Create viewer
		 */
		
		viewer = new MamutViewer(800, 600, sources, (int) img.dimension(3), model);
		viewer.render();
		
		/*
		 * Install key & mouse commands
		 */
		
		installKeyBindings();
		installMouseListeners();
	}		
	
	
	/*
	 * PUBLIC METHODS
	 */
	

	@Override
	public void modelChanged(ModelChangeEvent event) {
		// Just ask to repaint the TrackMate overlay
		viewer.refresh();
	}
	

	@Override
	public void setMinMax( final int min, final int max ) {
		for ( final AbstractLinearRange r : displayRanges ) {
			r.setMin( min );
			r.setMax( max );
		}
		viewer.refresh();
	}
	

	public void toggleBrightnessDialog() {
		brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
	}
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Executed at instantiation: install the key bindings for the GUI.
	 */
	private void installKeyBindings() {
		
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
				addSpot();
			}
			private static final long serialVersionUID = 1L;
		});
		
		/*
		 * Delete spot
		 */
		viewer.addKeyAction(deleteSpotKeystroke, new AbstractAction( "delete spot" ) {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				deleteSpot();
			}
			private static final long serialVersionUID = 1L;
		});
		
		/*
		 * Change radius
		 */
		
		viewer.addKeyAction(increaseRadiusKeystroke, new AbstractAction( "increase spot radius" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(1d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(increaseRadiusALotKeystroke, new AbstractAction( "increase spot radius a lot" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(10d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(increaseRadiusABitKeystroke, new AbstractAction( "increase spot radius a bit" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(0.1d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(decreaseRadiusKeystroke, new AbstractAction( "decrease spot radius" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(-1d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(decreaseRadiusALotKeystroke, new AbstractAction( "decrease spot radius a lot" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(-5d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(decreaseRadiusABitKeystroke, new AbstractAction( "decrease spot radius a bit" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(-0.1d); }
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
							String str = String.format(
									"Moved spot " + movedSpot + " to location X = %.1f, Y = %.1f, Z = %.1f.", 
									movedSpot.getFeature(Spot.POSITION_X), movedSpot.getFeature(Spot.POSITION_Y), 
									movedSpot.getFeature(Spot.POSITION_Z));
							viewer.getLogger().log(str);
							movedSpot = null;
						}
					}
				}
			}
			
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == moveSpotKeystroke.getKeyCode()) {
					movedSpot = getSpotWithinRadius();
				}
				
			}
		});
		
		
	}
	
	


	private void installMouseListeners() {
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
			public void mouseClicked(MouseEvent arg0) {
				Spot spot = getSpotWithinRadius();
				if (null != spot) {
					viewer.centerViewOn(spot);
				}
			}
		});
		
	}

	
	
	
	/**
	 * Adds a new spot at the mouse current location.
	 */
	private void addSpot() {
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates(gPos);
		double[] coordinates = new double[3];
		gPos.localize(coordinates);
		Spot spot = new Spot(coordinates);
		spot.putFeature(Spot.RADIUS, radius );
		model.beginUpdate();
		try {
			model.addSpotTo(spot, viewer.getCurrentTimepoint());
		} finally {
			model.endUpdate();
			String str = String.format(
					"Added spot " + spot + " at location X = %.1f, Y = %.1f, Z = %.1f, T = %.0f.", 
					spot.getFeature(Spot.POSITION_X), spot.getFeature(Spot.POSITION_Y), 
					spot.getFeature(Spot.POSITION_Z), spot.getFeature(Spot.FRAME));
			viewer.getLogger().log(str);
		}
	}
	
	/**
	 * Adds a new spot at the mouse current location.
	 */
	private void deleteSpot() {
		Spot spot = getSpotWithinRadius(); 
		if (null != spot) {
			// We can delete it
			model.beginUpdate();
			try {
				int frame = viewer.getCurrentTimepoint();
				model.removeSpotFrom(spot, frame);
			} finally {
				model.endUpdate();
				String str = "Removed spot " + spot + "."; 
				viewer.getLogger().log(str);
			}
		}
		
	}
	
	private void increaseSpotRadius(double factor) {
		Spot spot = getSpotWithinRadius();
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
		}
	}
	
	
	private void showHelp() {
		new HelpFrame();
	}

	
	/**
	 * Returns the closest {@link Spot} with respect to the current mouse location, and
	 * for which the current location is within its radius, or <code>null</code> if there is no such spot.
	 * In other words: returns the spot in which the mouse pointer is. 
	 * @return  the closest spot within radius.
	 */
	private Spot getSpotWithinRadius() {
		/*
		 * Get the closest spot
		 */
		int frame = viewer.getCurrentTimepoint();
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates(gPos);
		double[] coordinates = new double[3];
		gPos.localize(coordinates);
		Spot location = new Spot(coordinates);
		Spot closestSpot = model.getFilteredSpots().getClosestSpot(location, frame);
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
	
	
	
	
	/*
	 * MAIN METHOD
	 */
	
	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgIOException, FormatException, IOException {
		new MaMuT_<T>();
	}
	



}
