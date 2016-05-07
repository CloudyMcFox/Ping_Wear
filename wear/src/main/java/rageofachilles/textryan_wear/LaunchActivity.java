package rageofachilles.textryan_wear;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class LaunchActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    // UI variables
    private final int interval = 1000; // 1 Second timer
    private int dCountdown = 3; // Give 3 seconds to cancel
    boolean fSendCompleted = false;
    boolean fCancelHit = false;
    // End UI Variables

    GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError=false;


    private Handler handler = new Handler();
    private Runnable runnable = new Runnable()
    {
        public void run()
        {
            // When cancel is hit, bail immediately
            if (fCancelHit && 0 >= dCountdown) { // Bail when we've cancelled AND we waited showing Cancelled for a bit (if hit at 0 then oh well)
                finish();
                System.exit(0);
            }
            // Update counter
            dCountdown = dCountdown - 1;
            if (!fCancelHit && !fSendCompleted && 0 < dCountdown) {
                ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + dCountdown + "...");
            } else if (!fCancelHit && !fSendCompleted) {
                ((TextView) findViewById(R.id.lbl1)).setText("Sending in " + 0 + "...");
            }

            // Haven't sent yet, but we have finish counting down
            if (!fCancelHit && !fSendCompleted && 0 >= dCountdown) {
                Button btn = (Button) findViewById(R.id.btnCancel);
                btn.setEnabled(false);
                // Send Message to mobile
                String message = "5084940433;Text from TextRyan!";
                //Requires a new thread to avoid blocking the UI
                new SendToDataLayerThread("/message_path", message).start();

                ((TextView) findViewById(R.id.lbl1)).setText("Sent!");
                fSendCompleted = true;
            }

            // Sent so leave
            if (fSendCompleted && -2 >= dCountdown) { // Bail when we've sent AND we waited showing Sent for 2 seconds
                finish();
                System.exit(0);
            }
            handler.postAtTime(runnable, System.currentTimeMillis() + interval);
            handler.postDelayed(runnable, interval);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        this.setTitle("Text Ryan");
        // Start Timer
        handler.postAtTime(runnable, System.currentTimeMillis()+interval);
        handler.postDelayed(runnable, interval);

        // Request SMS Permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        // Handle UI Elements
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
         {
             @Override
             public void onLayoutInflated(WatchViewStub stub)
             {
                 TextView tv = (TextView) findViewById(R.id.lbl1);
                 if (tv != null) {
                     tv.setText("Sending in " + dCountdown + "...");
                 }


                 Button btn = (Button) findViewById(R.id.btnCancel);

                 if (null == btn) {
                     return;
                 }
                 //Listener to send the message (it is just an example)
                 btn.setOnClickListener(new View.OnClickListener()
                 {
                     @Override
                     public void onClick(View v)
                     {
                         fCancelHit = true;
                         // Cancelling
                         ((TextView) findViewById(R.id.lbl1)).setText("Canceling!");
                         Button btn = (Button) findViewById(R.id.btnCancel);
                         btn.setEnabled(false);
                     }
                 });
             }
         });


//        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
//        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
//            @Override
//            public void onLayoutInflated(WatchViewStub stub) {
//                Button btn = (Button) stub.findViewById(R.id.btnSend);
//
//                if(null == btn){
//                    return;
//                }
//                //Listener to send the message (it is just an example)
//                btn.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        String message = "Hello wearable\n Via the data layer";
//                        //Requires a new thread to avoid blocking the UI
//                        new SendToDataLayerThread("/message_path", message).start();
//                    }
//                });
//            }
//        });

    }// end OnCreate()


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String message = "5084940433;Text from TextRyan!";
                        //Requires a new thread to avoid blocking the UI
                        new SendToDataLayerThread("/message_path", message).start();
        }
    }
    public class SendToDataLayerThread extends Thread {

        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }

    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mGoogleApiClient.disconnect();
//    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

//    /*
//     * Resolve the node = the connected device to send the message to
//     */
//    private void resolveNode() {
//
//        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
//            @Override
//            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
//                for (Node node : nodes.getNodes()) {
//                    mNode = node;
//                }
//            }
//        });
//    }

//
//    private void sendMessage( final String path, final String text ) {
//        new Thread( new Runnable() {
//            @Override
//            public void run() {
//                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
//                for(Node node : nodes.getNodes()) {
//                    Wearable.MessageApi.sendMessage(
//                            mGoogleApiClient, node.getId(), path, text.getBytes() ).await();
//                }
//            }
//        }).start();
//    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }
    @Override
    public void onConnected(Bundle bundle) {
        //Wearable.MessageApi.addListener(mGoogleApiClient, this);
        //sendMessage("/start", null);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Improve your code
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }
}