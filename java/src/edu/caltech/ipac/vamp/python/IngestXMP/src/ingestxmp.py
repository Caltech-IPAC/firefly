#! /usr/bin/python
# -*- coding: utf-8 -*-
import sys
import os
import os.path
import csv
sys.path.append(os.path.pardir)

from libxmp import *

__author__="tlau"
__date__ ="$Feb 16, 2010 11:52:20 AM$"

def getDictionaryFromCSV(csvFile):
    d = dict()
    parser = csv.reader(csvFile)
    for data in parser:
        d[data[0]]=(data[1])
    return d

def parseDirectory(dirname, oWriter, keys, stat):
    print "Processing Directory: "+dirname+"..."
    for f in os.listdir(dirname):
        fullpath = os.path.join(dirname, f)
        if (os.path.isfile(fullpath)):
            print "Processing File: "+f+"..."
            xmpDict = parseFile(fullpath)
            if len(xmpDict)>0:
                try:
                    oWriter.writerow(generateOutput(f, dirname, keys, xmpDict))
                    print "Done."
                    stat['success'] = stat['success']+1
                except:
                    print "[ERROR] Fail to process "+f+":"
                    print sys.exc_info()
                    stat['failed'] = stat['failed']+1
            else:
                print "[ERROR] "+f+" does not contain any avm tags!"
                stat['skipped'] = stat['skipped']+1
        elif (os.path.isdir(fullpath)):            
            parseDirectory(fullpath, oWriter, keys, stat)

def parseFile(fullpath):
    from libxmp import utils
    d = dict()
    xmpfile = XMPFiles()
    xmpfile.open_file(fullpath)
    xmp = xmpfile.get_xmp()
    xmpDict = utils.object_to_dict(xmp)
    if (len (xmpDict) > 0):
        value = ""
        namespace = dict()
        for namespace in xmpDict:
            for data in xmpDict[namespace]:
                key = data[0].encode("utf-8", "replace")
                value = data[1].encode("utf-8", "replace")
                if ('\n' in value):
                    value = value.replace('\n', " ")
                if ("[" in key):
                    keyArray = key.rsplit("[")
                    key = keyArray[0]
                    idx = keyArray[1].rstrip(']')
                    if ('?' not in keyArray[1]):
                        try:
                            if (len(d[key])==0):
                                d[key]=value
                            else:
                                d[key] = d[key]+";"+value
                        except:
                            print "<"+key+"> or <"+value+"> failed."
                            print sys.exc_info()
                else:
                    d[key] = value            
    return d

def generateOutput(f, dirname, keys, xmpDict):
    a = [f, dirname]
    for key in keys:
        if (key=='File.Size'):
            a.append(int(os.path.getsize(os.path.join(dirname, f))/1000))
        elif (key=='File.Type'):
            a.append(getFileType(f))
        elif (key=='File.Dimension'):
            a.append(getDimension(xmpDict))
        elif (key=='File.BitDepth'):
            a.append(getBitDepth(xmpDict))
        elif (key=='thumbnail_path'):
            a.append("")
        elif (key in xmpDict):
            a.append(xmpDict[key])
        else:
            a.append("")

    return a

def getFileType(f):
    t = ''
    ex = os.path.splitext(f)[1]
    if (ex.lower()=='.jpg' or ex.lower()=='.jpeg'):
        t = 'jpeg'
    elif (ex.lower()=='.tif' or ex.lower()=='.tiff'):
        t = 'tiff'
    else:
        t = ex
    return t.upper().strip('.')

def getBitDepth(xmpDict):
    total = 0
    for key in xmpDict.keys():
        if (key.endswith(':BitsPerSample')):
            bitDepths = xmpDict[key]
            for bitDepth in bitDepths.split(';'):
                total = total + int(bitDepth)
            break
    return total

def getDimension(xmpDict):
    return xmpDict['exif:PixelXDimension']+';'+xmpDict['exif:PixelYDimension']

def main():
    from libxmp import utils
    stat = {"success":0, "failed":0, "skipped":0}

    if (len(sys.argv) > 1):
        if (os.path.isdir(sys.argv[1])):
            dirname = sys.argv[1]
        else:
            dirname = os.path.expanduser('~')+'/vamp_to_thumbnails'
    else:
        dirname = os.path.expanduser('~')+'/vamp_to_thumbnails'
    if (len(sys.argv) > 2):
        avmFile = sys.argv[2]
    else:
        avmFile = '../avm-Dictionary.csv'
    if (len(sys.argv) > 3):
        output = sys.argv[3]
    else:
        output = os.path.expanduser('~')+'/vamp_to_thumbnails/output.csv'


    avmDict = getDictionaryFromCSV(open(avmFile,'rU'))
    keys = sorted(avmDict.keys())
    columns = ['Filename','ParentURL']
    oWriter = csv.writer(open(output, 'w'), delimiter='|')

    for key in keys:
        columns.append(avmDict[key])

    oWriter.writerow(columns)
    parseDirectory(dirname, oWriter, keys, stat)
    print "Summary: success: "+str(stat['success'])+" failed: "+str(stat['failed'])+" skipped: "+str(stat['skipped'])
                
if __name__ == "__main__":
    if len(sys.argv) < 1:
            print 'Usage: extract_xmp.py [directory [avm-dictionary [output-csv]]]'
            sys.exit()
    main()

    print "Done."