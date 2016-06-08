package com.kueep.analytics;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.Webproperties;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * A simple example of how to access the Google Analytics API using a service
 * account.
 */
public class HelloAnalyticsWebService {

	final static Logger logger = Logger.getLogger(HelloAnalyticsWebService.class);

  private static final String APPLICATION_NAME = "Kueep";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String KEY_FILE_LOCATION = "/resources/credentials.p12";
  private static final String SERVICE_ACCOUNT_EMAIL = "kueep-ml-tomcat@placeq-prediction.iam.gserviceaccount.com";
  
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

  private static Analytics initializeAnalytics() throws Exception {
    // Initializes an authorized analytics service object.

    // Construct a GoogleCredential object with the service account email
    // and p12 file downloaded from the developer console.
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    GoogleCredential credential = new GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(JSON_FACTORY)
        .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
        .setServiceAccountPrivateKeyFromP12File(new File(KEY_FILE_LOCATION))
        .setServiceAccountScopes(AnalyticsScopes.all())
        .build();

    // Construct the Analytics service object.
    return new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME).build();
  }


  private static String getFirstProfileId(Analytics analytics) throws IOException {
    // Get the first view (profile) ID for the authorized user.
    String profileId = null;

    // Query for the list of all accounts associated with the service account.
    Accounts accounts = analytics.management().accounts().list().execute();

    if (accounts.getItems().isEmpty()) {
      System.err.println("No accounts found");
    } else {
      String firstAccountId = accounts.getItems().get(0).getId();

      // Query for the list of properties associated with the first account.
      Webproperties properties = analytics.management().webproperties()
          .list(firstAccountId).execute();

      if (properties.getItems().isEmpty()) {
        System.err.println("No Webproperties found");
      } else {
        String firstWebpropertyId = properties.getItems().get(0).getId();

        // Query for the list views (profiles) associated with the property.
        Profiles profiles = analytics.management().profiles()
            .list(firstAccountId, firstWebpropertyId).execute();

        if (profiles.getItems().isEmpty()) {
          System.err.println("No views (profiles) found");
        } else {
          // Return the first (view) profile associated with the property.
          profileId = profiles.getItems().get(0).getId();
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