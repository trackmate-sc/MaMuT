package fiji.plugin.mamut.providers;

import java.util.ArrayList;
import java.util.Collection;

import viewer.render.SourceAndConverter;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class MamutViewProvider extends ViewProvider {

	private SourceSettings ss;

	public MamutViewProvider(Model model, Settings settings, SelectionModel selectionModel) {
		super(model, settings, selectionModel);
		if (!(settings instanceof SourceSettings)) {
			throw new IllegalArgumentException("Settings must be an instance of SourceSettings.");
		}
		this.ss = (SourceSettings) settings;
	}
	
	@Override
	protected void registerViews() {
		names = new ArrayList<String>(2);
		names.add(TrackScheme.KEY);
		names.add(MamutViewer.KEY);
	}
	
	@Override
	public TrackMateModelView getView(String key) {
		TrackMateModelView val = super.getView(key);
		if (null == val) {
			if (key.equals(MamutViewer.KEY)) {
				
				Collection<SourceAndConverter<?>> sources = ss.getSources();
				val = new MamutViewer(MaMuT.DEFAULT_WIDTH, MaMuT.DEFAULT_HEIGHT, 
						sources, ss.nframes, model, selectionModel);
			}
		}
		
		return val;
	}

}
