## Firefly External Task Launcher

### Overview
Firefly tools deal with an image, a table, or JSON data, which can come from a file, a URL, or a server-side *search processor*, defined by Firefly. 

We have extended Firefly to allow getting image, table, or JSON (any data in JSON format) from an external process. Whenever server-side Firefly receives an external task request, it checks its properties to find the executable, which is then launched with the JSON task parameters passed from the client.

Before continuing, please familiarize yourself with [Firefly Tools JavaScript API](fftools-api-overview.md).


### Server side: Connecting Python task launcher to Firefly
To define an external task launcher (or Python task launcher) in Firefly, define the executable in `app.prop`, using the following convention:

      launchername.exe = @launchername.exe@

For example, Python task launcher is defined in `fftools/config/app.prop` by the following line:

      python.exe=@python.exe@

In `~/.gradle/build.config`, define the actual path to the launcher:

      python.exe= "/path/to/python /path/to/launcher.py"
 
 
### Server side: Python task launcher interface with Firefly

A sample Python task launcher can be found in 
`firefly/src/fyrefly/python/SamplePythonLauncher.py` 

Below are the details of communication protocol between Firefly and Python task launcher. We call it *external task launcher*, because it does not have to be Python. 


1. On server side, task parameters are passed from Firefly to the external task launcher via json file. 

    The json input is coming directly from user: it's the content of`taskParams` object in the Javascript API examples below.
    
    For example, let's assume Python task launcher has `aTableTask` task, which produces a table and expects two parameters: `param1` and `param2`.
    
    Javascript API call will look like this:

    ```js
     function onFireflyLoaded() {
        var tableData= { "processor" : "TableFromExternalTask",
                         "launcher" : "python",
                         "task" : "aTableTask",
                         "taskParams" : {
                              "param1" : "str-1",
                              "param2" : 23456
                         }
                       };
        firefly.showTable(tableData, "tableHere"); 
    ```

    On the server side, Firefly will create a json file containing `{"param1" : "str-1","param2" : 23456}` and pass it to Python task launcher.
      
    

2. All other parameters as passed via key/value pairs or options. This way there is no danger that the order or extra parameters will affect the existing behavior.

    This is the list of the options Firefly expects the external task launcher to handle:
    
    ```
    -d DIR, --work=DIR   work directory          
    -i FILE, --in=FILE   json file with task params 
    -n TASK, --name=TASK task name, no spaces
    -o DIR, --outdir=DIR directory for the final output file
    -s STR, --sep=STR    separator string, after which task status is written, defaults to ___TASK STATUS___ 
    ```

    Note that Firefly provides the suggested output directory rather than the suggested output file. It's up to the python launcher to create a unique file in this directory.

3. What is expected to be in the external task launcher standard error and standard output streams

    STDERR (Standard Error Stream) - whatever is coming from it is logged as warnings for now - we might come with a better idea later. 

    STDOUT (Standard Output Stream) - can contain debugging info, at the end there must be a line with the keyword, followed by JSON, which contains error message if any.

    ```
    ___TASK STATUS___
    {
        outfile: "/path/file.fits"
    }
    ```

      OR

    ```
    ___TASK STATUS___
    {
        error: "Description of the error"
    }        
    ```

4. External process exit status 0 means the execution was normal. Everything else means an error was encountered.



### Client Side: Javascript API Examples

A sample javascript, which builds up on the examples below is in
`firefly/src/fftools/html/interactive-demo-finish.html`

#### Example 1: Loading an image produced by Python task launcher 

Create an image viewer and place it into the `<div>` id `plotHere`.

```html
<div id="plotHere" style="width: 350px; height: 350px;"></div>
```

```js
 function onFireflyLoaded() {
    var iv2= firefly.makeImageViewer("plotHere");
    iv2.plot({
        "id" :"FileFromExternalTask",
        "launcher" :"python",
        "task" :"someImageTask",
        "taskParams" : {"p1":1,"p2":2},
        "Title" :"Example FITS Image'",
        "ColorTable" :"16",
        "RangeValues":firefly.serializeRangeValues("Sigma",-2,8,"Linear")
    });
 }
```

#### Example 2: Loading a table produced by Python task launcher

The table is plotted in the `<div>` id `tableHere`.

```html
  <div style="white-space: nowrap;">
    <div id="tableHere" style="display:inline-block; width: 600px; height: 250px; margin : 5px 8px 0px 10px; border: solid 1px;"></div>
  </div>
```

```js
 function onFireflyLoaded() {
     var tableData= { "processor" : "TableFromExternalTask",
         "launcher" : "python",
         "task" : "TestTask3",
         "taskParams" : { "param1" : "first arg", "param2" : "second arg" }
     }; 
     firefly.showTable(tableData, "tableHere");
 }
```


#### Example 3: Getting and using JSON data produced by Python task launcher
In this example, we get the histogram data from an exernal task and feed them to firefly `showHistogram` method. The histogram is placed into `div` with id `chartHere`.


```html
    <div style="white-space: nowrap;">
      <div id="chartHere"  style="display:inline-block; width: 800px; height: 350px; border: solid 1px;"></div>
    </div>
```

```js
 function onFireflyLoaded() {
      var launcher = 'python';
      var task = 'JsonTaskToGetHistogramData';
      var taskParams = { 'numbins': bins };
      firefly.getJsonFromTask(launcher, task, taskParams)
           .then(
                function (histdata) {
                     firefly.showHistogram(
                          {'descr' : 'Histogram data returned from python JSON task',
                           'binColor' : '#3d3033',
                           'height' : 350,
                           'data': histdata}, 'chartHere');
                }
           ).catch(function (reason) {
                     console.log('Error fetching JSON data from '+launcher+' task '+task+': '+reason);
                   }
           );
 }
```