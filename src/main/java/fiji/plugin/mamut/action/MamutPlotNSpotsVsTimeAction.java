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
package fiji.plugin.mamut.action;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.PlotNSpotsVsTimeAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.util.ExportableChartPanel;

@Plugin( type = MamutActionFactory.class )
public class MamutPlotNSpotsVsTimeAction extends AbstractTMAction implements MamutActionFactory
{

	@Override
	public String getInfoText()
	{
		return PlotNSpotsVsTimeAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return PlotNSpotsVsTimeAction.ICON;
	}

	@Override
	public String getKey()
	{
		return PlotNSpotsVsTimeAction.KEY;
	}

	@Override
	public String getName()
	{
		return PlotNSpotsVsTimeAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{
		return this;
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		// Collect data
		final Model model = trackmate.getModel();
		final Settings settings = trackmate.getSettings();
		final SpotCollection spots = model.getSpots();
		final int nFrames = spots.keySet().size();
		final double[][] data = new double[ 2 ][ nFrames ];
		int index = 0;
		for ( final int frame : spots.keySet() )
		{
			data[ 1 ][ index ] = spots.getNSpots( frame, true );
			if ( data[ 1 ][ index ] > 0 )
				data[ 0 ][ index ] = spots.iterator( frame, false ).next().getFeature( Spot.POSITION_T );
			else
				data[ 0 ][ index ] = frame * settings.dt;

			index++;
		}

		// Plot data
		final String xAxisLabel = "Time (" + trackmate.getModel().getTimeUnits() + ")";
		final String yAxisLabel = "N spots";
		final String title = "Nspots vs Time";
		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries( "Nspots", data );

		final JFreeChart chart = ChartFactory.createXYLineChart( title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false );
		chart.getTitle().setFont( FONT );
		chart.getLegend().setItemFont( SMALL_FONT );

		// The plot
		final XYPlot plot = chart.getXYPlot();
		plot.getRangeAxis().setLabelFont( FONT );
		plot.getRangeAxis().setTickLabelFont( SMALL_FONT );
		plot.getDomainAxis().setLabelFont( FONT );
		plot.getDomainAxis().setTickLabelFont( SMALL_FONT );

		final ExportableChartPanel panel = new ExportableChartPanel( chart );

		final JFrame frame = new JFrame( title );
		frame.setSize( 500, 270 );
		frame.getContentPane().add( panel );
		frame.setVisible( true );
	}

}
