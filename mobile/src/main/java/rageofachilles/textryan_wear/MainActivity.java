package rageofachilles.textryan_wear;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int interval = 1000; // 1 Second timer
    private int dCountdown = 3; // Give 3 seconds to cancel
    boolean fSendCompleted = false;
    boolean fCancelHit = false;
    SendTextHelper stHelper = new SendTextHelper();

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable(){
        public void run() {
            // When cancel is hit, bail immediately
            if (fCancelHit && 0 >= dCountdown) { // Bail when we've cancelled AND we waited showing Cancelled for a bit (if hit at 0 then oh well)
                finish();
                //System.exit(0);
            }
            // Update counter
            dCountdown = dCountdown - 1;
            if (!fCancelHit && !fSendCompleted && 0 < dCountdown ){
                ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + dCountdown + "...");
            } else if (!fCancelHit && !fSendCompleted) {
                ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + 0 + "...");
            }

            // Haven't sent yet, but we have finish counting down
            if (!fCancelHit && !fSendCompleted && 0 >=dCountdown )
            {
                Button btn = (Button) findViewById(R.id.btnCancel);
                btn.setEnabled(false);
                String message;
                if(stHelper.send("5084940433","Text from TextRyan!")) {
                    message = "Sent!";
                }
                else {
                    message = "Error!";
                }
                ((TextView) findViewById(R.id.lbl1)).setText(message);
                fSendCompleted = true;
            }

            // Sent so leave
            if ( fSendCompleted && -2 >= dCountdown) { // Bail when we've sent AND we waited showing Sent for 2 seconds
                finish();
                //System.exit(0);
            }
            handler.postAtTime(runnable, System.currentTimeMillis()+interval);
            handler.postDelayed(runnable, interval);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Text Ryan");
        ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + dCountdown + "...");

        handler.postAtTime(runnable, System.currentTimeMillis()+interval);
        handler.postDelayed(runnable, interval);

        // Request SMS Permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }

        Button btn = (Button)findViewById(R.id.btnCancel);

        if(null == btn){
            return;
        }
        //Listener to send the message (it is just an example)
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fCancelHit = true;
                // Cancelling
                ((TextView) findViewById(R.id.lbl1)).setText("Canceling!");
                Button btn = (Button)findViewById(R.id.btnCancel);
                btn.setEnabled(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String message;
            if(stHelper.send("5084940433","Text from TextRyan!")) {
                message = "Sent!";
            }
            else {
                message = "Error!";
            }
            ((TextView) findViewById(R.id.lbl1)).setText(message);
            fSendCompleted = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
