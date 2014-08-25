package fiji.plugin.mamut.gui;

import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.action.MamutActionFactory;
import fiji.plugin.mamut.providers.MamutActionProvider;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;

public class MamutGUI extends JFrame
{

	private static final long serialVersionUID = 1L;

	private final static ImageIcon ANNOTATION_ICON = new ImageIcon( MaMuT.class.getResource( "Logo50x50-color-nofont-72p.png" ) );

	private static final ImageIcon MAMUT_ICON = new ImageIcon( MaMuT.class.getResource( "mammouth-32x32.png" ) );

	private static final ImageIcon ACTION_ICON = new ImageIcon( MaMuT.class.getResource( "cog.png" ) );

	private static final Icon EXECUTE_ICON = new ImageIcon( MaMuT.class.getResource( "control_play_blue.png" ) );

	private final MamutControlPanel viewPanel;

	private final AnnotationPanel annotationPanel;

	private final MamutActionPanel actionPanel;

	public MamutGUI( final TrackMate trackmate, final MaMuT mamut )
	{
		setTitle( PLUGIN_NAME + " v" + PLUGIN_VERSION );
		setIconImage( MAMUT_ICON.getImage() );
		setSize( 320, 576 );
		setResizable( false );

		final JTabbedPane tabbedPane = new JTabbedPane( JTabbedPane.TOP );
		getContentPane().add( tabbedPane, BorderLayout.CENTER );
		setLocationByPlatform( true );
		setVisible( true );

		viewPanel = new MamutControlPanel( trackmate.getModel() );
		tabbedPane.addTab( "Views", MAMUT_ICON, viewPanel, "The control panel for views" );

		annotationPanel = new AnnotationPanel( mamut.getGuimodel() );
		tabbedPane.addTab( "Annotation", ANNOTATION_ICON, annotationPanel, "Annotation tools" );

		final MamutActionProvider actionProvider = new MamutActionProvider();
		final List< String > actionKeys = actionProvider.getVisibleKeys();
		final List< String > names = new ArrayList< String >( actionKeys.size() );
		final List< String > infoTexts = new ArrayList< String >( actionKeys.size() );
		final List< ImageIcon > icons = new ArrayList< ImageIcon >( actionKeys.size() );
		for ( final String key : actionKeys )
		{
			infoTexts.add( actionProvider.getFactory( key ).getInfoText() );
			icons.add( actionProvider.getFactory( key ).getIcon() );
			names.add( actionProvider.getFactory( key ).getName() );
		}

		final Action action = new AbstractAction( "Execute", EXECUTE_ICON )
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final int choice = actionPanel.getChoice();
				final String key = actionKeys.get( choice );
				final MamutActionFactory factory = actionProvider.getFactory( key );
				final TrackMateAction mamutAction = factory.create( mamut );
				mamutAction.setLogger( actionPanel.getLogPanel().getLogger() );
				mamutAction.execute( trackmate );
			}
		};
		actionPanel = new MamutActionPanel( names, infoTexts, icons, "action", action );
		tabbedPane.addTab( "Actions", ACTION_ICON, actionPanel, "Actions" );
	}

	public MamutControlPanel getViewPanel()
	{
		return viewPanel;
	}

	public AnnotationPanel getAnnotationPanel()
	{
		return annotationPanel;
	}
}
