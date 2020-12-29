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

import bdv.viewer.ViewerState;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import net.imglib2.realtransform.AffineTransform3D;

public class MamutOverlay
{

	/** The viewer state. */
	protected ViewerState state;

	/** The transform for the viewer current viewpoint. */
	protected final AffineTransform3D transform = new AffineTransform3D();

	/** The model to point on this overlay. */
	protected final Model model;

	/** The viewer in which this overlay is painted. */
	protected final MamutViewer viewer;

	/** The selection model. Items belonging to it will be highlighted. */
	protected final SelectionModel selectionModel;

	protected final DisplaySettings ds;

	public MamutOverlay( final Model model, final SelectionModel selectionModel, final MamutViewer viewer, final DisplaySettings ds )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.viewer = viewer;
		this.ds = ds;
	}

	public void paint( final Graphics2D g )
	{

		/*
		 * Collect current view.
		 */
		state.getViewerTransform( transform );

		/*
		 * Common display settings.
		 */

		final boolean doLimitDrawingDepth = ds.isZDrawingDepthLimited();
		final double drawingDepth = ds.getZDrawingDepth();
		final TrackDisplayMode trackDisplayMode = ds.getTrackDisplayMode();
		final Stroke normalStroke = new BasicStroke( ( float ) ds.getLineThickness() );
		final Stroke selectionStroke = new BasicStroke( ( float ) ds.getSelectionLineThickness() );
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

				Stroke stroke = normalStroke;
				boolean forceDraw = !doLimitDrawingDepth;
				final Color color;
				if ( selectionModel.getSpotSelection().contains( spot ) && trackDisplayMode != TrackDisplayMode.SELECTION_ONLY )
				{
					forceDraw = true; // Selection is drawn unconditionally.
					color = ds.getHighlightColor();
					stroke = selectionStroke;
				}
				else
				{
					color = spotColorGenerator.color( spot );

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
				final double zv = viewerCoords[ 2 ];
				final double dz2 = zv * zv;

				if ( !forceDraw && Math.abs( zv ) > drawingDepth )
					continue;

				if ( dz2 < rad * rad )
				{

					final double arad = Math.sqrt( rad * rad - dz2 );
					g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad ), ( int ) ( viewerCoords[ 1 ] - arad ), ( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );

					if ( doDisplayNames )
					{
						final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
						final int ty = ( int ) viewerCoords[ 1 ];
						g.drawString( spot.getName(), tx, ty );
					}

				}
				else
				{
					g.fillOval( ( int ) viewerCoords[ 0 ] - 2, ( int ) viewerCoords[ 1 ] - 2, 4, 4 );
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
						drawEdge( g, source, target, transform, 1f, doLimitDrawingDepth, drawingDepth );
					}
				}
				break;
			}

			case SELECTION_ONLY:
			{

				// Sort edges by their track id.
				final HashMap< Integer, ArrayList< DefaultWeightedEdge > > sortedEdges = new HashMap<>();
				for ( final DefaultWeightedEdge edge : selectionModel.getEdgeSelection() )
				{
					final Integer trackID = model.getTrackModel().trackIDOf( edge );
					ArrayList< DefaultWeightedEdge > edges = sortedEdges.get( trackID );
					if ( null == edges )
					{
						edges = new ArrayList<>();
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
						drawEdge( g, source, target, transform, transparency, doLimitDrawingDepth, drawingDepth );
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
						drawEdge( g, source, target, transform, transparency, doLimitDrawingDepth, drawingDepth );
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
					drawEdge( g, source, target, transform, 1f, false, drawingDepth );
				}
			}

			// Restore graphic device original settings
			g.setComposite( originalComposite );
			g.setStroke( originalStroke );
			g.setColor( originalColor );

		}

	}

	protected void drawEdge( final Graphics2D g2d, final Spot source, final Spot target, final AffineTransform3D tr, final float transparency, final boolean limitDrawingDetph, final double drawingDepth )
	{

		// Find x & y & z in physical coordinates
		final double x0i = source.getFeature( Spot.POSITION_X );
		final double y0i = source.getFeature( Spot.POSITION_Y );
		final double z0i = source.getFeature( Spot.POSITION_Z );
		final double[] physicalPositionSource = new double[] { x0i, y0i, z0i };

		final double x1i = target.getFeature( Spot.POSITION_X );
		final double y1i = target.getFeature( Spot.POSITION_Y );
		final double z1i = target.getFeature( Spot.POSITION_Z );
		final double[] physicalPositionTarget = new double[] { x1i, y1i, z1i };

		// In pixel units
		final double[] pixelPositionSource = new double[ 3 ];
		tr.apply( physicalPositionSource, pixelPositionSource );
		final double[] pixelPositionTarget = new double[ 3 ];
		tr.apply( physicalPositionTarget, pixelPositionTarget );

		if ( limitDrawingDetph && Math.abs( pixelPositionSource[ 2 ] ) > drawingDepth && Math.abs( pixelPositionTarget[ 2 ] ) > drawingDepth )
			return;

		// Round
		final int x0 = ( int ) Math.round( pixelPositionSource[ 0 ] );
		final int y0 = ( int ) Math.round( pixelPositionSource[ 1 ] );
		final int x1 = ( int ) Math.round( pixelPositionTarget[ 0 ] );
		final int y1 = ( int ) Math.round( pixelPositionTarget[ 1 ] );

		g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, transparency ) );
		g2d.drawLine( x0, y0, x1, y1 );
	}

	/**
	 * Update data to show in the overlay.
	 *
	 * @param state
	 *            the state of the data.
	 */
	public void setViewerState( final ViewerState state )
	{
		this.state = state;
	}
}
