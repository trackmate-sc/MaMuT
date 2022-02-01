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
