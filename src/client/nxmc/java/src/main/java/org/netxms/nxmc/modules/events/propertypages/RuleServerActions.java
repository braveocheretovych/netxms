/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2024 Victor Kirhenshtein
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
package org.netxms.nxmc.modules.events.propertypages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.netxms.client.ServerAction;
import org.netxms.client.events.ActionExecutionConfiguration;
import org.netxms.nxmc.base.widgets.SortableTableViewer;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.actions.dialogs.ActionSelectionDialog;
import org.netxms.nxmc.modules.events.dialogs.ActionExecutionConfigurationDialog;
import org.netxms.nxmc.modules.events.propertypages.helpers.ActionListLabelProvider;
import org.netxms.nxmc.modules.events.widgets.RuleEditor;
import org.netxms.nxmc.tools.ElementLabelComparator;
import org.netxms.nxmc.tools.WidgetHelper;
import org.xnap.commons.i18n.I18n;

/**
 * "Actions" property page for EPP rule
 */
public class RuleServerActions extends RuleBasePropertyPage
{
   private final I18n i18n = LocalizationHelper.getI18n(RuleServerActions.class);

	private SortableTableViewer viewer;
   private List<ActionExecutionConfiguration> actions = new ArrayList<ActionExecutionConfiguration>();
   private Button addButton;
	private Button editButton;
	private Button deleteButton;
   private Button activateButton;
   private Button deactivateButton;
	
   /**
    * Create property page.
    *
    * @param editor rule editor
    */
   public RuleServerActions(RuleEditor editor)
   {
      super(editor, LocalizationHelper.getI18n(RuleServerActions.class).tr("Server Actions"));
   }

   /**
    * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
    */
	@Override
	protected Control createContents(Composite parent)
	{
		Composite dialogArea = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = WidgetHelper.OUTER_SPACING;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
      dialogArea.setLayout(layout);

      final String[] columnNames = { i18n.tr("Action"), i18n.tr("State"), i18n.tr("Delay"), i18n.tr("Delay timer key"), i18n.tr("Snooze time"), i18n.tr("Snooze/blocking timer key") };
      final int[] columnWidths = { 300, 90, 90, 200, 90, 200 };
      viewer = new SortableTableViewer(dialogArea, columnNames, columnWidths, 0, SWT.UP, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
      viewer.setContentProvider(new ArrayContentProvider());
      viewer.setLabelProvider(new ActionListLabelProvider(editor.getEditorView()));
      viewer.setComparator(new ElementLabelComparator((ILabelProvider)viewer.getLabelProvider()));
      viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
            int size = viewer.getStructuredSelection().size();
				deleteButton.setEnabled(size > 0);
			}
      });
      viewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(DoubleClickEvent event)
         {
            editAction();
         }
      });

      for(ActionExecutionConfiguration c : rule.getActions())
         actions.add(new ActionExecutionConfiguration(c));
      viewer.setInput(actions);

      GridData gridData = new GridData();
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessVerticalSpace = true;
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.heightHint = 0;
      viewer.getControl().setLayoutData(gridData);

      Composite buttons = new Composite(dialogArea, SWT.NONE);
      RowLayout buttonLayout = new RowLayout();
      buttonLayout.type = SWT.HORIZONTAL;
      buttonLayout.pack = false;
      buttonLayout.marginLeft = 0;
      buttonLayout.marginRight = 0;
      buttons.setLayout(buttonLayout);
      gridData = new GridData();
      gridData.horizontalAlignment = SWT.RIGHT;
      buttons.setLayoutData(gridData);

      addButton = new Button(buttons, SWT.PUSH);
      addButton.setText(i18n.tr("&Add..."));
      addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				addAction();
			}
      });
      RowData rd = new RowData();
      rd.width = WidgetHelper.BUTTON_WIDTH_HINT;
      addButton.setLayoutData(rd);

      editButton = new Button(buttons, SWT.PUSH);
      editButton.setText(i18n.tr("&Edit..."));
      editButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            editAction();
         }
      });
      rd = new RowData();
      rd.width = WidgetHelper.BUTTON_WIDTH_HINT;
      editButton.setLayoutData(rd);
      
      deleteButton = new Button(buttons, SWT.PUSH);
      deleteButton.setText(i18n.tr("Delete"));
      deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				deleteAction();
			}
      });
      rd = new RowData();
      rd.width = WidgetHelper.BUTTON_WIDTH_HINT;
      deleteButton.setLayoutData(rd);
      deleteButton.setEnabled(false);
      
      activateButton = new Button(buttons, SWT.PUSH);
      activateButton.setText(i18n.tr("Activate"));
      activateButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            deactivate(false);
         }
      });
      rd = new RowData();
      rd.width = WidgetHelper.BUTTON_WIDTH_HINT;
      activateButton.setLayoutData(rd);
      
      deactivateButton = new Button(buttons, SWT.PUSH);
      deactivateButton.setText(i18n.tr("Deactivate"));
      deactivateButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            deactivate(true);
         }
      });
      rd = new RowData();
      rd.width = WidgetHelper.BUTTON_WIDTH_HINT;
      deactivateButton.setLayoutData(rd);
		
		return dialogArea;
	}

   /**
	 * Add new action
	 */
	private void addAction()
	{
      ActionSelectionDialog dlg = new ActionSelectionDialog(getShell(), editor.getEditorView().getActions());
		if (dlg.open() == Window.OK)
		{
         for(ServerAction a : dlg.getSelection())
            actions.add(new ActionExecutionConfiguration(a.getId(), null, null, null, null));
		}
      viewer.refresh();
	}

	/**
	 * Edit current action
	 */
	private void editAction()
	{
      IStructuredSelection selection = viewer.getStructuredSelection();
      if (selection.size() != 1)
         return;

      ActionExecutionConfigurationDialog dlg = new ActionExecutionConfigurationDialog(getShell(), (ActionExecutionConfiguration)selection.getFirstElement());
      if (dlg.open() == Window.OK)
      {
         viewer.update(selection.getFirstElement(), null);
      }
	}

	/**
	 * Delete action from list
	 */
	private void deleteAction()
	{
      IStructuredSelection selection = viewer.getStructuredSelection();
		Iterator<?> it = selection.iterator();
		if (it.hasNext())
		{
			while(it.hasNext())
			{
			   ActionExecutionConfiguration a = (ActionExecutionConfiguration)it.next();
            actions.remove(a);
			}
         viewer.refresh();
		}
	}

   /**
    * Activate/deactivate action
    * 
    * @param deactivate if action should be deactivated
    */
   protected void deactivate(boolean deactivate)
   {
      IStructuredSelection selection = viewer.getStructuredSelection();
      Iterator<?> it = selection.iterator();
      if (it.hasNext())
      {
         while(it.hasNext())
         {
            ActionExecutionConfiguration a = (ActionExecutionConfiguration)it.next();
            a.deactivate(deactivate);
         }
         viewer.refresh();
      }
   }


   /**
    * @see org.netxms.nxmc.base.propertypages.PropertyPage#applyChanges(boolean)
    */
   @Override
   protected boolean applyChanges(final boolean isApply)
	{
      rule.setActions(actions);
		editor.setModified(true);
      return true;
	}
}
