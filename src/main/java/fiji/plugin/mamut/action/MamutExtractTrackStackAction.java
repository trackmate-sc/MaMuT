package fiji.plugin.mamut.action;

import ij.ImagePlus;
import ij.ImageStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.util.MamutUtils;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class MamutExtractTrackStackAction extends AbstractTMAction
{
	public static final String NAME = "Extract track stack";

	public static final String KEY = "EXTRACT_TRACK_STACK";

	public static final String INFO_TEXT = "<html> " + "Generate a stack of images taken from the track " + "that joins two selected spots. " + "<p>" + "There must be exactly 2 spots selected for this action " + "to work, and they must belong to a track that connects " + "them." + "<p>" + "A stack of images will be generated from the spots that join " + "them, defining the image size with the largest spot encountered, multiplied by the current spot radius display factor. " + "The central spot slice is taken in case of 3D data. The source used is the one used at spot creation. " + "</html>";

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/magnifier.png" ) );

	/**
	 * By how much we resize the capture window to get a nice border around the
	 * spot.
	 */
	private static final float RESIZE_FACTOR = 1.5f;

	private final SelectionModel selectionModel;

	private final float radiusFactor;

	/*
	 * CONSTRUCTOR
	 */

	public MamutExtractTrackStackAction( final SelectionModel selectionModel, final float radiusFactor )
	{
		this.selectionModel = selectionModel;
		this.radiusFactor = radiusFactor;
	}

	/*
	 * METHODS
	 */

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Capturing track stack.\n" );

		final Model model = trackmate.getModel();
		final Set< Spot > selection = selectionModel.getSpotSelection();
		int nspots = selection.size();
		if ( nspots != 2 )
		{
			logger.error( "Expected 2 spots in the selection, got " + nspots + ".\nAborting.\n" );
			return;
		}

		// Get start & end
		Spot tmp1, tmp2, start, end;
		final Iterator< Spot > it = selection.iterator();
		tmp1 = it.next();
		tmp2 = it.next();
		if ( tmp1.getFeature( Spot.POSITION_T ) > tmp2.getFeature( Spot.POSITION_T ) )
		{
			end = tmp1;
			start = tmp2;
		}
		else
		{
			end = tmp2;
			start = tmp1;
		}

		// Find path
		final List< DefaultWeightedEdge > edges = model.getTrackModel().dijkstraShortestPath( start, end );
		if ( null == edges )
		{
			logger.error( "The 2 spots are not connected.\nAborting\n" );
			return;
		}

		// Build spot list
		// & Get largest diameter
		final List< Spot > path = new ArrayList< Spot >( edges.size() );
		path.add( start );
		Spot previous = start;
		Spot current;
		double radius = Math.abs( start.getFeature( Spot.RADIUS ) );
		for ( final DefaultWeightedEdge edge : edges )
		{
			current = model.getTrackModel().getEdgeSource( edge );
			if ( current == previous )
			{
				current = model.getTrackModel().getEdgeTarget( edge );
			}
			path.add( current );
			final double ct = Math.abs( current.getFeature( Spot.RADIUS ) );
			if ( ct > radius )
			{
				radius = ct;
			}
			previous = current;
		}
		path.add( end );

		// Sort spot by ascending frame number
		final TreeSet< Spot > sortedSpots = new TreeSet< Spot >( Spot.timeComparator );
		sortedSpots.addAll( path );
		nspots = sortedSpots.size();

		// Common coordinates
		final Settings s = trackmate.getSettings();
		if (!Settings.class.isInstance( s )) {
			logger.error( "The settings object must be a SourceSettings instance. Aborting.\n" );
			return;
		}
		final SourceSettings settings = ( SourceSettings ) s;
		final double[] calibration = new double[] { settings.dx,settings.dy, settings.dz};

		final int width = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR * radiusFactor / calibration[ 0 ] );
		final int height = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR * radiusFactor / calibration[ 1 ] );

		// Prepare new image holder:
		final ImageStack stack = new ImageStack( width, height );

		// Iterate over set to grab imglib image
		int zpos = 0;
		for ( final Spot spot : sortedSpots )
		{

			// Extract image for current frame
			final Img crop = MamutUtils.getImgAround( spot, width, height, settings.getSources() );

			// Copy to central holder
			stack.addSlice( spot.toString(), ImageJFunctions.wrap( crop, crop.toString() ).getProcessor() );

			logger.setProgress( ( float ) ( zpos + 1 ) / nspots );
			zpos++;

		}

		// Convert to plain ImageJ
		final ImagePlus stackTrack = new ImagePlus( "", stack );
		stackTrack.setTitle( "Path from " + start + " to " + end );
		stackTrack.setDimensions( 1, 1, nspots );

		// Display it
		stackTrack.show();
		stackTrack.resetDisplayRange();
	}
}
