/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2023 MaMuT development team.
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
package fiji.plugin.mamut.action;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultWeightedEdge;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.util.MamutUtils;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.view.Views;

public class MamutExtractTrackStackAction extends AbstractTMAction
{
	public static final String NAME = "Extract track stack";

	public static final String KEY = "EXTRACT_TRACK_STACK";

	public static final String INFO_TEXT = "<html> " + "Generate a stack of images taken from the track " + "that joins two selected spots. " + "<p>" + "There must be exactly 2 spots selected for this action " + "to work, and they must belong to a track that connects " + "them." + "<p>" + "A stack of images will be generated from the spots that join them. " + "A dialog will allow defining the source to use, the image size around the spot to capture and whether we generate a 3D stack or just grab the central slice. " + "</html>";

	/**
	 * By how much we resize the capture window to get a nice border around the
	 * spot.
	 */
	public static final float RESIZE_FACTOR = 1.5f;

	private final int targetSourceIndex;

	private final double diameterFactor;

	private final boolean do3d;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Instantiates a new action that extract the image context around a track
	 * joining two spots.
	 * 
	 * @param targetSourceIndex
	 *            the index of the source to use for image data.
	 * @param diameterFactor
	 *            the size of the target image, in spot diameter units, so that
	 *            id <code>d</code> is the diameter of the largest spot that
	 *            joins the track, then the image size is given by
	 *            <code>d × dimameterFactor</code>. An extra prefactor of
	 *            {@value #RESIZE_FACTOR} is applied on top of this one.
	 * @param do3d
	 *            if <code>true</code>, then a 3D volume will be captured around
	 *            each spot. If <code>false</code>, we just take the central
	 *            slice.
	 */
	public MamutExtractTrackStackAction( final SelectionModel selectionModel, final int targetSourceIndex, final double diameterFactor, final boolean do3d )
	{
		this.targetSourceIndex = targetSourceIndex;
		this.diameterFactor = diameterFactor;
		this.do3d = do3d;
	}

	/*
	 * METHODS
	 */


	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
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
		final List< Spot > path = new ArrayList<>( edges.size() );
		path.add( start );
		Spot previous = start;
		Spot current;
		double radius = Math.abs( start.getFeature( Spot.RADIUS ) );
		int frameLargest = start.getFeature( Spot.FRAME ).intValue();
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
				frameLargest = current.getFeature( Spot.FRAME ).intValue();
			}
			previous = current;
		}
		path.add( end );

		// Sort spot by ascending frame number
		final TreeSet< Spot > sortedSpots = new TreeSet<>( Spot.timeComparator );
		sortedSpots.addAll( path );
		nspots = sortedSpots.size();

		// Common coordinates
		final Settings s = trackmate.getSettings();
		if (!Settings.class.isInstance( s )) {
			logger.error( "The settings object must be a SourceSettings instance. Aborting.\n" );
			return;
		}
		final SourceSettings settings = ( SourceSettings ) s;
		final Source source = settings.getSources().get( targetSourceIndex ).getSpimSource();

		// Get scale
		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		source.getSourceTransform( frameLargest, 0, sourceToGlobal );
		final double dx = Affine3DHelpers.extractScale( sourceToGlobal, 0 );
		final double dy = Affine3DHelpers.extractScale( sourceToGlobal, 1 );
		final double dz = Affine3DHelpers.extractScale( sourceToGlobal, 2 );


		final int width = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR * diameterFactor / dx );
		final int height = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR * diameterFactor / dy );
		final int depth;
		if ( do3d )
		{
			depth = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR * diameterFactor / dz );
		}
		else
		{
			depth = 1;
		}

		// Prepare new image holder:
		final ImageStack stack = new ImageStack( width, height );

		// Iterate over set to grab imglib image
		int zpos = 0;
		for ( final Spot spot : sortedSpots )
		{
			if ( do3d )
			{
				// Extract stack for current frame
				final Img crop = MamutUtils.getStackAround( spot, width, height, depth, source );
				// Copy it so stack
				for ( int i = 0; i < crop.dimension( 2 ); i++ )
				{
					final ImageProcessor processor = ImageJFunctions.wrap( Views.hyperSlice( crop, 2, i ), crop.toString() ).getProcessor();
					stack.addSlice( spot.toString(), processor );
				}
			}
			else
			{
				// Extract image for current frame
				final Img crop = MamutUtils.getSliceAround( spot, width, height, source );

				// Copy to central holder
				stack.addSlice( spot.toString(), ImageJFunctions.wrap( crop, crop.toString() ).getProcessor() );
			}
			logger.setProgress( ( float ) ( zpos + 1 ) / nspots );
			zpos++;

		}

		// Convert to plain ImageJ
		final ImagePlus stackTrack = new ImagePlus( "", stack );
		final Calibration cal = stackTrack.getCalibration();
		cal.pixelWidth = dx;
		cal.pixelHeight = dy;
		cal.pixelDepth = dz;
		cal.frameInterval = 1d;
		cal.setTimeUnit( "link" );
		stackTrack.setCalibration( cal );
		stackTrack.setTitle( "Path from " + start + " to " + end );
		stackTrack.setDimensions( 1, depth, nspots );

		// Display it
		stackTrack.setOpenAsHyperStack( true );
		stackTrack.show();
		stackTrack.setZ( depth / 2 + 1 );
		stackTrack.resetDisplayRange();
	}
}
