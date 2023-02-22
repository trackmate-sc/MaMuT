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
package fiji.plugin.mamut.viewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;

/**
 * A copy of the {@link MamutOverlay} that discards any Z-depth information. All
 * spots and links are displayed as if they were in the view central plane.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class MamutZProjectedOverlay extends MamutOverlay
{


	public MamutZProjectedOverlay( final Model model, final SelectionModel selectionModel, final MamutViewer viewer, final DisplaySettings ds )
	{
		super( model, selectionModel, viewer, ds );
	}

	@Override
	public void paint( final Graphics2D g )
	{

		/*
		 * Collect current view.
		 */
		state.getViewerTransform( transform );

		/*
		 * Common display settings.
		 */

		final TrackDisplayMode trackDisplayMode = ds.getTrackDisplayMode();
		final Stroke selectionStroke = new BasicStroke( ( float ) ds.getSelectionLineThickness() );
		final Stroke normalStroke = new BasicStroke( ( float ) ds.getLineThickness() );
		final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, ds );
		final FeatureColorGenerator< DefaultWeightedEdge > trackColorGenerator = FeatureUtils.createTrackColorGenerator( model, ds );

		/*
		 * Draw spots.
		 */

		if ( ds.isSpotVisible() )
		{

			final double radiusRatio = ds.getSpotDisplayRadius();
			final boolean doDisplayNames = ds.isSpotShowName();
			g.setFont( ds.getFont() );

			/*
			 * Compute scale
			 */
			final double vx = transform.get( 0, 0 );
			final double vy = transform.get( 1, 0 );
			final double vz = transform.get( 2, 0 );
			final double transformScale = Math.sqrt( vx * vx + vy * vy + vz * vz );

			final Iterable< Spot > spots;
			final int frame = state.getCurrentTimepoint();
			if ( trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
			{
				spots = model.getSpots().iterable( frame, true );
			}
			else
			{
				final ArrayList< Spot > tmp = new ArrayList<>();
				for ( final Spot spot : selectionModel.getSpotSelection() )
				{
					if ( spot.getFeature( Spot.FRAME ).intValue() == frame )
						tmp.add( spot );
				}
				spots = tmp;
			}

			for ( final Spot spot : spots )
			{

				Color color;
				Stroke stroke;
				if ( selectionModel.getSpotSelection().contains( spot ) && trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
				{
					color = ds.getHighlightColor();
					stroke = selectionStroke;
				}
				else
				{
					color = spotColorGenerator.color( spot );
					stroke = normalStroke;
				}
				g.setColor( color );
				g.setStroke( stroke );

				final double x = spot.getFeature( Spot.POSITION_X );
				final double y = spot.getFeature( Spot.POSITION_Y );
				final double z = spot.getFeature( Spot.POSITION_Z );
				final double radius = spot.getFeature( Spot.RADIUS );

				final double[] globalCoords = new double[] { x, y, z };
				final double[] viewerCoords = new double[ 3 ];
				transform.apply( globalCoords, viewerCoords );

				final double rad = radius * transformScale * radiusRatio;
				g.drawOval( ( int ) ( viewerCoords[ 0 ] - rad ), ( int ) ( viewerCoords[ 1 ] - rad ), ( int ) ( 2 * rad ), ( int ) ( 2 * rad ) );

				if ( doDisplayNames )
				{
					final int tx = ( int ) ( viewerCoords[ 0 ] + rad + 5 );
					final int ty = ( int ) viewerCoords[ 1 ];
					g.drawString( spot.getName(), tx, ty );
				}
			}
		}

		/*
		 * Draw edges
		 */

		final boolean tracksVisible = ds.isTrackVisible();

		if ( tracksVisible && model.getTrackModel().nTracks( false ) > 0 )
		{

			// Save graphic device original settings
			final Composite originalComposite = g.getComposite();
			final Stroke originalStroke = g.getStroke();
			final Color originalColor = g.getColor();

			Spot source, target;

			// Non-selected tracks.
			final int currentFrame = state.getCurrentTimepoint();
			final int trackDisplayDepth = ds.getFadeTrackRange();
			final Set< Integer > filteredTrackIDs = model.getTrackModel().unsortedTrackIDs( true );

			g.setStroke( normalStroke );
			if ( trackDisplayMode == TrackDisplayMode.LOCAL )
				g.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER ) );

			// Determine bounds for limited view modes
			int minT = 0;
			int maxT = 0;
			switch ( trackDisplayMode )
			{
			case LOCAL:
			case SELECTION_ONLY:
				minT = currentFrame - trackDisplayDepth;
				maxT = currentFrame + trackDisplayDepth;
				break;
			case LOCAL_FORWARD:
				minT = currentFrame;
				maxT = currentFrame + trackDisplayDepth;
				break;
			case LOCAL_BACKWARD:
				minT = currentFrame - trackDisplayDepth;
				maxT = currentFrame;
				break;
			default:
				break;
			}

			double sourceFrame;
			float transparency;
			switch ( trackDisplayMode )
			{

			case FULL:
			{
				for ( final Integer trackID : filteredTrackIDs )
				{
					final Set< DefaultWeightedEdge > track = new HashSet<>( model.getTrackModel().trackEdges( trackID ) );
					for ( final DefaultWeightedEdge edge : track )
					{
						source = model.getTrackModel().getEdgeSource( edge );
						target = model.getTrackModel().getEdgeTarget( edge );
						g.setColor( trackColorGenerator.color( edge ) );
						drawEdge( g, source, target, transform, 1f, false, 0. );
					}
				}
				break;
			}

			case SELECTION_ONLY:
			{

				// Sort edges by their track id.
				final HashMap< Integer, ArrayList< DefaultWeightedEdge > > sortedEdges = new HashMap< >();
				for ( final DefaultWeightedEdge edge : selectionModel.getEdgeSelection() )
				{
					final Integer trackID = model.getTrackModel().trackIDOf( edge );
					ArrayList< DefaultWeightedEdge > edges = sortedEdges.get( trackID );
					if ( null == edges )
					{
						edges = new ArrayList< >();
						sortedEdges.put( trackID, edges );
					}
					edges.add( edge );
				}

				for ( final Integer trackID : sortedEdges.keySet() )
				{
					for ( final DefaultWeightedEdge edge : sortedEdges.get( trackID ) )
					{
						source = model.getTrackModel().getEdgeSource( edge );
						target = model.getTrackModel().getEdgeTarget( edge );

						sourceFrame = source.getFeature( Spot.FRAME ).intValue();
						if ( sourceFrame < minT || sourceFrame >= maxT )
							continue;

						transparency = ( float ) ( 1 - Math.abs( sourceFrame - currentFrame ) / trackDisplayDepth );
						target = model.getTrackModel().getEdgeTarget( edge );
						g.setColor( trackColorGenerator.color( edge ) );
						drawEdge( g, source, target, transform, transparency, false, 0. );
					}
				}
				break;
			}

			case LOCAL:
			case LOCAL_FORWARD:
			case LOCAL_BACKWARD:
			{

				g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

				for ( final int trackID : filteredTrackIDs )
				{
					final Set< DefaultWeightedEdge > track = model.getTrackModel().trackEdges( trackID );

					for ( final DefaultWeightedEdge edge : track )
					{
						source = model.getTrackModel().getEdgeSource( edge );
						sourceFrame = source.getFeature( Spot.FRAME ).intValue();
						if ( sourceFrame < minT || sourceFrame >= maxT )
							continue;

						transparency = ( float ) ( 1 - Math.abs( sourceFrame - currentFrame ) / trackDisplayDepth );
						target = model.getTrackModel().getEdgeTarget( edge );
						g.setColor( trackColorGenerator.color( edge ) );
						drawEdge( g, source, target, transform, transparency, false, 0. );
					}
				}
				break;

			}

			}

			if ( trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
			{
				// Deal with highlighted edges first: brute and thick display
				g.setStroke( selectionStroke );
				g.setColor( ds.getHighlightColor() );
				for ( final DefaultWeightedEdge edge : selectionModel.getEdgeSelection() )
				{
					source = model.getTrackModel().getEdgeSource( edge );
					target = model.getTrackModel().getEdgeTarget( edge );
					drawEdge( g, source, target, transform, 1f, false, 0. );
				}
			}

			// Restore graphic device original settings
			g.setComposite( originalComposite );
			g.setStroke( originalStroke );
			g.setColor( originalColor );
		}
	}
}
