#!/usr/bin/env python

from setuptools import setup

package_name = 'StixToMispTransform'
package_version = '1.0'
version_string = '{} v{}'.format(package_name, package_version)

setup(
    name=package_name,
    version=package_version,
    description='CERT Australia cyber threat intelligence (CTI) toolkit\'s Taxii pull software with MISP provided transform ',
    url='https://github.com/certau/cti-toolkit/',
    author='CTMART',
    author_email='',
    license='',
    classifiers=[
        'Development Status :: 5 - Production/Stable',
        'Environment :: Console',
        'Intended Audience :: Information Technology',
        'Intended Audience :: System Administrators',
        'License :: ',
        'Natural Language :: English',
        'Programming Language :: Python',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.6',
    ],
    keywords='STIX TAXII',
    packages={
        'misp_stix_ingest',
        'misp_stix_ingest/certau',
        'misp_stix_ingest/certau/util',
        'misp_stix_ingest/certau/util/stix',
        'misp_stix_ingest/certau/util/taxii',
        'misp_stix_ingest/certau/source',
    },
    entry_points={
        'console_scripts': [
            'stix_trans_client.py=misp_stix_ingest.stix_trans_client:main',
        ],
    },
    install_requires=[
        'configargparse',
        'lxml',
        'libtaxii>=1.1.111',  # needed for user-agent support
        'cybox==2.1.0.14',
        'stix==1.2.0.4',
        'stix-ramrod',
        'mixbox',
        'pymisp>=2.4.82',
        'requests',
        'six',
    ]
)