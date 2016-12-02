package fiji.plugin.mamut.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.CloseGapsByLinearInterpolationAction;
import fiji.plugin.trackmate.action.TrackMateAction;

@Plugin( type = MamutActionFactory.class )
public class MamutCloseGapsFactory implements MamutActionFactory
{

	@Override
	public String getInfoText()
	{
		return CloseGapsByLinearInterpolationAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return CloseGapsByLinearInterpolationAction.ICON;
	}

	@Override
	public String getKey()
	{
		return CloseGapsByLinearInterpolationAction.KEY;
	}

	@Override
	public String getName()
	{
		return CloseGapsByLinearInterpolationAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return new CloseGapsByLinearInterpolationAction();
	}
}
