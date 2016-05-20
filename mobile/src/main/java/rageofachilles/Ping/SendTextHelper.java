package rageofachilles.Ping;

import android.telephony.SmsManager;
import android.util.Log;

/**
 * Created by Ryan on 5/6/2016.
 */

public class SendTextHelper
{
    String TAG = "tagPing";
    Boolean m_fSendSucceeded = false;

    public Boolean send(String phoneNumber, String message)
    {
        try {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null);
            m_fSendSucceeded = true;
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }
}
