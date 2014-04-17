package fiji.plugin.mamut.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

@Plugin( type = MamutActionFactory.class )
public class MamutExtractStackActionFactory implements MamutActionFactory
{

	@Override
	public String getInfoText()
	{
		return MamutExtractTrackStackAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return MamutExtractTrackStackAction.ICON;
	}

	@Override
	public String getKey()
	{
		return MamutExtractTrackStackAction.KEY;
	}

	@Override
	public String getName()
	{
		return MamutExtractTrackStackAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		final Float radiusRatio = ( Float ) mamut.getGuimodel().getDisplaySettings().get( TrackMateModelView.KEY_SPOT_RADIUS_RATIO );
		float rad;
		if (radiusRatio != null) {
			rad = radiusRatio.floatValue();
		} else {
			rad = 1f;
		}
		return new MamutExtractTrackStackAction( mamut.getSelectionModel(), rad );
	}

}
