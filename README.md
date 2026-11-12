# Firefly

Firefly is an open-source web-based UI application, library, and framework for astronomical data archive access and visualization developed at the [NASA/IPAC Infrared Science Archive (IRSA)](https://irsa.ipac.caltech.edu).
It has been supported by NASA, principally through IRSA at [Caltech](https://caltech.edu), and by the National Science Foundation, through the [Vera C. Rubin Observatory](https://rubinobservatory.org/).

## Core Features
- Viewing FITS files
- Exploring multi-resolution HiPS maps
- Exploring tables interactively
- Creating charts, including spectra and spectral lines
- Brushing and linking across images, tables, and charts
- Overlaying catalogs or DS9 region files on images
- Searching VO services (TAP, SIA, ObsTAP)
- Using Firefly from Python ([details](docs/firefly-python-wrapper.md))

For details on using these features, see the [Firefly Online Help](https://irsacloud.ipac.caltech.edu/firefly/onlinehelp/).

## Quick Start: Installing Firefly

There are several ways to deploy Firefly. Pick the one that fits your needs, from personal use on a laptop to an operational deployment on Kubernetes.

### Option 1: Laptop or desktop (macOS or Linux, personal use)
[See laptop install details](docs/using-firefly-standalone.md)

```bash
curl -fsSL https://raw.githubusercontent.com/Caltech-IPAC/firefly/refs/heads/dev/bin/get-firefly | bash
```
Then follow the instructions.

### Option 2: Docker
[See Docker install details](docs/firefly-docker.md)

```bash
docker run -p 8090:8080 -m 4g --rm ipac/firefly:latest
```
Then open http://localhost:8090/firefly/

### Option 3: Helm / Kubernetes
[See Helm install details](helm/README.md)

Replace `firefly.example.com` with your own hostname.
```bash
helm upgrade --install firefly oci://ghcr.io/caltech-ipac/helm-charts/firefly \
  -n firefly \
  --create-namespace \
  --set ingress.host=firefly.example.com
```

## Supported Browsers

Firefly supports the following browsers:

- Safari >= 17
- Chrome >= 130
- Firefox >= 134
- Edge >= 130

## Firefly Framework

Firefly started as a set of archive-specific applications at [IRSA](https://irsa.ipac.caltech.edu) and was then generalized to serve data from many different archives at IRSA (and beyond). It was open sourced in 2015.

Firefly is designed to build complex web-based front-end applications making use of reusable [React](https://react.dev) components to enable accessing and exploring astronomical data with advanced data visualization capabilities.

The visualization provides users with an integrated experience with brushing and linking capabilities among images, catalogs, and plots. Firefly is used in [IRSA](https://irsa.ipac.caltech.edu) GUIs to query and visualize data from missions such as [Euclid](https://irsa.ipac.caltech.edu/Missions/euclid.html), [SPHEREx](https://irsa.ipac.caltech.edu/Missions/spherex.html), [WISE](https://irsa.ipac.caltech.edu/applications/wise/), [Spitzer](https://irsa.ipac.caltech.edu/applications/Spitzer/SHA/), [SOFIA](https://irsa.ipac.caltech.edu/applications/sofia/), [ZTF](https://irsa.ipac.caltech.edu/applications/ztf/), etc. and a large number of heavily used contributed data products from a diverse set of astrophysics projects. It is also used in various user interfaces of the NASA Exoplanet Science Institute ([NExScI](https://nexsci.caltech.edu)) and the NASA/IPAC Extragalactic Database ([NED](https://ned.ipac.caltech.edu)), as well as to construct the Portal Aspect of the Vera C. Rubin Observatory Science Platform.

Firefly is being reused for various archives because it is a framework of archive components. This has allowed us to implement multiple archive systems at a fraction of the cost of the first system, Spitzer Heritage Archive. Firefly has undergone an extensive re-write since then, adopted React/Redux framework for the client side, making it much easier to work with other JavaScript libraries. Firefly's general scientific visualizations are based on the Plotly.js graphing library.

### Firefly Viewer

The Firefly Viewer is the general-purpose web application this repository builds, and what each of the Quick Start options installs. Use it to view FITS images, HiPS maps, tables, and charts, and to search VO services such as TAP and SIA.

### Firefly APIs

The Firefly JavaScript API lets any web page embed Firefly's main components (image viewers for FITS and HiPS, tables, and charts) with a few lines of JavaScript. These components can share a data model, so a table, an image, and a chart respond to each other's selections and filters. See the [JavaScript API overview](docs/firefly-api-overview.md).

The web page does not need to be served from the same host as the Firefly server. Firefly supports cross-origin (CORS) requests, so one Firefly deployment can serve many web pages.

## Visualizer Feature Details

A closer look at the three visualizers at the core of Firefly: tables, images, and charts.

### Tabular Data Display
Firefly has implemented “Excel-like” tables on the webpage. In an easy, interactive way, a user can sort the results, filter the data on multiple constraints, hide or show columns, or select data for download or visualization. The Firefly server is optimized to show very large tables without significant performance degradation. It has been tested with tables of over 30 million rows and 50 columns.

### FITS and HiPS Visualization
Firefly provides first-class FITS visualization in the browser. All of the significant components you would expect to see in a basic desktop FITS application are available with data that Firefly displays. The FITS visualizer allows for overplotting any table data with Lon(RA) and Lat(Dec). Therefore, users can overlay multiple catalogs over their FITS image and interact with it.

In addition, Firefly is able to display all-sky images in the IVOA standard HiPS format, and display catalogs overlaid on them just as with FITS images.

### Scientific Plotting
Firefly shows 2D graphs interactively so that users can read the data point values as they move the mouse around or zoom in to investigate the data at a finer level. These graphs are used for plotting table columns. Users can specify any column that the tables are showing. Users can also define expressions from the columns for the XY plot, such as (col1\*col2/tan(col3)). Table-based charts share their data model with the table, so both table and chart can trigger or respond to the common events, such as filtering of the data. Firefly also supports creating and plotting 1D histograms and heatmaps (2D histograms).

## More Docs

- [Release notes](docs/release-notes.md)
- [Building Firefly from source](docs/firefly-developers.md)
- [Contributing to Firefly](CONTRIBUTING.md)
- [Firefly JavaScript API overview](docs/firefly-api-overview.md)
- [Firefly Remote API using Python](docs/firefly-python-wrapper.md)
- [Code examples using Firefly APIs](docs/firefly-api-code-examples.md)
- [Setting up the server for FITS files](docs/server-settings-for-fits-files.md)
- [Changing the Firefly runtime environment](docs/firefly-environment.md)
- [Adding an external task launcher or Python launcher](docs/firefly-external-task-launcher.md)
- [Release branches and tags](docs/tags-and-branches.md)

## Support

To report a bug, request a feature, or ask a question, open an issue on [GitHub Issues](https://github.com/Caltech-IPAC/firefly/issues).

## License

Firefly is released under a BSD 3-Clause license. See [License.txt](License.txt).
