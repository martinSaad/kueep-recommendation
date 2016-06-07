package com.kueep.analytics;


import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.GaData;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;


/**
 * A simple example of how to access the Google Analytics API.
 */
public class HelloAnalytics {
  // Path to client_secrets.json file downloaded from the Developer's Console.
  // The path is relative to HelloAnalytics.java.
  private static final String CLIENT_SECRET_JSON_RESOURCE = "client_secrets.json";

  // The directory where the user's credentials will be stored.
  private static final File DATA_STORE_DIR = new File(
      System.getProperty("user.home"), ".store/hello_analytics");

  final static Logger logger = Logger.getLogger(HelloAnalytics.class);
  
  private static final String APPLICATION_NAME = "Kueep";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static NetHttpTransport httpTransport;
  private static FileDataStoreFactory dataStoreFactory;
  
  private static final String USER_ID = "userId";
  private static final String GROUP_ID = "groupId";
  private static final String PAGE_VIEWS = "pageViews";
  private static final String TIME_ON_PAGE = "timeOnPage";
  
  
  public JSONArray pullAnalytics(Date startDate, Date endDate) throws Exception{
	  JSONArray data = new JSONArray();
	  logger.debug("preparing to connect to Google Analytics");
	  logger.debug("start date: " + startDate + "\nend date: " + endDate);
	  try {
	      Analytics analytics = initializeAnalytics();
	      String profile = getFirstProfileId(analytics);
	      data = parseResponse(getResults(analytics, profile, startDate, endDate));
	  	  logger.debug("Data from Analytics: " + data);
	} catch (Exception e) {
		logger.error("error while trying to pull analytics. original error: " + e);
		throw new Exception(e);
	}
	  return data;
  }

  private static Analytics initializeAnalytics() {

	  try {
		  httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		   dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
		   
		   logger.debug("loanding credentials from path " + CLIENT_SECRET_JSON_RESOURCE);

		    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
		        new InputStreamReader(HelloAnalytics.class.getClassLoader()
		            .getResourceAsStream(CLIENT_SECRET_JSON_RESOURCE)));

		    logger.debug("setting up authorization code flow for all auth scopes");
		    
		    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
		        .Builder(httpTransport, JSON_FACTORY, clientSecrets,
		        AnalyticsScopes.all()).setDataStoreFactory(dataStoreFactory)
		        .build();

		    logger.debug("authorizing...");
		    Credential credential = new AuthorizationCodeInstalledApp(flow,
		        new LocalServerReceiver()).authorize("user");
		    
		    // Construct the Analytics service object.
		    return new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
		        .setApplicationName(APPLICATION_NAME).build();

	} catch (Exception e) {
		logger.error("error connecting to google analytics ", e);
	}
	  return null;
  }

  private static String getFirstProfileId(Analytics analytics) throws IOException {
    // Get the first view (profile) ID for the authorized user.
    String profileId = null;

    // Query for the list of all accounts associated with the service account.
    com.google.api.services.analytics.model.Accounts accounts = analytics.management().accounts().list().execute();
    if (accounts.getItems().isEmpty()) {
    	logger.debug("No accounts found");
    } else {
      String firstAccountId = accounts.getItems().get(0).getId();

      // Query for the list of properties associated with the first account.
      com.google.api.services.analytics.model.Webproperties properties = analytics.management().webproperties()
          .list(firstAccountId).execute();

      if (properties.getItems().isEmpty()) {
    	  logger.debug("No properties found");
      } else {
        String firstWebpropertyId = properties.getItems().get(0).getId();

        // Query for the list views (profiles) associated with the property.
        com.google.api.services.analytics.model.Profiles profiles = analytics.management().profiles()
            .list(firstAccountId, firstWebpropertyId).execute();

        if (profiles.getItems().isEmpty()) {
        	logger.debug("No views (profiles) found");
        } else {
          // Return the first (view) profile associated with the property.
          profileId = profiles.getItems().get(1).getId();
        }
      }
    }
    return profileId;
  }

  private static GaData getResults(Analytics analytics, String profileId, Date startDate, Date endDate) throws IOException {
    // Query the Core Reporting API for the number of sessions
    // in the past seven days.
	  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	  
    return analytics.data().ga()
        .get("ga:" + profileId, dateFormat.format(startDate), dateFormat.format(endDate),
        		"ga:pageviews,ga:timeOnPage").setDimensions("ga:dimension4,ga:pagePath")
        			.execute();
  }
  
  public static JSONArray parseResponse(GaData results) throws Exception{
	  logger.debug("parsing data from Analytics");
	  if (results.getRows()==null){
		  logger.debug("no results. todays report is empty.");
		  return null;
	  }
	  
	  //if there are results
	  List<List<String>> rows = results.getRows();
	  JSONArray arr = new JSONArray();
	  JSONObject json = null;
	  /*
	   * each row is like the following: userId, pageUrl, pageViews, timeOnPage
	   */
	  for (int i=0; i<rows.size(); i++){
		  //see if the pageUrl is a specific group
		  String url = rows.get(i).get(1);
		  if (url.contains(GROUP_ID)){
			  String tmp = url.split("=")[1];
			  String groupId = tmp.split("&")[0];
			  String userId = rows.get(i).get(0);
			  String pageViews = rows.get(i).get(2);
			  String timeOnPage = rows.get(i).get(3);
			  
			  try {
				  json = new JSONObject();
				  json.put(USER_ID, userId);
				  json.put(GROUP_ID, groupId);
				  json.put(PAGE_VIEWS, pageViews);
				  json.put(TIME_ON_PAGE, timeOnPage);
				  				  
				  arr.put(json);
			} catch (Exception e) {
				logger.error("error while parsing analytics report. original error: " + e);
				throw new Exception(e);
			}
		  }
	  }
	  logger.debug("result from parsing response: " + arr.toString());
	  return arr;
  }
}