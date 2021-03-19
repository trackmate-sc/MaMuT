/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2021 MaMuT development team.
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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFactory;
import ij.ImageJ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CreateLargeModelExample
{
	private static final int N_STARTING_CELLS = 50;

	private static final int N_DIVISIONS = 5; // 14;

	private static final int N_FRAMES_PER_DIVISION = 5;

	private static final double VELOCITY = 5;

	private static final double RADIUS = 3;

	private final Model model;

	public CreateLargeModelExample()
	{
		this.model = new Model();
		long start = System.currentTimeMillis();
		run();
		long end = System.currentTimeMillis();
		System.out.println( "Model created in " + ( ( end - start ) / 1000 ) + " s." );
		System.out.println( "Total number of spots: " + model.getSpots().getNSpots( false ) );
		final int lastFrame = model.getSpots().keySet().last();
		System.out.println( "Total number of cells in the last frame: " + model.getSpots().getNSpots( lastFrame, false ) );
		System.out.println( "Total number of links: " + model.getTrackModel().edgeSet().size() );
		System.out.println( String.format( "Total memory used by the model: %.1f GB", ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1e9d ) );

		start = System.currentTimeMillis();
		save();
		end = System.currentTimeMillis();
		System.out.println( "Saving done in " + ( end - start ) / 1000 + " s." );

//		start = System.currentTimeMillis();
//		view(new HyperStackDisplayerFactory());
//		end = System.currentTimeMillis();
//		System.out.println( "Rendering in the main viewer done in " + ( end - start ) / 1000 + " s." );

		start = System.currentTimeMillis();
		view( new TrackSchemeFactory() );
		end = System.currentTimeMillis();
		System.out.println( "Rendering in TrackScheme done in " + ( end - start ) / 1000 + " s." );
	}

	public void save()
	{
		final TrackIndexAnalyzer ta = new TrackIndexAnalyzer();
		ta.process( model.getTrackModel().trackIDs( true ), model );

		final File file = new File( "/Users/tinevez/Desktop/LargeModel.xml" );
		final TmXmlWriter writer = new TmXmlWriter( file );
		writer.appendModel( model );
		try
		{
			writer.writeToFile();
		}
		catch ( final FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	public void run()
	{
		model.beginUpdate();
		try
		{

			for ( int ic = 0; ic < N_STARTING_CELLS; ic++ )
			{
				final double angle = 2d * ic * Math.PI / N_STARTING_CELLS;
				final double vx = VELOCITY * Math.cos( angle );
				final double vy = VELOCITY * Math.sin( angle );

				final int nframes = N_DIVISIONS * N_FRAMES_PER_DIVISION;
				final double x = nframes * VELOCITY + vx;
				final double y = nframes * VELOCITY + vy;
				final double z = N_DIVISIONS * VELOCITY;

				final Spot mother = new Spot( x, y, z, RADIUS, angle );
				model.addSpotTo( mother, 0 );

				addBranch( mother, vx, vy, 1 );
			}
		}
		finally
		{
			model.endUpdate();
		}
	}

	public void view( final ViewFactory factory )
	{
		ImageJ.main( null );

		final SelectionModel sm = new SelectionModel( model );
		final TrackMateModelView view = factory.create( model, null, sm );

		final TrackIndexAnalyzer ta = new TrackIndexAnalyzer();
		ta.process( model.getTrackModel().trackIDs( true ), model );

		final SpotColorGenerator scg = new SpotColorGenerator( model );
		scg.setFeature( Spot.QUALITY );
		view.setDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING, scg );
		final PerTrackFeatureColorGenerator tcg = new PerTrackFeatureColorGenerator( model, TrackIndexAnalyzer.TRACK_INDEX );
		view.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, tcg );

		view.setDisplaySettings( TrackMateModelView.KEY_TRACK_DISPLAY_MODE,
				TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL );

		view.render();

	}

	private void addBranch( final Spot start, final double vx, final double vy, final int iteration )
	{
		if ( iteration >= N_DIVISIONS ) { return; }

		// Extend
		Spot previousSpot = start;
		for ( int it = 0; it < N_FRAMES_PER_DIVISION; it++ )
		{
			final double x = previousSpot.getDoublePosition( 0 ) + vx;
			final double y = previousSpot.getDoublePosition( 1 ) + vy;
			final double z = previousSpot.getDoublePosition( 2 );
			final Spot spot = new Spot( x, y, z, RADIUS, iteration );
			final int frame = previousSpot.getFeature( Spot.FRAME ).intValue() + 1;
			model.addSpotTo( spot, frame );
			model.addEdge( previousSpot, spot, iteration );
			previousSpot = spot;
		}

		// Divide
		for ( int id = 0; id < 2; id++ )
		{
			final double sign = id == 0 ? 1 : -1;
			final double x;
			final double y;
			final double z;
			if ( iteration % 2 == 0 )
			{
				x = previousSpot.getDoublePosition( 0 );
				y = previousSpot.getDoublePosition( 1 );
				z = previousSpot.getDoublePosition( 2 ) + sign * VELOCITY * ( 1 - 0.5d * iteration / N_DIVISIONS ) * 2;
			}
			else
			{
				x = previousSpot.getDoublePosition( 0 ) - sign * vy * ( 1 - 0.5d * iteration / N_DIVISIONS ) * 2;
				y = previousSpot.getDoublePosition( 1 ) + sign * vx * ( 1 - 0.5d * iteration / N_DIVISIONS )* 2;
				z = previousSpot.getDoublePosition( 2 );
			}

			final Spot daughter = new Spot( x, y, z, RADIUS, sign );
			final int frame = previousSpot.getFeature( Spot.FRAME ).intValue() + 1;
			model.addSpotTo( daughter, frame );
			model.addEdge( previousSpot, daughter, sign );

			addBranch( daughter, vx, vy, iteration + 1 );
		}
	}

	public static void main( final String[] args )
	{
		new CreateLargeModelExample();
	}
}
