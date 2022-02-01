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
package fiji.plugin.mamut.feature;

import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.EdgeFeatureCalculator;
import fiji.plugin.trackmate.features.TrackFeatureCalculator;
import net.imglib2.algorithm.MultiThreaded;

/**
 * A specialized model feature updater, that can deal with BDV image data.
 * 
 * @author Jean-Yves Tinevez - December 2020
 */
public class MamutModelFeatureUpdater implements ModelChangeListener, MultiThreaded
{

	private final MamutSpotFeatureCalculator mamutSpotFeatureCalculator;
	private final EdgeFeatureCalculator edgeFeatureCalculator;
	private final TrackFeatureCalculator trackFeatureCalculator;

	private final Model model;

	private int numThreads;

	/**
	 * Constructs and activate a {@code ModelFeatureUpdater}. The new instance
	 * is registered to listen to model changes, and update its feature.
	 * 
	 * @param model
	 *            the model to listen to.
	 * @param settings
	 *            the {@link SourceSettings} the model is built against.
	 *            Required to access the raw data.
	 */
	public MamutModelFeatureUpdater( final Model model, final SourceSettings settings )
	{
		this.model = model;
		// don't log feature computation for updates.
		final boolean doLogIt = false;
		this.mamutSpotFeatureCalculator = new MamutSpotFeatureCalculator( settings );
		this.edgeFeatureCalculator = new EdgeFeatureCalculator( model, settings, doLogIt );
		this.trackFeatureCalculator = new TrackFeatureCalculator( model, settings, doLogIt );
		model.addModelChangeListener( this );
		setNumThreads();
	}

	/**
	 * Updates the model features against the change notified here. If the event
	 * is not a {@link ModelChangeEvent#MODEL_MODIFIED}, does nothing.
	 */
	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( event.getEventID() != ModelChangeEvent.MODEL_MODIFIED )
			return;

		// Build spot list
		final ArrayList< Spot > spots = new ArrayList<>( event.getSpots().size() );
		for ( final Spot spot : event.getSpots() )
			if ( event.getSpotFlag( spot ) != ModelChangeEvent.FLAG_SPOT_REMOVED )
				spots.add( spot );

		// Build edge list
		final ArrayList< DefaultWeightedEdge > edges = new ArrayList<>( event.getEdges().size() );
		for ( final DefaultWeightedEdge edge : event.getEdges() )
			if ( event.getEdgeFlag( edge ) != ModelChangeEvent.FLAG_EDGE_REMOVED )
				edges.add( edge );

		// Update spot features
		mamutSpotFeatureCalculator.updateSpotFeatures( spots );

		// Update edge features
		edgeFeatureCalculator.computeEdgesFeatures( edges, false );

		// Update track features
		trackFeatureCalculator.computeTrackFeatures( event.getTrackUpdated(), false );
	}

	/**
	 * Re-registers this instance from the listeners of the model, and stop
	 * updating its features.
	 */
	public void quit()
	{
		model.removeModelChangeListener( this );
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		setNumThreads( Runtime.getRuntime().availableProcessors() );
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
		edgeFeatureCalculator.setNumThreads( numThreads );
		trackFeatureCalculator.setNumThreads( numThreads );
	}
}
