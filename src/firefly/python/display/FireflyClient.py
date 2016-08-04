
__author__ = 'zhang' + ' Cindy Wang'

from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
import time
import socket
import urlparse


class FireflyClient(WebSocketClient):
    # class variables
    # serverEvents = {
    #       'name': ['EVT_CONN_EST', 'SvrBackgroundReport', 'WindowResize'],
    #       'scope': ['SELF', 'CHANNEL'],
    #       'dataType': ['STRING', 'JSON', 'BG_STATUS'],
    #       'data': ['channel']
    # }

    fftoolsCmd = '/firefly/sticky/CmdSrv'
    myLocalhost = 'localhost:8080'
    ALL = 'ALL_EVENTS_ENABLED'

    # for serializing the RangeValues object
    stretchTypeDict={'percent':88, 'maxmin': 89, 'absolute':90,  'zscale':91,'sigma':92}
    stretchAlgorithmDict={'linear':44, 'log':45,'loglog':46,'equal':47,
                          'squared':48, 'sqrt':49, 'asinh':50,
                          'powerlaw_gamma':51}

    # extension type
    extensionType = ['AREA_SELECT', 'LINE_SELECT', 'POINT']

    # id for table, region layer, extension
    itemID = {'Table': 0, 'RegionLayer': 0, 'Extension': 0}

    # actions from Firefly
    actionDict = {
       'ShowFits': 'ImagePlotCntlr.PlotImage',
       'AddExtension': 'ExternalAccessCntlr/extensionAdd',
       'ShowTable': 'table.search',
       'ZoomImage': 'ImagePlotCntlr.ZoomImage',
       'PanImage': 'ImagePlotCntlr.recenter',
       'StretchImage': 'ImagePlotCntlr.StretchChange',
       'CreateRegionLayer': 'DrawLayerCntlr.RegionPlot.createLayer',
       'DeleteRegionLayer': 'DrawLayerCntlr.RegionPlot.deleteLayer',
       'AddRegionData': 'DrawLayerCntlr.RegionPlot.addRegion',
       'RemoveRegionData': 'DrawLayerCntlr.RegionPlot.removeRegion'
   }



    #the constructor, define instance variables for the object
    def __init__(self, host=myLocalhost, channel=None):
        if host.startswith('http://'):
            host = host[7:]
        self.thisHost = host
        url = 'ws://%s/firefly/sticky/firefly/events' % host #web socket url
        if channel:
            url+= '?channelID=%s' % channel
        WebSocketClient.__init__(self, url)
        self.urlRoot = 'http://' + host + self.fftoolsCmd
        self.urlBW = 'http://' + self.thisHost + '/firefly/firefly.html;wsch='
        self.listeners = {}
        self.channel = channel
        self.session = requests.Session()
        #print 'websocket url:%s' % url
        self.connect()


    # def opened(self):
    #     print ("Opening websocket connection to fftools")

    # def closed(self, code, reason=None):
    #     print ("Closed down", code, reason)


    def _handleEvent(self, ev):
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

                self.session.cookies['seinfo'] = seinfo
            except:
                print ('from callback exception: ')
                print (m)
        else:
            self._handleEvent(ev)

    def _sendURLAsGet(self,url):
        """
        send URL in 'GET' request
        :param url:
        :return:
        """
        response= self.session.get(url)
        # print response.text
        status = json.loads(response.text)
        return status[0]

    def _sendURLAsPost(self, data):
        """
        send URL in 'POST' request
        :param data
        :return:
        """
        response = self.session.post(self.urlRoot, data=data )
        status = json.loads(response.text)
        return status[0]

    def _isPageConnected(self):
        ip=socket.gethostbyname(socket.gethostname())
        url = self.urlRoot + '?cmd=pushAliveCheck&ipAddress=%s' % ip
        retval= self._sendURLAsGet(url)
        return retval['active']

    # def onConnected(self, channel):
    #     #open the browser
    #     url = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID=' + channel
    #     webbrowser.open('http://localhost:8080/fftools/app.html?id=Loader&channelID=' + channel)
    #     webbrowser.open(url)


    def _makePidParam(self,plotId):
        return ','.join(plotId) if isinstance(plotId, list) else plotId


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

        url = 'http://' + self.thisHost + '/firefly/firefly.html?id=Loader&channelID='
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
        doOpen= True if force else not self._isPageConnected()
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
        url = 'http://' + self.thisHost + '/firefly/sticky/Firefly_FileUpload?preload=%s' % preLoad
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
        url = 'http://' + self.thisHost + '/firefly/sticky/Firefly_FileUpload?preload=true'
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
        url = 'http://' + self.thisHost + '/firefly/sticky/Firefly_FileUpload?preload='
        url+= 'true&type=FITS' if dataType=='FITS' else 'false&type=UNKNOWN'
        dataPack= {'data' : stream}
        result = self.session.post(url, files=dataPack)
        index = result.text.find('$')
        return result.text[index:]



    def dispatchRemoteAction(self, channel, actionType, payload):
        """
        dispatch the action to the server by using 'GET' request
        :param: channel
        :param: actionType
        :param: payload: payload for the action
        """

        action= {'type':actionType, 'payload':payload}
        url = self.urlRoot + '?channelID=' + channel + '&cmd=pushAction&action='
        url+= json.dumps(action)
        return self._sendURLAsGet(url)


    def dispatchRemoteActionByPost(self, channel, actionType, payload):
        """
        dispatch the action to the server by using 'POST' request
        :param: channel
        :param: actionType
        :param: payload: payload for the action
        """
        action={'type': actionType, 'payload': payload}
        data = {'channelID': channel, 'cmd':'pushAction', 'action': json.dumps(action)}

        return self._sendURLAsPost(data)


    #-------------------------
    # dispatch actions
    #-------------------------

    #----------------------------------------------------------------
    # action on showing fits, tables, XYPlot and adding extension
    #----------------------------------------------------------------

    def showFits(self, fileOnServer=None, plotId=None, **additionalParams):
        """
        Show a fits image
        :param: fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                          then it is the return value of the method. Otherwise it is a file that
                          firefly has direct read access to.
        :param: plotId: the id you assigned to the plot. This is necessary to further control the plot
        :param: additionalParam: dictionary of any valid fits viewer plotting parameters,
                          see firefly/docs/fits-plotting-parameters.md
        :return: status of call
        """
        wpRequest= {'plotGroupId':'groupFromPython',
                    'GroupLocked':False}
        payload= {'wpRequest': wpRequest,
                  'useContextModifications':True,
                  'viewerId':'DEFAULT_FITS_VIEWER_ID'}
        if plotId:
            payload['wpRequest'].update({ 'plotId': plotId})
        if fileOnServer:
            payload['wpRequest'].update({ 'file': fileOnServer})
        if additionalParams:
            payload['wpRequest'].update(additionalParams)
        return self.dispatchRemoteAction(self.channel, FireflyClient.actionDict['ShowFits'], payload)


    def showTable(self, fileOnServer, tblId=None, title=None, pageSize=100, isCatalog=True):
        """
        Show a table in Firefly
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param title: title on table
        :param pageSize: how many rows are shown.
        :param isCatalog: catalog or table
        :return: status of call
        """
    
    
        if not tblId:
            tblId = FireflyClient._genItemId('Table')
        if not title:
            title = tblId
        tblType='table' if not isCatalog else 'catalog'

        tblReq = {'startIdx': 0, 'pageSize': pageSize, 'source': fileOnServer, 'tblType': tblType,
                  'id': 'IpacTableFromSource', 'tbl_id': tblId}
        metaInfo = {'title': title, 'tbl_id': tblId}
        tblReq.update({'META_INFO': metaInfo})
        payload = {'request': tblReq}

        return self.dispatchRemoteAction(self.channel, FireflyClient.actionDict['ShowTable'], payload)


    def showXYPlot(self, fileOnServer, **additionalParams):
        """
        TODO
        Show a table in Firefly
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param additionalParams: XY Plot Viewer parameters
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushXYPlot"
        if additionalParams:
            url += '&' + '&'.join(['%s=%s' % (k, v) for k, v in additionalParams.items()])
        url += '&file=%s' % fileOnServer
        return self._sendURLAsGet(url)


    def addExtension(self, extType, plotId = None, title='', toolTip='', extensionId=None, imageSrc=None):
        """
        TODO: callback
        Add an extension to the plot.  Extensions are context menus that allows you to extend
        what firefly can so when certain actions happen
        :param extType: May be 'AREA_SELECT', 'LINE_SELECT', or 'POINT'. todo: 'CIRCLE_SELECT'
        :param title: The title that the user sees
        :param plotId: The it of the plot to put the extension on
        :param extensionId: The id of the extension
        :param imageSrc: image source of an icon to display the toolbar instead of title
        :return: status of call
        """

        if extType not in FireflyClient.extensionType:
            extType = 'NONE'

        if not extensionId:
            extensionId = FireflyClient._genItemId('Extension')

        imageUrl = FireflyClient.createImageURL(imageSrc) if imageSrc else None

        extension = {'id': extensionId, 'plotId': plotId, 'imageUrl': imageUrl,
                     'title': title, 'extType': extType, 'toolTip': toolTip}
        payload={'extension': extension}
        return self.dispatchRemoteActionByPost(self.channel, FireflyClient.actionDict['AddExtension'], payload)

    #----------------------------
    # actions on image
    #----------------------------

    def setZoom(self, plotId, factor = 1.0):
        """
        Zoom the image
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings.
        :param factor: number, zoom factor for the image
        :return:
        """

        def zoomOnePlot(plotId, factor):
            payload = {'plotId': plotId, 'userZoomType':'LEVEL', 'level': factor, 'actionScope':'SINGLE'}
            return  self.dispatchRemoteAction(self.channel, FireflyClient.actionDict['ZoomImage'], payload)

        if isinstance(plotId, tuple) or isinstance(plotId, list):
            return [zoomOnePlot(x, factor) for x in plotId]
        else:
            return zoomOnePlot(plotId, factor)


    def setPan(self, plotId, x=None, y=None, coord='image'):
        """
        Pan or scroll the image to center on the image coordinates passed
        if no (x, y) is given, the image is recentered at the center of the image
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings.
        :param x: number, new center x position to scroll to
        :param y: number, new center y position to scroll to
        :param coord: coordinate system, if coord == 'image', then x, y is for image pixel,
        :                                if coord == 'J2000', then x, y is ra, dec on EQ_J2000.
        :return:
        """

        payload = {'plotId': plotId}
        if x and y :
            if coord.startswith('image'):
                payload.update({'centerPt': {'x': x, 'y': y, 'type': 'ImagePt'}})
            else:
                payload.update({'centerPt': {'x': x, 'y': y, 'type': 'J2000'}})

        return self.dispatchRemoteAction(self.channel, FireflyClient.actionDict['PanImage'], payload)


    def setStretch(self, plotId, type=None, algorithm=None, **additionParams):
        """
        Change the stretch of the image (no band case)
        :param type: 'percent','maxmin','absolute','zscale', or 'sigma'
        :param algorithm: 'linear', 'log','loglog','equal', 'squared', 'sqrt', 'asinh', 'powerlaw_gamma'
        :param plotId: plotId to which this range should be added, parameter may be string or a list of strings.
        :param additionParams:
                  for type == 'zscale', optional keyword arguments: zscaleContrast, zscaleSamples, zscaleSamplesPerLine
                      other type,       optional keyword arguments: lowerValue, upperValue
        :return: status of call
        """

        if type and type.lower() == 'zscale':
            serializedRV = FireflyClient._createRangeValuesZScale(algorithm, **additionParams)
        else:
            serializedRV = FireflyClient._createRangeValuesStandard(algorithm, type, **additionParams)

        stData = [{ 'band': 'NO_BAND', 'rv': serializedRV, 'bandVisible': True}]
        payload = {'stretchData': stData, 'plotId': plotId}

        return self.dispatchRemoteAction(self.channel, FireflyClient.actionDict['StretchImage'], payload)


    #-----------------------------------------------------------------
    # Region Stuff
    #-----------------------------------------------------------------

    def overlayRegionLayer(self, fileOnServer=None, regionData=None, title=None, regionLayerId=None, plotId=None ):
        """
        Overlay a region layer on the loaded FITS images, the regions are defiend either by a file or by some text description
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                                            then it is the return value of the method. Otherwise it is a file that
                                            firefly has direct read access to.
        :param regionData: region description, either a list of strings or a string
        :param title: title of the region file
        :param regionLayerId: id of layer to add
        :param plotId: plotId to which this region should be added, parameter may be string or a list of strings.
                       If None then overlay region on all plots
        :return: status of call
        """

        if not regionLayerId:
            regionLayerId = FireflyClient._genItemId('RegionLayer')
        payload = {'regionId': regionLayerId}

        if title:
            payload.update({'layerTitle': title})
        if plotId:
            payload.update({'plotId': plotId})


        if fileOnServer:
            payload.update({'fileOnServer': fileOnServer})
            return self.dispatchRemoteAction(self.channel, FireflyClient.actionDict['CreateRegionLayer'], payload )
        elif regionData:
            payload.update({'regionAry': regionData})
            return self.dispatchRemoteActionByPost(self.channel, FireflyClient.actionDict['CreateRegionLayer'], payload )



    def deleteRegionLayer(self, regionLayerId, plotId=None):
        """
        Delete region layer on the loaded FITS images
        :param regionLayerId: regionLayer to remove
        :param plotId: plotId to which the region layer should be removed, if None, then remove region layer from all plots
        :return: status of call
        """

        payload = {'regionId': regionLayerId }
        if plotId:
            payload.update({'plotId': plotId})

        return self.dispatchRemoteAction(self.channel, FireflyClient.actionDict['DeleteRegionLayer'], payload)


    def addRegionData(self, regionData, regionLayerId):
        """
        Add the specified region entries
        :param regionData: a list of region entries
        :param regionLayerId: id of region to remove entries from
        :return: status of call
        """
        payload = {'regionChanges': regionData, 'regionId': regionLayerId}

        return self.dispatchRemoteActionByPost(self.channel, FireflyClient.actionDict['AddRegionData'], payload )


    def removeRegionData(self, regionData, regionLayerId):
        """
        Remove the specified region entries
        :param regionData: a list of region entries
        :param regionLayerId: id of region to remove entries from
        :return: status of call
        """
        payload = {'regionChanges': regionData, 'regionId': regionLayerId}

        return self.dispatchRemoteActionByPost(self.channel, FireflyClient.actionDict['RemoveRegionData'], payload )


    def addMask(self, maskId,bitNumber,imageNumber,color,plotId,bitDesc=None,fileOnServer=None):
        """
        Add a mask layer
        TODO
        :param maskId: id of mask
        :param bitNumber: bitNumber of the mask to overlay
        :param imageNumber: imageNumber of the mask layer
        :param color: color as an html color (eg. #FF0000 (red) #00FF00 (green)
        :param plotId: plot id to overlay the mask on
        :param bitDesc: (optional) description of the mask layer
        :param fileOnServer: (optional) file to get the mask from, if None then get it from the original file
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushAddMask"

        params= {
           'id'        : maskId,
           'bitNumber' : bitNumber,
           'color'     : color,
           'plotId'    : plotId,
           'imageNumber' : imageNumber,
        }
        if bitDesc:
            params['bitDesc']= bitDesc
        if fileOnServer:
            params['fileKey']= fileOnServer
        response = self.session.post(url, data=params)
        status = json.loads(response.text)
        return status[0]


    def removeMask(self, maskId):
        """
        TODO
        Remove a mask layer
        :param maskId: id of mask
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushRemoveMask&id=%s" % maskId
        return self._sendURLAsGet(url)


    #-----------------------------------------------------------------
    # Range Values
    #-----------------------------------------------------------------

    @classmethod
    def _createRV(cls, stretchType, lowerValue, upperValue, algorithm,
                 zscaleContrast=25, zscaleSamples=600, zscaleSamplesPerLine=120,
                 betaValue = 0.1, gammaValue = 2.0):
        retval= None
        st= stretchType.lower()
        a= algorithm.lower()
        if st in cls.stretchTypeDict and a in cls.stretchAlgorithmDict:
            retval='%d,%f,%d,%f,%f,%f,%d,%d,%d,%d' % \
                   (cls.stretchTypeDict[st], lowerValue,
                    cls.stretchTypeDict[st], upperValue,
                    betaValue, gammaValue,
                    cls.stretchAlgorithmDict[a],
                    zscaleContrast, zscaleSamples, zscaleSamplesPerLine)
        return retval



    @classmethod
    def _createRangeValuesStandard(cls, algorithm, stretchType='Percent', lowerValue=1, upperValue=99):
        """
        :param algorithm: must be 'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'
        :param stretchType: must be 'Percent','Absolute','Sigma'
        :param lowerValue: number, lower end of stretch
        :param upperValue: number, upper end of stretch
        :return: a serialized range values string
        """

        retval= cls._createRV(stretchType,lowerValue,upperValue,algorithm)
        if not retval:
            t= stretchType if stretchType.lower() in cls.stretchAlgorithmDict else 'percent'
            a= algorithm if algorithm.lower() in cls.stretchAlgorithmDict else 'linear'
            retval= FireflyClient._createRV(t,1,99,a)
        return retval


    @classmethod
    def _createRangeValuesZScale(cls, algorithm, zscaleContrast=25, zscaleSamples=600, zscaleSamplesPerLine=120):
        """
        :param algorithm: must be 'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'
        :param zscaleContrast: zscale contrast
        :param zscaleSamples: zscale samples
        :param zscaleSamplesPerLine: zscale sample per line
        :return: a serialized range values string
        """
        retval= cls._createRV('zscale',1,1,algorithm,zscaleContrast, zscaleSamples, zscaleSamplesPerLine)
        if not retval:
            a= algorithm if algorithm.lower() in cls.stretchAlgorithmDict else 'linear'
            retval= cls._createRV('zscale',1,2,a,25,600,120)
        return retval


    @classmethod
    def _genItemId(cls, item):
        """
        generate an ID for some entity like 'Table', 'RegionLayer', 'Extension'
        :param item: entity type
        :return: an ID string
        """

        if item in cls.itemID:
            cls.itemID[item] += 1
            return item + '-' + str(cls.itemID[item])
        else:
            return None

    @staticmethod
    def createImageURL(imageSource):
        """
        create image url according to image source
        :param imageSource: an image path or image url
        :return:
        """

        def is_url(url):
            return urlparse.urlparse(url).scheme != ''

        if not imageSource.startswith('data:image') and not is_url(imageSource):
            data_uri = open(imageSource, 'rb').read().encode('base64').replace('\n', '')
            return 'data:image/png;base64,%s' % data_uri

        return imageSource

