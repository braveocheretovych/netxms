/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2024 Raden Solutions
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
package org.netxms.nxmc.modules.networkmaps.widgets.helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.netxms.client.NXCSession;
import org.netxms.client.objects.AbstractObject;
import org.netxms.nxmc.Registry;

/**
 * DCI last value provider for map links
 */
public class MapObjectSyncer
{
   private static MapObjectSyncer instance;

   private Map<Long, Set<Long>> objectIdToMap = new HashMap<Long, Set<Long>>();
   private Map<Long, Set<Long>> requestData = new HashMap<Long, Set<Long>>();
   private NXCSession session = Registry.getSession();
	private Thread syncThread = null;
	private volatile boolean syncRunning = true;

   /**
    * Get value provider instance
    */
	public static MapObjectSyncer getInstance()
	{
	   if (instance == null)
	   {
	      instance = new MapObjectSyncer();
	   }	   
      return instance;
	}

   /**
    * Constructor
    */
   public MapObjectSyncer()
   {
      syncThread = new Thread(new Runnable() {
         @Override
         public void run()
         {
            syncObjects();
         }
      });
      syncThread.setDaemon(true);
      syncThread.start();
   }

	/**
	 * Synchronize last values in background
	 */
	private void syncObjects()
	{
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException e3)
		{
		}
		while(syncRunning)
		{
			synchronized(requestData)
			{
				try
				{
               if (requestData.size() > 0)
				   {
                  for (Entry<Long, Set<Long>> entry : requestData.entrySet())
                  { 
                     session.syncObjectSet(entry.getValue(), entry.getKey(), NXCSession.OBJECT_SYNC_NOTIFY | NXCSession.OBJECT_SYNC_ALLOW_PARTIAL);
                  }
				   }
				}
				catch(Exception e2)
				{
					e2.printStackTrace();	// for debug
				}
			}
			try
			{
				Thread.sleep(30000); 
			}
			catch(InterruptedException e1)
			{
			}
		}
	}

   /**
	 * 
	 */
	public void dispose()
	{
		syncRunning = false;
		syncThread.interrupt();
	}

	
	public void addNodes(long mapObjectId, final Set<Long> mapObjectIds)
	{
	   Set<Long> mapSpecificObjects = new HashSet<Long>();
	   synchronized(requestData)
      {
         for(Long objectId : mapObjectIds)
         {
            AbstractObject object = session.findObjectById(objectId, true);
            if ((object == null) || !object.isPartialObject())
               continue;
            
            if(objectIdToMap.containsKey(objectId))
            {
               objectIdToMap.get(objectId).add(mapObjectId);
            }
            else
            {
               HashSet<Long> newSet = new HashSet<Long>();
               newSet.add(objectId);
               objectIdToMap.put(objectId, newSet);
               mapSpecificObjects.add(objectId);
            }
         }
         
         if (mapSpecificObjects.size() > 0)
         {
            requestData.put(mapObjectId, mapSpecificObjects);
         }
      }
	}

   public void removeNodes(long mapObjectId, final Set<Long> mapObjectIds)
   {
      synchronized(requestData)
      {
         if (!requestData.containsKey(mapObjectId))  
            return;
               
         requestData.remove(mapObjectId);
         for(Long object : mapObjectIds)
         {
            Set<Long> maps = objectIdToMap.get(object);
            maps.remove(mapObjectId);
            if (maps.size() == 0)
            {
               maps.remove(object);
            }
            else
            {
               boolean requestedByOtherMap = false;
               for (Long mapid : maps)
               {
                  if(requestData.get(mapid) != null && requestData.get(mapid).contains(object))
                  {
                     requestedByOtherMap = true;
                     break;
                  }
               }
               if (!requestedByOtherMap)
               {
                  boolean added = false;
                  for (Long mapid : maps)
                  {
                     if(requestData.get(mapid) != null)
                     {
                        requestData.get(mapid).add(object);
                        added = true;
                        break;
                     }
                  }
                  if (!added)
                  {
                     HashSet<Long> newSet = new HashSet<Long>();
                     newSet.add(object);
                     requestData.put(maps.iterator().next(), newSet);                     
                  }
               }
            }            
         }
      }
   }
}
