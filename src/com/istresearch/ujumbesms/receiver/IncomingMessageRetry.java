
package com.istresearch.ujumbesms.receiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.istresearch.ujumbesms.App;
import com.istresearch.ujumbesms.IncomingMessage;

public class IncomingMessageRetry extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) 
    {        
        App app = (App) context.getApplicationContext();
        if (!app.isEnabled())
        {
            return;
        }
        
        IncomingMessage message = app.inbox.getMessage(intent.getData());
        
        if (message == null)
        {
            return;
        }
        
        app.inbox.enqueueMessage(message);        
    }        
}    
