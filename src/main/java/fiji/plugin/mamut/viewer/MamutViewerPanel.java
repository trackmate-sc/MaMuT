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
package fiji.plugin.mamut.viewer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import bdv.cache.CacheControl;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.TranslationAnimator;
import fiji.plugin.trackmate.Spot;
import net.imglib2.realtransform.AffineTransform3D;

public class MamutViewerPanel extends ViewerPanel
{

	private static final long serialVersionUID = 1L;

	/**
	 * The overlay on which the {@link fiji.plugin.trackmate.Model} will be
	 * painted.
	 */
	MamutOverlay overlay;

	public MamutViewerPanel( final List< SourceAndConverter< ? >> sources, final int numTimePoints, final CacheControl cache )
	{
		this( sources, numTimePoints, cache, ViewerOptions.options() );
	}

	public MamutViewerPanel( final List< SourceAndConverter< ? >> sources, final int numTimePoints, final CacheControl cache, final ViewerOptions optional )
	{
		super( sources, numTimePoints, cache, optional );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		super.drawOverlays( g );

		if ( null != overlay )
		{
			overlay.setViewerState( state() );
			overlay.paint( ( Graphics2D ) g );
		}
	}

	public void centerViewOn( final Spot spot )
	{
		final int tp = spot.getFeature( Spot.FRAME ).intValue();
		setTimepoint( tp );

		final AffineTransform3D t = new AffineTransform3D();
		state().getViewerTransform( t );
		final double[] spotCoords = new double[] { spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ) };

		// Translate view so that the target spot is in the middle of the
		// JFrame.
		final double dx = getDisplay().getWidth() / 2 - ( t.get( 0, 0 ) * spotCoords[ 0 ] + t.get( 0, 1 ) * spotCoords[ 1 ] + t.get( 0, 2 ) * spotCoords[ 2 ] );
		final double dy = getDisplay().getHeight() / 2 - ( t.get( 1, 0 ) * spotCoords[ 0 ] + t.get( 1, 1 ) * spotCoords[ 1 ] + t.get( 1, 2 ) * spotCoords[ 2 ] );
		final double dz = -( t.get( 2, 0 ) * spotCoords[ 0 ] + t.get( 2, 1 ) * spotCoords[ 1 ] + t.get( 2, 2 ) * spotCoords[ 2 ] );

		// But use an animator to do this smoothly.
		final double[] target = new double[] { dx, dy, dz };
		setTransformAnimator( new TranslationAnimator( t, target, 300 ) );
	}
}
