package fiji.plugin.mamut.action;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.MergeFileAction;
import fiji.plugin.trackmate.action.TrackMateAction;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

@Plugin( type = MamutActionFactory.class )
public class MamutMergeFileActionFactory implements MamutActionFactory
{

	private static final String NAME = "Merge another annotation";

	@Override
	public String getInfoText()
	{
		return MergeFileAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return MergeFileAction.ICON;
	}

	@Override
	public String getKey()
	{
		return MergeFileAction.KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return new MergeFileAction( mamut.getGUI() );
	}

}
