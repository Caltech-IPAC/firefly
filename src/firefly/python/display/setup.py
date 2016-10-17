# Licensed under a 3-clause BSD style license - see License.txt

from setuptools import setup

setup(
    name='firefly_client',
    version='1.0.0.b2',
    description='Python API for Firefly',
    author='IPAC LSST SUIT',
    license='BSD',
    url='http://github.com/Caltech-IPAC/firefly',
    packages = ['firefly_client'],
    install_requires=['ws4py', 'future'],
    classifiers=[
        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.7',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.5',
    ]
)

