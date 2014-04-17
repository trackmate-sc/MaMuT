package fiji.plugin.mamut.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.ExtractTrackStackAction;
import fiji.plugin.trackmate.action.TrackMateAction;

@Plugin( type = MamutActionFactory.class )
public class MamutExtractStackActionFactory implements MamutActionFactory
{

	@Override
	public String getInfoText()
	{
		return ExtractTrackStackAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ExtractTrackStackAction.ICON;
	}

	@Override
	public String getKey()
	{
		return ExtractTrackStackAction.KEY;
	}

	@Override
	public String getName()
	{
		return ExtractTrackStackAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return new ExtractTrackStackAction( mamut.getSelectionModel() );
	}

}
