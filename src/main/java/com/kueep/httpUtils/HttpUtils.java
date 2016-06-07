package com.kueep.httpUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.kueep.parse.ParseSample;

public class HttpUtils {

	final static Logger logger = Logger.getLogger(HttpUtils.class);
	private final static String PREF_TABLE = "Pref";


	public static JSONObject sendGet(String className, String userObjectId, String url){
		logger.debug("preparing to send GET request to Parse.");
		
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(buildGetUrl(className, userObjectId, url));
		JSONObject finalJSON = null;
		
		// add request headers

		request.addHeader("X-Parse-Application-Id", "GqwWATRcgsMvZxDSlkoOqadSKJoCWgOS3jna63qd");
		request.addHeader("X-Parse-REST-API-Key", "DsyqRVAqe4OXTDfyjhRYjOF2GHXJ8vNoaT0Cmvyk");
		
		try {
			logger.debug("sending GET request to URL " + request.getURI());
			
			org.apache.http.HttpResponse response = client.execute(request);
			
			logger.debug("response code: " + response.getStatusLine().getStatusCode());


			BufferedReader rd = new BufferedReader(
	                       new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			logger.debug("Parse response: " + result.toString());
			finalJSON = new JSONObject(result.toString());
		} catch (Exception e) {
			logger.error("error sending get request. orignal error: ",e);
		}		
		return finalJSON;
	}

	public static String buildGetUrl(String className, String userObjectId, String url){
		if (userObjectId==null)
			return url +className + "/";
		
		return url +className + "/" + userObjectId;
	}
	
	
	
	public static void sendPut(String userObjectId, String pref, String url){
		logger.debug("preparing to send PUT request to Parse.");
	    HttpClient httpClient = new DefaultHttpClient();
	    try {
	        HttpPut request = new HttpPut(buildPutUrl(userObjectId, url));
	        StringEntity params =new StringEntity(buildPutRequest(pref),"UTF-8");
	        //params.setContentType("application/json");
	        request.addHeader("content-type", "application/json");
	        request.addHeader("X-Parse-Application-Id", "GqwWATRcgsMvZxDSlkoOqadSKJoCWgOS3jna63qd");
	        request.addHeader("X-Parse-REST-API-Key", "DsyqRVAqe4OXTDfyjhRYjOF2GHXJ8vNoaT0Cmvyk");
	        request.setEntity(params);
	        
			logger.debug("sending PUT request to URL " + request.getURI());

	        org.apache.http.HttpResponse response = httpClient.execute(request);
	        
	        for (Header header : request.getAllHeaders()){
		        logger.debug("request header " + header.toString());

	        }
			logger.debug("requset body: " + pref);
			
	        if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204) {
				logger.debug("response code: " + response.getStatusLine().getStatusCode());
	            BufferedReader br = new BufferedReader(
	                    new InputStreamReader((response.getEntity().getContent())));

	            String output;
	            
            	logger.debug("response from Parse:");
	            while ((output = br.readLine()) != null) {
	            	logger.debug(output + "\n");
	            }
	        }
	        else{
	            throw new RuntimeException("Failed : HTTP error code : "
	                    + response.getStatusLine().getStatusCode());
	        }

	    }catch (Exception e) {
        	logger.error("error sending PUT request. original error: ", e);
	    } finally {
	        httpClient.getConnectionManager().shutdown();
	    }

	}
	
	
	public static String buildPutRequest(String pref) throws JSONException{
		return new JSONObject().put("pref", pref).toString();
	}
	
	public static String buildPutUrl(String userObjectId, String url){
		return url + PREF_TABLE + "/" + userObjectId;
	}
	
}
