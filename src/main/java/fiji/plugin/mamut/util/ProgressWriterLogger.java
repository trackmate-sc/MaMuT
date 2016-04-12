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
