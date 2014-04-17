package fiji.plugin.mamut.providers;

import fiji.plugin.mamut.action.MamutActionFactory;
import fiji.plugin.trackmate.providers.AbstractProvider;

public class MamutActionProvider extends AbstractProvider< MamutActionFactory >
{

	public MamutActionProvider()
	{
		super( MamutActionFactory.class );
	}

	public static void main( final String[] args )
	{
		final MamutActionProvider provider = new MamutActionProvider();
		System.out.println( provider.echo() );
	}


}
