/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2023 Victor Kirhenshtein
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.netxms.nxmc.modules.objects.views;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.netxms.client.objects.AbstractObject;
import org.netxms.client.objects.Node;
import org.netxms.client.topology.FdbEntry;
import org.netxms.nxmc.base.actions.CopyTableCellsAction;
import org.netxms.nxmc.base.actions.CopyTableRowsAction;
import org.netxms.nxmc.base.actions.ExportToCsvAction;
import org.netxms.nxmc.base.jobs.Job;
import org.netxms.nxmc.base.widgets.SortableTableViewer;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.objects.views.helpers.FDBComparator;
import org.netxms.nxmc.modules.objects.views.helpers.FDBFilter;
import org.netxms.nxmc.modules.objects.views.helpers.FDBLabelProvider;
import org.netxms.nxmc.resources.ResourceManager;
import org.netxms.nxmc.tools.WidgetHelper;
import org.xnap.commons.i18n.I18n;

/**
 * Switch forwarding database view
 */
public class SwitchForwardingDatabaseView extends ObjectView
{
   private final I18n i18n = LocalizationHelper.getI18n(SwitchForwardingDatabaseView.class);

	public static final int COLUMN_MAC_ADDRESS = 0;
   public static final int COLUMN_VENDOR = 1;
	public static final int COLUMN_PORT = 2;
	public static final int COLUMN_INTERFACE = 3;
   public static final int COLUMN_VLAN = 4;
	public static final int COLUMN_NODE = 5;
   public static final int COLUMN_TYPE = 6;

	private SortableTableViewer viewer;
   private boolean refreshPending = true;
   private Action actionExportToCsv;
   private Action actionExportAllToCsv;
   private Action actionCopyRowToClipboard;
   private Action actionCopyMACToClipboard;

   /**
    * Default constructor
    */
   public SwitchForwardingDatabaseView()
   {
      super(LocalizationHelper.getI18n(SwitchForwardingDatabaseView.class).tr("FDB"), ResourceManager.getImageDescriptor("icons/object-views/fdb.gif"), "objects.fdb", true);
   }

   /**
    * @see org.netxms.nxmc.modules.objects.views.ObjectView#isValidForContext(java.lang.Object)
    */
   @Override
   public boolean isValidForContext(Object context)
   {
      return (context != null) && (context instanceof Node) && ((Node)context).isBridge();
   }

   /**
    * @see org.netxms.nxmc.base.views.View#getPriority()
    */
   @Override
   public int getPriority()
   {
      return 150;
   }

   /**
    * @see org.netxms.nxmc.base.views.View#createContent(org.eclipse.swt.widgets.Composite)
    */
   @Override
   protected void createContent(Composite parent)
	{	   
		final String[] names = { 
            i18n.tr("MAC"), i18n.tr("NIC Vendor"), i18n.tr("Port"), i18n.tr("Interface"), i18n.tr("VLAN"), i18n.tr("Node"), i18n.tr("Type")
		   };
		final int[] widths = { 180, 200, 100, 200, 100, 250, 110 };
		viewer = new SortableTableViewer(parent, names, widths, COLUMN_MAC_ADDRESS, SWT.DOWN, SWT.FULL_SELECTION | SWT.MULTI);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new FDBLabelProvider(viewer));
		viewer.setComparator(new FDBComparator());
		FDBFilter filter = new FDBFilter();
		setFilterClient(viewer, filter);
		viewer.addFilter(filter);

      WidgetHelper.restoreTableViewerSettings(viewer, "SwitchForwardingDatabase");
		viewer.getTable().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e)
			{
            WidgetHelper.saveTableViewerSettings(viewer, "SwitchForwardingDatabase");
			}
		});

		createActions();
		createPopupMenu();
	}

	/**
	 * Create actions
	 */
   private void createActions()
	{
      actionCopyRowToClipboard = new CopyTableRowsAction(viewer, true);
      actionCopyMACToClipboard = new CopyTableCellsAction(viewer, 0, true, i18n.tr("Copy MAC address to clipboard"));
		actionExportToCsv = new ExportToCsvAction(this, viewer, true);
		actionExportAllToCsv = new ExportToCsvAction(this, viewer, false);
	}

	/**
	 * Create pop-up menu
	 */
	private void createPopupMenu()
	{
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr)
			{
				fillContextMenu(mgr);
			}
		});

		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	/**
	 * Fill context menu
	 * @param mgr Menu manager
	 */
	protected void fillContextMenu(IMenuManager manager)
	{
      manager.add(actionCopyRowToClipboard);
      manager.add(actionCopyMACToClipboard);
      manager.add(actionExportToCsv);
      manager.add(actionExportAllToCsv);
	}

   /**
    * @see org.netxms.nxmc.base.views.View#fillLocalToolBar(org.eclipse.jface.action.IToolBarManager)
    */
   @Override
   protected void fillLocalToolBar(IToolBarManager manager)
   {
      manager.add(actionExportAllToCsv);
   }

   /**
    * @see org.netxms.nxmc.base.views.View#fillLocalMenu(org.eclipse.jface.action.IMenuManager)
    */
   @Override
   protected void fillLocalMenu(IMenuManager manager)
   {
      manager.add(actionExportAllToCsv);
   }

   /**
    * @see org.netxms.nxmc.base.views.View#refresh()
    */
	@Override
   public void refresh()
	{
      final long objectId = getObjectId();
      if (objectId == 0)
         return;

      new Job(i18n.tr("Reading switch forwarding database"), this) {
         @Override
         protected void run(IProgressMonitor monitor) throws Exception
         {
            final List<FdbEntry> fdb = session.getSwitchForwardingDatabase(objectId);
            runInUIThread(new Runnable() {
               @Override
               public void run()
               {
                  viewer.setInput(fdb.toArray());
               }
            });
         }

         @Override
         protected String getErrorMessage()
         {
            return String.format(i18n.tr("Cannot get switch forwarding database for node %s"), session.getObjectName(objectId));
         }
      }.start();
	}

   /**
    * @see org.netxms.nxmc.base.views.View#activate()
    */
   @Override
   public void activate()
   {
      super.activate();
      if (refreshPending)
      {
         refreshPending = false;
         refresh();
      }
   }

   /**
    * @see org.netxms.nxmc.modules.objects.views.ObjectView#onObjectChange(org.netxms.client.objects.AbstractObject)
    */
   @Override
   protected void onObjectChange(AbstractObject object)
   {
      if (isActive())
         refresh();
      else
         refreshPending = true;
   }
}
