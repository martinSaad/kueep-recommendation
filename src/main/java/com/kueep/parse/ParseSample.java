package com.kueep.parse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.kueep.httpUtils.HttpUtils;

public class ParseSample {
	final static Logger logger = Logger.getLogger(ParseSample.class);

	private final static String URL = "https://api.parse.com/1/classes/";
	private final static String PARSE_APPLICATION_ID = "parse_application_id.txt";
	private final static String PARSE_REST_API_KEY = "parse_rest_api_key.txt";
	private final static String PREF_TABLE = "Pref";
	
	
	public String getProductId(String groupId) throws Exception{
		JSONObject response;
		String productId = null;
		try {
			response = HttpUtils.sendGet("Group_Buying", groupId, URL);
			productId = response.getJSONObject("product").getString("objectId");
		} catch (Exception e) {
			logger.error("error pulling productId from Parse. original error: " + e);
			throw new Exception(e);
		}
		return productId;
	}
	
	public String getObjectId(String userId) throws Exception{
		JSONObject response;
		String objectId = null;
		try {
			response = HttpUtils.sendGet(PREF_TABLE, null, URL);
			JSONArray results = response.getJSONArray("results");
			for (int i=0; i<results.length(); i++){
				JSONObject obj = results.getJSONObject(i);
				if (obj.getString("user_id").equals(userId)){
					return obj.getString("objectId");
				}
			}
		} catch (Exception e) {
			logger.error("error pulling objectId from Parse. original error: " + e);
			throw new Exception(e);
		}
		return objectId;
	}
	
	public String readFromFile(String path){
		StringBuilder builder = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new FileReader(path)))
		{

			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				builder.append(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} 
		return builder.toString();
	}
}
