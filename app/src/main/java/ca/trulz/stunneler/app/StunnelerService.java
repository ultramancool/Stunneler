package ca.trulz.stunneler.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class StunnelerService extends Service {
    public static final String ACTION_REPORT = "ca.trulz.stunneler.app.action.BROADCAST";
    public static final int FOREGROUND_ID = 775559;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder = new StunnelerBinder();
    private java.lang.Process process;
    private StringLineBuffer buffer;

    public StunnelerService() {
        buffer = new StringLineBuffer(100);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, StunnelerService.class);
        intent.putExtra("state", "start");
        context.startService(intent);
    }

    public String getOutput() {
        return buffer.buildString();
    }

    private void sendLogMessage(String s) {
        Intent broadcastIntent;
        broadcastIntent = new Intent(ACTION_REPORT).putExtra("line", s).putExtra("running", true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("StunnelerServiceHandler", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getStringExtra("state").equals("end")) {
            if (process != null)
                process.destroy();
        } else {
            Message msg = mServiceHandler.obtainMessage();
            mServiceHandler.sendMessage(msg);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

    }

    public boolean isRunning() {
        return process != null;
    }

    public class StunnelerBinder extends Binder {
        StunnelerService getService() {
            return StunnelerService.this;
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Intent notificationIntent = new Intent(StunnelerService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(StunnelerService.this, 0, notificationIntent, 0);
            Notification n = new NotificationCompat.Builder(StunnelerService.this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentText("Service is running...")
                    .setContentTitle("Stunneler")
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(pendingIntent).build();
            startForeground(FOREGROUND_ID, n);

            Intent broadcastIntent = new Intent(ACTION_REPORT).putExtra("running", true);
            LocalBroadcastManager.getInstance(StunnelerService.this).sendBroadcast(broadcastIntent);
            Log.i("tag", "starting service run");
            String stunnelBin = getApplicationContext().getApplicationInfo().nativeLibraryDir + "/libstunnel.so";

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(StunnelerService.this);
            String stunnelConfig = sharedPref.getString(SettingsActivity.CONFIG_FILE, "");
            if (stunnelConfig == null || stunnelConfig.isEmpty() || !new File(stunnelConfig).exists()) {
                sendLogMessage("Config file not set or non-existant. Fix this in settings.");
            } else {

                File stunnelDir = new File(new File(stunnelConfig).getParent());
                try {
                    ProcessBuilder builder = new ProcessBuilder(stunnelBin, stunnelConfig);
                    builder.directory(stunnelDir);
                    builder.redirectErrorStream(true);
                    process = builder.start();

                    InputStreamReader reader = new InputStreamReader(process.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    String s;
                    while ((s = bufferedReader.readLine()) != null) {
                        buffer.appendLine(s);
                        sendLogMessage(s);
                    }
                    bufferedReader.close();
                    process.waitFor();
                    process = null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            Log.i("tag", "ending service run");
            broadcastIntent = new Intent(ACTION_REPORT).putExtra("running", false);
            LocalBroadcastManager.getInstance(StunnelerService.this).sendBroadcast(broadcastIntent);
            stopForeground(true);
            stopSelf();
        }
    }
}
