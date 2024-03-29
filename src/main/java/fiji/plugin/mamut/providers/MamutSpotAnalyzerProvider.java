/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2023 MaMuT development team.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.mamut.providers;

import fiji.plugin.mamut.feature.spot.MamutSpotAnalyzerFactory;
import fiji.plugin.trackmate.providers.AbstractProvider;

/**
 * A provider for the spot analyzer factories for MaMuT only.
 */
@SuppressWarnings( "rawtypes" )
public class MamutSpotAnalyzerProvider extends AbstractProvider< MamutSpotAnalyzerFactory >
{

	private final int nChannels;

	public MamutSpotAnalyzerProvider( final int nChannels )
	{
		super( MamutSpotAnalyzerFactory.class );
		this.nChannels = nChannels;
	}

	@Override
	public MamutSpotAnalyzerFactory getFactory( final String key )
	{
		final MamutSpotAnalyzerFactory factory = super.getFactory( key );
		if ( factory == null )
			return null;

		factory.setNChannels( nChannels );
		return factory;
	}

	public static void main( final String[] args )
	{
		final MamutSpotAnalyzerProvider provider = new MamutSpotAnalyzerProvider( 2 );
		System.out.println( provider.echo() );
	}
}
