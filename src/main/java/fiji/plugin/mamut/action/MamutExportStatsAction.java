package fiji.plugin.mamut.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.action.TrackMateAction;

@Plugin( type = MamutActionFactory.class )
public class MamutExportStatsAction implements MamutActionFactory
{

	@Override
	public String getInfoText()
	{
		return ExportStatsToIJAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ExportStatsToIJAction.ICON;
	}

	@Override
	public String getKey()
	{
		return ExportStatsToIJAction.KEY;
	}

	@Override
	public String getName()
	{
		return ExportStatsToIJAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return new ExportStatsToIJAction( mamut.getSelectionModel() );
	}
}
