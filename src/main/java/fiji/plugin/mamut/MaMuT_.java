package fiji.plugin.mamut;

import ij.IJ;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class MaMuT_ <T extends RealType<T> & NativeType<T>> implements BrightnessDialog.MinMaxListener {

	public static final String PLUGIN_NAME = "MaMuT";
	public static final String PLUGIN_VERSION = "v0.5.0";
	private static final double DEFAULT_RADIUS = 1;
	
	private KeyStroke brightnessKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_C, 0 );
	private KeyStroke helpKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );
	private KeyStroke addSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_A, 0 );
	private KeyStroke deleteSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_D, 0 );
	private KeyStroke increaseRadiusKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_E, 0 );
	private KeyStroke decreaseRadiusKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_Q, 0 );

	private final ArrayList< AbstractLinearRange > displayRanges;
	private BrightnessDialog brightnessDialog;
	private MamutViewer viewer;
	private TrackMateModel model;
	private double radius = DEFAULT_RADIUS;


	public MaMuT_() throws ImgIOException, FormatException, IOException {

		final String id = "/Users/tinevez/Desktop/Data/Celegans-XY.tif";
		ImagePlus imp = IJ.openImage(id);
		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(imp);
		
//		final ImgPlus<T> img = ImgOpener.open(id); // does not work I don't know why.
		
		model = new TrackMateModel();
		
		Source<T> source = new ImgPlusSource<T>(img);

		final RealARGBConverter< T > converter = new RealARGBConverter< T >( 0, img.firstElement().getMaxValue() );
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >(1);
		sources.add( new SourceAndConverter< T >(source, converter ));
		
		displayRanges = new ArrayList< AbstractLinearRange >();
		displayRanges.add( converter );
		
		viewer = new MamutViewer(800, 600, sources, (int) img.dimension(3), model);
		
		installKeyBindings();
		
		
		
		
		
		
	}
	
	private void installKeyBindings() {
		
		// Help window
		viewer.addKeyAction( helpKeystroke, new AbstractAction( "help" ) {
			@Override
			public void actionPerformed( final ActionEvent arg0 ) {
				showHelp();
			}

			private static final long serialVersionUID = 1L;
		} );

		// Brightness dialog
		viewer.addKeyAction( brightnessKeystroke, new AbstractAction( "brightness settings" ) {
			@Override
			public void actionPerformed( final ActionEvent arg0 ) {
				toggleBrightnessDialog();
			}
			
			private static final long serialVersionUID = 1L;
		} );
		brightnessDialog = new BrightnessDialog( viewer.getJFrame() );
		viewer.installKeyActions( brightnessDialog );
		brightnessDialog.setListener( this );

		// Add cell
		viewer.addKeyAction(addSpotKeystroke, new AbstractAction( "add spot" ) {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addSpot();
			}
			private static final long serialVersionUID = 1L;
		});
		
		
	}
	
	private void addSpot() {
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates(gPos);
		double[] coordinates = new double[3];
		gPos.localize(coordinates);
		Spot spot = new Spot(coordinates);
		spot.putFeature(Spot.RADIUS, radius );
		model.beginUpdate();
		try {
			model.addSpotTo(spot, viewer.getViewerState().getCurrentTimepoint());
		} finally {
			model.endUpdate();
		}
		
	}
	
	
	private void showHelp() {
		new HelpFrame();
	}

	@Override
	public void setMinMax( final int min, final int max ) {
		for ( final AbstractLinearRange r : displayRanges ) {
			r.setMin( min );
			r.setMax( max );
		}
		viewer.requestRepaint();
	}

	public void toggleBrightnessDialog() {
		brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
	}
	
	/*
	 * MAIN METHOD
	 */
	
	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgIOException, FormatException, IOException {
		new MaMuT_<T>();
	}
	



}
