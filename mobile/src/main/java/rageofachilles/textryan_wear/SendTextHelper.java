package rageofachilles.textryan_wear;

import android.provider.SyncStateContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Ryan on 5/6/2016.
 */

public class SendTextHelper
{
    String TAG = "TextRyan_Wear";
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
