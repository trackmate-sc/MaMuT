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
package fiji.plugin.mamut.feature.track;

import static fiji.plugin.mamut.feature.spot.CellDivisionTimeAnalyzerSpotFactory.CELL_DIVISION_TIME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;

@Plugin( type = TrackAnalyzer.class )
public class CellDivisionTimeAnalyzer implements TrackAnalyzer
{

	/**
	 * If <code>true</code>, then we will exclude from the calculations the
	 * branches that do not start with a split event and do not end with a split
	 * event.
	 */
	private static final boolean EXCLUDE_OPEN_BRANCHES = true;

	public static final String KEY = "CELL_DIVISION_TIME_ANALYZER";

	private static final List< String > FEATURES = new ArrayList<>( 1 );

	public static final String DIVISION_TIME_MEAN = "DIVISION_TIME_MEAN";

	public static final String DIVISION_TIME_STD = "DIVISION_TIME_STD";

	private static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 2 );

	private static final Map< String, String > FEATURE_NAMES = new HashMap<>( 2 );

	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 2 );

	private static final Map< String, Boolean > IS_INT = new HashMap<>( 2 );

	private static final String INFO_TEXT = "<html>This analyzers measures the time between two cell divisions. It excludes </html>";

	private static final String NAME = "Cell division rate analyzer";

	static
	{
		FEATURES.add( DIVISION_TIME_MEAN );
		FEATURES.add( DIVISION_TIME_STD );

		FEATURE_SHORT_NAMES.put( DIVISION_TIME_MEAN, "Mean div. time" );
		FEATURE_SHORT_NAMES.put( DIVISION_TIME_STD, "Std div. time" );

		FEATURE_NAMES.put( DIVISION_TIME_MEAN, "Mean cell division time" );
		FEATURE_NAMES.put( DIVISION_TIME_STD, "Std cell division time" );

		FEATURE_DIMENSIONS.put( DIVISION_TIME_MEAN, Dimension.TIME );
		FEATURE_DIMENSIONS.put( DIVISION_TIME_STD, Dimension.TIME );

		IS_INT.put( DIVISION_TIME_MEAN, Boolean.FALSE );
		IS_INT.put( DIVISION_TIME_STD, Boolean.FALSE );
	}

	private long processingTime;

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public int getNumThreads()
	{
		return 1;
	}

	@Override
	public void setNumThreads()
	{}

	@Override
	public void setNumThreads( final int numThreads )
	{}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{
		final long start = System.currentTimeMillis();
		final FeatureModel fm = model.getFeatureModel();
		final TrackModel tm = model.getTrackModel();
		final TimeDirectedNeighborIndex neighborIndex = tm.getDirectedNeighborIndex();

		for ( final Integer trackID : trackIDs )
		{
			// For running variance and mean
			double sum = 0;
			double mean = 0;
			double M2 = 0;
			double delta, delta_n;
			double term1;
			int n1;

			// Others
			double val;
			int n = 0;

			final TrackBranchDecomposition branchDecomposition = ConvexBranchesDecomposition.processTrack( trackID, tm, neighborIndex, false, false );
			for ( final List< Spot > branch : branchDecomposition.branches )
			{
				final Spot first = branch.get( 0 );
				final Spot last = branch.get( branch.size() - 1 );

				if ( EXCLUDE_OPEN_BRANCHES )
				{
					// Check if this branch arose from a cell division
					final Set< Spot > predecessors = neighborIndex.predecessorsOf( first );
					if ( predecessors.size() == 0 )
					{
						continue;
					}
					final Spot predecessor = predecessors.iterator().next();
					if ( neighborIndex.successorsOf( predecessor ).size() < 2 )
					{
						continue;
					}

					// Check if this branch ends by a cell division
					if ( neighborIndex.successorsOf( last ).size() < 2 )
					{
						continue;
					}
				}

				// Ok, incorporate its duration
				val = last.diffTo( first, Spot.POSITION_T );

				/*
				 * Before we go on, we will add this value as a feature of all
				 * the spots of this branch.
				 */
				for ( final Spot spot : branch )
				{
					spot.putFeature( CELL_DIVISION_TIME, Double.valueOf( val ) );
				}

				// For variance and mean
				sum += val;

				n1 = n;
				n++;
				delta = val - mean;
				delta_n = delta / n;
				term1 = delta * delta_n * n1;
				mean = mean + delta_n;
				M2 = M2 + term1;
			}

			final double std;
			if ( n < 2 )
			{
				std = Double.NaN;
			}
			else
			{
				std = Math.sqrt( M2 / ( n - 1 ) );
			}
			if ( n < 1 )
			{
				mean = Double.NaN;
			}
			else
			{
				mean = sum / n;
			}

			fm.putTrackFeature( trackID, DIVISION_TIME_MEAN, Double.valueOf( mean ) );
			fm.putTrackFeature( trackID, DIVISION_TIME_STD, Double.valueOf( std ) );
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

}
