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
package fiji.plugin.mamut.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.scijava.ui.behaviour.util.AbstractNamedAction;

import bdv.BigDataViewerActions;
import bdv.tools.HelpDialog;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.Logger;

public class MamutActions
{


	/**
	 * Ratio (times the {@link MaMuT#RADIUS_CHANGE_FACTOR}) by which we change
	 * the radius upon change radius action.
	 */
	private static final double NORMAL_RADIUS_CHANGE = 1.;

	/**
	 * Ratio (times the {@link MaMuT#RADIUS_CHANGE_FACTOR}) by which we change
	 * the radius upon change radius a but action.
	 */
	private static final double ABIT_RADIUS_CHANGE = 0.1;

	/**
	 * Ratio (times the {@link MaMuT#RADIUS_CHANGE_FACTOR}) by which we change
	 * the radius upon change radius a lot action.
	 */
	private static final double ALOT_RADIUS_CHANGE = 10.;

	private MamutActions()
	{}

	public static final Action getAddSpotAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new AddSpotAction( mamut, viewer );
	}

	public static final Action getDeleteSpotAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new DeleteSpotAction( mamut, viewer );
	}

	public static final Action getSemiAutoTrackingAction( final MaMuT mamut )
	{
		return new SemiAutoTrackingAction( mamut );
	}

	public static final Action getShowHelpAction( final MamutViewer viewer )
	{
		return new ShowHelpAction( viewer );
	}

	public static final Action getStepWiseTimeBrowsingAction( final MaMuT mamut, final MamutViewer viewer, final boolean forward )
	{
		return new StepWiseTimeBrowsingAction( mamut, viewer, forward );
	}

	public static final Action getToggleBrightnessDialogAction( final MaMuT mamut )
	{
		return new ToggleBrightnessDialogAction( mamut );
	}

	public static final Action getToggleManualTransformAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new ToggleManualTransformAction( mamut, viewer );
	}

	public static final Action getIncreaseRadiusAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new IncreaseRadiusAction( mamut, viewer );
	}

	public static final Action getIncreaseRadiusALotAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new IncreaseRadiusALotAction( mamut, viewer );
	}

	public static final Action getIncreaseRadiusABitAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new IncreaseRadiusABitAction( mamut, viewer );
	}

	public static final Action getDecreaseRadiusAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new DecreaseRadiusAction( mamut, viewer );
	}

	public static final Action getDecreaseRadiusALotAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new DecreaseRadiusALotAction( mamut, viewer );
	}

	public static final Action getDecreaseRadiusABitAction( final MaMuT mamut, final MamutViewer viewer )
	{
		return new DecreaseRadiusABitAction( mamut, viewer );
	}

	public static final Action getToggleLinkingModeAction( final MaMuT mamut, final Logger logger )
	{
		return new ToggleLinkingModeAction( mamut, logger );
	}

	public static final Action getToggleLinkAction( final MaMuT mamut, final Logger logger )
	{
		return new ToggleLinkAction( mamut, logger );
	}

	/*
	 * INNER CLASSES
	 */

	private static final class StepWiseTimeBrowsingAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private final boolean forward;

		private final MamutGUIModel guiModel;

		private final MamutViewer viewer;

		public StepWiseTimeBrowsingAction( final MaMuT mamut, final MamutViewer viewer, final boolean forward )
		{
			this.viewer = viewer;
			this.guiModel = mamut.getGuimodel();
			this.forward = forward;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			final int currentT = viewer.getViewerPanel().state().getCurrentTimepoint();
			final int prevStep = ( currentT / guiModel.timeStep ) * guiModel.timeStep;
			int tp;
			if ( forward )
			{
				tp = prevStep + guiModel.timeStep;
			}
			else
			{
				if ( currentT == prevStep )
				{
					tp = currentT - guiModel.timeStep;
				}
				else
				{
					tp = prevStep;
				}
			}

			if ( tp < 0 )
				tp = 0;

			if ( tp > viewer.getViewerPanel().state().getNumTimepoints() - 1 )
				tp = viewer.getViewerPanel().state().getNumTimepoints() - 1;

			viewer.getViewerPanel().setTimepoint( tp );
		}
	}

	private static final class ToggleLinkingModeAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final Logger logger;

		public ToggleLinkingModeAction( final MaMuT mamut, final Logger logger )
		{
			this.mamut = mamut;
			this.logger = logger;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			mamut.toggleLinkingMode( logger );
		}
	}

	private static final class ToggleLinkAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final Logger logger;

		public ToggleLinkAction( final MaMuT mamut, final Logger logger )
		{
			this.mamut = mamut;
			this.logger = logger;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			mamut.toggleLink( logger );
		}

	}

	private static final class ToggleBrightnessDialogAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		public ToggleBrightnessDialogAction( final MaMuT mamut )
		{
			this.mamut = mamut;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			mamut.toggleBrightnessDialog();
		}

	}

	private static final class ToggleManualTransformAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public ToggleManualTransformAction( final MaMuT mamut, final MamutViewer viewer )
		{
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			mamut.toggleManualTransformation( viewer );
		}

	}

	private static final class ShowHelpAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MamutViewer viewer;

		private HelpDialog helpDialog;

		public ShowHelpAction( final MamutViewer viewer )
		{
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( null == helpDialog )
			{
				helpDialog = new HelpDialog( viewer, MaMuT.class.getResource( "Help.html" ) );
			}
			helpDialog.setVisible( !helpDialog.isVisible() );
			viewer.getViewerPanel().requestFocus();
		}

	}

	private static final class AddSpotAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public AddSpotAction( final MaMuT mamut, final MamutViewer viewer )
		{
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			mamut.addSpot( viewer );
		}

	}

	private static final class DeleteSpotAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public DeleteSpotAction( final MaMuT mamut, final MamutViewer viewer )
		{
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			mamut.deleteSpot( viewer );
		}

	}

	private static final class SemiAutoTrackingAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		public SemiAutoTrackingAction( final MaMuT mamut )
		{
			this.mamut = mamut;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			mamut.semiAutoDetectSpot();
		}
	}

	private static final class IncreaseRadiusAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public IncreaseRadiusAction( final MaMuT mamut, final MamutViewer viewer )
		{
			super( "increase radius" );
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			mamut.increaseSpotRadius( viewer, NORMAL_RADIUS_CHANGE );
		}
	}

	private static final class IncreaseRadiusALotAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public IncreaseRadiusALotAction( final MaMuT mamut, final MamutViewer viewer )
		{
			super( "increase spot radius a lot" );
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			mamut.increaseSpotRadius( viewer, ALOT_RADIUS_CHANGE );
		}
	}

	private static final class IncreaseRadiusABitAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public IncreaseRadiusABitAction( final MaMuT mamut, final MamutViewer viewer )
		{
			super( "increase spot radius a bit" );
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			mamut.increaseSpotRadius( viewer, ABIT_RADIUS_CHANGE );
		}
	}

	private static final class DecreaseRadiusAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public DecreaseRadiusAction( final MaMuT mamut, final MamutViewer viewer )
		{
			super( "decrease spot radius" );
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			mamut.increaseSpotRadius( viewer, -NORMAL_RADIUS_CHANGE / ( 1 + MaMuT.RADIUS_CHANGE_FACTOR * NORMAL_RADIUS_CHANGE ) );
		}
	}

	private static final class DecreaseRadiusALotAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public DecreaseRadiusALotAction( final MaMuT mamut, final MamutViewer viewer )
		{
			super( "decrease spot radius a lot" );
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			mamut.increaseSpotRadius( viewer, -ALOT_RADIUS_CHANGE / ( 1 + MaMuT.RADIUS_CHANGE_FACTOR * ALOT_RADIUS_CHANGE ) );
		}
	}

	private static final class DecreaseRadiusABitAction extends AbstractAction
	{

		private static final long serialVersionUID = 1L;

		private final MaMuT mamut;

		private final MamutViewer viewer;

		public DecreaseRadiusABitAction( final MaMuT mamut, final MamutViewer viewer )
		{
			super( "decrease spot radius a bit" );
			this.mamut = mamut;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			mamut.increaseSpotRadius( viewer, -ABIT_RADIUS_CHANGE / ( 1 + MaMuT.RADIUS_CHANGE_FACTOR * ABIT_RADIUS_CHANGE ) );
		}
	}

	/*
	 * CLASSES COPIED FROM BDV.
	 */

	private static abstract class MaMuTViewerAction extends AbstractNamedAction
	{
		protected final MamutViewer viewer;

		public MaMuTViewerAction( final String name, final MamutViewer viewer )
		{
			super( name );
			this.viewer = viewer;
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SetBookmarkAction extends MaMuTViewerAction
	{
		public SetBookmarkAction( final MamutViewer viewer )
		{
			super( BigDataViewerActions.SET_BOOKMARK, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.initSetBookmark();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class GoToBookmarkAction extends MaMuTViewerAction
	{
		public GoToBookmarkAction( final MamutViewer viewer )
		{
			super( BigDataViewerActions.GO_TO_BOOKMARK, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.initGoToBookmark();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class GoToBookmarkRotationAction extends MaMuTViewerAction
	{
		public GoToBookmarkRotationAction( final MamutViewer viewer )
		{
			super( BigDataViewerActions.GO_TO_BOOKMARK_ROTATION, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.initGoToBookmarkRotation();
		}

		private static final long serialVersionUID = 1L;
	}
}
