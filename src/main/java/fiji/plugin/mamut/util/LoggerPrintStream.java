package fiji.plugin.mamut.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import fiji.plugin.trackmate.Logger;

/**
 * A class that wraps a TrackMate {@link Logger} and exposes {@link PrintStream}
 * s that writes into this logger.
 * <p>
 * The PrintStream adaptation is taken from ImageJ LogStream class.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class LoggerPrintStream
{
	private final String endOfLineSystem = System.getProperty( "line.separator" );

	private final String endOfLineShort = String.format( "\n" );

	private final OutPrintStream outstream;

	private final ErrPrintStream errstream;

	public LoggerPrintStream( final Logger logger )
	{
		this.outstream = new OutPrintStream( logger );
		this.errstream = new ErrPrintStream( logger );
	}

	public PrintStream out()
	{
		return outstream;
	}

	public PrintStream err()
	{
		return errstream;
	}

	private abstract class PrintStreamAdaptor extends PrintStream
	{
		protected ByteArrayOutputStream byteStream;

		protected PrintStreamAdaptor()
		{
			super( new ByteArrayOutputStream() );
			this.byteStream = ( ByteArrayOutputStream ) this.out;
		}

		@Override
		// ever called?
		public void write( final byte[] b )
		{
			this.write( b, 0, b.length );
		}

		@Override
		public void write( final byte[] b, final int off, final int len )
		{
			final String msg = new String( b, off, len );
			if ( msg.equals( endOfLineSystem ) || msg.equals( endOfLineShort ) )
			{ // this is a newline sequence only
				ejectBuffer();
			}
			else
			{
				byteStream.write( b, off, len ); // append message to buffer
				if ( msg.endsWith( endOfLineSystem ) || msg.endsWith( endOfLineShort ) )
				{ // line terminated by Newline
					// note that this does not seem to happen ever (even with
					// format)!?
					ejectBuffer();
				}
			}
		}

		@Override
		public void write( final int b )
		{
			byteStream.write( b );
		}

		@Override
		public void flush()
		{
			if ( byteStream.size() > 0 )
			{
				final String msg = byteStream.toString();
				if ( msg.endsWith( endOfLineSystem ) || msg.endsWith( endOfLineShort ) )
					ejectBuffer();
			}
			super.flush();
		}

		@Override
		public void close()
		{
			super.close();
		}

		protected abstract void ejectBuffer();

	}

	private class OutPrintStream extends PrintStreamAdaptor
	{

		private final Logger logger;

		public OutPrintStream( final Logger logger )
		{
			this.logger = logger;
		}

		@Override
		protected void ejectBuffer()
		{
			logger.log( byteStream.toString() );
			byteStream.reset();
		}
	}

	private class ErrPrintStream extends PrintStreamAdaptor
	{
		private final Logger logger;

		public ErrPrintStream( final Logger logger )
		{
			this.logger = logger;
		}

		@Override
		protected void ejectBuffer()
		{
			logger.error( byteStream.toString() );
			byteStream.reset();
		}
	}

}
