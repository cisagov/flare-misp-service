# Flare MISP Service - scheduler for CTI-toolkit
The Flare MISP Service is a scheduling and logging tool for CTI-toolkit. 
It can be configured to run CTI-toolkit commands to pull from a TAXII server 
and convert the STIX files into MISP format or just download the STIX as xml 
at regular intervals. 

# To build the tarball:

1) Pull the most recent code 
   Set up Maven and Java 8 properly
2) go to the ~git/FLAREmispService/misp-trans-client-rest-service folder. 
   There should be a pom.xml file in this directory. Run command:
				mvn clean package
   in order to build the package
3) run the packageDeployment.sh script to pack the built mvn package into a tarball
   the tarball should be in ~git/FLAREmispService/misp-trans-client-rest-service/deploy/
   named FLAREmispService.tar
   
# Tutorial

See included TutorialOn_FLAREMispService.pptx
   
# Installation:

Refer to the SAG for more details

1)	Create the directory /opt/mtc and set its permissions:
sudo mkdir /opt/mtc
sudo chmod 755 /opt/mtc

2)	Copy the FLAREmispService.tar to /opt/mtc:
sudo cp FLAREmispService.tar /opt/mtc

3)	Untar the tarball.
tar –xvf FLAREmispService.tar

It ‘should’ contain the directories:
/opt/mtc
/opt/mtc/out
/opt/mtc/logs
/opt/mtc/config

The tarball should contain 2 properties files in /opt/confg:
application.propertiesflare
config.properties

The /logs and /out directories should be empty (initially).


4)	Verify the ownership of the subdirectories under /opt/mtc are owned by ‘flare’  (or ‘flaredev’)
	If the directories are not owned by the proper account, change them.
a)	cd /opt/mtc
b)	sudo chown flare:flare /opt/mtc/*

5)	Copy certificate and key for the TAXII Server (FlareSuite) to /opt/mtc/config
a.	The names and locations of the key and cert files are configurable with properties in  /opt/mtc/config/config.properties
b.	They default to the location /opt/mtc/config
c.	The key and cert files are specific to the FlareSuite TAXII Server


6)	Tweak the /opt/mtc/config/config.properties file to appropriate values for the Deployed Environment.
Modify property: bin.filepath to stixtransclient.py location
a.	Modify property: stixtransclient.client.key to be equal to path and name of the key file from step #5
b.	Modify property: stixtransclient.client.cert to be equal to path and name of the certificate file from step #5
c.	Modify/Uncomment property:  mtc.processtype
i.	set the property to:    mtc.processtype=stixToMisp
d.	Modify/Uncomment property:  mtc.quartz.frequency=5
e.	Modify property: stixtransclient.poll.baseurl to have the IP and port for the Flaresuite TAXII server in deployed environment
f.	Modify property: stixtransclient.poll.url 
i.	Start with the same value as stixtransclient.poll.baseurl property in step (6e)
ii.	append the following: /flare/taxii11/poll/
g.	Modify property: stixtransclient.misp.url to have the IP for the MISP Server.
h.	Modify property: stixtransclient.misp.key  (must be key to MISP server in deployed environment)
	
7)	Tweak the /opt/mtc/config/application.properties file to appropriate values for the Deployed Environment.
a.	Verify the logging property: logging.level.org.gd.ddcs.flare.misp = ERROR             (can be WARN, INFO, or DEBUG for additional information)
b.	Verify the logging property: logging.level.org.springframework.web = ERROR      (can be WARN, INFO, or DEBUG for additional information)

8)	To start the FlareMispService:
a.	Run the shell script:  /opt/mtc/runFLAREmispService.sh
b.	Output should display on the console


9)	Independently verify the service is running with the following command:
a.	ps -ef | grep –i mtc-rest-service-0.1.0.jar

10)	To verify the service can connect to the FlareSuite’s TAXII Server:
a.	http://localhost:8080/checkTaxiiStatus
b.	Should seem something similar to: {"resourceType":"FLARE/TAXII","resource":"https://10.23.218.172:8443","statusCode":200}
c.	The statusCode of value 200 indicates a successful connection to the TAXII server
d.	If the value is not 200, verify the ip address and port in step 6e and 6f above.
e.	Otherwise must verify that the FlareSuite TAXII server is running and accessible to the server where FlareMispService is running.

11)	To verify the service can connect to the MISP Server:
a.	http://localhost:8080/checkMispStatus
b.	Should seem something similar to: {"resourceType":"Misp","resource":"http://10.23.218.173","statusCode":200}
c.	The statusCode of value 200 indicates a successful connection to the TAXII server
d.	If the value is not 200, verify the ip address in step 6g above. (no port specified because MISP uses a predefined port #)
e.	Otherwise must verify that the MISP server is running and accessible to the server where FlareMispService is running.


12)	Quartz output messages:
a.	When it’s time for the Quartz job to poll the FlareSuite TAXI server for STIXX information, you can expect to see the following information on the command line and/or in /opt/mtc/logs/mtc-rest-service.log
(the dates and times will change accordingly)
2018-02-28 09:36:46 - Processing events...
2018-02-28 09:36:46 - TAXII health check passed.
2018-02-28 09:36:47 - Misp health check passed.
2018-02-28 09:36:48 - Processing events: Success
2018-02-28 09:36:48 - {"id":14,"status":"Success","detailedStatus":"Completed sucessfully","processType":"xmlOutput","collection":"MISP","beginTimestamp":"2018-02-28T09:35:47+00:00","endTimeStamp":"2018-02-28T09:36:47+00:00"}
   
# Endpoints
Verify the Quartz Scheduler is Up and Running
URL:  http://localhost:8080/listQuartzJobs
﻿Quartz Jobs:[jobName] : initializeQuartzJob [groupName] : group1 - Thu Mar 22 12:30:25 EDT 2018<r>


Verify connectivity to MISP Server
URL: http://localhost:8080/checkMispStatus
﻿{"resourceType":"Misp","resource":"http://10.23.218.173","statusCode":200}


Verify connectivity to FLARE TAXI Server
URL: http://localhost:8080/checkTaxiiStatus
﻿{"resourceType":"FLARE/TAXII","resource":"https://10.23.218.172:8443","statusCode":200}


Verify the configuration property file can be loaded/reloaded
This does not refresh the timestamps
URL: http://localhost:8080/refreshConfig
<blank page result is fine>  

Connect to FLARE TAXI, download any new STIX, then save as XML to temporary directory  “/opt/mtc/out”
URL: http://localhost:8080/misptransclient?processType=xmlOutput

Result:
﻿{"id":5,"status":"Success","detailedStatus":"Completed sucessfully","processType":"xmlOutput","collection":"MISP","beginTimestamp":"2018-03-22T11:10:56+00:00","endTimeStamp":"2018-03-22T12:33:05+00:00"}


Connect to FLARE TAXI, convert new STIX to MISP, then upload to MISP Server
URL: http://localhost:8080/misptransclient?processType=stixToMisp

Result:
﻿{"id":7,"status":"Success","detailedStatus":"Completed sucessfully","processType":"stixToMisp","collection":"MISP","beginTimestamp":"2018-03-22T12:33:23+00:00","endTimeStamp":"2018-03-22T12:34:18+00:00"}


