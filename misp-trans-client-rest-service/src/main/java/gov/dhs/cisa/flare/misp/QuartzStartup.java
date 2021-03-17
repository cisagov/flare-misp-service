package gov.dhs.cisa.flare.misp;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class QuartzStartup implements ApplicationListener<ApplicationReadyEvent> {

	//Concept from online article: //https://stackoverflow.com/questions/27405713/running-code-after-spring-boot-starts
	//Goal: To insure the Quartz Scheduler is started automatically without manual intervention.
	//      Spring/SpringBoot supports the @Component annotation and several EventHandler methods that you can implement.
	//      onApplicationEvent() is called once after all the setup of SpringBoot is complete.
	//      We can use the same code for initQuartz() here inside this event handler at this point in time.

	
	private static Logger log = LoggerFactory.getLogger(QuartzStartup.class);

	@Override
	public void onApplicationEvent(ApplicationReadyEvent arg0) {
		log.info("ApplicationListener - Asked to Initialize Quartz Scheduler...");

		String quartzFrequencyStr = Config.getProperty("mtc.quartz.frequency");
		int quartzFrequency = 2;

		if (!(quartzFrequencyStr == null || "".equals(quartzFrequencyStr))) {
			quartzFrequency = Integer.parseInt(quartzFrequencyStr);
		}

		try {
			JobDetail job1 = JobBuilder.newJob(InitializeQuartzJob.class).withIdentity("initializeQuartzJob", "group1")
					.build();

			Trigger trigger1 = TriggerBuilder.newTrigger().withIdentity("simpleTrigger", "group1")
					.withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(quartzFrequency)).build();
			Scheduler scheduler1 = new StdSchedulerFactory().getScheduler();

			if (!scheduler1.isStarted()) {
				log.info("ApplicationListener- Quartz Scheduler can be automatically started. Starting it now.");
				scheduler1.start();
				scheduler1.scheduleJob(job1, trigger1);
				log.info("ApplicationListener- Quartz Scheduler Started and Job 'initializeQuartzJob' successful.");
			} else {
				log.warn(
						"ApplicationListener- Quartz Scheduler has already been started. Ignoring this request to Start it again.");
			}
		} catch (SchedulerException ex) {
			log.error(
					"ApplicationListener- Quartz Scheduler Exception encountered while attempting to start the Scheduler.");
		}
	}
}
