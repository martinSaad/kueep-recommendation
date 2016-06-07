package com.kueep.start;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.kueep.recommendation.Recommendation;

public class Start {
	
	public static void start() throws SchedulerException, ParseException {
		JobDetail job = JobBuilder.newJob().ofType(Recommendation.class)
				.withIdentity("Recommendation Job", "group1").build();



			// Trigger the job to run on the next round minute
			Trigger trigger = TriggerBuilder
				.newTrigger()
				.withIdentity("dummyTriggerName", "group1")
				.withSchedule(
					SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(2)
						.repeatForever())
				.build();

			// schedule it
			Scheduler scheduler = new StdSchedulerFactory().getScheduler();
				
			SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
			
			
			Date[] arr = {getMeYesterday(), getMeYesterday()};
			//Date[] arr = {dt.parse("2016-06-03"), dt.parse("2016-06-03")};
			
			scheduler.getContext().put("myContextVar", arr);
			scheduler.start();
			scheduler.scheduleJob(job, trigger);
	}

//	public static void main(String[] args) throws SchedulerException, ParseException {
//		JobDetail job = JobBuilder.newJob().ofType(Recommendation.class)
//				.withIdentity("Recommendation Job", "group1").build();
//
//
//
//			// Trigger the job to run on the next round minute
//			Trigger trigger = TriggerBuilder
//				.newTrigger()
//				.withIdentity("dummyTriggerName", "group1")
//				.withSchedule(
//					SimpleScheduleBuilder.simpleSchedule()
//						.withIntervalInHours(24).repeatForever())
//				.build();
//
//			// schedule it
//			Scheduler scheduler = new StdSchedulerFactory().getScheduler();
//				
//			SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
//			
//			
//			Date[] arr = {getMeYesterday(), getMeYesterday()};
//			//Date[] arr = {dt.parse("2016-06-03"), dt.parse("2016-06-03")};
//			
//			scheduler.getContext().put("myContextVar", arr);
//			scheduler.start();
//			scheduler.scheduleJob(job, trigger);
//	}
	
	private static Date getMeYesterday(){
	     return new Date(System.currentTimeMillis()-24*60*60*1000);
	}
}
