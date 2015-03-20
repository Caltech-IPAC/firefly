#@PydevCodeAnalysisIgnore
#Lijun Zhang 03/16/16
#
import os
import requests
import webbrowser
import subprocess
import shutil


#global variables
urlroot='http://127.0.0.1:8080/fftools/sticky/CmdSrv'
gbid="mytag" 
actions={'fits': "pushFits",
            'reg': "pushRegion", 
            'table': "pushTable",
            'query': "pushQuery"
      }

def getUrlRoot():
    return urlroot

#initialize the push condition 
def init(bid=gbid):
    #send the url request to the server
    r=requests
    r.post(urlroot+"?cmd=createID&bid="+bid)
    #open the browser
    webbrowser.open('http://localhost:8080/fftools/app.html?#id=Loader&BID='+bid)
    return r

#push the file to display it at the browser initialized     
def push(pushType, fileName,  bid=gbid, r=None, path=None):
   
    if(r==None):
         r=init(bid)
    
    if(path==None):
        path=os.getcwd()+"/"  
    file=path+fileName  
    action = actions[pushType]     
    urlroot=getUrlRoot()
    r.post(urlroot +"?cmd="+action+"&bid="+bid,data={'file':file}, allow_redirects=True)
    return r

def killTomcat():
    grepCmd = "ps -aef | grep tomcat"
    grepResults = subprocess.check_output([grepCmd], shell=1).split()
    for i in range(1, len(grepResults), 9):
       pid = grepResults[i]
       killPidCmd = "kill -9 " + str(pid)
       subprocess.call([killPidCmd], shell=1)
  
def tomcat(process):
    if process == "start":
          #clean the cache
          cashe='/hydra/server/tomcat/temp/ehcache/'
          if os.path.exists( cashe):
                shutil.rmtree( cashe)
          subprocess.call(['/hydra/server/tomcat/bin/catalina.sh', 'start'])
    elif process == "stop":
             subprocess.call(['/hydra/server/tomcat/bin/catalina.sh', 'stop'])

def displayFits(filename, bid=gbid, path=None, location=None):
    return push( "fits",filename, path=path, r=location )   

#add the region to the existing image display
def addRegion(filename, location, path=None):
    return push("reg" ,filename, path=path, r=location)     



