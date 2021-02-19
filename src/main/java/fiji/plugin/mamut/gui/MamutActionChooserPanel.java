package fiji.plugin.mamut.gui;

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Icons.EXECUTE_ICON;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.action.MamutActionFactory;
import fiji.plugin.mamut.providers.MamutActionProvider;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.components.ModuleChooserPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class MamutActionChooserPanel extends ModuleChooserPanel< MamutActionFactory >
{

	private static final long serialVersionUID = 1L;

	public MamutActionChooserPanel( final MamutActionProvider actionProvider, final MaMuT mamut, final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		super( actionProvider, "action", CaptureOverlayAction.KEY );

		final LogPanel logPanel = new LogPanel();
		final GridBagConstraints gbcLogPanel = new GridBagConstraints();
		gbcLogPanel.insets = new Insets( 5, 5, 5, 5 );
		gbcLogPanel.fill = GridBagConstraints.BOTH;
		gbcLogPanel.gridx = 0;
		gbcLogPanel.gridy = 3;
		this.add( logPanel, gbcLogPanel );

		final JButton executeButton = new JButton( "Execute", EXECUTE_ICON );
		executeButton.setFont( FONT );
		final GridBagConstraints gbcExecBtn = new GridBagConstraints();
		gbcExecBtn.insets = new Insets( 5, 5, 5, 5 );
		gbcExecBtn.fill = GridBagConstraints.NONE;
		gbcExecBtn.anchor = GridBagConstraints.EAST;
		gbcExecBtn.gridx = 0;
		gbcExecBtn.gridy = 4;
		this.add( executeButton, gbcExecBtn );

		final Logger logger = logPanel.getLogger();
		executeButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				new Thread( "TrackMate action thread" )
				{
					@Override
					public void run()
					{
						try
						{
							executeButton.setEnabled( false );
							final String actionKey = MamutActionChooserPanel.this.getSelectedModuleKey();
							final TrackMateAction action = actionProvider.getFactory( actionKey ).create( mamut );
							if ( null == action )
							{
								logger.error( "Unknown action: " + actionKey + ".\n" );
							}
							else
							{
								action.setLogger( logger );
								action.execute(
										trackmate,
										selectionModel,
										displaySettings,
										( JFrame ) SwingUtilities.getWindowAncestor( MamutActionChooserPanel.this ) );
							}
						}
						finally
						{
							executeButton.setEnabled( true );
						}
					}
				}.start();
			}
		} );
	}
}
