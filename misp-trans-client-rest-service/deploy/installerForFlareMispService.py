#######################################
### installerForFlareMispService.py
### -------------------------------
###
### FlareMispService needs, redhat 6x, java 8x, curl, Python 2.7.14, and several python modules 
### (see the SAG for all requirements/dependencies and detailed install instructions).  
###
### Wizard-like Installation Script for FlareMispService (aka: FlareTransMispService)
### This Python script will prompt the Admin for Environment-Specific values.
### These values need to be updated in the Config Properties file
###
#######################################


#WEB SERVICE SPECIFIC PROMPTS
_ws_stix_output_directory  = raw_input('Enter the path of the Output Directory where the Web Service can dump STIX files (if needed): ')


#FLARE TAXI PROMPTS
_flare_taxi_ip              = raw_input('Enter the IP for FLARE TAXII: ')
_flare_taxi_port            = raw_input('Enter the PORT for FLARE TAXII: ')


####NOTE: we must modify the configuration properties dynamically because IP and PORT are used more than once
####  stixtransclient.poll.baseurl  is where it's first defined
####  stixtransclient.poll.url      is where it's used again. 
####  Why make Admin type it in twice ?
####  AND how would the Admin know about adding the URL Fragment  '/flare/taxii11/poll' ??

_flare_poll_frequency       = raw_input('Enter the wait time (in minutes) between polling the FLARE TAXII : ')
_flare_certificate_filepath = raw_input('Enter the path and filename for the FLARE TAXII Certificate: ')
_flare_key_filepath         = raw_input('Enter the path and filename for the FLARE TAXII User Certificate: ')
_flare_stix_collection      = raw_input('Enter the STIX collection name on FLARE TAXII to poll: ')


#MISP PROMPTS
_misp_server_ip      = raw_input('Enter the IP for MISP Server: ')
_misp_automation_key =  raw_input('Enter the Automation-Key for MISP Server: ')


#############
##### TODO Still need to make use of the the inputs obtained from the SysAdmin 
##### Need to take the inputs, locate the appropriate properties, and substitute values where appropriate.
##### Note: Not going to be a blind replacement.  IP and Port for example can be reused in multiple lines.
#############       

