package fiji.plugin.mamut.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.TrackBranchAnalysis;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.Icons;

@Plugin( type = MamutActionFactory.class )
public class MamutTrackBranchAnalysis implements MamutActionFactory
{

	private static final String INFO_TEXT = "<html>This action analyzes each branch of all tracks, "
			+ "and outputs in an ImageJ results table the number of its predecessors, "
			+ "of successors, and its duration."
			+ "<p>"
			+ "The results table is in sync with the selection. Clicking on a line will "
			+ "select the target branch.</html>";

	private static final String KEY = "TRACK_BRANCH_ANALYSIS";

	private static final String NAME = "Branch hierarchy analysis";

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return Icons.BRANCH_ICON_16x16;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return new TrackBranchAnalysis();
	}
}
