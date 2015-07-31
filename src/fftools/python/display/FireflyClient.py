__author__ = 'zhang'

from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
import time
import socket


class FireflyClient(WebSocketClient):
    # class variables
    # serverEvents = {'name': ['EVT_CONN_EST', 'SvrBackgroundReport', 'WindowResize'],
    #                 'scope': ['SELF', 'CHANNEL'],
    #                 'dataType': ['STRING', 'JSON', 'BG_STATUS'],
    #                 'data': ['channel']
    #                 }

    fftoolsCmd = '/fftools/sticky/CmdSrv'
    myLocalhost = 'localhost:8080'
    ALL = 'ALL_EVENTS_ENABLED'

    # for serializing the RangeValues object - modify when new stretch types added
    stretchTypeDict={'percent':88,'absolute':90,'zscale':91,'sigma':92}
    stretchAlgorithmDict={'linear':44, 'log':45,'loglog':46,'equal':47,'squared':48, 'sqrt':49}


    #the constructor, define instance variables for the object
    def __init__(self, host=myLocalhost, channel=None):
        if host.startswith('http://'):
            host = host[7:]
        self.thisHost = host
        url = 'ws://%s/fftools/sticky/firefly/events' % host #web socket url
        if channel:
            url+= '?channelID=%s' % channel
        WebSocketClient.__init__(self, url)
        self.urlRoot = 'http://' + host + self.fftoolsCmd
        self.urlBW = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID='
        self.listeners = {}
        self.channel = channel
        self.session = requests.Session()
        self.connect()
        # print 'websocket url:%s' % url


    # def opened(self):
    #     print ("Opening websocket connection to fftools")

    # def closed(self, code, reason=None):
    #     print ("Closed down", code, reason)


    def handleEvent(self, ev):
        for callback, eventIDList  in self.listeners.items():
            if ev['name'] in eventIDList or FireflyClient.ALL in eventIDList:
                callback(ev)


                 #overridde the superclass's method
    def received_message(self, m):
        ev = json.loads(m.data.decode('utf8'))
        eventName = ev['name']

        if eventName == 'EVT_CONN_EST':
            try:
                connInfo = ev['data']
                if self.channel is None:
                    self.channel = connInfo['channel']
                connID = ''
                if 'connID' in connInfo:
                    connID = connInfo['connID']
                seinfo = self.channel
                if (len(connID) ) > 0:
                    seinfo = connID + '_' + seinfo

                # print ("Connection established: " + seinfo)
                self.session.cookies['seinfo'] = seinfo
                #self.onConnected(self.channel)
            except:
                print ('from callback exception: ')
                print (m)
        else:
            # print "call calling handleEvnet"
            # print sevent
            self.handleEvent(ev)

    def sendURLAsGet(self,url):
        response= self.session.get(url)
        # print response.text
        status = json.loads(response.text)
        return status[0]

    def isPageConnected(self):
        ip=socket.gethostbyname(socket.gethostname())
        url = self.urlRoot + '?cmd=pushAliveCheck&ipAddress=%s' % ip
        retval= self.sendURLAsGet(url)
        return retval['active']

    # def onConnected(self, channel):
    #     #open the browser
    #     url = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID=' + channel
    #     webbrowser.open('http://localhost:8080/fftools/app.html?id=Loader&channelID=' + channel)
    #     webbrowser.open(url)


    @staticmethod
    def createRV(stretchType, lowerValue, upperValue, algorithm,
                 zscaleContrast=25, zscaleSamples=600, zscaleSamplesPerLine=120):
        retval= None
        ffc= FireflyClient
        st= stretchType.lower()
        a= algorithm.lower()
        if st in ffc.stretchTypeDict and a in ffc.stretchAlgorithmDict:
            retval='%d,%f,%d,%f,%d,%d,%d,%d' % \
                   (ffc.stretchTypeDict[st], lowerValue,
                    ffc.stretchTypeDict[st], upperValue,
                    ffc.stretchAlgorithmDict[a],
                    zscaleContrast, zscaleSamples, zscaleSamplesPerLine)
        return retval


    def makePidParam(self,plotId):
        return ','.join(plotId) if type(plotId) is list else plotId


#-----------------------------------------------------------------
#-----------------------------------------------------------------
# Public API Begins
#-----------------------------------------------------------------
#-----------------------------------------------------------------

    def addListener(self, callback, name=ALL):
        """
        Add a function to listen for events on the firefly client
        :param callback: set the function to be called when a event happens on the firefly client
        :param name: set the name of the events, default to all events
        :return:
        """
        if callback not in self.listeners.keys():
            self.listeners[callback]= []
        if name not in self.listeners[callback]:
            self.listeners[callback].append(name)


    def removeListener(self, callback, name=ALL):
        """
        remove a callback
        :param callback: a previous set function that is to be removed
        :param name: set the name of the event, default to all events
        :return:
        """
        if callback in self.listeners.keys():
            if name in self.listeners[callback]:
                self.listeners[callback].remove(name)
            if len(self.listeners[callback])==0:
                self.listeners.pop(callback)


    def waitForEvents(self):
        """
        Pause and do not exit.  Wait over events from the server.
        This is optional. You should not use this method in ipython notebook
        Event will get called anyway.
        """
        WebSocketClient.run_forever(self)


    def getFireflyUrl(self, mode=None, channel=None):
        """
        Get URL to Firefly Tools viewer and the channel set. Normally this method
        will be call without any parameters.
        :param mode: mode maybe one of 'full', or 'minimal'.  Defaults to minimal.
        :param channel: a different channel string than the default
        :return: the full url string
        """
        if not channel:
            channel = self.channel

        url = 'http://' + self.thisHost + '/fftools/minimal.html?channelID='
        if mode == "full" :
            url = self.urlBW
        return url + channel


    def launchBrowser(self, url=None, channel=None, force=False):
        """
        Launch a browsers with the Firefly Tools viewer and the channel set. Normally this method
        will be call without any parameters.
        :param url: the url, overriding the default
        :param channel: a different channel than the default
        :return: the channel
        """
        if not channel:
            channel = self.channel
        if not url :
            url=self.urlBW
        doOpen= True if force else not self.isPageConnected()
        if doOpen:
            webbrowser.open(self.getFireflyUrl(url,channel))
        time.sleep(5) # todo: find something better to do than sleeping
        return channel


    def stayConnected(self):
        self.ws.run()

    def disconnect(self):
        self.close()

    def uploadFile(self, path, preLoad=True):
        """ Upload a file to the Firefly Server
        :param path: uploaded file can be fits, region, and various types of table files
        """
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload=%s' % preLoad
        files = {'file': open(path, 'rb')}
        result = self.session.post(url, files=files)
        index = result.text.find('$')
        return result.text[index:]


    def uploadFitsData(self, stream):
        """
        Upload a FITS file like object to the Firefly server. The method should allows file like data
        to be streamed without using a actual file.
        :param stream: a file like object
        :return: status
        """
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload=true'
        dataPack= {'data' : stream}
        result = self.session.post(url, files=dataPack)
        index = result.text.find('$')
        return result.text[index:]

    def uploadTextData(self, stream):
        """
        Upload a Text file like object to the Firefly server. The method should allows file like data
        to be streamed without using a actual file.
        :param stream: a file like object
        :return: status
        """
        return self.uploadData(stream,'UNKNOWN')


    def uploadData(self, stream, dataType):
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload='
        url+= 'true&type=FITS' if dataType=='FITS' else 'false&type=UNKNOWN'
        dataPack= {'data' : stream}
        result = self.session.post(url, files=dataPack)
        index = result.text.find('$')
        return result.text[index:]


    def showFits(self, fileOnServer=None, plotId=None, additionalParams=None):
        """ Show a fits image
        :param: fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                          then it is the return value of the method. Otherwise it is a file that
                          firefly has direct read access to.
        :param: plotId: the id you assigned to the plot. This is necessary to further control the plot
        :param: additionalParam: dictionary of any valid fits viewer plotting parameters,
                          see firefly/docs/fits-plotting-parameters.md
        :return: status of call
        """
        url = self.urlRoot + '?cmd=pushFits'
        if additionalParams:
            url+= '&' + '&'.join(['%s=%s' % (k, v) for (k, v) in additionalParams.items()])
        if plotId:
            url+= '&plotId=%s' % plotId
        if fileOnServer:
            url+= '&file=%s' % fileOnServer
        return self.sendURLAsGet(url)


    def showTable(self, fileOnServer, title=None, pageSize=None):
        """
        Show a table in Firefly
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param title: title on table
        :param pageSize: how many rows are shown.
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushTable&tblType=pushed"
        if title:
            url+= '&Title=%s' % title
        if pageSize:
            url+= '&pageSize=%s' % str(pageSize)
        url+= "&file=%s" % fileOnServer
        return self.sendURLAsGet(url)

    def showXYPlot(self, fileOnServer, additionalParams=None):
        """
        Show a table in Firefly
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param additionalParams: XY Plot Viewer parameters
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushXYPlot"
        if additionalParams:
            url+= '&' + '&'.join(['%s=%s' % (k, v) for (k, v) in additionalParams.items()])
        url+= '&file=%s' % fileOnServer
        return self.sendURLAsGet(url)


    def addExtension(self, extType, title, plotId, extensionId, image=None):
        """
        Add an extension to the plot.  Extensions are context menus that allows you extend
        what firefly can so when certain actions happen
        :param extType: May be 'AREA_SELECT', 'LINE_SELECT', or 'POINT'. todo: 'CIRCLE_SELECT'
        :param title: The title that the user sees
        :param plotId: The it of the plot to put the extension on
        :param extensionId: The id of the extension
        :param image: An url of an icon to display the toolbar instead of title
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushExt&plotId=%s&id=%s&extType=%s&Title=%s" % (plotId,extensionId,extType,title)
        if image:
            url+= "&image=%s" % image
        return self.sendURLAsGet(url)


    def zoom(self, plotId, factor):
        """
        Zoom the image
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings.
        :param factor: number, zoom factor for the image
        :return:
        """
        url = self.urlRoot + "?cmd=pushZoom&plotId=%s&zoomFactor=%.3f" % (self.makePidParam(plotId),factor)
        return self.sendURLAsGet(url)


    def pan(self, plotId, x, y):
        """
        Pan or scroll the image to center on the image coordinates passed
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings.
        :param x: number, new center x position to scroll to
        :param y: number, new center y position to scroll to
        :return:
        """
        url = self.urlRoot + "?cmd=pushPan&plotId=%s&scrollX=%d&scrollY=%d" % (self.makePidParam(plotId),x,y)
        return self.sendURLAsGet(url)

    def stretch(self, plotId, serializedRV):
        """
        Change the stretch of the image
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings.
        :param serializedRV: the range values parameter
        :return:
        """
        url = self.urlRoot + "?cmd=pushRangeValues&plotId=%s&rangeValues=%s" % (self.makePidParam(plotId),serializedRV)
        return self.sendURLAsGet(url)


    #-----------------------------------------------------------------
    # Region Stuff
    #-----------------------------------------------------------------


    def overlayRegion(self, fileOnServer, title=None, regionLayerId=None, plotId=None):
        """
        Overlay a region on the loaded FITS images
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param title: title of the region file
        :param regionLayerId: id of layer to add
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings.
                       If non the tne region is overlay on all plots
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushRegion&file=%s" % fileOnServer
        if title:
            url+= '&Title=%s' % title
        if regionLayerId:
            url+= '&id=%s' % regionLayerId
        if plotId:
            url+= '&plotId=%s' % self.makePidParam(plotId)
        return self.sendURLAsGet(url)


    def removeRegion(self, regionLayerId, plotId=None):
        """
        Overlay a region on the loaded FITS images
        :param regionLayerId: regionLayer to remove
        :return: status of call
        """
        url= self.urlRoot +"?cmd=pushRemoveRegion&id=%s" % regionLayerId
        if plotId:
            url+= '&plotId=%s' % self.makePidParam(plotId)
        return self.sendURLAsGet(url)


    def overlayRegionData(self, regionData, regionLayerId, title=None, plotId=None):
        """
        Overlay a region on the loaded FITS images. Note: the plotId is ignored if you have already put this
        region id on a plot.  In that case it just add the regions to the existing id.
        :param regionData: a list of region entries
        :param regionLayerId: id of region overlay to create or add too
        :param title: title of the region file
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings
                       If non the tne region is overlay on all plots
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushRegionData&id=%s" % regionLayerId
        if title:
            url+= '&Title=%s' % title
        if plotId:
            url+= '&plotId=%s' % self.makePidParam(plotId)
        response = self.session.post(url, data={'ds9RegionData' : '['+"--STR--".join(regionData)+']'})
        status = json.loads(response.text)
        return status[0]



    def removeRegionData(self, regionData, regionLayerId):
        """
        Remove the specified region entries
        :param regionData: a list of region entries
        :param regionLayerId: id of region to remove entries from
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushRemoveRegionData&id=%s" % regionLayerId
        response = self.session.post(url,
                                     data={'ds9RegionData' : '['+"--STR--".join(regionData)+']'})
        status = json.loads(response.text)
        return status[0]

    #-----------------------------------------------------------------
    # Range Values
    #-----------------------------------------------------------------

    @staticmethod
    def createRangeValuesStandard(algorithm, stretchType='Percent', lowerValue=1, upperValue=99):
        """
        :param algorithm: must be 'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'
        :param stretchType: must be 'Percent','Absolute','Sigma'
        :param lowerValue: number, lower end of stretch
        :param upperValue: number, upper end of stretch
        :return: a serialized range values string
        """
        retval= FireflyClient.createRV(stretchType,lowerValue,upperValue,algorithm,25,600,120)
        ffc= FireflyClient
        if not retval:
            t= stretchType if stretchType.lower() in ffc.stretchAlgorithmDict else 'percent'
            a= algorithm if algorithm.lower() in ffc.stretchAlgorithmDict else 'linear'
            retval= FireflyClient.createRV(t,1,99,a)
        return retval


    @staticmethod
    def createRangeValuesZScale(algorithm, zscaleContrast=25, zscaleSamples=600, zscaleSamplesPerLine=120):
        """
        :param algorithm: must be 'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'
        :param zscaleContrast: zscale contrast
        :param zscaleSamples: zscale samples
        :param zscaleSamplesPerLine: zscale sample per line
        :return: a serialized range values string
        """
        retval= FireflyClient.createRV('zscale',1,2,algorithm,zscaleContrast, zscaleSamples, zscaleSamplesPerLine)
        if not retval:
            a= algorithm if algorithm.lower() in FireflyClient.stretchAlgorithmDict else 'linear'
            retval= FireflyClient.createRV('zscale',1,2,a,25,600,120)
        return retval



