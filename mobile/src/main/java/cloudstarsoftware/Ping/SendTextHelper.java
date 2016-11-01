package cloudstarsoftware.Ping;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Created by Ryan on 5/6/2016.
 */

public class SendTextHelper
{
    final String TAG = "Ping!:SendTextHelper";

    public Boolean send(String phoneNumber, String message)
    {
        try {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }
}
