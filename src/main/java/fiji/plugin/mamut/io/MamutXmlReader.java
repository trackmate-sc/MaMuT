/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2021 MaMuT development team.
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
package fiji.plugin.mamut.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_WIDTH;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_X;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE_POSITION_Y;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdom2.DataConversionException;
import org.jdom2.Element;

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.brightness.SetupAssignments;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.mamut.viewer.MamutViewerFactory;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class MamutXmlReader extends TmXmlReader
{

	public MamutXmlReader( final File file )
	{
		super( file );
	}

	/**
	 * Returns the collection of views that were saved in this file. The views
	 * returned <b>will be rendered</b>.
	 *
	 * @param provider
	 *            the {@link ViewProvider} to instantiate the view. Each saved
	 *            view must be known by the specified provider.
	 * @return the collection of views.
	 * @see TrackMateModelView#render()
	 */
	@Override
	public Collection< TrackMateModelView > getViews( final ViewProvider provider, final Model model, final Settings settings, final SelectionModel selectionModel )
	{
		final Element guiel = root.getChild( GUI_STATE_ELEMENT_KEY );
		if ( null != guiel )
		{

			final List< Element > children = guiel
					.getChildren( GUI_VIEW_ELEMENT_KEY );
			final Collection< TrackMateModelView > views = new ArrayList< >(
					children.size() );

			for ( final Element child : children )
			{
				final String viewKey = child
						.getAttributeValue( GUI_VIEW_ATTRIBUTE );
				if ( null == viewKey )
				{
					logger.error( "Could not find view key attribute for element "
							+ child + ".\n" );
					ok = false;
				}
				else
				{

					final ViewFactory viewFactory = provider.getFactory( viewKey );

					if ( null == viewFactory )
					{
						logger.error( "Unknown view for key " + viewKey + ".\n" );
						ok = false;

					}
					else
					{

						final TrackMateModelView view = viewFactory.create( model, settings, selectionModel );
						views.add( view );

						new Thread( "MaMuT view rendering thread")
						{
							@Override
							public void run()
							{

								if ( viewKey.equals( MamutViewerFactory.KEY ) )
								{
									final MamutViewer mv = ( MamutViewer ) view;
									// mv.render();

									try
									{
										final int mvx = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_X )
												.getIntValue();
										final int mvy = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_Y )
												.getIntValue();
										final int mvwidth = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_WIDTH )
												.getIntValue();
										final int mvheight = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT )
												.getIntValue();
										mv.setLocation( mvx, mvy );
										mv.setSize( mvwidth, mvheight );
									}
									catch ( final DataConversionException e )
									{
										e.printStackTrace();
									}

								}
								else if ( viewKey.equals( "TRACKSCHEME" ) )
								{
									final TrackScheme ts = ( TrackScheme ) view;
									// ts.render();

									try
									{
										final int mvx = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_X )
												.getIntValue();
										final int mvy = child.getAttribute(
												GUI_VIEW_ATTRIBUTE_POSITION_Y )
												.getIntValue();
										final int mvwidth = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_WIDTH )
												.getIntValue();
										final int mvheight = child
												.getAttribute(
														GUI_VIEW_ATTRIBUTE_POSITION_HEIGHT )
												.getIntValue();
										ts.getGUI().setLocation( mvx, mvy );
										ts.getGUI().setSize( mvwidth, mvheight );
									}
									catch ( final DataConversionException e )
									{
										e.printStackTrace( );
									}
								}
							}
						}.start();
					}
				}
			}
			return views;

		}
		
		logger.error( "Could not find GUI state element.\n" );
		ok = false;
		return null;
	}

	public void readSetupAssignments( final SetupAssignments setupAssignments )
	{
		final Element guiel = root.getChild( GUI_STATE_ELEMENT_KEY );
		if ( null != guiel )
		{
			// brightness & color settings
			if ( guiel.getChild( "SetupAssignments" ) != null )
			{
				setupAssignments.restoreFromXml( guiel );
			}
			else
			{
				logger.error( "Could not find SetupAssignments element.\n" );
			}
		}
		else
		{
			logger.error( "Could not find GUI state element.\n" );
			ok = false;
		}
	}

	public void readBookmarks( final Bookmarks bookmarks )
	{
		final Element guiel = root.getChild( GUI_STATE_ELEMENT_KEY );
		if ( null != guiel )
		{
			bookmarks.restoreFromXml( guiel );
		}
		else
		{
			logger.error( "Could not find GUI state element.\n" );
			ok = false;
		}
	}

}
