import dateutil
import json
import logging
import os
import sys
import tempfile
import uuid

from argparse import ArgumentParser
from logging.handlers import RotatingFileHandler
from pymisp import ExpandedPyMISP
from pymisp.exceptions import PyMISPError
from misp_stix_ingest.certau.source import TaxiiContentBlockSource
from misp_stix_ingest.certau.util.taxii.client import SimpleTaxiiClient
from misp_stix_ingest.certau.util.stix.ais import ais_refactor
from misp_stix_ingest.certau.util.stix.helpers import package_tlp


_LOGGER = logging.getLogger(__name__)
rotating_file_handler = RotatingFileHandler(os.path.join(os.getcwd(), 'logs', 'stix_trans_client.log'),
                                            backupCount=3)
rotating_file_handler.setLevel(logging.DEBUG)
log_formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
rotating_file_handler.setFormatter(log_formatter)
_LOGGER.addHandler(rotating_file_handler)


def get_stix_version(stix):
    # We only handle version 1 stix at this time.
    return '1'


def ingest_to_misp(path, version, url, key, verifycert):
    try:
        misp = ExpandedPyMISP(url, key, verifycert)
    except PyMISPError:
        _LOGGER.error(f'Unable to connect to MISP ({url}). Please make sure the API key and the URL are correct.')
        raise PyMISPError(f'Unable to connect to MISP ({url}). Please make sure the API key and the URL are correct.')

    errors = []
    response = misp.upload_stix(path, version=version)
    if response.status_code != 200:
        errors.append(path)

    if errors:
        file = "file: " if len(errors) == 1 else "files:\n- "
        print_errors = '\n- '.join(errors)
        _LOGGER.error(f'Error with the ingestion of the following {file}{print_errors}')

    _LOGGER.info(f'Successfully ingested {path}.')
    return


def main():
    # Set an initial logger level until the config file is read.
    _LOGGER.setLevel(logging.ERROR)
    # Args are variables sent by the Java App /-\ Options are values in the python config.json folder and are static per run
    parser = ArgumentParser(description='')
    parser.add_argument('--poll-url', required=True, help='URL of the TAXII instance you want to connect to in order to get STIX files.')
    parser.add_argument('--taxii-key', required=True, help='KEY file for the TAXII connection.')
    parser.add_argument('--taxii-cert', required=True, help='CRT file for the TAXII connection.')
    parser.add_argument('--misp-url', required=True, help='URL of the MISP instance you want to connect to.')
    parser.add_argument('--misp-key', required=True, help='API key of the user you want to use.')
    parser.add_argument('--collection', required=True, help='TAXII Collection to poll')
    parser.add_argument('--begin-timestamp', required=True, help='Beginning Poll Timestamp from TAXII')
    parser.add_argument('--end-timestamp', required=True, help='End Poll Timestamp from TAXII')
    parser.add_argument('--output', default='misp', choices=['misp', 'xml_output'])
    args = parser.parse_args()
    with open('config.json', 'rt', encoding='utf-8') as f:
        options = json.loads(f.read())
    _LOGGER.setLevel(options.get('logger_level', logging.ERROR))
    for k, v in options.items():
        _LOGGER.debug(f'Args - Parameter {k}: {v}')
    for k, v in args.__dict__.items():
        _LOGGER.debug(f'Options - Parameter {k}: {v}')
    try:
        # Poll TAXII for files
        _LOGGER.info("Processing a TAXII message")
        taxii_client = SimpleTaxiiClient(
            username=args.username,
            password=args.password,
            key_file=args.key,
            cert_file=args.cert,
            ca_file=args.ca_file,
        )

        # Parse begin and end timestamps if provided
        if args.begin_timestamp:
            begin_timestamp = dateutil.parser.parse(args.begin_timestamp)
        else:
            begin_timestamp = None

        if args.end_timestamp:
            end_timestamp = dateutil.parser.parse(args.end_timestamp)
        else:
            end_timestamp = None

        # Sanity checks for timestamps
        if (begin_timestamp is not None and end_timestamp is not None and
                begin_timestamp > end_timestamp):
            raise ValueError('poll end_timestamp is earlier than begin_timestamp')

        _LOGGER.info('Polling %s for collection %s between %s and %s', args.poll_url, args.collection, begin_timestamp, end_timestamp)

        content_blocks = taxii_client.poll(
            poll_url=args.poll_url,
            collection=args.collection,
            subscription_id=options.subscription_id,
            begin_timestamp=begin_timestamp,
            end_timestamp=end_timestamp,
            state_file=options.state_file,
        )

        source = TaxiiContentBlockSource(
            content_blocks=content_blocks,
            collection=args.collection,
        )

        _LOGGER.info("Processing TAXII content blocks")

        for source_item in source.source_items():
            package = source_item.stix_package
            if package is not None:
                _LOGGER.info("Processing TAXII content block - package - %s", package)
                if options.ais_marking:
                    tlp = package_tlp(package) or options.ais_default_tlp
                    ais_refactor(
                        package=package,
                        proprietary=options.ais_proprietary,
                        consent=options.ais_consent,
                        color=tlp,
                        country=options.ais_country,
                        industry=options.ais_industry_type,
                        admin_area=options.ais_administrative_area,
                        organisation=options.ais_organisation,
                    )
                if options.xml_output:
                    # This will raise an error as it's not implemented
                    source_item.save(options.xml_output)
                else:
                    # The python interface to MISP looks for a local file path, so we make one.
                    with tempfile.TemporaryDirectory() as d:
                        path = os.path.join(d, str(uuid.uuid4()), '.stix')
                        with open(path, 'w') as f:
                            f.write(source_item.stix_package.to_xml())
                        ingest_to_misp(path, get_stix_version(source_item), args.misp_url, args.misp_key, options.misp_verifycert)
    except Exception as e:
        # Return a failure code
        _LOGGER.error(str(e), exc_info=e)
        sys.exit(2)


if __name__ == '__main__':
    main()

