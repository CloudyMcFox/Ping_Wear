package rageofachilles.Ping;

import android.telephony.SmsManager;
import android.util.Log;

/**
 * Created by Ryan on 5/6/2016.
 */

public class SendTextHelper
{
    Boolean m_fSendSucceeded = false; // used in main timer to check if we sent yet

    public Boolean send(String phoneNumber, String message)
    {
        try {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null);
            m_fSendSucceeded = true;
            return true;
        } catch (Exception e) {
            Log.e("pingTag", e.getMessage());
            return false;
        }
    }
}
