__author__ = 'zhang'

from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
import time


class FireflyClient(WebSocketClient):
    # class variables
    serverEvents = {'name': ['EVT_CONN_EST', 'SvrBackgroundReport', 'WindowResize'],
                    'scope': ['SELF', 'CHANNEL'],
                    "dataType": ['STRING', 'JSON', 'BG_STATUS'],
                    "data": ['channel']
                    }

    fftoolsCmd = '/fftools/sticky/CmdSrv'
    true = 1
    false = 0
    myLocalhost = 'localhost:8080'
    ALL = "ALL_EVENTS_ENABLED"

    #the constructor, define instance variables for the object
    def __init__(self, host=myLocalhost, channel=None):
        #assign instance variables  todo-need to understand how to get the default channel
        if host.startswith("http://"):
            host = host[7:]

        self.thisHost = host
        #web socket event listener url
        url = 'ws://%s/fftools/sticky/firefly/events' % host
        print 'websocket url:%s' % url
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
                    seinfo = connID + "_" + seinfo

                print ("Connection established: " + seinfo)
                self.session.cookies['seinfo'] = seinfo
                #self.onConnected(self.channel)
            except:
                print ("from callback exception: ")
                print (m)
        else:
            # print "call calling handleEvnet"
            # print sevent
            self.handleEvent(ev)

    def sendURLAsGet(self,url):
        response= self.session.get(url)
        print response.text
        status = json.loads(response.text)
        return status[0]


    # def onConnected(self, channel):
    #     #open the browser
    #     url = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID=' + channel
    #     webbrowser.open('http://localhost:8080/fftools/app.html?id=Loader&channelID=' + channel)
    #     webbrowser.open(url)

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

    def launchBrowser(self, url=None, channel=None):
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
        webbrowser.open(url + channel)
        time.sleep(5)
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
        Upload a file like object to the Firefly server. The method should allows file like data
        to be streamed without using a actual file.
        :param stream: a file like object
        :return: status
        """
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload=true'
        dataPack= {'data' : stream}
        result = self.session.post(url, files=dataPack)
        index = result.text.find('$')
        return result.text[index:]


    def showFits(self, fileOnServer=None, plotID=None, additionalParams=None):
        """ Show a fits image
        :param: fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                          then it is the return value of the method. Otherwise it is a file that
                          firefly has direct read access to.
        :param: plotID: the id you assigned to the plot. This is necessary to further controlling
                          the plot
        :param: additionalParam: dictionary of any valid fits viewer plotting parameter,
                          see firefly/docs/fits-plotting-parameters.md
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushFits"
        dictStr = ''
        if additionalParams is not None:
            for key, value in additionalParams.items():
                dictStr+= '%s&=%s' % (key,value)
        if plotID is not None:
            url+= "&plotId=%s" % plotID
        if fileOnServer is not None:
            url+= "&file=%s" % fileOnServer
        url = url + dictStr
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
        titleStr = ''
        if title is not None:
            titleStr = '&titile=%s' % title

        pageSizeStr = ''
        if pageSize is not None:
            pageSizeStr = 'pageSize=%s' % pageSize
        if fileOnServer is not None:
            url+= "&file=%s" % fileOnServer
        url+= titleStr + pageSizeStr
        return self.sendURLAsGet(url)



    def overlayRegion(self, fileOnServer, title=None):
        """
        Overlay a region on the loaded FITS images
        :param fileOnServer: the is the name of the file on the server.  If you used uploadFile()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param title: title of the region file
        :return: status of call
        """
        url = self.urlRoot + "?cmd=pushRegion"
        url+= "&file=%s" % fileOnServer
        if title is not None:
            url+= '&title=%s' % title
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


