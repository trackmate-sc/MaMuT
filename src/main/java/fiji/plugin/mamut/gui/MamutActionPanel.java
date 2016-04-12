package fiji.plugin.mamut.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SpringLayout;

import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;

public class MamutActionPanel extends ListChooserPanel
{
	private static final long serialVersionUID = 1L;

	private final HashMap< String, ImageIcon > iconsMap;

	private final LogPanel logPanel;

	private final Action action;

	public MamutActionPanel( final List< String > items, final List< String > infoTexts, final List< ImageIcon > icons, final String typeName, final Action action )
	{
		super( items, infoTexts, typeName );
		this.action = action;
		this.iconsMap = new HashMap< String, ImageIcon >( icons.size() );
		for ( int i = 0; i < icons.size(); i++ )
		{
			iconsMap.put( items.get( i ), icons.get( i ) );
		}
		this.logPanel = new LogPanel();
		init();
	}

	/*
	 * PRIVATE METHODS
	 */

	private void init()
	{

		final SpringLayout layout = ( SpringLayout ) getLayout();
		layout.removeLayoutComponent( jLabelHelpText );

		add( logPanel );

		final JButton executeButton = new JButton( action );
		executeButton.setFont( FONT );
		add( executeButton );

		layout.putConstraint( SpringLayout.NORTH, jLabelHelpText, 5, SpringLayout.SOUTH, jComboBoxChoice );
		layout.putConstraint( SpringLayout.WEST, jLabelHelpText, 10, SpringLayout.WEST, this );
		layout.putConstraint( SpringLayout.EAST, jLabelHelpText, -10, SpringLayout.EAST, this );
		jLabelHelpText.setPreferredSize( new Dimension( 600, 150 ) );

		layout.putConstraint( SpringLayout.WEST, executeButton, 10, SpringLayout.WEST, this );
		layout.putConstraint( SpringLayout.EAST, executeButton, 170, SpringLayout.WEST, this );
		layout.putConstraint( SpringLayout.NORTH, executeButton, 5, SpringLayout.SOUTH, jLabelHelpText );

		layout.putConstraint( SpringLayout.NORTH, logPanel, 5, SpringLayout.SOUTH, executeButton );
		layout.putConstraint( SpringLayout.SOUTH, logPanel, -10, SpringLayout.SOUTH, this );
		layout.putConstraint( SpringLayout.WEST, logPanel, 10, SpringLayout.WEST, this );
		layout.putConstraint( SpringLayout.EAST, logPanel, -10, SpringLayout.EAST, this );

		final IconListRenderer renderer = new IconListRenderer();
		jComboBoxChoice.setRenderer( renderer );
	}

	/**
	 * Exposes the Log panel of this panel.
	 *
	 * @return the {@link LogPanel}.
	 */
	public LogPanel getLogPanel()
	{
		return logPanel;
	}

	/*
	 * INNER CLASS
	 */

	private class IconListRenderer extends DefaultListCellRenderer
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent( final JList< ? extends Object > list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
		{
			final JLabel label = ( JLabel ) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			final ImageIcon icon = iconsMap.get( value );
			label.setIcon( icon );
			return label;
		}
	}

}
