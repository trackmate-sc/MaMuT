/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2022 MaMuT development team.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.mamut;

import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;

import java.awt.Checkbox;
import java.awt.Font;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import fiji.plugin.mamut.io.MamutXmlWriter;
import fiji.plugin.mamut.io.TGMMImporter2;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.util.gui.GenericDialogPlus;
import ij.plugin.PlugIn;
import ij.text.TextWindow;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

public class ImportTGMMAnnotationPlugin implements PlugIn
{

	private static final Font BIG_FONT = new Font( "Arial", Font.BOLD, 16 );
	
	private static final URL AMAT_PAPER_LINK;
	static
	{
		URL temp;
		try
		{
			temp = new URL( "http://www.nature.com/nmeth/journal/v11/n9/full/nmeth.3036.html" );
		}
		catch ( final java.net.MalformedURLException e )
		{
			temp = null;
		}
		AMAT_PAPER_LINK = temp;
	}

	private static final String HELP_MESSAGE = "<html>"
			+ "This plugin creates a MaMuT file from a BigDataViewer "
			+ "XML/HDF5 image and a folder containing the file generated "
			+ "by the TGMM algorithm. "
			+ "<p>"
			+ "See the paper from Fernando Amat and colleagues to generate "
			+ "these annotations: <br>"
			+ "<a href=\"" + AMAT_PAPER_LINK.toString() + "\">"
			+ "Fast, accurate reconstruction of cell lineages from large-scale fluorescence microscopy data</a>."
			+ "<p>"
			+ "This plugin requires two inputs and one file input:"
			+ "<ul>"
			+ "	<li> First you need to specify where is the image data BLABLA."
			+ "</html>";

	private static final String ANGLE_HELP_MESSAGE = "<html>Patati, patata.</html>";

	private Logger logger = Logger.DEFAULT_LOGGER;

	private static int defaultTTo = 99;

	private static int defaultTFrom = 0;

	private static double defaultZTo = 511;

	private static double defaultZFrom = 0;

	private static double defaultYTo = 511;

	private static double defaultYFrom = 0;

	private static double defaultXTo = 511;

	private static double defaultXFrom = 0;

	private static boolean defaultDoCrop = false;

	private static String defaultOutputPath;

	private static String defaultTGMMPath;

	private static String defaultXmlHDF5Path;

	@Override
	public void run( final String arg )
	{
		showDialog();
	}

	private void showDialog()
	{
		logger = Logger.IJ_LOGGER;
		final GenericDialogPlus dialog = new GenericDialogPlus( PLUGIN_NAME + " v" + PLUGIN_VERSION );

		dialog.addMessage( "Import TGMM annotations", BIG_FONT );
		dialog.addImage( MaMuT.MAMUT_ICON );

		if ( null == defaultXmlHDF5Path )
		{
			final File folder = new File( System.getProperty( "user.dir" ) );
			final File parent = folder.getParentFile();
			defaultXmlHDF5Path = parent == null ? null : parent.getParentFile().getAbsolutePath();
		}
		dialog.addMessage( "Select the image data (XML of the xml/hdf5 couple)." );
		dialog.addFileField( "Image data", defaultXmlHDF5Path, 30 );

		if ( null == defaultTGMMPath )
		{
			final File folder = new File( System.getProperty( "user.dir" ) );
			final File parent = folder.getParentFile();
			defaultTGMMPath = parent == null ? null : parent.getParentFile().getAbsolutePath();
		}
		dialog.addMessage( "Select the TGMM annotation folder." );
		dialog.addDirectoryField( "TGMM folder", defaultTGMMPath, 30 );

		if ( null == defaultOutputPath )
		{
			final File folder = new File( System.getProperty( "user.dir" ) );
			final File parent = folder.getParentFile();
			defaultOutputPath = parent == null ? null : parent.getParentFile().getAbsolutePath();
		}
		dialog.addMessage( "Output to file:" );
		dialog.addFileField( "MaMuT file", defaultOutputPath, 30 );

		/*
		 * Interval controls
		 */

		dialog.addCheckbox( "Crop on import", defaultDoCrop );
		final Checkbox checkbox = ( Checkbox ) dialog.getCheckboxes().get( 0 );

		dialog.addNumericField( "X from", defaultXFrom, 1 );
		dialog.addNumericField( "X to", defaultXTo, 1 );
		dialog.addNumericField( "Y from", defaultYFrom, 1 );
		dialog.addNumericField( "Y to", defaultYTo, 1 );
		dialog.addNumericField( "Z from", defaultZFrom, 1 );
		dialog.addNumericField( "Z to", defaultZTo, 1 );
		dialog.addNumericField( "T from", defaultTFrom, 0 );
		dialog.addNumericField( "T to", defaultTTo, 0 );

		checkbox.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( final ItemEvent arg0 )
			{
				for ( final Object o : dialog.getNumericFields() )
				{
					final TextField tf = ( TextField ) o;
					tf.setEnabled( checkbox.getState() );
				}
			}
		} );
		for ( final Object o : dialog.getNumericFields() )
		{
			final TextField tf = ( TextField ) o;
			tf.setEnabled( checkbox.getState() );
		}

		dialog.addHelp( HELP_MESSAGE );

		dialog.showDialog();

		/*
		 * Process inputs
		 */

		if ( dialog.wasCanceled() ) { return; }

		final String xmlHDF5Path = dialog.getNextString();
		final String tgmmPath = dialog.getNextString();
		final String outputPath = dialog.getNextString();
		final boolean doCrop = dialog.getNextBoolean();
		final RealInterval interval;
		int tFrom = 0;
		int tTo = Integer.MAX_VALUE;
		if ( doCrop )
		{
			final double xfrom = dialog.getNextNumber();
			final double xto = dialog.getNextNumber();
			final double yfrom = dialog.getNextNumber();
			final double yto = dialog.getNextNumber();
			final double zfrom = dialog.getNextNumber();
			final double zto = dialog.getNextNumber();
			tFrom = ( int ) dialog.getNextNumber();
			tTo = ( int ) dialog.getNextNumber();
			final double[] min = new double[] { xfrom, yfrom, zfrom };
			final double[] max = new double[] { xto, yto, zto };
			interval = new FinalRealInterval( min, max );
			defaultXFrom = xfrom;
			defaultXTo = xto;
			defaultYFrom = yfrom;
			defaultYTo = yto;
			defaultZFrom = zfrom;
			defaultZTo = zto;
			defaultTFrom = tFrom;
			defaultTTo = tTo;
		}
		else
		{
			interval = null;
		}

		/*
		 * Copy to default
		 */

		defaultXmlHDF5Path = xmlHDF5Path;
		defaultTGMMPath = tgmmPath;
		defaultOutputPath = outputPath;
		defaultDoCrop = doCrop;

		/*
		 * Ask for a view setup
		 */

		SpimDataMinimal spimData;
		try
		{
			spimData = new XmlIoSpimDataMinimal().load( xmlHDF5Path );
		}
		catch ( final SpimDataException e )
		{
			logger.error( "Problem reading the transforms in image data file:\n" + e.getMessage() + "\n" );
			return;
		}

		/*
		 * Read view setup
		 */

		final List< ? extends BasicViewSetup > viewSetupsOrdered = spimData.getSequenceDescription().getViewSetupsOrdered();
		final int numViewSetups = viewSetupsOrdered.size();
		final String[] angles = new String[ numViewSetups ];
		for ( int setup = 0; setup < numViewSetups; setup++ )
		{
			final Angle angle = viewSetupsOrdered.get( setup ).getAttribute( Angle.class );
			angles[ setup ] = "angle " + ( angle == null ? setup : angle.getName() );
		}

		/*
		 * Dialog to select target angle.
		 */

		final GenericDialogPlus dialogAngles = new GenericDialogPlus( PLUGIN_NAME + " v" + PLUGIN_VERSION );
		dialogAngles.addMessage( "Select the view that was used to run TGMM.", BIG_FONT );
		dialogAngles.addImage( MaMuT.MAMUT_ICON );

		dialogAngles.addChoice( "View:", angles, angles[ 0 ] );

		dialogAngles.addHelp( ANGLE_HELP_MESSAGE );
		dialogAngles.showDialog();

		/*
		 * Load corresponding angle
		 */

		if ( dialogAngles.wasCanceled() ) { return; }

		final int angleIndex = dialogAngles.getNextChoiceIndex();
		final int setupID = spimData.getSequenceDescription().getViewSetupsOrdered().get( angleIndex ).getId();
		exec( xmlHDF5Path, setupID, tgmmPath, outputPath, interval, tFrom, tTo );
	}

	public void exec( final String xmlHDF5Path, final int setupID, final String tgmmPath, final String outputPath, final RealInterval interval, final int tFrom, final int tTo )
	{
		SpimDataMinimal spimData;
		try
		{
			spimData = new XmlIoSpimDataMinimal().load( xmlHDF5Path );
		}
		catch ( final SpimDataException e )
		{
			logger.error( "Problem reading the transforms in image data file:\n" + e.getMessage() + "\n" );
			return;
		}
		final Model model = createModel( new File( tgmmPath ), spimData, setupID, interval, tFrom, tTo );
		model.setLogger( logger );
		final Settings settings = createSettings( new File( xmlHDF5Path ) );

		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( 1 );
		trackmate.computeSpotFeatures( true );
		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		save( outputPath, model, settings );
	}

	private void save( final String outputPath, final Model model, final Settings settings )
	{

		final File mamutFile = new File( outputPath );
		MamutXmlWriter writer = null;
		try
		{
			logger.log( "Saving to " + mamutFile + '\n' );
			writer = new MamutXmlWriter( mamutFile, logger );
			writer.appendModel( model );
			writer.appendSettings( settings );
			writer.writeToFile();
			logger.log( "Done.\n" );
		}
		catch ( final FileNotFoundException e )
		{
			logger.error( "Could not find file " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			logger.error( "Could not write to " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
		catch ( final Exception e )
		{
			logger.error( "Something wrong happened while saving to " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
	}

	private void somethingWrongHappenedWhileSaving( final MamutXmlWriter writer )
	{
		if ( null != writer )
		{
			final String text = "A problem occured when saving to a file. "
					+ "To recuperate your work, ust copy/paste the text "
					+ "below the line and save it to an XML file.\n"
					+ "__________________________\n"
					+ writer.toString();
			new TextWindow( PLUGIN_NAME + " v" + PLUGIN_VERSION + " save file dump", text, 600, 800 );
		}
	}

	protected SourceSettings createSettings( final File file )
	{
		final SourceSettings settings = new SourceSettings( file.getParent(), file.getName() );
		settings.addAllAnalyzers();
		return settings;
	}

	protected Model createModel( final File tgmmFolder, final SpimDataMinimal spimData, final int setupID, final RealInterval interval, final int tFrom, final int tTo )
	{

		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final ViewRegistrations regs = spimData.getViewRegistrations();
		final List< AffineTransform3D > transforms = new ArrayList<>( seq.getTimePoints().size() );
		for ( final TimePoint t : seq.getTimePoints().getTimePointsOrdered() )
		{
			transforms.add( regs.getViewRegistration( t.getId(), setupID ).getModel() );
		}

		final TGMMImporter2 importer = new TGMMImporter2( tgmmFolder, transforms, TGMMImporter2.DEFAULT_PATTERN, logger, interval, tFrom, tTo );
		if ( !importer.checkInput() || !importer.process() )
		{
			logger.error( importer.getErrorMessage() );
		}
		final Model model = importer.getResult();

		/*
		 * Hack to set the POSITION_T feature of imported spots.
		 */
		final Settings settings = new Settings();
		settings.dt = 1;
		final TrackMate trackmate = new TrackMate( model, settings );
		final ResetSpotTimeFeatureAction action = new ResetSpotTimeFeatureAction();
		action.execute( trackmate, null, null, null );

		return model;
	}
}
