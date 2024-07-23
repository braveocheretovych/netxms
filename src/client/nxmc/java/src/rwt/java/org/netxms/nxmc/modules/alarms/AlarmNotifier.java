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
package org.netxms.nxmc.modules.alarms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Display;
import org.netxms.client.NXCSession;
import org.netxms.client.SessionListener;
import org.netxms.client.SessionNotification;
import org.netxms.client.events.Alarm;
import org.netxms.client.events.BulkAlarmStateChangeData;
import org.netxms.nxmc.PreferenceStore;
import org.netxms.nxmc.Registry;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.alarms.dialogs.AlarmReminderDialog;
import org.netxms.nxmc.tools.MessageDialogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

/**
 * Alarm notifier
 */
public class AlarmNotifier
{
   private static final Logger logger = LoggerFactory.getLogger(AlarmNotifier.class);
   public static final String[] SEVERITY_TEXT = { "NORMAL", "WARNING", "MINOR", "MAJOR", "CRITICAL", "REMINDER" };

   /**
    * Get instance for current session
    *
    * @return instance
    */
   private static AlarmNotifier getInstance()
   {
      return (AlarmNotifier)RWT.getUISession().getAttribute("netxms.alarmNotifier");
   }

   /**
    * Get instance for current session
    *
    * @param display current display
    * @return instance
    */
   private static AlarmNotifier getInstance(Display display)
   {
      return (AlarmNotifier)RWT.getUISession(display).getAttribute("netxms.alarmNotifier");
   }

   private I18n i18n = LocalizationHelper.getI18n(AlarmNotifier.class);
   private Display display;
   private SessionListener listener = null;
   private Map<Long, Integer> alarmStates = new HashMap<Long, Integer>();
   private int outstandingAlarms = 0;
   private long lastReminderTime = 0;
   private NXCSession session;
   private PreferenceStore ps;
   private LinkedBlockingQueue<String> soundQueue = new LinkedBlockingQueue<String>(4);
   private File soundFilesDirectory;

   /**
    * Initialize alarm notifier
    *
    * @param session communication session
    * @param display owning display
    */
   public static void init(NXCSession session, Display display)
   {
      RWT.getUISession(display).setAttribute("netxms.alarmNotifier", new AlarmNotifier(session, display));
   }

   /**
    * Create new alarm notifier for given session
    * 
    * @param session communication session
    * @param display owning display
    */
   private AlarmNotifier(NXCSession session, Display display)
   {
      this.session = session;
      this.display = display;

      ps = PreferenceStore.getInstance(display);
      soundFilesDirectory = new File(Registry.getStateDir(display), "sounds");
      if (!soundFilesDirectory.isDirectory())
         soundFilesDirectory.mkdirs();

      checkSounds();

      lastReminderTime = System.currentTimeMillis();

      try
      {
         Map<Long, Alarm> alarms = session.getAlarms();
         for(Alarm a : alarms.values())
         {
            alarmStates.put(a.getId(), a.getState());
            if (a.getState() == Alarm.STATE_OUTSTANDING)
               outstandingAlarms++;
         }
         logger.info(String.format("Received %d alarms from server (%d outstanding)", alarms.size(), outstandingAlarms));
      }
      catch(Exception e)
      {
         logger.error("Exception while initializing alarm notifier", e);
      }

      listener = new SessionListener() {
         @Override
         public void notificationHandler(SessionNotification n)
         {
            if ((n.getCode() == SessionNotification.NEW_ALARM) || (n.getCode() == SessionNotification.ALARM_CHANGED))
            {
               processNewAlarm((Alarm)n.getObject());
            }
            else if ((n.getCode() == SessionNotification.ALARM_TERMINATED) || (n.getCode() == SessionNotification.ALARM_DELETED))
            {               
               Alarm a = (Alarm)n.getObject();
               Integer state = alarmStates.remove(a.getId());
               if ((state != null) && (state == Alarm.STATE_OUTSTANDING))
                  outstandingAlarms--;
            }
            else if (n.getCode() == SessionNotification.MULTIPLE_ALARMS_RESOLVED)
            {               
               BulkAlarmStateChangeData d = (BulkAlarmStateChangeData)n.getObject();
               for(Long id : d.getAlarms())
               {
                  Integer state = alarmStates.get(id);
                  if ((state != null) && (state == Alarm.STATE_OUTSTANDING))
                     outstandingAlarms--;
                  alarmStates.put(id, Alarm.STATE_RESOLVED);
               }
            }
            else if (n.getCode() == SessionNotification.MULTIPLE_ALARMS_TERMINATED)
            {               
               BulkAlarmStateChangeData d = (BulkAlarmStateChangeData)n.getObject();
               for(Long id : d.getAlarms())
               {
                  Integer state = alarmStates.remove(id);
                  if ((state != null) && (state == Alarm.STATE_OUTSTANDING))
                     outstandingAlarms--;
               }
            }
         }
      };
      session.addListener(listener);

      Thread reminderThread = new Thread(() -> {
         while(true)
         {
            try
            {
               Thread.sleep(10000);
            }
            catch(InterruptedException e)
            {
            }

            long currTime = System.currentTimeMillis();
            if (ps.getAsBoolean("AlarmNotifier.OutstandingAlarmsReminder", false) && (outstandingAlarms > 0) && (lastReminderTime + 300000 <= currTime))
            {
               display.syncExec(() -> {
                  soundQueue.offer(SEVERITY_TEXT[SEVERITY_TEXT.length - 1]);
                  AlarmReminderDialog dlg = new AlarmReminderDialog(null);
                  dlg.open();
               });
               lastReminderTime = currTime;
            }
         }
      }, "AlarmReminderThread");
      reminderThread.setDaemon(true);
      reminderThread.start();

      Thread playerThread = new Thread(new Runnable() {
         @Override
         public void run()
         {
            while(true)
            {
               String soundId;
               try
               {
                  soundId = soundQueue.take();
               }
               catch(InterruptedException e)
               {
                  continue;
               }
               
               Clip sound = null;
               try
               {
                  String fileName = getSoundAndDownloadIfRequired(soundId);
                  if (fileName != null)
                  {
                     sound = (Clip)AudioSystem.getLine(new Line.Info(Clip.class));
                     sound.open(AudioSystem.getAudioInputStream(new File(soundFilesDirectory, fileName).getAbsoluteFile()));
                     sound.start();
                     while(!sound.isRunning())
                        Thread.sleep(10);
                     while(sound.isRunning())
                     {
                        Thread.sleep(10);
                     }
                  }
               }
               catch(Exception e)
               {
                  logger.error("Exception in alarm sound player", e);
               }
               finally
               {
                  if ((sound != null) && sound.isOpen())
                     sound.close();
               }
            }
         }
      }, "AlarmSoundPlayer");
      playerThread.setDaemon(true);
      playerThread.start();
   }

   /**
    * Check if required sounds exist locally and download them from server if required.
    */
   private void checkSounds()
   {
      for(String s : SEVERITY_TEXT)
      {
         getSoundAndDownloadIfRequired(s);
      }
   }

   /**
    * @param severity
    * @return
    */
   private String getSoundAndDownloadIfRequired(String severity)
   {
      String soundName = ps.getAsString("AlarmNotifier.Sound." + severity);
      if ((soundName == null) || soundName.isEmpty())
         return null;

      if (!isSoundFileExist(soundName))
      {
         try
         {
            File fileContent = session.downloadFileFromServer(soundName);
            if (fileContent != null)
            {
               FileInputStream src = null;
               FileOutputStream dest = null;
               try
               {
                  src = new FileInputStream(fileContent);
                  File f = new File(soundFilesDirectory, soundName);
                  f.createNewFile();
                  dest = new FileOutputStream(f);
                  FileChannel fcSrc = src.getChannel();
                  dest.getChannel().transferFrom(fcSrc, 0, fcSrc.size());
               }
               catch(IOException e)
               {
                  logger.error("Cannot copy sound file", e);
               }
               finally
               {
                  if (src != null)
                     src.close();
                  if (dest != null)
                     dest.close();
               }
            }
            else
            {
               logger.error("Cannot download sound file " + soundName + " from server");
               soundName = null; // download failure
            }
         }
         catch(final Exception e)
         {
            soundName = null;
            ps.set("AlarmNotifier.Sound." + severity, ""); //$NON-NLS-1$ //$NON-NLS-2$
            display.asyncExec(new Runnable() {
               @Override
               public void run()
               {
                  MessageDialogHelper.openError(
                        display.getActiveShell(),
                        i18n.tr("Missing Sound File"),
                        String.format(i18n.tr(
                              "Sound file is missing and cannot be downloaded from server. Sound is removed and will not be played again. Error details: %s"),
                              e.getLocalizedMessage()));
               }
            });
         }
      }
      return soundName;
   }

   /**
    * @param name
    * @return
    */
   private boolean isSoundFileExist(String name)
   {
      if (name.isEmpty())
         return true;
      File f = new File(soundFilesDirectory, name);
      return f.isFile();
   }

   /**
    * Stop alarm notifier
    */
   public static void stop()
   {
      NXCSession session = Registry.getSession();
      AlarmNotifier instance = getInstance();
      if ((session != null) && (instance != null) && (instance.listener != null))
         session.removeListener(instance.listener);
   }

   /**
    * Check if global sound is enabled
    * @param display 
    * 
    * @return true if enabled
    */
   public static boolean isGlobalSoundEnabled(Display display)
   {
      return !getInstance(display).ps.getAsBoolean("AlarmNotifier.LocalSound", false);
   }

   /**
    * PLay sound for new alarm
    * 
    * @param alarm new alarm
    */
   public static void playSounOnAlarm(final Alarm alarm)
   {
      getInstance().soundQueue.offer(SEVERITY_TEXT[alarm.getCurrentSeverity().getValue()]);
   }

   /**
    * Process new alarm
    */
   private void processNewAlarm(final Alarm alarm)
   {
      Integer state = alarmStates.get(alarm.getId());
      if (state != null)
      {
         if (state == Alarm.STATE_OUTSTANDING)
         {
            outstandingAlarms--;
         }
      }
      alarmStates.put(alarm.getId(), alarm.getState());

      if (alarm.getState() != Alarm.STATE_OUTSTANDING)
         return;

      if (!ps.getAsBoolean("AlarmNotifier.LocalSound", false))
      {
         soundQueue.offer(SEVERITY_TEXT[alarm.getCurrentSeverity().getValue()]);
      }

      if (outstandingAlarms == 0)
         lastReminderTime = System.currentTimeMillis();
      outstandingAlarms++;
   }
}
