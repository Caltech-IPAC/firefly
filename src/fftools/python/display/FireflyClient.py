__author__ = 'zhang'

import requests
from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
import sys
import os


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

    #the constructor, define instance variables for the object
    def __init__(self, host=myLocalhost, channel=None):
        #assign instance variables
        if host.startswith("http://"):
            host = host[7:]

        self.thisHost = host
        #web socket event listener url
        url = 'ws://' + host + '/fftools/sticky/firefly/events'
        WebSocketClient.__init__(self, url)
        self.urlroot = 'http://' + host + self.fftoolsCmd
        self.urlBW = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID='
        self.listeners = {}
        self.channel = channel
        self.session = requests.Session()

    #overridde the superclass's method
    def opened(self):
        print ("Opening websocket connection to fftools")

    #overridde the superclass's method
    def closed(self, code, reason=None):
        print ("Closed down", code, reason)


    def addListener(self, name, callback):

        self.listeners['name'] = name
        self.listeners['callback'] = callback

    def removeListener(self, rname):
         if rname in a:
               del self.listeners['name']


    def handleEvent(self, sevent):

        for name, callback  in self.listeners.items():
            if name == sevent['name'] and name == None:
                callback(sevent)


    #overridde the superclass's method
    def received_message(self, m):

        sevent = json.loads(m.data.decode('utf8'))
        eventName = sevent['name']

        if eventName == 'EVT_CONN_EST':
            try:

                connInfo = sevent['data']
                if (self.channel == None):
                    self.channel = connInfo['channel']
                connID = ''
                if ('connID' in connInfo):
                    connID = connInfo['connID']
                seinfo = self.channel
                if (len(connID) ) > 0:
                    seinfo = connID + "_" + seinfo

                print ("Connection established: " + seinfo)
                self.session.cookies['seinfo'] = seinfo
                #self.onConnected(self.channel)
            except:
                self.msgCallback(m)
        else:
            self.handleEvent(sevent)

    def msgCallback(self, message):
        print ("from callback:", message)

    def onConnected(self, channel):
        #open the browser
        url = 'http://' + self.thisHost + '/fftools/app.html?id=Loader&channelID=' + channel
        webbrowser.open('http://localhost:8080/fftools/app.html?id=Loader&channelID=' + channel)
        webbrowser.open(url)


    def waitForEvents(self):
        WebSocketClient.run_forever()


    def checkResult(self, result):
        if 'true' not in result.text:
            print("Error:" + result.text)

    def lanuchBrowser(self, url=None, channel=None):
        if (channel == None):
            channel = self.channel
        if (url=='' or url==None):
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



    def showFits(self, path, plotID=None, addtlParams=None):

        url = self.urlroot + "?cmd=pushFits"
        dictStr = ''
        if (addtlParams != None):
            for key in addtlParams:
                dictStr = dictStr + '&' + key + '=' + addtlParams[key]

        if (plotID != None):
            url = url + "&plotID=" + plotID
        url = url + dictStr
        self.session.post(url, data={'file': path})



    def showTable(self, path, title=None, pageSize=None):

        url = self.urlroot + "?cmd=pushTable"
        titleStr = ''
        if (title != None):
            titleStr = '&titile=' + title

        pageSizeStr = ''
        if (pageSize != None):
            pageSizeStr = 'pageSize=' + pageSize

        url = url + titleStr + pageSizeStr

        self.session.post(url, data={'file': path})



    def overylayRegion(self, path, extType='reg', title=None, id=None, image=None):

        url = self.urlroot + "?cmd=pushRegion" + '&extType=' + extType
        if (title != None):
            url = url + '&Title=' + title
        if (id != None):
            url = url + '&id=' + id

        if (image != None):
            url = url + "&image=" + image

        result = self.session.post(url, data={'file': path})

    def addExtension(self, extType, title, plotId, id, image=None):


        url = self.urlroot + "?cmd=pushExt" + "&plotId=" + plotId + "&id=" + id + "&extType=" + extType + "&Title=" + title
        if (image != None):
            url = url + "&image=" + image
        self.session.post(url, allow_redirects=True)

    def zoom(self, factor):
        return

    def pan(self, direction, factor):
        #do something
        return





