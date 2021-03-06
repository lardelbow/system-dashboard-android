package org.docsapps.mobapps.systemdashboard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * @class RestWebService
 *
 * @brief This class extends the IntentService, which provides a
 *        framework that simplifies programming and processing Android
 *        Started Services concurrently.
 * 
 *        DownloadIntentService receives an Intent containing a URL
 *        (which is a type of URI) and a Messenger (which is an IPC
 *        mechanism). It downloads the file at the URL, stores it on
 *        the file system, then returns the path name to the caller
 *        using the supplied Messenger.
 * 
 *        The IntentService class implements the CommandProcessor
 *        pattern and the Template Method Pattern.  The Messenger is
 *        used as part of the Active Object pattern.
 */
public class RestWebService extends IntentService {
	
	public static final String MESSENGER_KEY = "MESSENGER";
	public static final String RESTURL_KEY = "RESTURL";
	public static final String JSON_KEY = "JSON";
	public static final String TOKEN_KEY = "TOKEN";
	
    /**
     * The default constructor for this service. Simply forwards
     * construction to IntentService, passing in a name for the Thread
     * that the service runs in.
     */
    public RestWebService() { 
        super("RestWebService Worker Thread"); 
    }

    /**
     * Optionally allow the instantiator to specify the name of the
     * thread this service runs in.
     */
    public RestWebService(String name) {
        super(name);
    }

    /**
     * Make an intent that will start this service if supplied to
     * startService() as a parameter.
     * 
     * @param context		The context of the calling component.
     * @param handler		The handler that the service should use to respond with a result  
     * @param uri            The web URL of a file to download
     * 
     * The returned intent is a Command in the Command Processor Pattern. The intent contains a
     * messenger, which plays the role of Proxy in the Active ObjectPattern.
     * 
     * Returns an intent that can start this Service.
     */
    public static Intent makeIntent(Context context, Handler handler, String url, String token) {
    	Messenger messenger = new Messenger(handler);
    	Intent intent = new Intent(context, RestWebService.class);
    	intent.putExtra(MESSENGER_KEY, messenger);
    	intent.putExtra(RESTURL_KEY, url);
    	intent.putExtra(TOKEN_KEY, token);
        return intent;
    }

    /**
     * Hook method called when a component calls startService() with
     * the proper intent.  This method serves as the Executor in the
     * Command Processor Pattern. It receives an Intent, which serves
     * as the Command, and executes some action based on that intent
     * in the context of this service.
     * 
     * This method is also a Hook Method in the Template Method
     * Pattern. The Template class has an overall design goal and
     * strategy, but it allows subclasses to how some steps in the
     * strategy are implemented. For example, IntentService handles
     * the creation and lifecycle of a started service, but allows a
     * user to define what happens when an Intent is actually handled.
     * 
     * This method is triggered when the startService() method is called on the caller.
     */
    @Override
	protected void onHandleIntent (Intent intent) {
    	String jsonMessage=null;
    	try {
    		// Call the remote API with a restful GET request and assign the response to jsonMessage.
			jsonMessage=getRequestToAPI(intent.getStringExtra(RestWebService.RESTURL_KEY), intent.getStringExtra(RestWebService.TOKEN_KEY));
		} catch (IOException e) {
    		e.printStackTrace();
    		jsonMessage="NoData";
    	}
    	
    	//try {
		//	Thread.sleep(5000);
		//} catch (InterruptedException e1) {}
		//jsonMessage="{\"systems\":[{\"name\":\"kirk\",\"status\":\"green\"},{\"name\":\"spock\",\"status\":\"amber\"},{\"name\":\"bones\",\"status\":\"red\"}]}";
    	
        try {
        	// Return the Message object with the JSON response back to the caller.
        	((Messenger) intent.getExtras().get(RestWebService.MESSENGER_KEY)).send(setReturnMessage(jsonMessage));
        } catch (RemoteException e) {e.printStackTrace();}
    }
    
    // Create a Message object containing the JSON response.
    private Message setReturnMessage(String jsonMessage) {
        Message msg = Message.obtain();
        Bundle data = new Bundle();
        data.putString(JSON_KEY, jsonMessage);
        msg.setData(data);
        return msg;
    }
    
    // Does all the hard work in calling the remote API and returning the JSON response.
	private String getRequestToAPI(String url, String token) throws IOException {
		String jsonMessage=null;
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpGet=new HttpGet(url);
	    httpGet.addHeader("Authorization", token);
	    httpGet.addHeader("Accept",returnContentType());
	    HttpResponse response = httpclient.execute(httpGet);
	    StatusLine statusLine = response.getStatusLine();
	    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        response.getEntity().writeTo(out);
	        out.close();
	        jsonMessage = out.toString();
	    } else {
	        response.getEntity().getContent().close();
	        throw new IOException(statusLine.getReasonPhrase());
		}
		return jsonMessage;
	}
	
	private String returnContentType() {
		return "application/json";
	}
}