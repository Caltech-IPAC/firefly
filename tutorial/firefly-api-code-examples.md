## Code Examples Using Firefly Tools

- [Image FITS Viewer](#image-fits-viewer)
- [FITS Viewers in a group](#fits-viewers-in-a-group)
- [Tracking the mouse](#tracking-the-mouse)
- [Tracking the selection](#tracking-the-selection)

### Starting Firefly Tools in JavaScript
Firefly Tools are loaded when you load`firefly_loader.js` from a server of your choice. For example, to load Firefly Tools from your local server, include the following declaration in your HTML file:

```html
 <script type="text/javascript" language="javascript" src="http://localhost:8080/fftools/firefly_loader.js">   
```

Your html document must define some divs, where you will load the viewer widgets, and `onFireflyLoaded()` function.

When Firefly Tools completes loading, it calls `onFireflyLoaded()` JavaScript function. This is where you create Firefly components and place them into HTML `<div>` elements with specified IDs. 

The following are several examples of what can be done in the `onFireflyLoaded()` function. All test data can be found at `http://web.ipac.caltech.edu/staff/roby/demo`. 

### Image FITS Viewer
Create an image viewer and place it into the `<div>` id `plotHere`.

```html
<div id="plotHere" style="width: 350px; height: 350px;"></div>
```

```js
function onFireflyLoaded() {
    firefly.showImage('plotHere', {
        URL      : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m31-level1-3.fits',
        Title    : 'Example FITS Image',
        ColorTable : 16,
        RangeValues : firefly.util.image.RangeValues.serializeSimple('Sigma',-2,8,'Linear')
    });
```


### FITS Viewers in a group
In this example, we create four image viewers. Each belong to the same group `wise-group`. We then set some global parameters so all the plots display the same. Now we plot each of the four images by specifying the URL of the FITS file. By doing this, all the plotting controls will work on all four images simultaneously. 


```html
  <div style="white-space: nowrap;">
    <div id="w1" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
    <div id="w2" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
    <div id="w3" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
    <div id="w4" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
  </div>
```

```js
 function onFireflyLoaded() {
   firefly.setGlobalImageDef({ ZoomType    : 'TO_WIDTH',
                                    ColorTable  : 8,
                                    ZoomToWidth : 250 });   
   w1.showImage('w1', { URL  : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band1.fits',
             Title: 'WISE band 1', plotGroupId : 'wise-group' });
   w2.showImage('w2', { URL  : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits',
             Title: 'WISE band 2', plotGroupId : 'wise-group' });
   w3.showImage('w3', { URL  : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band3.fits',
             Title: 'WISE band 3', plotGroupId : 'wise-group' });
   w4.showImage('w4', { URL  : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band4.fits',
             Title: 'WISE band 4', plotGroupId : 'wise-group' });
 }
```

### Tracking the mouse 

The following example will plot FITS image then add a callback to get the mouse readout and log it to the console.

```html
<div id="plotHere" style="width: 500px; height: 500px;"></div>
```

```js
// callback on mouse readout
function onReadoutData(action) {
    if (action.payload.hasValues) {
        // readoutItems contain j2000, image, screen points and other info
        console.log(action.payload.readoutItems.imagePt);
    }
}
function onFireflyLoaded() {
    const imageRequest = {
        plotId: 'plotHere',
        URL: 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits',
        Title: 'WISE m51'
    };
    firefly.showImage('image_div', imageRequest);
    // add a callback, which will be called for every READOUT_DATA action
    // returned is the function to remove listener
    const trackReadoutRemover = firefly.util.addActionListener(firefly.action.type.READOUT_DATA, onReadoutData);
}
```

### Tracking the selection 

Firefly viewer allows to make the following selections:
  - Area Select (square)
  - Line Select
  - Point Select
  - Circle Select (*coming soon*)

The following example will plot a FITS image then add a callback to get selection information and log it to the console.

```html
<div id="plotHere" style="width: 500px; height: 500px;"></div>
```

```js
const onPlotAttributeChange = function(action) {
    console.log(action.payload.plotId);
    // attribute key: 'ACTIVE_POINT','ACTIVE_DISTANCE', or 'SELECTION'
    console.log(action.payload.attKey);
    // value that defines the selection
    console.log(action.payload.attValue);
}
function onFireflyLoaded() {
    const imageRequest = {
        plotId: 'plotHere',
        URL: 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits',
        Title: 'WISE m51'
    };
    firefly.showImage('image_div', imageRequest);
    // turn on point selection
    firefly.action.dispatchChangePointSelection('requester', true);
    // add a callback, which will be called for every CHANGE_PLOT_ATTRIBUTE action
    // returned is the function to remove listener
    const trackSelectionRemover = firefly.util.addActionListener(firefly.action.type.CHANGE_PLOT_ATTRIBUTE, onPlotAttributeChange);
}
```
