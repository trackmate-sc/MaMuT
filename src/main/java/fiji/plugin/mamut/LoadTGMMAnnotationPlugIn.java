/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2016 MaMuT development team.
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

import fiji.plugin.mamut.io.TGMMImporter2;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.io.IOUtils;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class LoadTGMMAnnotationPlugIn implements PlugIn
{
	private static File staticTGMMFolder;

	private static File staticImageFile;

	protected Logger logger = Logger.IJ_LOGGER;

	private RealInterval interval;

	private File askForImageFile()
	{
		if ( null == staticImageFile )
		{
			staticImageFile = NewMamutAnnotationPlugin.proposeBdvXmlFileToOpen();
		}
		staticImageFile = IOUtils.askForFileForLoading( staticImageFile, "Open a hdf5/xml file", IJ.getInstance(), logger );
		if ( null == staticImageFile ) { return null; }
		return staticImageFile;
	}

	private File askForTGMMFolder()
	{
		if ( null == staticTGMMFolder )
		{
			File folder = new File( System.getProperty( "user.dir" ) );
			if ( folder.getParentFile() != null )
				folder = folder.getParentFile();
			if ( folder == null )
				folder = new File( "./" );
			staticTGMMFolder = folder;
		}
		staticTGMMFolder = IOUtils.askForFolder( staticTGMMFolder, "Open a TGMM /xml folder", IJ.getInstance(), logger );
		if ( null == staticTGMMFolder ) { return null; }
		return staticTGMMFolder;
	}

	private int askForAngle( final String[] angles )
	{
		final Component frame = IJ.getInstance();
		final Icon icon = MaMuT.MAMUT_ICON;
		final String s = ( String ) JOptionPane.showInputDialog( frame, "Select the view that was used by the TGMM:", "MaMuT TGMM import", JOptionPane.PLAIN_MESSAGE, icon, angles, angles[ 0 ] );
		if ( s == null || s.length() == 0 ) { return -1; }

		final int setupID = Arrays.asList( angles ).indexOf( s );
		return setupID;
	}

	@Override
	public void run( final String ignored )
	{
		final File imageFile = askForImageFile();
		if ( null == imageFile ) { return; }

		final File tgmmFolder = askForTGMMFolder();
		if ( null == tgmmFolder ) { return; }

		SpimDataMinimal spimData;
		try
		{
			spimData = new XmlIoSpimDataMinimal().load( imageFile.getAbsolutePath() );
		}
		catch ( final SpimDataException e )
		{
			logger.error( "Problem reading the transforms in image data file:\n" + e.getMessage() + "\n" );
			return;
		}
		final String[] angles = readSetupNames( spimData.getSequenceDescription() );
		final int angleIndex = askForAngle( angles );
		if ( angleIndex < 0 ) { return; }

		final int setupID = spimData.getSequenceDescription().getViewSetupsOrdered().get( angleIndex ).getId();
		launchMamut( imageFile, tgmmFolder, setupID, interval );
	}

	public void launchMamut( final File imageFile, final File tgmmFile, final int setupID, final RealInterval interval )
	{
		SpimDataMinimal spimData;
		try
		{
			spimData = new XmlIoSpimDataMinimal().load( imageFile.getAbsolutePath() );
		}
		catch ( final SpimDataException e )
		{
			logger.error( "Problem reading the transforms in image data file:\n" + e.getMessage() + "\n" );
			return;
		}
		final Model model = createModel( tgmmFile, spimData, setupID, interval );
		final SourceSettings settings = createSettings();
		new MaMuT( imageFile, model, settings );
	}

	protected Model createModel( final File tgmmFolder, final SpimDataMinimal spimData, final int setupID, final RealInterval interval )
	{
		final List< AffineTransform3D > transforms = pickTransform( spimData, setupID );

		final TGMMImporter2 importer = new TGMMImporter2( tgmmFolder, transforms, TGMMImporter2.DEFAULT_PATTERN, logger, interval, 0, Integer.MAX_VALUE );
		if ( !importer.checkInput() || !importer.process() )
		{
			logger.error( importer.getErrorMessage() );
			return new Model();
		}
		final Model model = importer.getResult();

		/*
		 * Hack to set the POSITION_T feature of imported spots.
		 */
		final Settings settings = new Settings();
		settings.dt = 1;
		final TrackMate trackmate = new TrackMate( model, settings );
		final ResetSpotTimeFeatureAction action = new ResetSpotTimeFeatureAction();
		action.execute( trackmate );

		return model;
	}

	protected SourceSettings createSettings()
	{
		return new SourceSettings();
	}

	protected String[] readSetupNames( final AbstractSequenceDescription< ?, ?, ? > sequenceDescriptionMinimal )
	{
		final List< ? extends BasicViewSetup > viewSetupsOrdered = sequenceDescriptionMinimal.getViewSetupsOrdered();
		final int numViewSetups = viewSetupsOrdered.size();
		final String[] angles = new String[ numViewSetups ];
		for ( int setup = 0; setup < numViewSetups; setup++ )
		{
			final Angle angle = viewSetupsOrdered.get( setup ).getAttribute( Angle.class );
			angles[ setup ] = "angle " + ( angle == null ? setup : angle.getName() );
		}
		return angles;
	}

	protected List< AffineTransform3D > pickTransform( final SpimDataMinimal spimData, final int setupID )
	{
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final ViewRegistrations regs = spimData.getViewRegistrations();
		final List< AffineTransform3D > transforms = new ArrayList<>( seq.getTimePoints().size() );
		for ( final TimePoint t : seq.getTimePoints().getTimePointsOrdered() )
		{
			transforms.add( regs.getViewRegistration( t.getId(), setupID ).getModel() );
		}
		return transforms;
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new LoadTGMMAnnotationPlugIn().run( "" );

		//		final File imageFile = new File( "/Volumes/Data/BDV_MVD_5v_final.xml" );
		//		final File tgmmFolder = new File( "/Volumes/Data/TGMM_TL0-528_xmls_curated" );
		//		final int angleIndex = 0;
		//		final long[] min = new long[] { 200, 550, 150 };
		//		final long[] max = new long[] { 550, 950, 550 };
		//		final RealInterval interval = new FinalInterval( min, max );
		//
		//		final LoadTGMMAnnotationPlugIn plugin = new LoadTGMMAnnotationPlugIn();
		//		plugin.logger = Logger.DEFAULT_LOGGER;
		//		plugin.launchMamut( imageFile, tgmmFolder, angleIndex, interval );
	}

}
