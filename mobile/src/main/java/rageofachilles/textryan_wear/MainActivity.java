package rageofachilles.textryan_wear;

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
import android.telephony.PhoneNumberUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final int interval = 1000; // 1 Second timer
    private int dDefaultCount = 3; // Give 3 seconds to cancel
    private int dCountdown; // Set in RunApp()
    boolean fSendCompleted = false;
    boolean fCancelHit = false;
    SendTextHelper stHelper = new SendTextHelper();

    // Timer Handlers
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            TimerTick();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Text Ryan");
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Set cancel buttons handler
        (findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelHit();
            }
        });

        (findViewById(R.id.btnRetry)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                RunApp();
            }
        });

        ((TextView) findViewById(R.id.lbl1)).setText("");

        // Request SMS Permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        } else {
                RunApp();
        }
    } // end onCreate()

    // Main function
    protected void RunApp()
    {
        handler.removeCallbacksAndMessages(null); // remove all callbacks
        dCountdown = dDefaultCount;
        fCancelHit = false;
        fSendCompleted = false;
        // Retry button off by default unless cancel is hit
        Button retryBtn = (Button)findViewById(R.id.btnRetry);
        retryBtn.setEnabled(false);
        retryBtn.setVisibility(View.INVISIBLE);
        // Cancel enabled by default
        (findViewById(R.id.btnCancel)).setEnabled(true);
        // Check if there is a number in the settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String number = prefs.getString("phoneNumber","");
        if (number.isEmpty() || number.startsWith("Enter") /* Default check */){
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
        ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + dCountdown + "...");
        // Start Timer
        //handler.postAtTime(runnable, System.currentTimeMillis()+interval);
        handler.postDelayed(runnable, interval);
    }

    protected void cancelHit()
    {
        fCancelHit = true;
        // Cancelling
        ((TextView) findViewById(R.id.lbl1)).setText("Cancelled!");
        Button btn = (Button)findViewById(R.id.btnCancel);
        btn.setEnabled(false);

        // enable retry button
        Button retryBtn = (Button)findViewById(R.id.btnRetry);
        retryBtn.setEnabled(true);
        retryBtn.setVisibility(View.VISIBLE);
    }

    protected void TimerTick()
    {
        // When cancel is hit, bail immediately
        if (fCancelHit && 0 >= dCountdown) { // Bail when we've cancelled AND we waited showing Cancelled for a bit (if hit at 0 then oh well)
            return;
        }
        // Update counter
        dCountdown--;
        if (!fCancelHit && !fSendCompleted && 0 < dCountdown ){
            ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + dCountdown + "...");
        } else if (!fCancelHit && !fSendCompleted) {
            ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + 0 + "...");
        }

        // Haven't sent yet, but we have finish counting down
        if (!fCancelHit && !fSendCompleted && 0 >= dCountdown )
        {
            Button btn = (Button) findViewById(R.id.btnCancel);
            btn.setEnabled(false);
            // Check if there is a number in the settings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String labelMessage;
            String message = prefs.getString("message","");
            String number = prefs.getString("phoneNumber","");
            if(stHelper.send(number, message)) {
                labelMessage = "Sent!";
            }
            else {
                labelMessage = "Error!";
            }
            ((TextView) findViewById(R.id.lbl1)).setText(labelMessage);
            fSendCompleted = true;
        }

        // Sent so leave
        if ( fSendCompleted && -2 >= dCountdown) { // Bail when we've sent AND we waited showing Sent for 2 seconds
            finish();
            System.exit(0);
        }
        //handler.postAtTime(runnable, System.currentTimeMillis()+interval);
        handler.postDelayed(runnable, interval);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (PackageManager.PERMISSION_GRANTED != grantResults[0]) {
            ((TextView) findViewById(R.id.lbl1)).setText("No SMS Permissions!");
            findViewById(R.id.btnCancel).setEnabled(false);
            return;
        }
        RunApp();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

// Menu Handlers
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
