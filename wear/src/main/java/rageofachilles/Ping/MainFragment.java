package rageofachilles.Ping;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class MainFragment extends android.app.Fragment implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks
{
    private final int interval = 1000; // 1 Second timer
    private int dDefaultCount = 3; // Give 3 seconds to cancel
    private int dCountdown;  // Set in RunApp()
    boolean fSendCompleted = false;
    boolean fCancelHit = false;
    boolean m_fHavePermission = false;
    View m_view;
    LaunchActivity m_hostActivity;

    // Vars for waiting for number from phone
    boolean receivedResponse = false;
    String m_number;
    final int timeoutWaitingForResponse = 5; // 5 seconds waiting for response.
    int timeWaitedForResponse = 0;
    private final int intervalResponse = 50; // 50 ms


    // Timer handler and runner
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable()
    {
        public void run()
        {
            // When cancel is hit, bail immediately
            if (fCancelHit && 0 >= dCountdown) { // Bail when we've cancelled AND we waited showing Cancelled for a bit (if hit at 0 then oh well)
                return;
            }
            // Update counter
            dCountdown = dCountdown - 1;
            if (!fCancelHit && !fSendCompleted && 0 < dCountdown) {
                ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sending in " + dCountdown + "...");
            } else if (!fCancelHit && !fSendCompleted) {
                ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sending in " + 0 + "...");
            }

            // Haven't sent yet, but we have finish counting down
            if (!fCancelHit && !fSendCompleted && 0 >= dCountdown) {
                Button btn = (Button) m_view.findViewById(R.id.btnCancel);
                btn.setEnabled(false);
                // Send Message to mobile
                // Requires a new thread to avoid blocking the UI
                SendToDataLayerThread thread = new SendToDataLayerThread("/message_path", "Send");
                thread.start();
                // Don't care to wait for thread to finish

                ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sent!");
                fSendCompleted = true;
            }

            // Sent so leave
            if (fSendCompleted && -2 >= dCountdown) { // Bail when we've sent AND we waited showing Sent for 2 seconds
                m_hostActivity.finish();
                System.exit(0);
            }
            handler.postAtTime(runnable, System.currentTimeMillis() + interval);
            handler.postDelayed(runnable, interval);
        }
    };

    private Runnable runnableCheckPhone = new Runnable()
    {
        public void run()
        {
            if (receivedResponse) {
                if (null == m_number || m_number.isEmpty() || m_number.startsWith("Enter") /* Default check */){
                    // Default value, alert to add one
                    AlertDialog.Builder message = new AlertDialog.Builder(getContext());
                    message.setMessage("Please set a phone number in the phone app's settings.");
                    message.setPositiveButton("Ok", null);
                    message.show();
                    // Mimic cancelled hit
                    cancelHit();
                    // send them to the open on phone fragment
                    if (null != m_hostActivity) {
                        m_hostActivity.mPager.setCurrentItem(0,1);
                    }
                    return;
                } else {
                    receivedResponse = false;
                    // Ready to send, start countdown
                    // Cancel enabled by default
                    (m_view.findViewById(R.id.btnCancel)).setEnabled(true);
                    ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sending in " + dCountdown + "...");
                    // Start Timer
                    handler.postDelayed(runnable, interval);
                    return;
                }
            }
            if (timeoutWaitingForResponse * 1000 < timeWaitedForResponse) {
                Context ctx = getContext();
                if (null != ctx) {
                    AlertDialog.Builder message = new AlertDialog.Builder(ctx);
                    message.setMessage("Timed out waiting for communication with phone.");
                    message.setPositiveButton("Ok", null);
                    message.show();
                }
                //TODO: automatically show next fragment
                cancelHit();
                return; // end here we waited to long
            }
            timeWaitedForResponse += intervalResponse;
            handler.postDelayed(runnableCheckPhone, intervalResponse);
        }
    };

    private OnFragmentInteractionListener mListener;

    public MainFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }// end OnCreate()


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        m_view = inflater.inflate(R.layout.fragment_main, container, false);
        m_hostActivity = (LaunchActivity)getActivity();

        // Register listener to listen for messages from phone
        Wearable.MessageApi.addListener(m_hostActivity.mGoogleApiClient, this);

        // Need to wait for inflate before accessing UI elements, so finally, check permissions or run app
        // Request SMS Permissions if needed
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{Manifest.permission.SEND_SMS}, 0);
        } else {
            m_fHavePermission = true;

            // Inflate the layout for this fragment
            final WatchViewStub stub = (WatchViewStub) m_view.findViewById(R.id.watch_view_stub);
            // Handle UI Elements
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
            {
                @Override
                public void onLayoutInflated(WatchViewStub stub)
                {
                    TextView tv = (TextView) m_view.findViewById(R.id.lbl1);
                    if (null != tv) {
                        tv.setText(""); // Will be set in RunApp()
                    }

                    Button btn = (Button) m_view.findViewById(R.id.btnCancel);

                    if (null == btn) {
                        return;
                    }
                    btn.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            cancelHit();
                        }
                    });

                    (m_view.findViewById(R.id.btnResend)).setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            RunApp();
                        }
                    });
                    RunApp();
                }
            });
        }
        return m_view;
    }// end OnCreateView()

    protected void RunApp()
    {
        if (!m_fHavePermission) {
            return;
        }
        handler.removeCallbacksAndMessages(null); // remove all callbacks
        dCountdown = dDefaultCount;
        fCancelHit = false;
        fSendCompleted = false;
        // Retry button off by default unless cancel is hit
        Button retryBtn = (Button)m_view.findViewById(R.id.btnResend);
        retryBtn.setEnabled(false);
        retryBtn.setVisibility(View.INVISIBLE);

        // Get phone number by sending a message to phone
        ((TextView) m_view.findViewById(R.id.lbl1)).setText("");
        receivedResponse = false;
        timeWaitedForResponse = 0;
        SendToDataLayerThread thread = new SendToDataLayerThread("/message_path", "GetNumber");
        thread.start();

        // Run timer to wait for response
        handler.postDelayed(runnableCheckPhone, intervalResponse);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (PackageManager.PERMISSION_GRANTED != grantResults[0]) {
            ((TextView) m_view.findViewById(R.id.lbl1)).setText("No SMS Permissions!");
            m_view.findViewById(R.id.btnCancel).setEnabled(false);
            m_fHavePermission = false;
            return;
        }
        m_fHavePermission = true;
        RunApp();
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
        Wearable.MessageApi.removeListener(m_hostActivity.mGoogleApiClient, this);
        //m_hostActivity.mGoogleApiClient.disconnect();
    }


    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onMainFragmentInteraction(String string);
    }

    protected void cancelHit()
    {
        fCancelHit = true;
        // Cancelling
        ((TextView) m_view.findViewById(R.id.lbl1)).setText("Cancelled!");
        Button btn = (Button)m_view.findViewById(R.id.btnCancel);
        btn.setEnabled(false);

        // enable retry button
        Button retryBtn = (Button)m_view.findViewById(R.id.btnResend);
        retryBtn.setEnabled(true);
        retryBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.v("myTag", "OnConnected");
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        //Improve your code
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        final String inputMessage = new String(messageEvent.getData());
        Log.v("myTag", "onMessageReceived");
        if (inputMessage.startsWith("Number:"))
        {
            final String searchStr = "Number:";
            final int index = inputMessage.indexOf("Number:") + searchStr.length() ;
            m_number = inputMessage.substring(index);
            Log.v("myTag", "onMessageReceived from Phone with number: " + m_number);
        }
        receivedResponse = true;
    }

    // TODO: make this a shared function!
    public class SendToDataLayerThread extends Thread {

        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(m_hostActivity.mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(m_hostActivity.mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent from wear to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }

    }
}
