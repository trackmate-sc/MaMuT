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

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.IOUtils;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

@SuppressWarnings( "deprecation" )
public class NewMamutAnnotationPlugin implements PlugIn
{

	private static File file;

	@Override
	public void run( final String fileStr )
	{

		final Logger logger = Logger.IJ_LOGGER;

		if ( null != fileStr && fileStr.length() > 0 )
		{
			// Skip dialog
			file = new File( fileStr );
			if ( file.isDirectory() )
			{
				file = IOUtils.askForFileForLoading( file, "Open a BDV xml/h5 file", IJ.getInstance(), logger );
				if ( null == file ) { return; }
			}
			if ( !file.exists() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot find image file " + fileStr );
				return;
			}
			if ( !file.canRead() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot read image file " + fileStr );
				return;
			}

		}
		else
		{

			if ( null == file )
			{
				file = proposeBdvXmlFileToOpen();
			}
			file = IOUtils.askForFileForLoading( file, "Open a hdf5/xml file", IJ.getInstance(), logger );
			if ( null == file )
				return;
		}


		final Model model = new Model();
		model.setLogger( logger );
		final SourceSettings settings = new SourceSettings( file.getParent(), file.getName() );
		settings.addAllAnalyzers();
		final MaMuT mamut = new MaMuT( model, settings, DisplaySettingsIO.readUserDefault() );

		/*
		 * Initialize spatial units.
		 */

		if ( !settings.getSources().isEmpty() )
		{
			final SourceAndConverter< ? > sac = settings.getSources().get( 0 );
			if ( sac != null && sac.getSpimSource() != null )
			{
				final VoxelDimensions vdim = sac.getSpimSource().getVoxelDimensions();
				model.setPhysicalUnits( vdim.unit(), "frame" );
			}
		}

		/*
		 * Initialize default settings.
		 */

		final String fs = file.toString();
		final int ixml = fs.lastIndexOf( ".xml" );
		final String settingsFileStr = fs.substring( 0, ixml ) + ".settings" + fs.substring( ixml );
		final File settingsFile = new File( settingsFileStr );

		if ( settingsFile.exists() && settingsFile.isFile() && settingsFile.canRead() )
		{
			logger.log( "Found a settings file: " + settingsFileStr + '\n' );
			final SAXBuilder sax = new SAXBuilder();
			try
			{
				final Document doc = sax.build( settingsFile );
				final Element root = doc.getRootElement();
				mamut.getSetupAssignments().restoreFromXml( root );
				mamut.getBookmarks().restoreFromXml( root );
			}
			catch ( final JDOMException e )
			{
				logger.error( "Could not read from settings file:\n" + e.getMessage() + '\n' );
			}
			catch ( final IOException e )
			{
				logger.error( "Could not read from settings file:\n" + e.getMessage() + '\n' );
			}

		}
		else
		{
			/*
			 * No settings file. We put in defaults.
			 */

			initBrightness( 0.001, 0.999, settings.getSources(), mamut.getSetupAssignments() );
		}

	}

	public static File proposeBdvXmlFileToOpen()
	{
		File folder = new File( System.getProperty( "user.dir" ) );
		if ( folder.getParentFile() != null )
			folder = folder.getParentFile();
		if ( folder != null )
			return new File( folder.getPath() + File.separator + "data.xml" );

		return new File( "data.xml" );
	}

	/*
	 * STATIC UTILS.
	 */

	private static void initBrightness( final double cumulativeMinCutoff, final double cumulativeMaxCutoff, final List< SourceAndConverter< ? > > sources, final SetupAssignments setupAssignments )
	{
		final int nSources = sources.size();

		// Loop over sources.
		for ( int s = 0; s < nSources; s++ )
		{
			final Source< ? > source = sources.get( s ).getSpimSource();

			// Fidn a timepoint for this source.
			int timepoint = 0;
			while ( !source.isPresent( timepoint ) && timepoint < 1000 )
				timepoint++;

			if ( !source.isPresent( timepoint ) )
				return;

			if ( !UnsignedShortType.class.isInstance( source.getType() ) )
				return;

			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval< UnsignedShortType > ) source.getSource( timepoint, source.getNumMipmapLevels() - 1 );
			final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

			final int numBins = 6535;
			final Histogram1d< UnsignedShortType > histogram = new Histogram1d<>( Views.iterable( Views.hyperSlice( img, 2, z ) ), new Real1dBinMapper< UnsignedShortType >( 0, 65535, numBins, false ) );
			final DiscreteFrequencyDistribution dfd = histogram.dfd();
			final long[] bin = new long[] { 0 };
			double cumulative = 0;
			int i = 0;
			for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
			{
				bin[ 0 ] = i;
				cumulative += dfd.relativeFrequency( bin );
			}
			final int min = i * 65535 / numBins;
			for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
			{
				bin[ 0 ] = i;
				cumulative += dfd.relativeFrequency( bin );
			}
			final int max = i * 65535 / numBins;
			final MinMaxGroup minmax = setupAssignments.getMinMaxGroups().get( s );
			minmax.getMinBoundedValue().setCurrentValue( min );
			minmax.getMaxBoundedValue().setCurrentValue( max );
		}
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );

		final NewMamutAnnotationPlugin plugin = new NewMamutAnnotationPlugin();
		plugin.run(
//				"/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg.xml"
//				"/Users/tinevez/Desktop/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml" );
//				"D:/Projects/JYTinevez/Mastodon/Tutorial/datasethdf5.xml" );
//				"D:/Projects/JYTinevez/MaMuT/Mastodon-dataset/MaMuT_Parhyale_demo.xml" );
				"C:/Users/tinevez/Desktop/ImageBDV.xml" );
//		plugin.run( "/Users/tinevez/Desktop/Celegans.xml" );
	}

}
