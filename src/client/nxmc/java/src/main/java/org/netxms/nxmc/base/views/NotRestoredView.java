package org.netxms.nxmc.base.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.netxms.nxmc.Memento;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.resources.ResourceManager;

/**
 * View replacement for non restorable views
 */
public class NotRestoredView extends View
{
   private String viewName;
   private String message;
   
   /**
    * View constructor from exception
    * 
    * @param exception 
    */
   public NotRestoredView(ViewNotRestoredException exception)
   {
      super(LocalizationHelper.getI18n(NotRestoredView.class).tr("Not Restored"), ResourceManager.getImageDescriptor("icons/invalid-report.png"), exception.getViewName());
      viewName = exception.getViewName();
      message = exception.getMessage();
   }
   
   /**
    * Restore view constructor
    */
   protected NotRestoredView()
   {
      super(LocalizationHelper.getI18n(NotRestoredView.class).tr("Not Restored"), ResourceManager.getImageDescriptor("icons/invalid-report.png"), null);
   }

   /**
    * @see org.netxms.nxmc.base.views.View#createContent(org.eclipse.swt.widgets.Composite)
    */
   @Override
   protected void createContent(Composite parent)
   {
      Text text = new Text(parent, SWT.READ_ONLY);
      text.setText(viewName + ": " + message);
   }

   /**
    * @see org.netxms.nxmc.base.views.View#saveState(org.netxms.nxmc.Memento)
    */
   public void saveState(Memento memento)
   {      
      super.saveState(memento);
      memento.set("viewName", viewName);
      memento.set("message", message);
   }

   /**
    * @throws ViewNotRestoredException 
    * @see org.netxms.nxmc.base.views.ViewWithContext#restoreState(org.netxms.nxmc.Memento)
    */
   @Override
   public void restoreState(Memento memento) throws ViewNotRestoredException
   {      
      super.restoreState(memento);
      viewName = memento.getAsString("viewName");
      message = memento.getAsString("message");
   }
}
