# Firefly

Firefly is an open-source web-based UI application, library, and framework for astronomical data archive access and visualization developed at the [NASA/IPAC Infrared Science Archive (IRSA)](https://irsa.ipac.caltech.edu).
It has been supported by NASA, principally through IRSA at [Caltech](https://caltech.edu), and by the National Science Foundation, through the [Vera C. Rubin Observatory](https://www.lsst.org/).

### Core Features
- Viewing FITS files
- Exploring multi-resolution HiPS maps
- Exploring tables interactively
- Creating charts, including spectra and spectral lines
- Brushing and linking across images, tables, and charts
- Overlaying catalogs or DS9 region files on images
- Searching VO services (TAP, SIA, ObsTAP)
- Using Firefly from Python ([details](docs/firefly-python-wrapper.md))

For details on using these features, see the [Firefly Online Help](https://irsacloud.ipac.caltech.edu/firefly/onlinehelp/).

### Quick Start: Installing Firefly

There are several ways to deploy Firefly. Pick the one that fits your needs, from personal use on a laptop to an operational deployment on Kubernetes.

#### Option 1: Laptop or desktop (macOS or Linux, personal use)
[See laptop install details](docs/using-firefly-standalone.md)

```bash
curl -fsSL https://raw.githubusercontent.com/Caltech-IPAC/firefly/refs/heads/dev/bin/get-firefly | bash
```
Then follow the instructions.

#### Option 2: Docker
[See Docker install details](docs/firefly-docker.md)

```bash
docker run -p 8090:8080 -m 4g --rm ipac/firefly:latest
```
Then open http://localhost:8090/firefly/

#### Option 3: Helm / Kubernetes
[See Helm install details](helm/README.md)

Replace `firefly.example.com` with your own hostname.
```bash
helm upgrade --install firefly oci://ghcr.io/caltech-ipac/helm-charts/firefly \
  -n firefly \
  --create-namespace \
  --set ingress.host=firefly.example.com
```

### Supported Browsers

Firefly supports the following browsers:

- Safari >= 17
- Chrome >= 130
- Firefox >= 134
- Edge >= 130

## Release note

Firefly builds are available on Docker and additional notes for using it are on the [Docker Page](https://hub.docker.com/r/ipac/firefly).

[Release Notes are Here](docs/release-notes.md).


## Firefly Framework

Firefly started as a set of archive-specific applications at [IRSA](https://irsa.ipac.caltech.edu) and was then generalized to serve data from many different archives at IRSA (and beyond). It was open sourced in 2015.

Firefly is designed to build complex web-based front-end applications making use of reusable [React](https://reactjs.org) components to enable accessing and exploring astronomical data with advanced data visualization capabilities.

The visualization provides users with an integrated experience with brushing and linking capabilities among images, catalogs, and plots. Firefly is used in [IRSA](https://irsa.ipac.caltech.edu) GUIs to query and visualize data from missions such as [WISE](https://irsa.ipac.caltech.edu/applications/wise/), [Spitzer](https://irsa.ipac.caltech.edu/applications/Spitzer/SHA/), [SOFIA](https://irsa.ipac.caltech.edu/applications/sofia/), [ZTF](https://irsa.ipac.caltech.edu/applications/ztf/), [PTF](https://irsa.ipac.caltech.edu/applications/ptf/), etc. and a large number of heavily used contributed data products from a diverse set of astrophysics projects. It is also used in various user interfaces of the NASA Exoplanet Science Institute ([NExScI](https://nexsci.caltech.edu)) and the NASA/IPAC Extragalactic Database ([NED](https://ned.ipac.caltech.edu)), as well as to construct the Portal Aspect of the Vera C. Rubin Observatory Science Platform.

Firefly is being reused for various archives because it is a framework of archive components. This has allowed us to implement multiple archive systems at a fraction of the cost of the first system, Spitzer Heritage Archive. Firefly has undergone an extensive re-write since then, adopted React/Redux framework for the client side, making it much easier to work with other JavaScript libraries. Firefly's general scientific visualizations are based on the Plotly.js graphing library.

### Firefly Viewer

By default, this repository builds a default example webapp. This tool can be used for viewing FITS data, catalogs, and xyplots. It is a general viewer tool for retrieving and viewing astronomy related data.

### Firefly APIs

Firefly APIs exposes the most powerful components of Firefly in a way that can be used by any web page with no prerequisites. It allows any web developer access to Firefly's FITS visualizers or Table Tool with just a very few lines of JavaScript. The goal is to make these APIs very easy to use with only a 10 minute learning curve.

An important feature is that the Firefly APIs server can be installed cross-site. In other words, it is not required to be on the same server as the web page. Firefly APIs can do this because it uses JSONP or CORS for the server communication. This allows Firefly APIs not to be limited by the server's Same Origin Policy and to give the developer a lot of flexibility. The web developer does not have to do any installation, but can simply just start using Firefly APIs.

## Visualizers
The firefly components contain three main visualizers for astronomy data.


### Tabular Data Display
Firefly has implemented “Excel-like” tables on the webpage. In an easy, interactive way, a user can sort the results, filter the data on multiple constraints, hide or show columns, or select data for download or visualization. The Firefly APIs server is optimized to show very large tables without significant performance degradation. We have tested it with over 10 million rows, 50 column tables.

### FITS and HiPS Visualization
Firefly provides a first-class FITS visualization on the Web without any plugins. All of the significant components you would expect to see in a basic desktop FITS application are available with data that Firefly displays. The FITS visualizer allows for overplotting any table data with Lon(RA) and Lat(Dec). Therefore, users can overlay multiple catalogs over their FITS image and interact with it.

In addition, Firefly is able to display all-sky images in the IVOA standard HiPS format, and display catalogs overlaid on them just as with FITS images.

### Scientific Plotting
Firefly shows 2D graphs interactively so that a user can read the data point values as moves mouse around or zooms in to investigate the data at a finer level. These graphs are used for plotting table columns. The user can specify any column that the tables are showing. The user can also define expressions from the columns for the XY Plot, such as (col1\*col2/tan(col3)). Table based charts share their data model with the table, so both table and chart can trigger or respond to the common events, such as filtering of the data. Firefly also supports creating and plotting 1D histograms and heatmaps (2D histograms).

## More Docs

### Building Firefly from source
See [firefly-developers.md](docs/firefly-developers.md)

### Firefly JavaScript API overview
See [firefly-api-overview.md](docs/firefly-api-overview.md)
 (Deprecated [docs/fftools-api-overview.md])

### Firefly Remote API using Python overview
See [firefly-python-wrapper.md](docs/firefly-python-wrapper.md)

### Code Examples Using Firefly APIs
See [firefly-api-code-examples.md](tutorial/firefly-api-code-examples.md)
 (Deprecated [docs/fftools-api-code-examples.md])

### Setting up the Server correctly for FITS files
See [server-settings-for-fits-files.md](docs/server-settings-for-fits-files.md)

### Changing the Firefly runtime environment
See [firefly-environment.md](docs/firefly-environment.md)

### Adding external task launcher or Python Launcher to Firefly
See [firefly-python-launcher.md](docs/firefly-external-task-launcher.md)

### Release branches and tags
See [tags-and-branches.md](docs/tags-and-branches.md)
