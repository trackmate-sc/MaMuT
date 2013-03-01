package fiji.plugin.mamut;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas;

public class MultiViewDisplayer extends AbstractTrackMateModelView  {

	private static final boolean DEBUG = false;
	public static final String NAME = "MultiView Displayer";
	public static final String INFO_TEXT = "<html>" +
			"<ul>" +
			"	<li><b>A</b> creates a new spot under the mouse" +
			"	<li><b>D</b> deletes the spot under the mouse" +
			"	<li><b>Q</b> and <b>E</b> decreases and increases the radius of the spot " +
			"under the mouse (shift to go faster)" +
			"	<li><b>Space</b> + mouse drag moves the spot under the mouse" +
			"</ul>" +
			"</html>";

	private Collection<ImagePlus> imps;
	Map<ImagePlus, StackWindow> windows = new HashMap<ImagePlus, StackWindow>();
	Map<ImagePlus, OverlayedImageCanvas> canvases = new HashMap<ImagePlus, OverlayedImageCanvas>();
	Map<ImagePlus, TransformedSpotOverlay> spotOverlays = new HashMap<ImagePlus, TransformedSpotOverlay>();
	Map<ImagePlus, TransformedTrackOverlay> trackOverlays = new HashMap<ImagePlus, TransformedTrackOverlay>();

	private MVSpotEditTool editTool;
	private Map<ImagePlus, List<AffineTransform3D>> transforms;

	/** The updater instance in charge of setting the views Z & T when {@link #centerViewOn(Spot)} is called. */
	private ViewRefresher spotViewUpdater;
	/** The spot to center on when {@link #centerViewOn(Spot)} is called asynchronously. */
	private Spot spotToCenterOn;
	/** The updater instance in charge of setting the views in T when {@link #setT(int)} is called. */
	private ViewRefresher targetTUpdater;
	/** The frame to move to asynchronously when {@link #setT(int)} is called. */ 
	private int targetT;
	/** The updater instance in charge of setting the views in T when {@link #moveZOf(int, ImagePlus)} is called. */
	private ViewRefresher targetZUpdater;
	private Map<ImagePlus, Integer> targetZMap;
	private ImagePlus targetImpForZUpdate;
	private final AdjustmentListener zAdjustementListener;
	private final AdjustmentListener timeAdjustmentListener;

	/*
	 * CONSTRUCTORS
	 */

	public MultiViewDisplayer(final Collection<ImagePlus> imps, final Map<ImagePlus, List<AffineTransform3D>> impMap, final TrackMateModel model) {
		super(model);
		this.imps = imps;
		this.transforms = impMap;
		this.targetZMap = new HashMap<ImagePlus, Integer>(imps.size());
		for (ImagePlus imagePlus : imps) {
			targetZMap.put(imagePlus, imagePlus.getZ());
		}
		this.targetT = imps.iterator().next().getT();
		// Set up spot view refresher
		this.spotViewUpdater = new ViewRefresher(new Refreshable() {
			@Override
			public void refresh() { refreshCenterViewOnSpot(); }
		});
		// Set up T refresher
		this.targetTUpdater = new ViewRefresher(new Refreshable() {
			@Override
			public void refresh() { refreshTargetT(); }
		});
		// Set up Z refresher
		this.targetZUpdater = new ViewRefresher(new Refreshable() {
			@Override
			public void refresh() { refreshTargetZ(); }
		});
		// Prepare Z adjustment listener - will be used in render()
		this.zAdjustementListener = new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(final AdjustmentEvent event) {
				new Thread() {
					@Override
					public void run() {
						ImagePlus source = ((ImageWindow) ((ScrollbarWithLabel) event.getSource()).getParent() ).getImagePlus();
						try {
							Thread.sleep(500);
							targetZMap.put(source, event.getValue());
						} catch (InterruptedException e) { 	}
					}
				}.start();					
			}
		};
		// Prepare T adjustment listener - will be used in render()
		this.timeAdjustmentListener = new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(final AdjustmentEvent event) {
				new Thread() {
					@Override
					public void run() {
						setT(event.getValue());
					}
				}.start();					
			}
		};
	}

	public MultiViewDisplayer(final Map<ImagePlus, List<AffineTransform3D>> transforms, final TrackMateModel model) {
		this(transforms.keySet(), transforms, model);
	}

	/*
	 * DEFAULT METHODS
	 */

	final Spot getCLickLocation(final Point point, ImagePlus imp) {
		OverlayedImageCanvas canvas = canvases.get(imp);
		if (null == canvas) {
			System.err.println("ImagePlus "+imp+" is unknown to this displayer "+this);
			return null;
		}
		final int frame = imp.getT() - 1;
		final double ix = canvas.offScreenXD(point.x) + 0.5;
		final double iy = canvas.offScreenYD(point.y) + 0.5;
		final double iz = imp.getSlice()-1;
		double[] physicalCoords = new double[3];
		double[] pixelCoords = new double[] { ix, iy, iz };
		transforms.get(imp).get(frame).applyInverse(physicalCoords, pixelCoords);

		if (DEBUG) {
			System.out.println("[MultiViewDisplayer] Got a mouse click on "+Util.printCoordinates(pixelCoords)+". Converted it to physical coords: "+Util.printCoordinates(physicalCoords));
		}

		return new Spot(physicalCoords);
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static TransformedSpotOverlay createSpotOverlay(final ImagePlus imp, final List<AffineTransform3D> list, final TrackMateModel model, final Map<String, Object> displaySettings) {
		return new TransformedSpotOverlay(model, imp, list, displaySettings);
	}

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static TransformedTrackOverlay createTrackOverlay(final ImagePlus imp, final List<AffineTransform3D> list, final TrackMateModel model, final Map<String, Object> displaySettings) {
		return new TransformedTrackOverlay(model, imp, list, displaySettings);
	}


	/*
	 * PUBLIC METHODS
	 */

	
	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (DEBUG)
			System.out.println("[MultiViewDisplayer] Received model changed event ID: "+event.getEventID()+" from "+event.getSource());
		boolean redoOverlay = false;

		switch (event.getEventID()) {

		case ModelChangeEvent.MODEL_MODIFIED:
			// Rebuild track overlay only if edges were added or removed, or if at least one spot was removed. 
			final Set<DefaultWeightedEdge> edges = event.getEdges();
			if (edges != null && edges.size() > 0) {
				redoOverlay = true;				
			} 
			break;

		case ModelChangeEvent.SPOTS_COMPUTED:
			for (TransformedSpotOverlay spotOverlay : spotOverlays.values()) {
				spotOverlay.computeSpotColors();
			}
			redoOverlay = true;
			break;

		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case ModelChangeEvent.TRACKS_COMPUTED:
			redoOverlay = true;
			break;
		}

		if (DEBUG)
			System.out.println("[MultiViewDisplayer] Do I need to refresh all views? "+redoOverlay);

		if (redoOverlay)
			refresh();
	}

	@Override
	public void centerViewOn(final Spot spot) {
		spotToCenterOn = spot;
		spotViewUpdater.doUpdate();
	}

	private final void refreshCenterViewOnSpot() {
		for (ImagePlus imp : imps) {
			double[] physicalCoords = new double[] {
					spotToCenterOn.getFeature(Spot.POSITION_X), 
					spotToCenterOn.getFeature(Spot.POSITION_Y), 
					spotToCenterOn.getFeature(Spot.POSITION_Z) 
			};
			double[] pixelCoords =  new double[3];
			int frame = spotToCenterOn.getFeature(Spot.FRAME).intValue();
			transforms.get(imp).get(frame).apply(physicalCoords, pixelCoords);
			long z = Math.round(pixelCoords[2]) + 1;
			imp.setPosition(1, (int) z, frame + 1);
		}
	}

	private final void refreshTargetT() {
		for (ImagePlus imp : imps) {
			imp.setT(targetT);
		}
	}

	private final void refreshTargetZ() {
		if (targetZMap.containsKey(targetImpForZUpdate)) {
			targetImpForZUpdate.setZ( targetZMap.get(targetImpForZUpdate) );
		}
	}

	public int getT() {
		return targetT;
	}


	public void quit() {
		spotViewUpdater.quit();
		targetTUpdater.quit();
		targetZUpdater.quit();
		model.removeTrackMateSelectionChangeListener(this);
		model.removeTrackMateModelChangeListener(this);
		for (ImagePlus imp : imps) {
			// Remove listeners for the sliders
			Component[] cs = imp.getWindow().getComponents();
			((ScrollbarWithLabel) cs[1]).removeAdjustmentListener ( zAdjustementListener ); 
			if (cs.length > 2) {
				((ScrollbarWithLabel) cs[2]).removeAdjustmentListener ( timeAdjustmentListener );
			}
			editTool.quit();
		}
		
	}
	
	@Override
	public void render() {
		clear();

		model.addTrackMateModelChangeListener(this);

		for (ImagePlus imp : imps) {
			imp.setOpenAsHyperStack(true);

			TransformedSpotOverlay spotOverlay = createSpotOverlay(imp, transforms.get(imp), model, displaySettings);
			spotOverlays.put(imp, spotOverlay);

			TransformedTrackOverlay trackOverlay = createTrackOverlay(imp, transforms.get(imp), model, displaySettings);
			trackOverlays.put(imp, trackOverlay);

			OverlayedImageCanvas canvas = new OverlayedImageCanvas(imp);
			canvases.put(imp, canvas);
			canvas.addOverlay(spotOverlay);
			canvas.addOverlay(trackOverlay);

			StackWindow window = new StackWindow(imp, canvas);
			window.setVisible(true);
			windows.put(imp,  window);

			// Add a listener for time slider
			Component[] cs = window.getComponents();
			
			/*
			 * We keep a reference to each Z for each imp when the user changes it with the keyboard.
			 * We therefore need to update it when the user changes the Z with the slider, but not 
			 * too quick to avoid clashes.
			 * 
			 */
			((ScrollbarWithLabel) cs[1]).addAdjustmentListener ( zAdjustementListener ); 
			
			/*
			 * When the user chnage the T with the slider, we change the T for all other views.
			 */
			if (cs.length > 2) {
				((ScrollbarWithLabel) cs[2]).addAdjustmentListener ( timeAdjustmentListener ); // We assume the 2nd is for time
			}

		}

		model.addTrackMateSelectionChangeListener(this);
		registerEditTool();

	}

	@Override
	public void selectionChanged(SelectionChangeEvent event) {
		super.selectionChanged(event);
		refresh();
	}

	@Override
	public void refresh() {
		for (ImagePlus imp : imps) {
			if (DEBUG) {
				System.out.println("[MultiViewDisplayer] Refreshing display of "+imp);
			}
			imp.updateAndDraw();
		}
	}

	@Override
	public void clear() {
		for (OverlayedImageCanvas canvas : canvases.values()) {
			if (canvas != null)
				canvas.clearOverlay();
		}
	}	

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public String toString() {
		return NAME;
	}


	/*
	 * PRIVATE METHODS
	 */

	private void registerEditTool() {
		editTool = MVSpotEditTool.getInstance();
		if (!MVSpotEditTool.isLaunched())
			editTool.run("");
		else {
			for (ImagePlus imp : imps) {
				editTool.imageOpened(imp);
			}
		}
		for (ImagePlus imp : imps) {
			editTool.register(imp, this);
		}
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		super.setDisplaySettings(key, value);
		// If we modified the feature coloring, then we recompute NOW the colors.
		if (key == TrackMateModelView.KEY_SPOT_COLOR_FEATURE) {
			for (TransformedSpotOverlay spotOverlay : spotOverlays.values()) {
				spotOverlay.computeSpotColors();
			}
		}
		if (key == TrackMateModelView.KEY_TRACK_COLORING) {
			TrackColorGenerator colorGenerator = (TrackColorGenerator) value;
			for (TransformedTrackOverlay trackOverlay : trackOverlays.values()) {
				// unregister the old one
				TrackColorGenerator oldColorGenerator = (TrackColorGenerator) displaySettings.get(KEY_TRACK_COLORING);
				oldColorGenerator.terminate();
				// pass the new one to the track overlay - we ignore its spot coloring and keep the spot coloring
				trackOverlay.setTrackColorGenerator(colorGenerator);
			}
		}
	}

	public TransformedSpotOverlay getSpotOverlay(final ImagePlus imp) {
		return spotOverlays.get(imp);
	}

	public OverlayedImageCanvas getCanvas(final ImagePlus imp) {
		return canvases.get(imp);
	}

	public  AffineTransform3D getTransform(final ImagePlus imp, final int frame) {
		return transforms.get(imp).get(frame);
	}

	public Settings getSttings() {
		return model.getSettings();
	}

	public void setT(final int targetT) {
		this.targetT = targetT;
		targetTUpdater.doUpdate();
	}

	public void moveZOf(ImagePlus targetImp, int inc) {
		targetImpForZUpdate = targetImp;
		if (targetZMap.containsKey(targetImpForZUpdate)) {
			int currentZ = targetZMap.get(targetImpForZUpdate);
			int targetZ = currentZ + inc;
			if (targetZ >= 1 && targetZ <= targetImpForZUpdate.getNSlices()) {
				targetZMap.put(targetImpForZUpdate, targetZ);
				targetZUpdater.doUpdate();
			}
		}
	}




	/*
	 * PRIVATE CLASSES
	 * Welcome to code duplication land. 
	 */


	/**
	 * Helper class that buffers request to set the view on a  particular spot.
	 * @see MultiViewDisplayer#spotToCenterOn
	 */
	private class ViewRefresher extends Thread {
		long request = 0;
		private final Refreshable refreshable;

		// Constructor autostarts thread
		ViewRefresher(final Refreshable refreshable) {
			super("TrackMate focus on spot thread");
			this.refreshable = refreshable;
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call displayer update from this thread
					if (r > 0)
						refreshable.refresh(); 
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private interface Refreshable {
		void refresh();
	}

}
