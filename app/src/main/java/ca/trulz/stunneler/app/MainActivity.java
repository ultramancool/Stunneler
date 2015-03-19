package ca.trulz.stunneler.app;

import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    private Button stopButton;
    private TextView textView;
    private boolean serviceRunning;
    private ScrollView scrollView;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StunnelerService.StunnelerBinder binder = (StunnelerService.StunnelerBinder) service;
            setServiceRunning(binder.getService().isRunning());
            textView.setText(binder.getService().getOutput());
            scrollToBottom();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public MainActivity() {
        super();
        serviceRunning = false;
    }

    public void setServiceRunning(boolean serviceRunning) {
        this.serviceRunning = serviceRunning;
        if (serviceRunning) {
            stopButton.setText("Stop Service");
        } else {
            stopButton.setText("Start Service");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);
        stopButton = (Button)findViewById(R.id.stopButton);
        scrollView = (ScrollView)findViewById(R.id.scrollView);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceRunning) {
                    stopStunnelerService();
                } else {
                    startStunnelerService();
                }
            }
        });
        textView.setText("");

        ResponseReceiver responseReceiver =  new ResponseReceiver();
        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(responseReceiver,
                new IntentFilter(StunnelerService.ACTION_REPORT));


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean("start_on_start", false)) {
            startStunnelerService();
        }

    }

    private void scrollToBottom() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void stopStunnelerService() {
        Intent intent = new Intent(this, StunnelerService.class);
        intent.putExtra("state", "end");
        startService(intent);
    }

    private void startStunnelerService() {
        StunnelerService.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, StunnelerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("line"))
                textView.append(intent.getStringExtra("line") + "\n");
                scrollToBottom();
            if (intent.hasExtra("running")) {
                setServiceRunning(intent.getBooleanExtra("running", true));
            }
        }
    }

}
