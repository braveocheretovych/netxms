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
package org.netxms.nxmc.modules.events.propertypages.helpers;

import java.text.DateFormat;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.netxms.client.events.TimeFrame;
import org.netxms.nxmc.localization.DateFormatFactory;
import org.netxms.nxmc.localization.LocalizationHelper;

/**
 * Label provider for event template objects
 */
public class TimeFrameLabelProvider extends LabelProvider implements IColorProvider
{   
   private static final Color HINT_FOREGROUND = new Color(Display.getDefault(), 192, 192, 192);

   /**
    * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
    */
	@Override
   public String getText(Object element)
   {
      if (element instanceof String)
         return (String)element;

      TimeFrame frame = (TimeFrame)element;
      final DateFormat dfTime = DateFormatFactory.getShortTimeFormat();     
      return frame.getFormattedDateString(dfTime, LocalizationHelper.getLocale());
   }

   /**
    * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
    */
   @Override
   public Color getForeground(Object element)
   {
      if (element instanceof String)
         return HINT_FOREGROUND;
      return null;
   }

   /**
    * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
    */
   @Override
   public Color getBackground(Object element)
   {
      return null;
   }
}
