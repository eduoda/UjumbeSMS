package com.istresearch.ujumbesms.task;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.os.Build;

import com.istresearch.ujumbesms.App;
import com.istresearch.ujumbesms.JsonUtils;
import com.istresearch.ujumbesms.R;
import com.istresearch.ujumbesms.XmlUtils;

abstract public class BaseHttpTask extends AsyncTask<String, Void, HttpResponse> {
       
    protected App app;
    protected String url;    
    protected List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();    

    private List<FormBodyPart> formParts;
    protected boolean useMultipartPost = false;    
    protected HttpPost post;
    protected Throwable requestException;
    
    public BaseHttpTask(App app, String url, BasicNameValuePair... paramsArr)
    {
        this.url = url;
        this.app = app;                
        params = new ArrayList<BasicNameValuePair>(Arrays.asList(paramsArr));
        
        params.add(new BasicNameValuePair("version", "" + app.getPackageInfo().versionCode));
    }
    
    public void addParam(String name, String value)
    {
        params.add(new BasicNameValuePair(name, value));
    }    
    
    public void setFormParts(List<FormBodyPart> formParts)
    {
        useMultipartPost = true;
        this.formParts = formParts;
    }                     

    protected HttpPost makeHttpPost() throws Exception
    {
        HttpPost httpPost = new HttpPost(url);
                
        httpPost.setHeader("User-Agent", app.getText(R.string.app_name) + "/" + app.getPackageInfo().versionName + " (Android; SDK "+Build.VERSION.SDK_INT + "; " + Build.MANUFACTURER + "; " + Build.MODEL+")");

        if (useMultipartPost)
        {
            MultipartEntity entity = new MultipartEntity();//HttpMultipartMode.BROWSER_COMPATIBLE);

            Charset charset = Charset.forName("UTF-8");

            for (BasicNameValuePair param : params)
            {
                entity.addPart(param.getName(), new StringBody(param.getValue(), charset));
            }

            for (FormBodyPart formPart : formParts)
            {
                entity.addPart(formPart);
            }
            httpPost.setEntity(entity);                                                
        }
        else
        {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        }        
        
        return httpPost;
    }
    
    protected HttpResponse doInBackground(String... ignored) 
    {    
        try
        {
            post = makeHttpPost();
            
            HttpClient client = app.getHttpClient();
            return client.execute(post);            
        }     
        catch (Throwable ex) 
        {
            requestException = ex;
            
            try
            {
                String message = ex.getMessage();
                // workaround for https://issues.apache.org/jira/browse/HTTPCLIENT-881
                if ((ex instanceof IOException) 
                        && message != null && message.equals("Connection already shutdown"))
                {
                    // app.log("Retrying request");
                    post = makeHttpPost();
                    HttpClient client = app.getHttpClient();
                    
                    return client.execute(post);  
                }
            }
            catch (Throwable ex2)
            {
                requestException = ex2;
            }            
        }   
        
        return null;
    }

    /* leaving in for edu-tainment purposes
	protected String streamToString(InputStream aInputStream) {
		StringBuffer theStringBuffer = new StringBuffer();
		BufferedReader theReader = new BufferedReader(new InputStreamReader(aInputStream));
		String theLine;
		try {
			while ((theLine = theReader.readLine()) != null) {
				theStringBuffer.append(theLine + "\n");
			}
		} catch (IOException e) {
		}
		return theStringBuffer.toString();
	}
	*/
	
    protected String getResponseText(HttpResponse aResponse) {
    	String theResult;
    	try {
			//theResult = streamToString(aResponse.getEntity().getContent());
            theResult = IOUtils.toString(aResponse.getEntity().getContent(),"UTF-8");
    	} catch (Exception e) {
			theResult = e.getMessage();
		}
    	return theResult;
    }
    
    protected String getErrorText(HttpResponse aResponse) {
    	String theMimeType = getContentType(aResponse);
    	String theResult = null;
    	try {
        	if (theMimeType.startsWith("application/json")) {
        		theResult = JsonUtils.getErrorText(JsonUtils.parseResponse(aResponse));
        	} else if (theMimeType.startsWith("text/xml")) {
        		theResult = XmlUtils.getErrorText(XmlUtils.parseResponse(aResponse.getEntity().getContent()));
        	} else {
        		theResult = "HTTP "+aResponse.getStatusLine().getStatusCode();
        	}
    	} catch (Exception e) {
   			theResult = e.getMessage();
    	}
    	return theResult;
    }
    
    protected String getContentType(HttpResponse response)
    {
        Header contentTypeHeader = response.getFirstHeader("Content-Type");
        return (contentTypeHeader != null) ? contentTypeHeader.getValue() : "";
    }
    
    @Override
    protected void onPostExecute(HttpResponse response) {
        if (response != null)
        {                
            try
            {
                int statusCode = response.getStatusLine().getStatusCode();                
                
                if (statusCode == 200) 
                {
                    handleResponse(response);
                } 
                else if (statusCode >= 400 && statusCode <= 499)
                {
                    handleErrorResponse(response);
                    handleFailure();
                }
                else
                {
                    throw new Exception("HTTP " + statusCode);
                }

                response.getEntity().consumeContent();
            }
            catch (Throwable ex)
            {
                post.abort();
                handleResponseException(ex);
                handleFailure();
            }
            
        }
        else
        {
            handleRequestException(requestException);
            handleFailure();
        }
    }
    
    abstract protected void handleResponse(HttpResponse response) throws Exception;
    abstract protected void handleErrorResponse(HttpResponse response) throws Exception;
    abstract protected void handleFailure();
    abstract protected void handleRequestException(Throwable ex);
    abstract protected void handleResponseException(Throwable ex);
 
}