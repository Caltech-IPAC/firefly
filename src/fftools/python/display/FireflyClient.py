__author__ = 'zhang'

from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
import time
import socket


class FireflyClient(WebSocketClient):
    # class variables
    serverEvents = {'name': ['EVT_CONN_EST', 'SvrBackgroundReport', 'WindowResize'],
                    'scope': ['SELF', 'CHANNEL'],
                    'dataType': ['STRING', 'JSON', 'BG_STATUS'],
                    'data': ['channel']
                    }

    fftoolsCmd = '/fftools/sticky/CmdSrv'
    myLocalhost = 'localhost:8080'
    ALL = 'ALL_EVENTS_ENABLED'
    stretchTypeDict={'Percent':88,'Absolute':90,'ZScale':91,'Sigma':92}
    stretchAlgorithmDict={'Linear':44, 'Log':45,'LogLog':46,'Equal':47,'Squared':48, 'Sqrt':49}


    #the constructor, define instance variables for the object
    def __init__(self, host=myLocalhost, channel=None):
        #assign instance variables  todo-need to understand how to get the default channel
        if host.startswith('http://'):
            host = host[7:]

        self.thisHost = host
        #web socket event listener url
        url = 'ws://%s/fftools/sticky/firefly/events' % host
        # print 'websocket url:%s' % url
        if channel is not None:
            url+= '?channelID=%s' % channel
        WebSocketClient.__init__(self, url)
        self.urlRoot = 'http://' + host + self.fftoolsCmd
        self.urlBW = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID='
        self.listeners = {}
        self.channel = channel
        self.session = requests.Session()
        self.connect()


    # def opened(self):
    #     print ("Opening websocket connection to fftools")

    # def closed(self, code, reason=None):
    #     print ("Closed down", code, reason)


    def handleEvent(self, ev):
        for callback, eventIDList  in self.listeners.items():
            if ev['name'] in eventIDList or self.ALL in eventIDList:
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
        if stretchType in ffc.stretchTypeDict and algorithm in ffc.stretchAlgorithmDict:
            retval='%d,%f,%d,%f,%d,%d,%d,%d' % \
                   (ffc.stretchTypeDict[stretchType], lowerValue,
                    ffc.stretchTypeDict[stretchType], upperValue,
                    ffc.stretchAlgorithmDict[algorithm],
                    zscaleContrast, zscaleSamples, zscaleSamplesPerLine)
        return retval



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
        This is optional. Event will get call anyway.
        """
        WebSocketClient.run_forever(self)


    # Get URL to Firefly Tools viewer and the channel set. Normally this method
    # will be call without any parameters.
    # url - the url, overriding the default
    # channel - a different channel than the default
    def getFireflyUrl(self, url=None, channel=None):
        if channel is None:
            channel = self.channel
        if url=='' or url is None:
            url=self.urlBW
        return url + channel


    def launchBrowser(self, url=None, channel=None, force=False):
        """
        Launch a browsers with the Firefly Tools viewer and the channel set. Normally this method
        will be call without any parameters.
        :param url: the url, overriding the default
        :param channel: a different channel than the default
        :return: the channel
        """
        if channel is None:
            channel = self.channel
        if url=='' or url is None:
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


    def showFits(self, fileOnServer=None, plotID=None, additionalParams=None):
        """ Show a fits image
        :param: fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                          then it is the return value of the method. Otherwise it is a file that
                          firefly has direct read access to.
        :param: plotID: the id you assigned to the plot. This is necessary to further control the plot
        :param: additionalParam: dictionary of any valid fits viewer plotting parameters,
                          see firefly/docs/fits-plotting-parameters.md
        :return: status of call
        """
        url = self.urlRoot + '?cmd=pushFits'
        if additionalParams is not None:
            url+= '&' + '&'.join(['%s=%s' % (k, v) for (k, v) in additionalParams.items()])
        if plotID is not None:
            url+= '&plotId=%s' % plotID
        if fileOnServer is not None:
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
        url = self.urlRoot + "?cmd=pushTable"
        if title is not None:
            url+= '&Titile=%s' % title
        if pageSize is not None:
            url+= '&pageSize=%s' % str(pageSize)
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
        if image is not None:
            url+= "&image=%s" % image
        return self.sendURLAsGet(url)


    # Zoom the image
    # todo
    def zoom(self, plotId, factor):
        #todo - add when http api supports this
        return


    # Pan or scroll the image
    # todo
    def pan(self, plotId, direction, factor):
        #todo - add when http api supports this
        return


    #-----------------------------------------------------------------
    # Region Stuff
    #-----------------------------------------------------------------


    def overlayRegion(self, fileOnServer, title=None, regionLayerId=None):
        """
        Overlay a region on the loaded FITS images
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param title: title of the region file
        :param regionLayerId: id of layer to add
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushRegion&file=%s" % fileOnServer
        if title is not None:
            url+= '&Title=%s' % title
        if regionLayerId is not None:
            url+= '&id=%s' % regionLayerId
        return self.sendURLAsGet(url)


    def removeRegion(self, regionLayerId):
        """
        Overlay a region on the loaded FITS images
        :param regionLayerId: regionLayer to remove
        :return: status of call
        """
        return self.sendURLAsGet(self.urlRoot + "?cmd=pushRemoveRegion&id=%s" % regionLayerId)


    def overlayRegionData(self, regionData, regionLayerId, title=None):
        """
        Overlay a region on the loaded FITS images
        :param regionData: a list of region entries
        :param regionLayerId: id of region overlay to create or add too
        :param title: title of the region file
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushRegionData&id=%s" % regionLayerId
        if title is not None:
            url+= '&Title=%s' % title
        response = self.session.post(url,
                                     data={'ds9RegionData' : '['+"--STR--".join(regionData)+']'})
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
        :param stretchType: must be 'Percent','Absolute','Sigma'
        :param lowerValue: number, lower end of stretch
        :param upperValue: number, upper end of stretch
        :param algorithm: must be 'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'
        :return: a serialized range values string
        """
        retval= FireflyClient.createRV(stretchType,lowerValue,upperValue,algorithm,25,600,120)
        ffc= FireflyClient
        if not retval:
            t= stretchType if stretchType in ffc.stretchAlgorithmDict else 'Percent'
            a= algorithm if algorithm in ffc.stretchAlgorithmDict else 'Linear'
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
        retval= FireflyClient.createRV('ZScale',1,2,algorithm,zscaleContrast, zscaleSamples, zscaleSamplesPerLine)
        if not retval:
            a= algorithm if algorithm in FireflyClient.stretchAlgorithmDict else 'Linear'
            retval= FireflyClient.createRV('ZScale',1,2,a,25,600,120)
        return retval



