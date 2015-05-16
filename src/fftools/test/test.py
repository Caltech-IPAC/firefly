__author__ = 'zhang'

import sys
import os
sys.path.append('/hydra/cm/firefly/src/fftools/python/display/')

from FireflyClient import *

def split(txt, seps):
    default_sep = seps[0]

    # we skip seps[0] because that's the default seperator
    for sep in seps[1:]:
        txt = txt.replace(sep, default_sep)
    return [i.strip() for i in txt.split(default_sep)]

def help():
    print(" '>conn' - make socket connection")
    print ("'>browser'- launch a new browser using a default channel")
    print (' ">browser url=url channel=myChannel" - launch a new browser in a new channel')
    print ("'> file|url'- display a fits image ")
    print ("'> file1|url file2|url'- display both files")
    print ("'> file1|url file2|url file3|url'- display three files ")
    print ("'> fitsfile|url plotID additionalParameters'- display your fits image ")
    print ("'> regFile|url extType title id'- display a region file ")
    print ("'> tableFile|url title=tile pageSize=pageSize'- display a table  ")
    print ("'> tableFile|url title=title pageSize=pageSize'- display a table  ")
    print ("'> FitsFile|url plotID=plotID'- display a fits with plotID  ")
    print ("'> extType title, plotId ,id'- show extension ")


def isExist(reg, xList):
    res=any(reg in s for s in xList)
    return res


def handleOneArg(fc, x):
      if isExist('.fits',xList):
          fc.showFits(fc.uploadFile(x[0]))
      elif isExist('.tbl',xList):
         fc.showTable(fc.uploadFile(x[0]))
      elif isExist('.reg',xList):
         fc.overylayRegion(fc.uploadFile(xList[0]))

def handleTwoArgs(fc, xList):
    if isExist('.fits',xList)  and isExist('.reg',xList):
         if 'fits' in xList[0]:
              fc.showFits(fc.uploadFile(xList[0]))
              fc.overylayRegion(fc.uploadFile(xList[1]))
         else:
              fc.showFits(fc.uploadFile(xList[1]))
              fc.overylayRegion(fc.uploadFile(xList[0]))

    elif isExist('.fits',xList) and isExist('.tb1',xList) in xList:
           if 'fits' in xList[0]:
                fc.showFits(fc.uploadFile(xList[0]))
                fc.showTable(fc.uploadFile(xList[1]))
           else:
                fc.showFits(fc.uploadFile(xList[1]))
                fc.showTable(fc.uploadFile(xList[0]))

    elif isExist('.fits',xList) and  not isExist('.reg',xList) and not isExist('.tbl',xList): #fits argument
         fc.showFits(fc.uploadFile(xList[0]), plotID=xList[1])
    elif not isExist('.fits',xList) and isExist('.tbl',xList):
         fc.showTable(fc.uploadFile(xList[0 ]),xList[1] )

def handleMultiArgs(fc, xList):

    fIdxL = [xList.index(item) for item in xList if '.fits'  in item]
    rIdxL = [xList.index(item) for item in xList if '.reg' in item]
    tIdxL = [xList.index(item) for item in xList if '.tbl'  in item]
    fIdx=-1
    rIdx=-1
    tIdx=-1
    if (len(fIdxL)==1 ):
        fIdx=fIdxL[0]
    if (len(rIdxL)==1 ):
        rIdx=rIdxL[0]
    if (len(tIdxL)==1 ):
        tIdx=tIdxL[0]


    if fIdx>=0 and rIdx>=0  and tIdx >=0:
         fc.showFits(fc.uploadFile(xList[fIdx]))
         fc.overylayRegion(fc.uploadFile(xList[rIdx]))
         fc.showTable(fc.uploadFile(xList[tIdx ]))

    elif fIdx>=0 and rIdx<0  and tIdx>=0:
         fc.showTable(fc.uploadFile(xList[tIdx ]))
         fc.showFits(fc.uploadFile(xList[fIdx]), xList[fIdx+1])
    elif fIdx>=0 and tIdx<0 and rIdx>=0:
         fc.showFits(fc.uploadFile(xList[fIdx]), xList[fIdx+1])
         fc.overylayRegion(fc.uploadFile(xList[rIdx]))
    elif fIdx>=0 and rIdx<0  and tIdx<0:
         fc.showFits(fc.uploadFile(xList[fIdx]), xList[fIdx+1], xList[fIdx+2])

    elif fIdx<0 and tIdx>=0:
         fc.showTable(fc.uploadFile(xList[tIdx ]),xList[tIdx+1], xList[tIdx+2] )
    elif len(xList==4):
        fc.addExtension(xList[tIdx ], xList[tIdx+1], xList[tIdx+2], xList[tIdx+3])
    else:
        print('this type of functionality =' + xList + ' is not supported yet')



true=1
path= os.getcwd() + '/data/'


print('===================help info=======================')
help()
fc =FireflyClient()
print('===================================================')



x=''
name=None

while true:

   x=raw_input("input>")
   if(x==''):
       pass

   fc.addListener(name,x)
   xList=split(x, (',' ';', ' ') )

   if x=='-v':
         print(sys.version_info[0])
   elif x=='-h':
         help()
   elif ('conn' in x):
         #if fc.isConnected()==0:
            fc.connect()
            #fc.run_forever()
   elif 'browser' in x:
       args = split(x, (',' ';', ' ') )
       parm=''
       if (len(args)>1):
         for arg in args[1:]:
             parm=parm+','+arg

       if len(parm)==0:
          fc.lanuchBrowser()
       else:
          fc.lanuchBrowser(parm)

   else:
     n = len(xList)
     if (n==1):
         handleOneArg(fc, xList)

     elif n==2:
         handleTwoArgs(fc, xList)
     else :
        handleMultiArgs(fc, xList)



   if x== KeyboardInterrupt:
      fc.disconnect()
      fc.sesson.close()
      exit()