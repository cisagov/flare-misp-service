This application is a service that transfers STIX Events from a STIX Server to a MISP Server.

To generate encrypted password:

The encryption library (Encryption.jar) is located in the config folder.
To generate an encrypted password run the following command in terminal:
java -cp Encryption.jar xor.bcmcgroup.Main <password> <encryptionKey>

*** Keys and Certs ***

This application depends on a key value to connect to the MISP server, and depends on Cert and Key files to connect to the FLARE server.
Contact the system administrators for these systems to request these items.
The key value to connect to the MISP server should be assigned to the stixtransclient.misp.key property in the config.properties file.
The Cert and Key files to connect to the FLARE server should be saved to the config directory.
Please note that Development Key and Cert values are required to pass the unit tests when packaging the application, but production files will need to
be written to /opt/mtc/config after the application has been deployed.


To run the application:

mvn spring-boot:run

To Run Quartz job to periodically run the Misp Trans Client:

https://localhost:8443/initQuartz

FLARE to XML:

Create destination directory (/home/flaredev/flare/stix3)
Start application: mvn spring-boot:run
Access URL: https://localhost:8443/misptransclient?processType=xmlOutput

FLARE to MISP:

Start application: mvn spring-boot:run
Access URL: https://localhost:8443/misptransclient?processType=stixToMisp

This service was constructed based on an example which can be found at this URL:

https://spring.io/guides/gs/rest-service/

Build an executable JAR

You can run the application from the command line with Gradle or Maven. 
Or you can build a single executable JAR file that contains all the necessary 
dependencies, classes, and resources, and run that. This makes it easy to ship, 
version, and deploy the service as an application throughout the development 
lifecycle, across different environments, and so forth.


If you are using Maven, you can run the application using 

mvn spring-boot:run. 


Or you can build the JAR file with 

mvn clean package. 

Then you can run the JAR file:

java -jar target/mtc-rest-service-0.1.0.jar


** packageDeployment.sh **

The packageDeployment.sh script is a script that will package the application for deployment.

The script performs the following steps:

* TARs up the existing install in a time stamped file that is written to /tmp.
* Created output directory /opt/mtc/out
* Installs files necessary to perform unit tests to /opt/mtc/config
* Runs mvn clean package, which runs the unit tests and builds the JAR file
* Packages the JAR and all the application related files in a TAR file which can then be unTARed to /opt/mtc
* Once the TAR file has been unpacked, run /opt/mtc/runFLAREmispService.sh to launch the application

Sample commands:

initQuartz
https://localhost:8443/initQuartz

listQuartzJobs
https://localhost:8443/listQuartzJobs

stopQuartzJobs
https://localhost:8443/stopQuartzJobs

xmlOutput
https://localhost:8443/misptransclient?processType=xmlOutput

stixToMisp
https://localhost:8443/misptransclient?processType=stixToMisp

checkTaxiiStatus
https://localhost:8443/checkTaxiiStatus

checkMispStatus
https://localhost:8443/checkMispStatus

xmlOutput - NCPS_Automated
https://localhost:8443/misptransclient?processType=xmlOutput&collection=NCPS_Automated
 




