/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2022 MaMuT development team.
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
package fiji.plugin.mamut.util;

import java.awt.Color;
import java.io.PrintStream;

import bdv.export.ProgressWriter;
import fiji.plugin.trackmate.Logger;

/**
 * A class which provides a BDV {@link ProgressWriter} that reuses a TrackMate
 * {@link Logger}.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class ProgressWriterLogger extends Logger implements ProgressWriter
{
	private final Logger logger;

	private final LoggerPrintStream ps;

	public ProgressWriterLogger( final Logger logger )
	{
		this.ps = new LoggerPrintStream( logger );
		this.logger = logger;
	}

	@Override
	public void log( final String message, final Color color )
	{
		logger.log( message, color );
	}

	@Override
	public void error( final String message )
	{
		logger.error( message );
	}

	@Override
	public void setProgress( final double val )
	{
		logger.setProgress( val );
	}

	@Override
	public void setStatus( final String status )
	{
		logger.setStatus( status );
	}

	@Override
	public PrintStream out()
	{
		return ps.out();
	}

	@Override
	public PrintStream err()
	{
		return ps.err();
	}

}
