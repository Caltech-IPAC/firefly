__author__ = 'zhang'

from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
# import sys
# import os


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
        url = 'ws://' + host + '/fftools/sticky/firefly/events'
        if channel is not None:
            url+= '?channelID='+channel
        WebSocketClient.__init__(self, url)
        self.urlroot = 'http://' + host + self.fftoolsCmd
        self.urlBW = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID='
        self.listeners = {}
        self.channel = channel
        self.session = requests.Session()
        self.connect()


    # def opened(self):
    #     print ("Opening websocket connection to fftools")

    # def closed(self, code, reason=None):
    #     print ("Closed down", code, reason)


    def handleEvent(self, sevent):
        for callback, eventIDList  in self.listeners.items():
            if sevent['name'] in eventIDList or self.ALL in eventIDList:
                callback(sevent)


                 #overridde the superclass's method
    def received_message(self, m):
        sevent = json.loads(m.data.decode('utf8'))
        eventName = sevent['name']

        if eventName == 'EVT_CONN_EST':
            try:
                connInfo = sevent['data']
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
                print ("from callback exception: "+ m)
        else:
            # print "call calling handleEvnet"
            # print sevent
            self.handleEvent(sevent)

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

    #  callback - set the function to be called when a event happens on the firefly client
    #  name - set the name of the events, default to all events
    def addListener(self, callback, name=ALL):
        if callback not in self.listeners.keys():
            self.listeners[callback]= []
        if name not in self.listeners[callback]:
            self.listeners[callback].append(name)


    #  callback - remove a callback
    #  name - set the name of the events, default to all events
    def removeListener(self, callback, name=ALL):
        if callback in self.listeners.keys():
            if name in self.listeners[callback]:
                self.listeners[callback].remove(name)
            if len(self.listeners[callback])==0:
                self.listeners.pop(callback)


    # Pause and do not exit.  Wait over events from the server.
    # This is optional. Event will get call anyway.
    def waitForEvents(self):
        WebSocketClient.run_forever(self)


    # def checkResult(self, result):
    #     if 'true' not in result.text:
    #         print("Error:" + result.text)

    # Launch a browsers with the Firefly Tools viewer and the channel set. Normally this method
    # will be call without any parameters.
    # url - the url, overriding the default
    # channel - a different channel than the default
    def launchBrowser(self, url=None, channel=None):
        if channel is None:
            channel = self.channel
        if url=='' or url is None:
            url=self.urlBW
        webbrowser.open(url + channel)
        return channel
        #return

    def stayConnected(self):
        self.ws.run()

    def disconnect(self):
        self.close()

    # Upload a file to the Firefly Server
    # uploaded file can be fits, region, and various types of table files
    # path - the path to the upload file
    def uploadFile(self, path):
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload=true'
        files = {'file': open(path, 'rb')}
        result = self.session.post(url, files=files)  #, headers={'Content-Type': files.content_type} )#  data=path)
        index = result.text.find('$')
        return result.text[index:]


    # Upload a file like object to the Firefly server. The method should allows file like data
    # to be streamed without using a actual file.
    # Uploaded data can be fits, region, and various types of table files
    # TODO: i think this is the concept for how this method should work, need to tested
    def uploadFitsData(self, stream, contentType='image/x-fits'):
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload=true'
        myHeaders={'Content-Type': contentType}
        result = self.session.post(url, data=stream, headers=myHeaders)
        index = result.text.find('$')
        return result.text[index:]


    # Show a fits image
    # fileOnServer   - the is the name of the file on the server.  If you used uploadFile()
    #                  then it is the return value of the method. Otherwise it is a file that
    #                  firefly has direct read access to.
    # plotID         - the id you assigned to the plot. This is necessary to further controlling
    #                  the plot
    # additionalParam- dictionary of any valid fits viewer plotting parameter,
    #                  see firefly/docs/fits-plotting-parameters.md
    def showFits(self, fileOnServer=None, plotID=None, additionalParams=None):
        url = self.urlroot + "?cmd=pushFits"
        dictStr = ''
        if additionalParams is not None:
            for key, value in additionalParams.items():
                dictStr+= '&' + key + '=' + value
        if plotID is not None:
            url = url + "&plotId=" + plotID
        if fileOnServer is not None:
            url = url + "&file=" + fileOnServer
        url = url + dictStr
        return self.sendURLAsGet(url)


    # Show a table in Firefly
    # fileOnServer   - the is the name of the file on the server.  If you used uploadFile()
    #                  then it is the return value of the method. Otherwise it is a file that
    #                  firefly has direct read access to.
    # title          - title on table
    # pageSize       - how many rows are shown.
    def showTable(self, fileOnServer, title=None, pageSize=None):
        url = self.urlroot + "?cmd=pushTable"
        titleStr = ''
        if title is not None:
            titleStr = '&titile=' + title

        pageSizeStr = ''
        if pageSize is not None:
            pageSizeStr = 'pageSize=' + pageSize
        if fileOnServer is not None:
            url+= "&file=" + fileOnServer
        url+= titleStr + pageSizeStr
        return self.sendURLAsGet(url)



    # Overlay a region on the loaded FITS images
    # fileOnServer   - the is the name of the file on the server.  If you used uploadFile()
    #                  then it is the return value of the method. Otherwise it is a file that
    #                  firefly has direct read access to.
    # title          - title of the region file
    def overlayRegion(self, fileOnServer, extType='reg', title=None):

        url = self.urlroot + "?cmd=pushRegion" + '&extType=' + extType
        url+= "&file=" + fileOnServer
        if title is not None:
            url+= '&title=' + title
        return self.sendURLAsGet(url)


    # Add an extension to the plot.  Extensions are context menus that allows you extend
    # what firefly can so when certain actions happen
    #  extType - May be 'AREA_SELECT', 'LINE_SELECT', or 'POINT'. todo: 'CIRCLE_SELECT'
    #  title - The title that the user sees
    #  plotId - The it of the plot to put the extension on
    #  extensionId - The id of the extension
    #  image - An url of an icon to display the toolbar instead of title
    def addExtension(self, extType, title, plotId, extensionId, image=None):
        url = self.urlroot + "?cmd=pushExt" + "&plotId=" + plotId + "&id=" + extensionId + "&extType=" + extType + "&Title=" + title
        if image is not None:
            url = url + "&image=" + image
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


