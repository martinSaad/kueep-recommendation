package launch;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import com.kueep.recommendation.Recommendation;
import org.apache.log4j.Logger;

public class Main {
	
	final static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        String webappDirLocation = "src/main/webapp/";
        Tomcat tomcat = new Tomcat();

        //The port that we should run on can be set into an environment variable
        //Look for that variable and default to 8080 if it isn't there.
        String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }

        tomcat.setPort(Integer.valueOf(webPort));

        StandardContext ctx = (StandardContext) tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());
        System.out.println("configuring app with basedir: " + new File("./" + webappDirLocation).getAbsolutePath());

        // Declare an alternative location for your "WEB-INF/classes" dir
        // Servlet 3.0 annotation will work
        File additionWebInfClasses = new File("target/classes");
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                additionWebInfClasses.getAbsolutePath(), "/"));
        ctx.setResources(resources);

        logger.info("\n\n=========starting apache tomcat=========\n\n");
        tomcat.start();
        
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
		Date[] dates = {getMeYesterday(), getMeYesterday()};
		//Date[] dates = {dt.parse("2016-06-01"), dt.parse("2016-06-12")};
		
		Recommendation r = new Recommendation();
		Map<String, String> preferences = r.setNewPreferences(dates[0], dates[1]);
		if (preferences!=null)
			r.saveNewPreferences(preferences);
        
        
        tomcat.getServer().await();
    }
    
	private static Date getMeYesterday(){
	     return new Date(System.currentTimeMillis()-24*60*60*1000);
	}
}