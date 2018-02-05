package org.gd.ddcs.flare.misp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 

import org.quartz.DisallowConcurrentExecution;

//@DisallowConcurrentExecution
@DisallowConcurrentExecution
public class InitializeQuartzJob implements Job
{
    private final Logger log = LoggerFactory.getLogger(InitializeQuartzJob.class);
	String urlStr = Config.getProperty("mtc.baseurl") + "?processType=" + Config.getProperty("mtc.processtype");

	public void execute(JobExecutionContext context)
	throws JobExecutionException {
		try {
			URL url = new URL(urlStr);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

			String output;
			//log.info("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				log.info(output);
			}

			conn.disconnect();

		  } catch (MalformedURLException e) {

			//e.printStackTrace();
			log.info("ERROR: MalformedURLException");

		  } catch (IOException e) {

			//e.printStackTrace();
			log.info("ERROR: IOException");
		  }
	}
}
