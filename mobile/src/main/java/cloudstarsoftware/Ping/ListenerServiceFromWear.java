package cloudstarsoftware.Ping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Ryan on 5/1/2016.
 */
public class ListenerServiceFromWear extends WearableListenerService {
    final String TAG = "Ping:ListenerSvcFrmWear";
    GoogleApiClient m_GoogleApiClient;
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived:");

        if (messageEvent.getPath().equals("/message_path")) {
            final String inputMessage = new String(messageEvent.getData());
            Log.v(TAG, "Message path received on phone is: " + messageEvent.getPath());
            Log.v(TAG, "Message received on phone is: " + inputMessage);

            //Connect the GoogleApiClient
            m_GoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            m_GoogleApiClient.connect();

            // Just send text
            SendTextHelper sth = new SendTextHelper();
            if (inputMessage.equals("Send")) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String message = prefs.getString("message","");
                String number = prefs.getString("phoneNumber","");
                //TODO: What happens if settings aren't set on app yet and wear app is used, need to handle.
                //TODO: Validate number?? at least for default
                sth.send(number, message);

            } else if (inputMessage.equals("GetNumber")) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final String number = prefs.getString("phoneNumber","");
                new SendToDataLayerThread("/message_path", "Number:" + number).start();

            } else if (inputMessage.equals("Open")) {
                Intent startIntent = new Intent(this, MainActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            }

        } else {
            super.onMessageReceived(messageEvent);
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.v("pingTag", "onPeerConnected:");
    }

    public class SendToDataLayerThread extends Thread {

        String m_path;
        String m_message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            m_path = p;
            m_message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(m_GoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(m_GoogleApiClient, node.getId(), m_path, m_message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("pingTag", "Message: {" + m_message + "} sent from phone to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.e("pingTag", "ERROR: failed to send Message");
                }
            }
            m_GoogleApiClient.disconnect();
        }
    }
}
