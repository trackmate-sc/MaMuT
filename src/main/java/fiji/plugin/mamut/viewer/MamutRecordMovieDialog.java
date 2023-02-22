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
package fiji.plugin.mamut.viewer;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.cache.CacheControl;
import bdv.export.ProgressWriter;
import bdv.util.Prefs;
import bdv.viewer.BasicViewerState;
import bdv.viewer.OverlayRenderer;
import bdv.viewer.ViewerState;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.render.RenderTarget;
import bdv.viewer.render.awt.BufferedImageRenderResult;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Adapted from BDV {@code RecordMovieDialog} to also record the MaMuT overlay.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class MamutRecordMovieDialog extends JDialog implements OverlayRenderer
{

	private static final long serialVersionUID = 1L;

	private final MamutViewerPanel viewer;

	private final int maxTimepoint;

	private final ProgressWriter progressWriter;

	private final JTextField pathTextField;

	private final JSpinner spinnerMinTimepoint;

	private final JSpinner spinnerMaxTimepoint;

	private final JSpinner spinnerWidth;

	private final JSpinner spinnerHeight;

	public MamutRecordMovieDialog( final Frame owner, final MamutViewerPanel viewer, final ProgressWriter progressWriter )
	{
		super( owner, "record movie", false );
		this.viewer = viewer;
		maxTimepoint = viewer.state().getNumTimepoints() - 1;
		this.progressWriter = progressWriter;

		final JPanel boxes = new JPanel();
		getContentPane().add( boxes, BorderLayout.NORTH );
		boxes.setLayout( new BoxLayout( boxes, BoxLayout.PAGE_AXIS ) );

		final JPanel saveAsPanel = new JPanel();
		saveAsPanel.setLayout( new BorderLayout( 0, 0 ) );
		boxes.add( saveAsPanel );

		saveAsPanel.add( new JLabel( "save to" ), BorderLayout.WEST );

		pathTextField = new JTextField( "./record/" );
		saveAsPanel.add( pathTextField, BorderLayout.CENTER );
		pathTextField.setColumns( 20 );

		final JButton browseButton = new JButton( "Browse" );
		saveAsPanel.add( browseButton, BorderLayout.EAST );

		final JPanel timepointsPanel = new JPanel();
		boxes.add( timepointsPanel );

		timepointsPanel.add( new JLabel( "timepoints from" ) );

		spinnerMinTimepoint = new JSpinner();
		spinnerMinTimepoint.setModel( new SpinnerNumberModel( 0, 0, maxTimepoint, 1 ) );
		timepointsPanel.add( spinnerMinTimepoint );

		timepointsPanel.add( new JLabel( "to" ) );

		spinnerMaxTimepoint = new JSpinner();
		spinnerMaxTimepoint.setModel( new SpinnerNumberModel( maxTimepoint, 0, maxTimepoint, 1 ) );
		timepointsPanel.add( spinnerMaxTimepoint );

		final JPanel widthPanel = new JPanel();
		boxes.add( widthPanel );
		widthPanel.add( new JLabel( "width" ) );
		spinnerWidth = new JSpinner();
		spinnerWidth.setModel( new SpinnerNumberModel( 800, 10, 5000, 1 ) );
		widthPanel.add( spinnerWidth );

		final JPanel heightPanel = new JPanel();
		boxes.add( heightPanel );
		heightPanel.add( new JLabel( "height" ) );
		spinnerHeight = new JSpinner();
		spinnerHeight.setModel( new SpinnerNumberModel( 600, 10, 5000, 1 ) );
		heightPanel.add( spinnerHeight );

		final JPanel buttonsPanel = new JPanel();
		boxes.add( buttonsPanel );
		buttonsPanel.setLayout( new BorderLayout( 0, 0 ) );

		final JButton recordButton = new JButton( "Record" );
		buttonsPanel.add( recordButton, BorderLayout.EAST );

		spinnerMinTimepoint.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int min = ( Integer ) spinnerMinTimepoint.getValue();
				final int max = ( Integer ) spinnerMaxTimepoint.getValue();
				if ( max < min )
					spinnerMaxTimepoint.setValue( min );
			}
		} );

		spinnerMaxTimepoint.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int min = ( Integer ) spinnerMinTimepoint.getValue();
				final int max = ( Integer ) spinnerMaxTimepoint.getValue();
				if ( min > max )
					spinnerMinTimepoint.setValue( max );
			}
		} );

		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled( false );
		fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

		browseButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				fileChooser.setSelectedFile( new File( pathTextField.getText() ) );
				final int returnVal = fileChooser.showSaveDialog( null );
				if ( returnVal == JFileChooser.APPROVE_OPTION )
				{
					final File file = fileChooser.getSelectedFile();
					pathTextField.setText( file.getAbsolutePath() );
				}
			}
		} );

		recordButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final String dirname = pathTextField.getText();
				final File dir = new File( dirname );
				if ( !dir.exists() )
					dir.mkdirs();
				if ( !dir.exists() || !dir.isDirectory() )
				{
					System.err.println( "Invalid export directory " + dirname );
					return;
				}
				final int minTimepointIndex = ( Integer ) spinnerMinTimepoint.getValue();
				final int maxTimepointIndex = ( Integer ) spinnerMaxTimepoint.getValue();
				final int width = ( Integer ) spinnerWidth.getValue();
				final int height = ( Integer ) spinnerHeight.getValue();
				new Thread()
				{
					@Override
					public void run()
					{
						try
						{
							recordButton.setEnabled( false );
							recordMovie( width, height, minTimepointIndex, maxTimepointIndex, dir );
							recordButton.setEnabled( true );
						}
						catch ( final Exception ex )
						{
							ex.printStackTrace();
						}
					}
				}.start();
			}
		} );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}

			private static final long serialVersionUID = 1L;
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	public void recordMovie( final int width, final int height, final int minTimepointIndex, final int maxTimepointIndex, final File dir ) throws IOException
	{
		final ViewerState renderState = new BasicViewerState( viewer.state().snapshot() );
		final int canvasW = viewer.getDisplay().getWidth();
		final int canvasH = viewer.getDisplay().getHeight();

		final AffineTransform3D affine = new AffineTransform3D();
		renderState.getViewerTransform( affine );
		affine.set( affine.get( 0, 3 ) - canvasW / 2, 0, 3 );
		affine.set( affine.get( 1, 3 ) - canvasH / 2, 1, 3 );
		affine.scale( ( double ) width / canvasW );
		affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
		affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
		renderState.setViewerTransform( affine );

		final ScaleBarOverlayRenderer scalebar = Prefs.showScaleBarInMovie() ? new ScaleBarOverlayRenderer() : null;

		class MyTarget implements RenderTarget< BufferedImageRenderResult >
		{
			final BufferedImageRenderResult renderResult = new BufferedImageRenderResult();

			@Override
			public BufferedImageRenderResult getReusableRenderResult()
			{
				return renderResult;
			}

			@Override
			public BufferedImageRenderResult createRenderResult()
			{
				return new BufferedImageRenderResult();
			}

			@Override
			public void setRenderResult( final BufferedImageRenderResult renderResult )
			{}

			@Override
			public int getWidth()
			{
				return width;
			}

			@Override
			public int getHeight()
			{
				return height;
			}
		}
		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, () -> {}, new double[] { 1 }, 0, 1, null, false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );
		progressWriter.setProgress( 0 );
		for ( int timepoint = minTimepointIndex; timepoint <= maxTimepointIndex; ++timepoint )
		{
			renderState.setCurrentTimepoint( timepoint );
			renderer.requestRepaint();
			renderer.paint( renderState );

			final BufferedImage bi = target.renderResult.getBufferedImage();
			final Graphics2D g2 = bi.createGraphics();
			if ( Prefs.showScaleBarInMovie() )
			{
				g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
				g2.setClip( 0, 0, width, height );
				scalebar.setViewerState( renderState );
				scalebar.paint( g2 );
			}

			g2.setClip( 0, 0, width, height );
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
			viewer.overlay.setViewerState( renderState );
			viewer.overlay.paint( g2 );

			ImageIO.write( bi, "png", new File( String.format( "%s/img-%03d.png", dir, timepoint ) ) );
			progressWriter.setProgress( ( double ) ( timepoint - minTimepointIndex + 1 ) / ( maxTimepointIndex - minTimepointIndex + 1 ) );
		}
	}

	@Override
	public void drawOverlays( final Graphics g )
	{}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		spinnerWidth.setValue( width );
		spinnerHeight.setValue( height );
	}

}
