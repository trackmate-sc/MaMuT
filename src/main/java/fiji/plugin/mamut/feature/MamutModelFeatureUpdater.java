package fiji.plugin.mamut.feature;

import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.EdgeFeatureCalculator;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
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
	 * Constructs and activate a {@link ModelFeatureUpdater}. The new instance
	 * is registered to listen to model changes, and update its feature.
	 * 
	 * @param model
	 *            the model to listen to.
	 * @param settings
	 *            the {@link Settings} the model is built against. Required to
	 *            access the raw data.
	 */
	public MamutModelFeatureUpdater( final Model model, final SourceSettings settings )
	{
		this.model = model;
		this.mamutSpotFeatureCalculator = new MamutSpotFeatureCalculator( settings );
		this.edgeFeatureCalculator = new EdgeFeatureCalculator( model, settings );
		this.trackFeatureCalculator = new TrackFeatureCalculator( model, settings );
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
