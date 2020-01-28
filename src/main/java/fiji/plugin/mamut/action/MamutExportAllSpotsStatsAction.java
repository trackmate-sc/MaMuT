package fiji.plugin.mamut.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.ExportAllSpotsStatsAction;
import fiji.plugin.trackmate.action.TrackMateAction;

@Plugin( type = MamutActionFactory.class )
public class MamutExportAllSpotsStatsAction implements MamutActionFactory
{

	@Override
	public String getInfoText()
	{
		return ExportAllSpotsStatsAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ExportAllSpotsStatsAction.ICON;
	}

	@Override
	public String getKey()
	{
		return ExportAllSpotsStatsAction.KEY;
	}

	@Override
	public String getName()
	{
		return ExportAllSpotsStatsAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return new ExportAllSpotsStatsAction( mamut.getSelectionModel() );
	}

}
