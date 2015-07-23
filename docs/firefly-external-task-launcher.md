## Firefly External Task Launcher

### Overview
Firefly widgets expect an image, a table, or JSON data. We have extended Firefly to allow getting image, table, or JSON (any data in JSON format) from an external process. Whenever Firefly receives an external task request, it checks its properties to find the executable, which is then launched with the JSON parameters passed from the client.

### Connecting Python task launcher to Firefly
To define an external launcher (or Python Launcher) in Firefly, define the executable in `app.prop`, using the following convention:

      launchername.exe = @launchername.exe@

For example, to define python launcher add the following to `fftools/config/app.prop`

      python.exe=@python.exe@

In `~/.gradle/build.config`, define the actual path to the launcher. For example:

      python.exe= "/path/to/python /path/to/launcher.py"
 
### Interface between Python Launcher and Firefly

Below are the details of the interface between Firefly and Python Launcher. (We call it external task launcher, because it does not have to be python.)

1. Task parameters are passed via json file. The json input is coming directly from user: it's JSON.stringify from `taskParams` object below. 

```js
 function onFireflyLoaded() {
    var tableData= { "processor" : "TableFromExternalTask",
                     "launcher" : "python",
                     "task" : "TestTask",
                     "taskParams" : {
                          "param1" : "str-1",
                          "param2" : 23456
                     }
                   };
    firefly.showTable(tableData, "tableHere"); 
```

In the above call the input json file will contain

      {"param1" : "str-1","param2" : 23456}

2. All other parameters as passed via key/value pairs or options. This way there is no danger that the order or extra parameters will affect the existing behavior.

This is the list of currently used options:

|  -d DIR, --work=DIR   | work directory             |
|  -i FILE, --in=FILE   | json file with task params |
|  -n TASK, --name=TASK | task name (no spaces)      |
|  -o DIR, --outdir=DIR | directory for the final output file |


In future:

|  -s STR, --sep=STR    |  separator string, after which task status is written (default to "___TASK STATUS___") |   

3. A suggested output directory is provided rather than a suggested output file. It's up to the python launcher to create a unique file in this directory.

4. What is expected to be in standard error and standard output stream

* STDERR (Standard Error Stream) - whatever is coming from it is logged as warnings for now - we might come with a better idea later. 

* STDOUT (Standard Output Stream) - can contain debugging info, the final output must be a line with the keyword, followed by JSON, which contains error message if any.

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

5. External process exit status 0 means the execution was normal. Everything else means an error was encountered.



### Examples

A sample python launcher can be found in `firefly/src/fyrefly/python/SamplePythonLauncher.py`. A sample javascript, which builds up on the examples below can be found at `firefly/src/fftools/html/interactive-demo-finish`

#### Example 1: Loading an image  

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

#### Example 2: Loading a table
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


#### Example 3: Using JSON data
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