package rageofachilles.Ping;

import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;



public class OpenOnPhoneFragment extends android.app.Fragment
{
    View m_view;
    LaunchActivity m_hostActivity;

    private OnFragmentInteractionListener mListener;

    public OpenOnPhoneFragment()
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
        // Inflate the layout for this fragment
        m_view = inflater.inflate(R.layout.fragment_open_on_phone, container, false);
        m_hostActivity = (LaunchActivity)getActivity();

        // Inflate the layout for this fragment
        final WatchViewStub stub = (WatchViewStub) m_view.findViewById(R.id.watch_view_stub_open);
        // Handle UI Elements
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                Button btnOpen = (Button) m_view.findViewById(R.id.btnOpen);
                if (null == btnOpen) {
                    return;
                }
                btnOpen.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        openOnPhone();
                    }
                });
            }
        });
        return m_view;
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
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onOpenOnPhoneFragmentInteraction(String string);
    }

    private void openOnPhone()
    {
        SendToDataLayerThread thread = new SendToDataLayerThread("/message_path", "Open");
        thread.start();
        LaunchActivity act = (LaunchActivity) getActivity();
        if (null != act) {
            act.mPager.setCurrentItem(0,0); // set back to main page
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
