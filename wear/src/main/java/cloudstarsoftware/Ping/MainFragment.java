package cloudstarsoftware.Ping;

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
    private final String TAG = "Ping!:WearMainFrag";
    private final int INTERVAL = 1000; // 1 Second timer
    private int m_dDefaultCount = 3; // Give 3 seconds to cancel
    private int m_dCountdown;  // Set in RunApp()
    private boolean m_fSendCompleted = false;
    private boolean m_fCancelHit = false;
    private boolean m_fHavePermission = false;
    private View m_view;
    private LaunchActivity m_hostActivity;

    // Vars for waiting for number from phone
    private boolean m_fReceivedResponse = false;
    private String m_number;
    private final int TIMEOUT_WAITING_FOR_RESPONSE = 5; // 5 seconds waiting for response.
    private int m_dTimeWaitedForResponse = 0;
    private final int m_dIntervalResponse = 50; // 50 ms


    // Timer handler and runner
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable()
    {
        public void run()
        {
            // When cancel is hit, bail immediately
            if (m_fCancelHit && 0 >= m_dCountdown) { // Bail when we've cancelled AND we waited showing Cancelled for a bit (if hit at 0 then oh well)
                return;
            }
            // Update counter
            m_dCountdown--;
            if (!m_fCancelHit && !m_fSendCompleted && 0 < m_dCountdown) {
                ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sending in " + m_dCountdown + "...");
            } else if (!m_fCancelHit && !m_fSendCompleted) {
                ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sending in " + 0 + "...");
            }

            // Haven't sent yet, but we have finish counting down
            if (!m_fCancelHit && !m_fSendCompleted && 0 >= m_dCountdown) {
                Button btn = (Button) m_view.findViewById(R.id.btnCancel);
                btn.setEnabled(false);
                // Send Message to mobile
                // Requires a new thread to avoid blocking the UI
                SendToDataLayerThread thread = new SendToDataLayerThread("/message_path", "Send");
                thread.start();
                // Don't care to wait for thread to finish

                ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sent!");
                m_fSendCompleted = true;
            }

            // Sent so leave
            if (m_fSendCompleted && -2 >= m_dCountdown) { // Bail when we've sent AND we waited showing Sent for 2 seconds
                m_hostActivity.finish();
                System.exit(0);
            }
            handler.postAtTime(runnable, System.currentTimeMillis() + INTERVAL);
            handler.postDelayed(runnable, INTERVAL);
        }
    };

    private Runnable runnableCheckPhone = new Runnable()
    {
        public void run()
        {
            if (m_fReceivedResponse) {
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
                    m_fReceivedResponse = false;
                    // Ready to send, start countdown
                    // Cancel enabled by default
                    (m_view.findViewById(R.id.btnCancel)).setEnabled(true);
                    ((TextView) m_view.findViewById(R.id.lbl1)).setText("Sending in " + m_dCountdown + "...");
                    // Start Timer
                    handler.postDelayed(runnable, INTERVAL);
                    return;
                }
            }
            if (TIMEOUT_WAITING_FOR_RESPONSE * 1000 < m_dTimeWaitedForResponse) {
                Context ctx = getContext();
                if (null != ctx) {
                    AlertDialog.Builder message = new AlertDialog.Builder(ctx);
                    message.setMessage("Timed out waiting for communication with phone.");
                    message.setPositiveButton("Ok", null);
                    message.show();
                }
                cancelHit();
                return; // end here we waited to long
            }
            m_dTimeWaitedForResponse += m_dIntervalResponse;
            handler.postDelayed(runnableCheckPhone, m_dIntervalResponse);
        }
    };

    public MainFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }

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
        m_dCountdown = m_dDefaultCount;
        m_fCancelHit = false;
        m_fSendCompleted = false;
        // Retry button off by default unless cancel is hit
        Button retryBtn = (Button)m_view.findViewById(R.id.btnResend);
        retryBtn.setEnabled(false);
        retryBtn.setVisibility(View.INVISIBLE);

        // Get phone number by sending a message to phone
        ((TextView) m_view.findViewById(R.id.lbl1)).setText("");
        m_fReceivedResponse = false;
        m_dTimeWaitedForResponse = 0;
        SendToDataLayerThread thread = new SendToDataLayerThread("/message_path", "GetNumber");
        thread.start();

        // Run timer to wait for response
        handler.postDelayed(runnableCheckPhone, m_dIntervalResponse);

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
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        Wearable.MessageApi.removeListener(m_hostActivity.mGoogleApiClient, this);
    }

    public interface OnFragmentInteractionListener
    {
        void onMainFragmentInteraction(String string);
    }


    protected void cancelHit()
    {
        m_fCancelHit = true;
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
        Log.v(TAG, "OnConnected");
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        // Do Nothing
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        final String inputMessage = new String(messageEvent.getData());
        Log.v(TAG, "onMessageReceived");
        if (inputMessage.startsWith("Number:"))
        {
            final String searchStr = "Number:";
            final int index = inputMessage.indexOf("Number:") + searchStr.length() ;
            m_number = inputMessage.substring(index);
            Log.v(TAG, "onMessageReceived from Phone with number: " + m_number);
        }
        m_fReceivedResponse = true;
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
                    Log.v(TAG, "Message: {" + message + "} sent from wear to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.v(TAG, "ERROR: failed to send Message");
                }
            }
        }

    }


}
