package org.gd.ddcs.flare.misp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisallowConcurrentExecution
public class InitializeQuartzJob implements Job
{
    private static Logger log = LoggerFactory.getLogger(InitializeQuartzJob.class);
	String urlStr = Config.getProperty("mtc.baseurl") + "?processType=" + Config.getProperty("mtc.processtype");

	public void execute(JobExecutionContext context)
	throws JobExecutionException {
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			URL url = new URL(urlStr);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			is = conn.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			String output;
			//log.info("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				log.info(output);
			}

			br.close();
			isr.close();
			is.close();
			conn.disconnect();

		  } catch (MalformedURLException e) {
			  log.error("ERROR: MalformedURLException");
		  } catch (IOException e) {
			  log.error("ERROR: IOException");
		  }
		  finally{
			if(br != null) {
				safeCloseBR(br);
			}
			
			if(isr != null) {
				safeCloseISR(isr);
			}
			
			if(is != null) {
				safeCloseIS(is);
			}
		}
	}
	
	private static void safeCloseBR(BufferedReader br) {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
				log.error("safeCloseBR",e);
			}
		}
	}
	
	private static void safeCloseIS(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				log.error("safeCloseIS",e);
			}
		}
	}
	
	private static void safeCloseISR(InputStreamReader isr) {
		if (isr != null) {
			try {
				isr.close();
			} catch (IOException e) {
				log.error("safeCloseISR",e);
			}
		}
	}
}
