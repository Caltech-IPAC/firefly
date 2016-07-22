__author__ = 'zhang'
import sys
import os
sys.path.append('/hydra/cm/firefly/src/fftools/python/display/')

from FireflyClient import *


path= os.getcwd() + '/data/'

#create a FireflyClient instance
fc =FireflyClient()

#push a fits file
raw_input("Load a FITS file.   Press Enter to continue...")
fc.showFits( fc.uploadImage(path+"c.fits") )
