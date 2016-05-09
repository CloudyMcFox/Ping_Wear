package rageofachilles.textryan_wear;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Ryan on 5/1/2016.
 */
public class ListenerServiceFromWear extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v("myTag", "onMessageReceived:");

        if (messageEvent.getPath().equals("/message_path")) {
            final String inputMessage = new String(messageEvent.getData());
            Log.d("TextRyan", "Message path received on watch is: " + messageEvent.getPath());
            Log.d("myTag", "Message received on watch is: " + inputMessage);


            // Just send text
            SendTextHelper sth = new SendTextHelper();
            if (inputMessage.equals("Send")) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String message = prefs.getString("message","");
                String number = prefs.getString("phoneNumber","");
                //TODO: What happens if settings aren't set on app yet and wear app is used, need to handle.
                //TODO: Validate number?? at least for default
                sth.send(number, message);
            }
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.v("myTag", "onPeerConnected:");
    }
}
