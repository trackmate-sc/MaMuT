package fiji.plugin.mamut.action;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.action.TrackMateAction;

public interface MamutActionFactory extends TrackMateModule
{

	public TrackMateAction create( MaMuT mamut );

}
