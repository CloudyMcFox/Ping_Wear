package cloudstarsoftware.Ping;

import android.Manifest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
    private final String TAG = "Ping!:MainActivity";
    private final int m_dInterval = 1000; // 1 Second timer
    private int m_dDefaultCount = 3; // Give 3 seconds to cancel
    private int m_dCountdown; // Set in RunApp()
    boolean m_fSendCompleted = false;
    boolean m_fCancelHit = false;
    SendTextHelper m_stHelper = new SendTextHelper();

    // Timer Handlers
    private Handler m_handler = new Handler();
    private Runnable m_runnable = new Runnable()
    {
        public void run()
        {
            TimerTick();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Ping!");
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // Set cancel buttons handler
        (findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cancelHit();
            }
        });

        (findViewById(R.id.btnRetry)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                RunApp();
            }
        });

        ((TextView) findViewById(R.id.lbl1)).setText("");

        // Request read contacts if needed
        Boolean fNeedsSmsPerm = ContextCompat.
                checkSelfPermission(this, Manifest.permission.SEND_SMS)!= PackageManager.PERMISSION_GRANTED;
        Boolean fNeedsContactsPerm = ContextCompat.
                checkSelfPermission(this, Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED;

        ArrayList<String> permList = new ArrayList<>();
        if (fNeedsSmsPerm) {
            permList.add(Manifest.permission.SEND_SMS);
        }
        if (fNeedsContactsPerm) {
            permList.add(Manifest.permission.READ_CONTACTS);
        }

        if (permList.isEmpty()) {
            RunApp();
        } else {
            try{
                ActivityCompat.requestPermissions(this, permList.toArray(new String[permList.size()]), 1);
            } catch (Exception exp) {
                Log.e(TAG, exp.getMessage());
            }
        }
    } // end onCreate()

    // Main function
    protected void RunApp()
    {
        m_handler.removeCallbacksAndMessages(null); // remove all callbacks
        m_dCountdown = m_dDefaultCount;
        m_fCancelHit = false;
        m_fSendCompleted = false;
        // Retry button off by default unless cancel is hit
        Button retryBtn = (Button) findViewById(R.id.btnRetry);
        retryBtn.setEnabled(false);
        retryBtn.setVisibility(View.INVISIBLE);
        // Cancel enabled by default
        (findViewById(R.id.btnCancel)).setEnabled(true);
        // Check if there is a number in the settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String number = prefs.getString("phoneNumber", "");
        if (number.isEmpty() || number.startsWith("Enter") /* Default check */) {
            // Default value, alert to add one
            AlertDialog.Builder message = new AlertDialog.Builder(this);
            message.setMessage("Please set a phone number in the settings.");
            message.setPositiveButton("Ok", null);
            message.show();
            // Mimic cancelled hit
            cancelHit();
            return;
        }

        // Ready to send, start countdown
        ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + m_dCountdown + "...");
        // Start Timer
        m_handler.postDelayed(m_runnable, m_dInterval);
    }

    protected void cancelHit()
    {
        m_fCancelHit = true;
        // Cancelling
        ((TextView) findViewById(R.id.lbl1)).setText("Cancelled!");
        Button btn = (Button) findViewById(R.id.btnCancel);
        btn.setEnabled(false);

        // enable retry button
        Button retryBtn = (Button) findViewById(R.id.btnRetry);
        retryBtn.setEnabled(true);
        retryBtn.setVisibility(View.VISIBLE);
    }

    protected void TimerTick()
    {
        // When cancel is hit, bail immediately
        if (m_fCancelHit && 0 >= m_dCountdown) { // Bail when we've cancelled AND we waited showing cancelled for a bit (if hit at 0 then oh well)
            return;
        }
        // Update counter
        m_dCountdown--;

        if (!m_fCancelHit && !m_fSendCompleted && 0 < m_dCountdown) {
            ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + m_dCountdown + "...");
        } else if (!m_fCancelHit && !m_fSendCompleted) {
            ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + 0 + "...");
        }

        // Haven't sent yet, but we have finish counting down
        if (!m_fCancelHit && !m_fSendCompleted && 0 >= m_dCountdown) {
            Button btnCancel = (Button) findViewById(R.id.btnCancel);
            btnCancel.setEnabled(false);
            // Check if there is a number in the settings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String labelMessage;
            String message = prefs.getString("message", "");
            String number = prefs.getString("phoneNumber", ""); // send actual number, not text
            if (m_stHelper.send(number, message)) {
                labelMessage = "Sent!";
            } else {
                labelMessage = "Error!";
            }
            ((TextView) findViewById(R.id.lbl1)).setText(labelMessage);
            m_fSendCompleted = true;
        }

        // Sent so leave
        if (m_fSendCompleted && -2 >= m_dCountdown) { // Bail when we've sent AND we waited showing Sent for 2 seconds
            finish();
            System.exit(0);
        }

        // Re-trigger timer to count again
        m_handler.postDelayed(m_runnable, m_dInterval);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        // Only check for SMS perms! We don't need READ_CONTACTS to send SMS...
        for (int i =0 ; i < permissions.length && i < grantResults.length; i++) {
            if (permissions[i].equals(Manifest.permission.SEND_SMS) && PackageManager.PERMISSION_GRANTED != grantResults[i]) {
                ((TextView) findViewById(R.id.lbl1)).setText("SMS Permissions Required!\nRestart the app.");
                findViewById(R.id.btnCancel).setEnabled(false);
                findViewById(R.id.btnRetry).setEnabled(false);
                findViewById(R.id.toolbar).setVisibility(View.INVISIBLE);
                return;
            }
        }
        RunApp();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid()); // not needed?
    }

    // Menu Handlers
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.menu_settings:
                cancelHit();
                // Launch Settings Page
                Intent startIntent = new Intent(this, SettingsActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
                break;
            default:
                break;
        }

        return true;
    }
    // End Menu Handlers
}
