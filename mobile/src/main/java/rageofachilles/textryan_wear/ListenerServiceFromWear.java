package rageofachilles.textryan_wear;

import android.content.Intent;
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
            final String message = new String(messageEvent.getData());
            Log.d("TextRyan", "Message path received on watch is: " + messageEvent.getPath());
            Log.d("myTag", "Message received on watch is: " + message);

//            // Launch phone app
//            Intent startIntent = new Intent(this, MainActivity.class);
//            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(startIntent);

            // Just send text
            SendTextHelper sth = new SendTextHelper();
            String[] messageArray = message.split(";");
            if (2 == messageArray.length) {
                sth.send(messageArray[0], messageArray[1]);
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
