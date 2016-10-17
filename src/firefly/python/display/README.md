# firefly_client

Python API for Firefly, IPAC's Advanced Astronomy Web UI Framework

## Usage

The client must be connected to a Firefly server.

```
from firefly_client import FireflyClient
fc = FireflyClient('localhost:8080', 'mychannel')
```

A FITS image may be uploaded and displayed.

```
fval = fc.upload_file('image.fits')
fc.show_fits(fval, 'myimage')


