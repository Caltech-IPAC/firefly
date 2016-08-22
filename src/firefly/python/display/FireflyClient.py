from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
import time
import socket
import urlparse
import math

__author__ = 'zhang' + ' Cindy Wang'


class FireflyClient(WebSocketClient):
    # class variables
    # serverEvents = {
    #       'name': ['EVT_CONN_EST', 'SvrBackgroundReport', 'WindowResize'],
    #       'scope': ['SELF', 'CHANNEL'],
    #       'dataType': ['STRING', 'JSON', 'BG_STATUS'],
    #       'data': ['channel']
    # }

    fftools_cmd = '/firefly/sticky/CmdSrv'
    my_localhost = 'localhost:8080'
    ALL = 'ALL_EVENTS_ENABLED'

    # for serializing the RangeValues object
    STRETCH_TYPE_DICT = {'percent': 88, 'maxmin': 89, 'absolute': 90,  'zscale': 91, 'sigma': 92}
    STRETCH_ALGORITHM_DICT = {'linear': 44, 'log': 45, 'loglog': 46, 'equal': 47, 'squared': 48, 'sqrt': 49,
                              'asinh': 50, 'powerlaw_gamma': 51}

    # extension type
    EXTENSION_TYPE = ['AREA_SELECT', 'LINE_SELECT', 'POINT']

    # actions from Firefly
    ACTION_DICT = {
        'ShowFits': 'ImagePlotCntlr.PlotImage',
        'AddExtension': 'ExternalAccessCntlr/extensionAdd',
        'ShowTable': 'table.search',
        'ZoomImage': 'ImagePlotCntlr.ZoomImage',
        'PanImage': 'ImagePlotCntlr.recenter',
        'StretchImage': 'ImagePlotCntlr.StretchChange',
        'CreateRegionLayer': 'DrawLayerCntlr.RegionPlot.createLayer',
        'DeleteRegionLayer': 'DrawLayerCntlr.RegionPlot.deleteLayer',
        'AddRegionData': 'DrawLayerCntlr.RegionPlot.addRegion',
        'RemoveRegionData': 'DrawLayerCntlr.RegionPlot.removeRegion',
        'PlotMask': 'ImagePlotCntlr.plotMask',
        'DeleteOverlayMask': 'ImagePlotCntlr.deleteOverlayPlot'}

    # id for table, region layer, extension
    _item_id = {'Table': 0, 'RegionLayer': 0, 'Extension': 0, 'MaskLayer': 0}

    # the constructor, define instance variables for the object
    def __init__(self, host=my_localhost, channel=None):
        if host.startswith('http://'):
            host = host[7:]
        self.this_host = host
        url = 'ws://%s/firefly/sticky/firefly/events' % host  # web socket url
        if channel:
            url += '?channelID=%s' % channel
        WebSocketClient.__init__(self, url)
        self.url_root = 'http://' + host + self.fftools_cmd
        self.url_bw = 'http://' + self.this_host + '/firefly/firefly.html;wsch='
        self.listeners = {}
        self.channel = channel
        self.session = requests.Session()

        # print 'websocket url:%s' % url
        self.connect()

    # def opened(self):
    #     print ("Opening websocket connection to fftools")

    # def closed(self, code, reason=None):
    #     print ("Closed down", code, reason)

    def _handle_event(self, ev):
        for callback, eventIDList in self.listeners.items():
            if ev['name'] in eventIDList or FireflyClient.ALL in eventIDList:
                callback(ev)

    # overridde the superclass's method
    def received_message(self, m):
        ev = json.loads(m.data.decode('utf8'))
        event_name = ev['name']

        if event_name == 'EVT_CONN_EST':
            try:
                conn_info = ev['data']
                if self.channel is None:
                    self.channel = conn_info['channel']
                conn_id = ''
                if 'conn_id' in conn_info:
                    conn_id = conn_info['conn_id']
                seinfo = self.channel
                if (len(conn_id)) > 0:
                    seinfo = conn_id + '_' + seinfo

                self.session.cookies['seinfo'] = seinfo
            except:
                print ('from callback exception: ')
                print (m)
        else:
            self._handle_event(ev)

    def _send_url_as_get(self, url):
        """
        send URL in 'GET' request
        :param url:
        :return:
        """
        response = self.session.get(url)
        # print response.text
        status = json.loads(response.text)
        return status[0]

    def _send_url_as_post(self, data):
        """
        send URL in 'POST' request
        :param data
        :return:
        """
        response = self.session.post(self.url_root, data=data)
        status = json.loads(response.text)
        return status[0]

    def _is_page_connected(self):
        ip = socket.gethostbyname(socket.gethostname())
        url = self.url_root + '?cmd=pushAliveCheck&ipAddress=%s' % ip
        retval = self._send_url_as_get(url)
        return retval['active']

    # def onConnected(self, channel):
    #     #open the browser
    #     url = 'http://' + self.this_host + '/fftools/app.html?id=Loader&channelID=' + channel
    #     webbrowser.open('http://localhost:8080/fftools/app.html?id=Loader&channelID=' + channel)
    #     webbrowser.open(url)

    def _make_pid_param(self, plot_id):
        return ','.join(plot_id) if isinstance(plot_id, list) else plot_id

# -----------------------------------------------------------------
# -----------------------------------------------------------------
# Public API Begins
# -----------------------------------------------------------------
# -----------------------------------------------------------------

    def add_listener(self, callback, name=ALL):
        """
        Add a function to listen for events on the firefly client
        :param callback: set the function to be called when a event happens on the firefly client
        :param name: set the name of the events, default to all events
        :return:
        """
        if callback not in self.listeners.keys():
            self.listeners[callback] = []
        if name not in self.listeners[callback]:
            self.listeners[callback].append(name)

    def remove_listener(self, callback, name=ALL):
        """
        remove a callback
        :param callback: a previous set function that is to be removed
        :param name: set the name of the event, default to all events
        :return:
        """
        if callback in self.listeners.keys():
            if name in self.listeners[callback]:
                self.listeners[callback].remove(name)
            if len(self.listeners[callback]) == 0:
                self.listeners.pop(callback)

    def wait_for_events(self):
        """
        Pause and do not exit.  Wait over events from the server.
        This is optional. You should not use this method in ipython notebook
        Event will get called anyway.
        """
        WebSocketClient.run_forever(self)

    def get_firefly_url(self, mode=None, channel=None):
        """
        Get URL to Firefly Tools viewer and the channel set. Normally this method
        will be call without any parameters.
        :param mode: mode maybe one of 'full', or 'minimal'.  Defaults to minimal.
        :param channel: a different channel string than the default
        :return: the full url string
        """
        if not channel:
            channel = self.channel

        url = 'http://' + self.this_host + '/firefly/firefly.html?id=Loader&channelID='
        if mode == "full":
            url = self.url_bw
        return url + channel

    def launch_browser(self, url=None, channel=None, force=False):
        """
        Launch a browsers with the Firefly Tools viewer and the channel set. Normally this method
        will be call without any parameters.
        :param url: the url, overriding the default
        :param channel: a different channel than the default
        :return: the channel
        """
        if not channel:
            channel = self.channel
        if not url:
            url = self.url_bw
        do_open = True if force else not self._is_page_connected()
        if do_open:
            webbrowser.open(self.get_firefly_url(url, channel))
        time.sleep(5)  # todo: find something better to do than sleeping
        return channel

    def stay_connected(self):
        self.ws.run()

    def disconnect(self):
        self.close()

    def upload_file(self, path, pre_load=True):
        """ Upload a file to the Firefly Server
        :param path: uploaded file can be fits, region, and various types of table files
        """
        url = 'http://' + self.this_host + '/firefly/sticky/Firefly_FileUpload?preload=%s' % pre_load
        files = {'file': open(path, 'rb')}
        result = self.session.post(url, files=files)
        index = result.text.find('$')
        return result.text[index:]

    def upload_fits_data(self, stream):
        """
        Upload a FITS file like object to the Firefly server. The method should allows file like data
        to be streamed without using a actual file.
        :param stream: a file like object
        :return: status
        """
        url = 'http://' + self.this_host + '/firefly/sticky/Firefly_FileUpload?preload=true'
        data_pack = {'data': stream}
        result = self.session.post(url, files=data_pack)
        index = result.text.find('$')
        return result.text[index:]

    def upload_text_data(self, stream):
        """
        Upload a Text file like object to the Firefly server. The method should allows file like data
        to be streamed without using a actual file.
        :param stream: a file like object
        :return: status
        """
        return self.upload_data(stream, 'UNKNOWN')

    def upload_data(self, stream, data_type):
        url = 'http://' + self.this_host + '/firefly/sticky/Firefly_FileUpload?preload='
        url += 'true&type=FITS' if data_type == 'FITS' else 'false&type=UNKNOWN'
        data_pack = {'data': stream}
        result = self.session.post(url, files=data_pack)
        index = result.text.find('$')
        return result.text[index:]

    def dispatch_remote_action(self, channel, action_type, payload):
        """
        dispatch the action to the server by using 'GET' request
        :param: channel
        :param: actionType
        :param: payload: payload for the action
        """

        action = {'type': action_type, 'payload': payload}
        url = self.url_root + '?channelID=' + channel + '&cmd=pushAction&action='
        url += json.dumps(action)
        return self._send_url_as_get(url)

    def dispatch_remote_action_by_post(self, channel, action_type, payload):
        """
        dispatch the action to the server by using 'POST' request
        :param: channel
        :param: actionType
        :param: payload: payload for the action
        """
        action = {'type': action_type, 'payload': payload}
        data = {'channelID': channel, 'cmd': 'pushAction', 'action': json.dumps(action)}

        return self._send_url_as_post(data)

    # -------------------------
    # dispatch actions
    # -------------------------

    # ----------------------------------------------------------------
    # action on showing fits, tables, XYPlot and adding extension
    # ----------------------------------------------------------------

    def show_fits(self, file_on_server=None, plot_id=None, **additional_params):
        """
        Show a fits image
        :param: file_on_server: the is the name of the file on the server.  If you used upload_file()
                          then it is the return value of the method. Otherwise it is a file that
                          firefly has direct read access to.
        :param: plot_id: the id you assigned to the plot. This is necessary to further control the plot
        :param: additionalParam: dictionary of any valid fits viewer plotting parameters,
                          see firefly/docs/fits-plotting-parameters.md
        :return: status of call
        """
        wp_request = {'plotGroupId': 'groupFromPython',
                      'GroupLocked': False}
        payload = {'wpRequest': wp_request,
                   'useContextModifications': True,
                   'viewerId': 'DEFAULT_FITS_VIEWER_ID'}
        if plot_id:
            payload['wpRequest'].update({'plotId': plot_id})
        if file_on_server:
            payload['wpRequest'].update({'file': file_on_server})
        if additional_params:
            payload['wpRequest'].update(additional_params)
        return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['ShowFits'], payload)

    def show_table(self, file_on_server, tbl_id=None, title=None, page_size=100, is_catalog=True):
        """
        Show a table in Firefly
        :param file_on_server: the is the name of the file on the server.  If you used upload_file()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param tbl_id: table Id
        :param title: title on table
        :param page_size: how many rows are shown.
        :param is_catalog: catalog or table
        :return: status of call
        """

        if not tbl_id:
            tbl_id = FireflyClient._gen_item_id('Table')
        if not title:
            title = tbl_id
        tbl_type = 'table' if not is_catalog else 'catalog'

        tbl_req = {'startIdx': 0, 'pageSize': page_size, 'source': file_on_server, 'tblType': tbl_type,
                   'id': 'IpacTableFromSource', 'tbl_id': tbl_id}
        meta_info = {'title': title, 'tbl_id': tbl_id}
        tbl_req.update({'META_INFO': meta_info})
        payload = {'request': tbl_req}

        return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['ShowTable'], payload)

    def show_xyplot(self, file_on_server, **additional_params):
        """
        TODO
        Show a table in Firefly
        :param file_on_server: the is the name of the file on the server.  If you used upload_file()
                       then it is the return value of the method. Otherwise it is a file that
                       firefly has direct read access to.
        :param additional_params: XY Plot Viewer parameters
        :return: status of call
        """
        url = self.url_root + "?cmd=pushXYPlot"
        if additional_params:
            url += '&' + '&'.join(['%s=%s' % (k, v) for k, v in additional_params.items()])
        url += '&file=%s' % file_on_server
        return self._send_url_as_get(url)

    def add_extension(self, ext_type, plot_id=None, title='', tool_tip='',
                      extension_id=None, image_src=None):
        """
        TODO: callback
        Add an extension to the plot.  Extensions are context menus that allows you to extend
        what firefly can so when certain actions happen
        :param ext_type: May be 'AREA_SELECT', 'LINE_SELECT', or 'POINT'. todo: 'CIRCLE_SELECT'
        :param title: The title that the user sees
        :param plot_id: The it of the plot to put the extension on
        :param extension_id: The id of the extension
        :param image_src: image source of an icon to display the toolbar instead of title
        :return: status of call
        """

        if ext_type not in FireflyClient.EXTENSION_TYPE:
            ext_type = 'NONE'

        if not extension_id:
            extension_id = FireflyClient._gen_item_id('Extension')

        image_url = FireflyClient.create_image_url(image_src) if image_src else None

        extension = {'id': extension_id, 'plotId': plot_id, 'imageUrl': image_url,
                     'title': title, 'extType': ext_type, 'toolTip': tool_tip}
        payload = {'extension': extension}
        return self.dispatch_remote_action_by_post(self.channel, FireflyClient.ACTION_DICT['AddExtension'],
                                                   payload)

    # ----------------------------
    # actions on image
    # ----------------------------

    def set_zoom(self, plot_id, factor=1.0):
        """
        Zoom the image
        :param plot_id: plotId to which the region is added, parameter may be string or a list of strings.
        :param factor: number, zoom factor for the image
        :return:
        """

        def zoom_oneplot(plot_id, factor):
            payload = {'plotId': plot_id, 'userZoomType': 'LEVEL', 'level': factor, 'actionScope': 'SINGLE'}
            return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['ZoomImage'], payload)

        if isinstance(plot_id, tuple) or isinstance(plot_id, list):
            return [zoom_oneplot(x, factor) for x in plot_id]
        else:
            return zoom_oneplot(plot_id, factor)

    def set_pan(self, plot_id, x=None, y=None, coord='image'):
        """
        Pan or scroll the image to center on the image coordinates passed
        if no (x, y) is given, the image is recentered at the center of the image
        :param plot_id: plot_id to which the region is added, parameter may be string or a list of strings.
        :param x: number, new center x position to scroll to
        :param y: number, new center y position to scroll to
        :param coord: coordinate system, if coord == 'image', then x, y is for image pixel,
        :                                if coord == 'J2000', then x, y is ra, dec on EQ_J2000.
        :return:
        """

        payload = {'plotId': plot_id}
        if x and y:
            if coord.startswith('image'):
                payload.update({'centerPt': {'x': x, 'y': y, 'type': 'ImagePt'}})
            else:
                payload.update({'centerPt': {'x': x, 'y': y, 'type': 'J2000'}})

        return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['PanImage'], payload)

    def set_stretch(self, plot_id, type=None, algorithm=None, **additional_params):
        """
        Change the stretch of the image (no band case)
        :param type: 'percent','maxmin','absolute','zscale', or 'sigma'
        :param algorithm: 'linear', 'log','loglog','equal', 'squared', 'sqrt', 'asinh', 'powerlaw_gamma'
        :param plot_id: plotId to which the range is added, parameter may be string or a list of strings.
        :param additional_params:
                for type is 'zscale', kwargs: zscale_contrast, zscale_samples, zscale_samples_perline
                for type is others,   kwargs: lower_value, upper_value
        :return: status of call
        """

        if type and type.lower() == 'zscale':
            serialized_rv = self._create_rangevalues_zscale(algorithm, **additional_params)
        else:
            serialized_rv = self._create_rangevalues_standard(algorithm, type, **additional_params)

        st_data = [{'band': 'NO_BAND', 'rv': serialized_rv, 'bandVisible': True}]
        payload = {'stretchData': st_data, 'plotId': plot_id}

        return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['StretchImage'], payload)

    # -----------------------------------------------------------------
    # Region Stuff
    # -----------------------------------------------------------------

    def overlay_region_layer(self, file_on_server=None, region_data=None, title=None,
                             region_layer_id=None, plot_id=None):
        """
        Overlay a region layer on the loaded FITS images, the regions are defiend either by a file or
        by some text description
        :param file_on_server: the is the name of the file on the server.  If you used upload_file()
                                            then it is the return value of the method. Otherwise it
                                            is a file that firefly has direct read access to.
        :param region_data: region description, either a list of strings or a string
        :param title: title of the region file
        :param region_layer_id: id of layer to add
        :param plot_id: plotId to which this region should be added, parameter may be string or
                        a list of strings. If None then overlay region on all plots
        :return: status of call
        """

        if not region_layer_id:
            region_layer_id = FireflyClient._gen_item_id('RegionLayer')
        payload = {'drawLayerId': region_layer_id}

        if title:
            payload.update({'layerTitle': title})
        if plot_id:
            payload.update({'plotId': plot_id})

        if file_on_server:
            payload.update({'fileOnServer': file_on_server})
        elif region_data:
            payload.update({'regionAry': region_data})

        return self.dispatch_remote_action_by_post(
                self.channel, FireflyClient.ACTION_DICT['CreateRegionLayer'], payload)

    def delete_region_layer(self, region_layer_id, plot_id=None):
        """
        Delete region layer on the loaded FITS images
        :param region_layer_id: regionLayer to remove
        :param plot_id: plotId to which the region layer should be removed, if None,
                        then remove region layer from all plots
        :return: status of call
        """

        payload = {'drawLayerId': region_layer_id}
        if plot_id:
            payload.update({'plotId': plot_id})

        return self.dispatch_remote_action(self.channel,
                                           FireflyClient.ACTION_DICT['DeleteRegionLayer'], payload)

    def add_region_data(self, region_data, region_layer_id, title=None, plot_id=None):
        """
        Add the specified region entries
        :param region_data: a list of region entries
        :param region_layer_id: id of region to remove entries from
        :return: status of call
        """
        payload = {'regionChanges': region_data, 'drawLayerId': region_layer_id}
        if plot_id:
            payload.update({'plotId': plot_id})
        if title:
            payload.update({'layerTitle': title})

        return self.dispatch_remote_action_by_post(self.channel,
                                                   FireflyClient.ACTION_DICT['AddRegionData'], payload)

    def remove_region_data(self, region_data, region_layer_id):
        """
        Remove the specified region entries
        :param region_data: a list of region entries
        :param region_layer_id: id of region to remove entries from
        :return: status of call
        """
        payload = {'regionChanges': region_data, 'drawLayerId': region_layer_id}

        return self.dispatch_remote_action_by_post(self.channel,
                                                   FireflyClient.ACTION_DICT['RemoveRegionData'], payload)

    def add_mask(self,  bit_number, image_number, plot_id, mask_id=None, color=None, title=None,
                 file_on_server=None):
        """
        Add a mask layer
        :param image_number: imageNumber of the mask layer
        :param bit_number: bitNumber of the mask to overlay
        :param plot_id: plot id to overlay the mask on
        :param mask_id: id of mask
        :param color: color as an html color (eg. #FF0000 (red) #00FF00 (green)
        :param title: title of the mask layer
        :param file_on_server: (optional) file to get the mask from,
                                if None then get it from the original file
        :return: status of call
        """

        if not mask_id:
            mask_id = FireflyClient._gen_item_id('MaskLayer')
        if not title:
            title = 'bit %23 ' + str(bit_number)

        payload = {'plotId': plot_id, 'imageOverlayId': mask_id, 'imageNumber': image_number,
                   'maskNumber': bit_number, 'maskValue': int(math.pow(2, bit_number)), 'title': title}
        if color:
            payload.update({'color': color})
        if file_on_server:
            payload.update({'fileKey': file_on_server})

        return self.dispatch_remote_action(self.channel,
                                           FireflyClient.ACTION_DICT['PlotMask'], payload)

    def remove_mask(self, plot_id, mask_id):
        """
        Remove a mask layer
        :param plot_id: plot id of the overlay the mask is on
        :param mask_id: id of mask
        :return: status of call
        """

        payload = {'plotId': plot_id, 'imageOverlayId': mask_id}
        return self.dispatch_remote_action(self.channel,
                                           FireflyClient.ACTION_DICT['DeleteOverlayMask'], payload)

    # -----------------------------------------------------------------
    # Range Values
    # -----------------------------------------------------------------

    def _create_rv(self, stretch_type, lower_value, upper_value, algorithm,
                   zscale_contrast=25, zscale_samples=600, zscale_samples_perline=120,
                   beta_value=0.1, gamma_value=2.0):
        retval = None
        st = stretch_type.lower()
        a = algorithm.lower()
        if st in FireflyClient.STRETCH_TYPE_DICT and a in FireflyClient.STRETCH_ALGORITHM_DICT:
            retval = '%d,%f,%d,%f,%f,%f,%d,%d,%d,%d' % \
                   (FireflyClient.STRETCH_TYPE_DICT[st], lower_value,
                    FireflyClient.STRETCH_TYPE_DICT[st], upper_value,
                    beta_value, gamma_value,
                    FireflyClient.STRETCH_ALGORITHM_DICT[a],
                    zscale_contrast, zscale_samples, zscale_samples_perline)
        return retval

    def _create_rangevalues_standard(self, algorithm, stretch_type='Percent', lower_value=1, upper_value=99):
        """
        :param algorithm: must be 'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'
        :param stretch_type: must be 'Percent','Absolute','Sigma'
        :param lower_value: number, lower end of stretch
        :param upper_value: number, upper end of stretch
        :return: a serialized range values string
        """

        retval = self._create_rv(stretch_type, lower_value, upper_value, algorithm)
        if not retval:
            t = stretch_type if stretch_type.lower() in FireflyClient.STRETCH_ALGORITHM_DICT else 'percent'
            a = algorithm if algorithm.lower() in FireflyClient.STRETCH_ALGORITHM_DICT else 'linear'
            retval = self._create_rv(t, 1, 99, a)
        return retval

    def _create_rangevalues_zscale(self, algorithm, zscale_contrast=25,
                                   zscale_samples=600, zscale_samples_perline=120):
        """
        :param algorithm: must be 'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'
        :param zscale_contrast: zscale contrast
        :param zscale_samples: zscale samples
        :param zscale_samples_perline: zscale sample per line
        :return: a serialized range values string
        """
        retval = self._create_rv('zscale', 1, 1, algorithm,
                                 zscale_contrast, zscale_samples, zscale_samples_perline)
        if not retval:
            a = algorithm if algorithm.lower() in FireflyClient.STRETCH_ALGORITHM_DICT else 'linear'
            retval = self._create_rv('zscale', 1, 2, a, 25, 600, 120)
        return retval

    @classmethod
    def _gen_item_id(cls, item):
        """
        generate an ID for some entity like 'Table', 'RegionLayer', 'Extension'
        :param item: entity type
        :return: an ID string
        """

        if item in cls._item_id:
            cls._item_id[item] += 1
            return item + '-' + str(cls._item_id[item])
        else:
            return None

    @staticmethod
    def create_image_url(image_source):
        """
        create image url according to image source
        :param image_source: an image path or image url
        :return:
        """

        def is_url(url):
            return urlparse.urlparse(url).scheme != ''

        if not image_source.startswith('data:image') and not is_url(image_source):
            data_uri = open(image_source, 'rb').read().encode('base64').replace('\n', '')
            return 'data:image/png;base64,%s' % data_uri

        return image_source
