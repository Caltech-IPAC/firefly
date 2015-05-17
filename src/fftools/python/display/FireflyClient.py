__author__ = 'zhang'

# import requests
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
        self.connect();


    #overridde the superclass method
    def opened(self):
        print ("Opening websocket connection to fftools")

    #overridde the superclass method
    def closed(self, code, reason=None):
        print ("Closed down", code, reason)


    def addListener(self, callback, name=ALL):
        if callback not in self.listeners.keys():
            self.listeners[callback]= []
        if name not in self.listeners[callback]:
            self.listeners[callback].append(name)
        # print 'there are %d members in callback dict' % len(self.listeners)
        # for key, value  in self.listeners.items():
        #     print 'value=%s' % value

    def removeListener(self, callback, name=ALL):
        if callback in self.listeners.keys():
           if name in self.listeners[callback]:
               self.listeners[callback].remove(name);
           if len(self.listeners[callback])==0:
               self.listeners.pop(callback)

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


    # def onConnected(self, channel):
    #     #open the browser
    #     url = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID=' + channel
    #     webbrowser.open('http://localhost:8080/fftools/app.html?id=Loader&channelID=' + channel)
    #     webbrowser.open(url)


    def waitForEvents(self):
        WebSocketClient.run_forever(self)


    # def checkResult(self, result):
    #     if 'true' not in result.text:
    #         print("Error:" + result.text)

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

    def uploadFile(self, path):
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload=true'
        files = {'file': open(path, 'rb')}
        result = self.session.post(url, files=files)  #, headers={'Content-Type': files.content_type} )#  data=path)
        index = result.text.find('$')
        return result.text[index:]


        # TODO: i think this is the concept for how this method should work, need to test
    def uploadFitsData(self, stream, contentType='image/x-fits'):
        url = 'http://' + self.thisHost + '/fftools/sticky/Firefly_FileUpload?preload=true'
        myHeaders={'Content-Type': contentType}
        result = self.session.post(url, data=stream, headers=myHeaders)
        index = result.text.find('$')
        return result.text[index:]


    def showFits(self, path, plotID=None, addtlParams=None):
        url = self.urlroot + "?cmd=pushFits"
        dictStr = ''
        if addtlParams is not None:
            for key, value in addtlParams:
                dictStr+= '&' + key + '=' + value

        if plotID is not None:
            url = url + "&plotId=" + plotID
        url = url + dictStr
        self.session.post(url, data={'file': path})



    def showTable(self, path, title=None, pageSize=None):
        url = self.urlroot + "?cmd=pushTable"
        titleStr = ''
        if title is not None:
            titleStr = '&titile=' + title

        pageSizeStr = ''
        if pageSize is not None:
            pageSizeStr = 'pageSize=' + pageSize

        url = url + titleStr + pageSizeStr

        self.session.post(url, data={'file': path})



    def overylayRegion(self, path, extType='reg', title=None, id=None, image=None):

        url = self.urlroot + "?cmd=pushRegion" + '&extType=' + extType
        if title is not None:
            url = url + '&Title=' + title
        if id is not None:
            url = url + '&id=' + id

        if image is not None:
            url = url + "&image=" + image

        result = self.session.post(url, data={'file': path})


    def addExtension(self, extType, title, plotId, id, image=None):
        url = self.urlroot + "?cmd=pushExt" + "&plotId=" + plotId + "&id=" + id + "&extType=" + extType + "&Title=" + title
        if image is not None:
            url = url + "&image=" + image
        self.session.post(url, allow_redirects=True)


    def zoom(self, factor):
        #todo - add when http api supports this
        return


    def pan(self, direction, factor):
        #todo - add when http api supports this
        return


