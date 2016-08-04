package com.kueep.recommendation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;

import com.google.gson.Gson;
//import com.kueep.analytics.HelloAnalytics;
import com.kueep.analytics.HelloAnalyticsWebService;
import com.kueep.httpUtils.HttpUtils;
import com.kueep.parse.ParseSample;

public class Recommendation implements Job{
	
	final static Logger logger = Logger.getLogger(Recommendation.class);

	private ParseSample parse = new ParseSample();
	//private HelloAnalytics analytics = new HelloAnalytics();
	private HelloAnalyticsWebService analytics = new HelloAnalyticsWebService();
	
	private static final String USER_ID = "userId";
	private static final String GROUP_ID = "groupId";
	private static final String PRODUCT_ID = "productId";
	private static final String PAGE_VIEWS = "pageViews";
	private static final String TIME_ON_PAGE = "timeOnPage";
	private final static String URL = "https://api.parse.com/1/classes/";

	
	private static final double TIME_ON_PAGE_CALCULATION = 0.5;
	private static final double PAGE_VIEWS_CALCULATION = 0.5;
	
	/*
	 * this method pulls all users and their current preferences from Parse.
	 * it returns map with objectId as key and preferences as value
	 * 
	 * the preferences value is actually a JSONArray represented as a String
	 */
	public Map<String, String> pullPreferences(){
		logger.debug("pulling preferences from Parse");
		JSONObject response = HttpUtils.sendGet("Pref",null, URL);
		Map<String, String> preferences = new HashMap<String,String>();
		
		try {
			JSONArray results = new JSONArray();
			results = response.getJSONArray("results");
			for (int i=0; i<results.length(); i++){
				JSONObject obj = results.getJSONObject(i);
				String objectId = obj.getString("objectId");
				String pref = obj.getString("pref");
				if (pref.equals("null"))
					preferences.put(objectId, null);
				else
					preferences.put(objectId, pref);
				
				logger.debug("user: " + objectId + " preferences: " + pref);
			}
			
		} catch (Exception e) {
			logger.error("error pulling preferences from Parse. original error: {}",e);
			throw new RuntimeException(e);
		}
		return preferences;	
	}
	
	public JSONArray convertGroupToProductAnalytics(JSONArray analytics){
		logger.debug("converting group to product in analytics report");
		JSONObject obj = null;
		JSONArray arr = new JSONArray();
		for (int i=0; i<analytics.length(); i++){
			try {
				obj = analytics.getJSONObject(i);
				String groupId = obj.getString(GROUP_ID);
				if (groupId.equals("Eaqow4gNMn") || groupId.equals("wElIuM8lwy")){
					continue;
				}
				//call parse and pull the productId
				String productId = parse.getProductId(groupId);
				
				//remove groupId and replace with productId
				obj.remove(GROUP_ID);
				obj.put(PRODUCT_ID, productId);
				
				arr.put(obj);
			} catch (Exception e) {
				logger.error("error while converting group to product in analytics. original error: "+e);
				throw new RuntimeException(e);
			}
		}
		return arr;
	}
	
	/*
	 * main method. pulls the analytics and current preferences.
	 * if it's a new user - set up their preferences.
	 * it it's an existing user - update their preferences.
	 */
	public Map<String, String> setNewPreferences(Date startDate, Date endDate){
		logger.info("\n\n====starting ML=====\n\n");
		JSONArray analyticsResults = new JSONArray();
		Map<String,String> preferences = null;
		try {
			//set up all data
			analyticsResults = analytics.pullAnalytics(startDate, endDate);
			if (analyticsResults==null) return null;
			JSONArray analyticsResultsConverted = convertGroupToProductAnalytics(analyticsResults);
			preferences = pullPreferences();
			
			//the algorithm
			for (int i=0; i<analyticsResultsConverted.length(); i++){
				JSONObject userAnalytics = analyticsResultsConverted.getJSONObject(i);
				String userId = userAnalytics.getString(USER_ID);
				
				//preferences map is build objectId as key (for future parse usage)
				String objectId = parse.getObjectId(userId);
				
				//if this specific user already has preferences
				if (preferences.get(objectId) != null && !preferences.get(objectId).isEmpty()){
					JSONArray prefList = setPrefList(userAnalytics, userId, preferences.get(objectId));
					preferences.replace(objectId, sort(prefList).toString());
				}
				//this is a new user
				else{
					JSONArray prefList = setPrefList(userAnalytics, userId, null);
					preferences.replace(objectId, sort(prefList).toString());
				}
			}		
		} catch (Exception e) {
			logger.error("error setting new preferences. original error: " + e);
			throw new RuntimeException(e);
		}
		return preferences;
	}

	//saves all new preferences on Parse
	public void saveNewPreferences(Map<String,String> preferences){
		logger.debug("saving new preferences on Parse");
		if (preferences!=null){
		    for (String key : preferences.keySet()) {
		        HttpUtils.sendPut(key, preferences.get(key), URL);
		    }	
		}
	}
	
	/*
	 * if currentPref is empty - set new pref list
	 * if not - update the current pref list.
	 * 
	 * the method calculates the "score" by pageViews and TimeOnPage calculation values.
	 */
	public JSONArray setPrefList(JSONObject userAnalytics, String userId, String currentPref){
		logger.debug("setting up the preferences list");
		JSONArray result = new JSONArray();
		try {
			String pageViews = userAnalytics.getString(PAGE_VIEWS);
			String timeOnPage = userAnalytics.getString(TIME_ON_PAGE);
			double score = Integer.parseInt(pageViews)*PAGE_VIEWS_CALCULATION
					+ Double.parseDouble(timeOnPage)*TIME_ON_PAGE_CALCULATION;
			userAnalytics.put("score", score);
			
			if (currentPref==null){
				result.put(userAnalytics);
			}
			else{
				JSONArray arr = new JSONArray(currentPref);
				arr.put(userAnalytics);
				return arr;			
			}		
		} catch (Exception e) {
			logger.error("error setting up preferences list. original error: " + e);
			throw new RuntimeException(e);
		}
		return result;	
	}

	/*
	 * receives JSONArray and sort it according the "score" parameter.
	 * we cannot sort JSONArray, that's why we convert it to "rec" class using gson
	 */
	public JSONArray sort(JSONArray arr){
		logger.debug("sorting preferences list");
		JSONArray result = new JSONArray();
		List<rec> recList = new ArrayList<>();
		Gson gson = new Gson();
		try {
			//convert each json to "rec" object
			for (int i=0; i<arr.length(); i++){
				rec r = gson.fromJson(arr.getJSONObject(i).toString(), rec.class);
				recList.add(r);
			}
			
			//sort
			Collections.sort(recList, new recommendationComperator());
			
			//convert back to JSONArray
			for (rec r:recList){
				String obj = gson.toJson(r);
				result.put(new JSONObject(obj));
			}	
		} catch (Exception e) {
			logger.error("error sorting preferences list. original error: " + e);
			throw new RuntimeException(e);
		}
		return result;
	}
	
	
	@SuppressWarnings("unused")
	public class rec{
		private String userId;
		private String productId;
		private double score;
	}
	
	public class recommendationComperator implements Comparator<rec>{
		@Override
		public int compare(rec o1, rec o2) {
			return o1.score>o2.score ? 1:0;
		}	
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			System.out.println("=====new job===== from syso");
			logger.info("\n\n=================new job===================\n\n");
			
			SchedulerContext schedulerContext = arg0.getScheduler().getContext();
		    Date[] dates = (Date[]) schedulerContext.get("myContextVar");
		    
		    logger.debug("job for date: " + dates[0]);
		    
			Map<String, String> preferences = setNewPreferences(dates[0], dates[1]);
			if (preferences!=null)
				saveNewPreferences(preferences);	
			
			logger.info("\n\n=================job finished===================\n\n");
		} catch (Exception e) {
			logger.error("error executing job. original error: ",e);
		}
	}
}
