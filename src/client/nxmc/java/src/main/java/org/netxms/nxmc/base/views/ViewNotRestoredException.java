package org.netxms.nxmc.base.views;

/**
 * Exception thrown when node 
 */
public class ViewNotRestoredException extends Exception
{
   private static final long serialVersionUID = 7195603121566698942L; //TODO: read about it
   
   private String viewName; 

   //View name 
   //not restore reason
   
   //Create view that will get this exception and will show it on the screen 
   
   
   
   /**
    * Create exception with description message
    * 
    * @param message 
    */
   public ViewNotRestoredException(String viewName, String message)
   {
      super(message);
      this.viewName = viewName;
   }

   /**
    * Create exception with description message and exception
    * 
    * @param message
    * @param cause
    */
   public ViewNotRestoredException(String message, String viewName, Throwable cause)
   {
      super(message, cause);
      this.viewName = viewName;
   }

   /**
    * @return the viewName
    */
   public String getViewName()
   {
      return viewName;
   }
}
