##############################################################################################
#
# This is a sample Python launcher that will work with Firefly.
# It accepts task name and a json file with task parameters and produces an image, a table, or json.
#
# The purpose of this code is to illustrate the interface between Firefly and Python launcher.
#
# For usage: python SamplePythonLauncher.py -h
# Example: python SamplePythonLauncher.py -n 'JsonTask' -d /tmp -o /tmp -i taskparams.json
#
# Below are the details of the launcher's interface with Firefly.
#
# 1. Task parameters are passed via json file. The json input is coming directly from user: it's JSON.stringify from "taskParams" object below.
#
# function onFireflyLoaded() {
#    var tableData= { "processor" : "TableFromExternalTask",
#                     "launcher" : "python",
#                     "task" : "TestTask",
#                     "taskParams" : {
#                          "param1" : "str-1",
#                          "param2" : 12345
#                     }
#                   };
#    firefly.showTable(tableData, "tableHere");
#
# In the above call the input json file will contain
#      {"param1" : "str-1","param2" : 12345}
#
# 2. All other parameters as passed via key/value pairs or options. This way there is no danger that the order or extra parameters will affect the existing behavior.
#
# This is the list of currently used options:
#  -d DIR, --work=DIR    work directory
#  -i FILE, --in=FILE    json file with task params
#  -n TASK, --name=TASK  task name (no spaces)
#  -o DIR, --outdir=DIR  directory for the final output file
#
# In future (requested by Gregory):
#   -s STR, --sep=STR     separator string, after which task status is written (default to "___TASK STATUS___")
#
# 3. A suggested output directory is provided rather than a suggested output file. It's up to the python launcher to create a unique file in this directory.
#
# 4. What is expected to be in standard error and standard output stream
#
# * STDERR (Standard Error Stream) - whatever is coming from it is logged as warnings for now - we might come with a better idea later.
#
# * STDOUT (Standard Output Stream) - can contain debugging info, the final output must be a line with the keyword, followed by JSON, which contains error message if any.
#
# ___TASK STATUS___
# {
#       outfile: "/path/file.fits"
# }
#
# OR
#
# ___TASK STATUS___
# {
#       error: "Description of the error"
# }
#
# 5. External process exit status 0 means the execution was normal. Everything else means an error was encountered.
#
##############################################################################################

import json
import os
from optparse import OptionParser
import sys
from shutil import copyfile
import tempfile
import numpy

errors = []
try:
    usage = "usage: %prog [options]"
    parser = OptionParser(usage=usage)

    parser.add_option("-d", "--work", dest="workdir",
                      help="work directory", metavar="DIR")
    parser.add_option("-i", "--in", dest="infile",
                      help="json file with task params", metavar="FILE")
    parser.add_option("-n", "--name", dest="task",
                      help="task name (no spaces)", metavar="TASK")
    parser.add_option("-o", "--outdir", dest="outdir",
                      help="directory for the final output file", metavar="DIR")
    parser.add_option("-s", "--sep", dest="separator", default='___TASK STATUS___',
                      help="separator string, after which task status is written", metavar="STR")

    (options, args) = parser.parse_args()

    taskParams = None
    if (options.infile):
        print 'Input file: '+options.infile
        with open(options.infile) as paramfile:    
            taskParams = json.load(paramfile)
            print json.dumps(taskParams)
    print 'Work directory: '+options.workdir
    print 'Requested output dir: '+options.outdir
    print 'Task: '+options.task

    isImageTask = (options.task.find("Image") > -1)
    isJsonTask = (options.task.find("Json") > -1)
    if isImageTask:
        print 'Output: image'
    elif isJsonTask:
        print 'Output: json'
    else:
        print 'Output: table'
    
    out = {}     
    if (os.path.isdir(options.outdir)):
        if isImageTask:
            # sample fits file return 
            (fd, outfile) = tempfile.mkstemp(suffix='.fits', prefix=options.task, dir=options.outdir)
            src = '/hydra/cm/firefly/src/fftools/test/data/c.fits'
            copyfile(src, outfile)
        elif isJsonTask:
            # sample json return
            # if numbins parameter is present - create sample histogram data
            # otherwise output task parameters
            histdata = None
            if taskParams is None:
                out['out'] = 'something'
            else:    
                for attr,v  in taskParams.items():
                    if (attr=="numbins"):
                        #genarate histogram data
                        numbins = v
                        data = numpy.random.random(5000)
                        bins = numpy.linspace(0, 1, numbins+1)
                        histdata=zip(numpy.histogram(data, bins)[0],bins,bins[1:numbins+1])
                        break  
                    else:
                        out[attr+'_out']=v
            (fd, outfile) = tempfile.mkstemp(suffix='.json', prefix=options.task, dir=options.outdir)
            f = os.fdopen(fd, "w")
            if histdata is None:
                json.dump(out, f)
            else:
                json.dump(histdata, f)
        else:
            # sample table - generate histogram data file
            numbins=50
            data = numpy.random.random(1000)
            bins = numpy.linspace(0, 1, numbins+1)
            tosave=zip(numpy.histogram(data, bins)[0],bins,bins[1:numbins+1])
            (fd, outfile) = tempfile.mkstemp(suffix='.csv', prefix=options.task, dir=options.outdir)
            numpy.savetxt(outfile, tosave, delimiter=',', comments='', header='numbins,binmin,binmax', fmt='%d,%.2f,%.2f')

        print 'Writing output to '+outfile

    else:
        errors.extend(("Output directory does not exist.", "Can not create output."))
except Exception as e:
    errors.extend((e.__doc__, str(e))) 
       
# required output
print options.separator
if errors:
    status = {
        'error' : ' '.join(errors)
    }
    print json.dumps(status)
    sys.exit(1)
else:
    status = {
        'outfile' : outfile
    }
    print json.dumps(status)
    sys.exit(0)
