## Ingest STIX

Python script to ingest STIX files on MISP.

### Requirements

There are a few requirements for this little python script to run, which are included in the MISP requirements:
- PyMISP
- Python 3.6+ (because PyMISP is Python 3.6+)
- Your API key

The recommended python setup for MISP is described within [the following documentation](https://www.circl.lu/doc/misp/updating-python/).

### Description

The aim of this small piece of code is to ingest STIX files.

In order to ingest STIX data into MISP, there are 2 end points to query, `/events/upload_stix` for STIX 1, and `/events/upload_stix/2` for STIX 2.  
The content of the STIX file to ingest has then to be passed in the body of the query.

The equivalent is available in PyMISP with the `upload_stix` method. The only difference is instead of passing the STIX content, the filename(s) of the file(s) to import are passed.

MISP creates then an event for each file ingested, using the [stix import](https://github.com/MISP/MISP/blob/2.4/app/files/scripts/stix2misp.py) or [stix2 import](https://github.com/MISP/MISP/blob/2.4/app/files/scripts/stix2/stix2misp.py) scripts.

### Installation
Installation is streamlined using Python's setuptools. The following

#. Install prerequisites required by setuptools and libtaxii::

    $ sudo apt-get install python-pip python-dev libxml2-dev libxslt1-dev libz-dev

#. Install the stix_trans_client tool::

    $ sudo pip install stix_trans_client

That's it. You should now be able to run utilities, such as
``stix_trans_client.py``::

    $ stix_trans_client.py -h

### Usage

Depending of the python environment set in your MISP server, you will have to use the correct python command in order to be sure to reach the correct environment containing all the required libraries and dependencies:
- The recommended environment installed by default in most of our installation scripts, and virtual machine is a virtualenv available using `/var/www/MISP/venv/bin/python`
- If any other python environment is set instead, use the corresponding command. As an example, the built-in python3 provided with most of the linux distribution is available with a simple `python3`
**Please replace the python command in the next examples with your own setting if needed**

In order to connect to MISP, we need an URL and an API key:

Args:
- `misp_url`: the URL of your MISP server
- `misp_key`: your MISP API key
- `poll-url`: taxii server url to poll for stix files
- `taxii-key`: taxii server key
- `taxii-cert`: taxii server cert
- `collection`: taxii collection to poll
- `begin-timestamp`: beginning timestamp to poll taxii server
- `end-timestamp`: end timestamp to poll taxii server
- `output`: misp or xml_output - based upon server config 

Config:
- `misp_verifycert`: (`true` or `false`) to check or not the validity of the certificate
- `logger_level`: the level of the logger... ERROR, WARNING, INFO, DEBUG
- `state-file`: path to the file that maintains the poll times - required along with timestamp options
- `subscription-id`: a subscription id for the poll request