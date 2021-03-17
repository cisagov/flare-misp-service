# Issues
During our development, we have encountered the following coding related issues:

1. Input Path Not Canonicalized, line 44 (3 repeats) of Config.java

```
Method getProperty at line 42 of flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/Config.java gets dynamic data from the getProperty element.
This element’s value then flows through the code and is eventually used in a file path for local disk access in getOutputFilePath at line 133 of flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/Config.java. This may cause a Path Traversal vulnerability.
```

2. Spring Overly Permissieve Cross Origin Resource Sharing Policy, lines 135, 145, 150, 205, 220, 253, and 289 of MispTransClientController.java

```
The method event found at line 135 in flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/MispTransClientController.java sets an overly permissive CORS
access control origin header.
```

3. Improper Resource Access Authorizatin, line 44 of Config.java; 225 Taxii11Response.java; lines 49, 65 of MispTransHelper.java;

```
An I\O action occurs at flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/Config.java in 42 without authorization checks.
```

4. Incorrect Permission Assignment for Critical Resources, lines 135, 69, 55 of Config.java; line 136 of MispTransHelper.java

```
A file is created on the file system by df in flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/Config.java at line 133 with potentially dangerous permissions.
```

5. Log Forging, lines 135 (3 repeats), 136 of MispTransClientController.java

```
Method event at line 135 of flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/MispTransClientController.java gets user input from element
processType. This element’s value flows through the code without being properly sanitized or validated, and is eventually used in writing an audit log in processPollResponse at line 126 of flare-misp-service-master/misptrans-client-rest-service/src/main/java/gov/dhs/cisa/flare/misp/Taxii11Response.java.
```

6. Information Exposure Through an Error Message, line 213 of Taxii11Response.java; line 68 of SSLHostnameVerification.java; 

```
Method stixPackages.stream, at line 203 of flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/Taxii11Response.java, handles an Exception or runtime Error e.
During the exception handling code, the application exposes the exception details to printStackTrace, in
method stixPackages.stream of flare-misp-service-master/misp-trans-client-restservice/
src/main/java/gov/dhs/cisa/flare/misp/Taxii11Response.java, line 203.
```

7. Use of Hardcoded Password in Config, line 4, 6 of application.properties

```
The configuration file flare-misp-service-master/misp-trans-client-restservice/src/main/resources/application.properties contains a hardcoded password in line 1
```

8. Not Using a Random IV with CBC Mode, line 47 of EncryptionUtil.java; line 2 of Application.java

```
The encryption method encrypt does not use a random IV at line 44, hence reducing the entropy of the
encrypted data. Allowing an attacker to decrypt the data eventually.
```

9. Spring Missing Content Security Policy, line 2 Application.java

```
A Content Security Policy is not explicitly defined within the web-application.
```

10. Potential XXE Injection, line 44 of Config.java; 

```
The processPollResponse loads and parses XML using parse, at line 38 of flare-misp-service-master/misptrans-client-rest-service/src/main/java/gov/dhs/cisa/flare/misp/Taxii11Response.java.
```

11. Undocumented API, lines 135, 145, 150, 220, 253, 289 MispTransClientController.java

```
The application's event method (line 135) is an undocumented API endpoint. This is not a best practice and
may lead to unintended behavior.
```

12. Portability Flaw in File Separator, line 27 of MispTransHelper.java
13. Spring Missing Expect CT Header, line 2 of Application.java:

```
The web-application does not define an Expect-CT header, leaving it more vulnerable to attacks.
```

Cyber Threat Management @ Raytheo Technologies, Monday 1 March 2021.


## Mitigation Plan:
We plan to resolve these issues in our next development increment by June 30 2021.
