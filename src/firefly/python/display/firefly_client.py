"""
Module of firefly_client.py
--------------------------
This module defines class 'FireflyClient' and methods to remotely communicate to Firefly viewer
by dispatching remote actions.
"""
from __future__ import print_function
from future import standard_library
standard_library.install_aliases()
from builtins import str
from ws4py.client.threadedclient import WebSocketClient
import requests
import webbrowser
import json
import time
import socket
import urllib.parse
import math
import mimetypes
import base64

__docformat__ = 'restructuredtext'


class FireflyClient(WebSocketClient):
    """
    For Firefly client to build interface to remotely communicate to the Firefly viewer.

    methods
    -------
    add_listener(callback, name)
        Add listener to events on Firefly client.
    remove_listener(callback, name)
        Remove event name from callback listener.
    launch_browser()
        Launch a browsers with the Firefly Tools viewer and the channel set.
    wait_for_events()
        Wait over events from the server.
    get_firefly_url(mode, channel)
        Get URL to Firefly Tools viewer and the channel set.
    stay_connected()
        Keep WebSocket connected.
    disconnect()
        Disconnect the WebSocket.
    upload_file(path, pre_load)
        Upload a file to the Firefly Server.
    upload_fits_data(stream)
        Upload fits file like object to the Firefly server.
    upload_text_data(stream)
        Upload a text file like object to the Firefly server.
    upload_data(stream, data_type)
        Upload a file like object to the Firefly server
    create_image_url(image_source)
        Create image url or data uri.
    dispatch_remote_action(channel, action_type, payload)
        Dispatch the action to the server by sending 'GET' request.
    dispatch_remote_action_by_post(channel, action_type, payload)
        Dispatch the action to the server by sending 'POST' request.
    show_fits(file_on_server, plot_id, **additional_params)
        Show a fits image.
    show_table(file_on_server, tbl_id, title, page_size, is_catalog)
        Show a table.
    show_xyplot(file_on_server, **additional_params)
        Show a XY plot.
    add_extension(ext_type, plot_id, title, tool_tip, extension_id, image_src)
        Add extension to the plot.
    set_zoom(plot_id, factor)
        Zoom the image.
    set_pan(plot_id, x, y, coord)
        Relocate the center of the image.
    set_stretch(plot_id, type, algorithm, **additional_params)
        Change the stretch of the image (no band case).
    overlay_region_layer(file_on_server, region_data, title, region_layer_id, plot_id)
        Overlay a region layer on the image plot.
    delete_region_layer(region_layer_id, plot_id)
        Delete region layer from the image plot.
    add_region_data(region_data, region_layer_id, title, plot_id)
        Add region entries to the region layer.
    remove_region_data(region_data, region_layer_id)
        Remove region entries from the region layer.
    add_mask(bit_number, image_number, plot_id, mask_id, color, title, file_on_server)
        Add a mask layer to the image plot.
    remove_mask(plot_id, mask_id)
         Remove a mask layer from the image plot.

    Attributes
    ----------
    STRETCH_TYPE_DICT, STRETCH_ALGORITHM_DICT : dict
        Definition of stretch type and algorithm.
    ACTION_DICT : dict
        Definition of FIrefly action.
    EXTENSION_TYPE : list of str
        Type of plot where the extension is added to.
    """

    _fftools_cmd = '/firefly/sticky/CmdSrv'
    _my_localhost = 'localhost:8080'
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

    # urls:
    # launch browser:  http://<host>/firefly/firefly.html;wsch=<channel id> or (mode == 'full')
    #                  http://<host>/firefly/firefly.html;id=Loader&channelID=<channel id>
    # dispatch action: http://<host>/firefly/sticky/CmdSrv?channelID=<channel id>
    #                  &cmd=pushAction&Action=<ACTION_DICT>
    # open websocket:  ws://<host>/firefly/sticky/firefly/events?channdleID=<channel id>

    def __init__(self, host=_my_localhost, channel=None):
        """initialize a 'FireflyClient' object and build websocket.

        Parameters
        ----------
        host : str
            Firefly host.
        channel : str
            WebSocket channel id.
        """

        if host.startswith('http://'):
            host = host[7:]

        self.this_host = host

        url = 'ws://%s/firefly/sticky/firefly/events' % host  # web socket url
        if channel:
            url += '?channelID=%s' % channel
        WebSocketClient.__init__(self, url)

        self.url_root = 'http://' + host + self._fftools_cmd
        self.url_bw = 'http://' + self.this_host + '/firefly/firefly.html;wsch='

        self.listeners = {}
        self.channel = channel
        self.session = requests.Session()
        # print 'websocket url:%s' % url
        self.connect()

    def _handle_event(self, ev):
        for callback, eventIDList in self.listeners.items():
            if ev['name'] in eventIDList or FireflyClient.ALL in eventIDList:
                callback(ev)

    # override the superclass's method
    # serverEvents (message)
    # {
    #    'name': ['EVT_CONN_EST', 'SvrBackgroundReport', 'WindowResize'],
    #    'scope': ['SELF', 'CHANNEL'],
    #    'dataType': ['STRING', 'JSON', 'BG_STATUS'],
    #    'data': {'channel': , 'connID': }
    # }
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
                print('from callback exception: ')
                print(m)
        else:
            self._handle_event(ev)

    def _send_url_as_get(self, url):
        """Send URL in 'GET' request and return status."""

        response = self.session.get(url)
        status = json.loads(response.text)
        return status[0]

    def _send_url_as_post(self, data):
        """Send URL in 'POST' request and return status."""

        response = self.session.post(self.url_root, data=data)
        status = json.loads(response.text)
        return status[0]

    def _is_page_connected(self):
        """Check if the page is connected."""

        ip = socket.gethostbyname(socket.gethostname())
        url = self.url_root + '?cmd=pushAliveCheck&ipAddress=%s' % ip
        retval = self._send_url_as_get(url)
        return retval['active']

    @staticmethod
    def _make_pid_param(plot_id):
        return ','.join(plot_id) if isinstance(plot_id, list) else plot_id

# -----------------------------------------------------------------
# -----------------------------------------------------------------
# Public API Begins
# -----------------------------------------------------------------
# -----------------------------------------------------------------

    def add_listener(self, callback, name=ALL):
        """
        Add a function to listen for events on the Firefly client.

        Parameters
        ----------
        callback : function
            The function to be called when a event happens on the Firefly client.
        name : str, optional
            The name of the events (the default is ALL, all events).

        """

        if callback not in self.listeners.keys():
            self.listeners[callback] = []
        if name not in self.listeners[callback]:
            self.listeners[callback].append(name)

    def remove_listener(self, callback, name=ALL):
        """
        Remove a event name from the callback listener.

        Parameters
        ----------
        callback : function
            A previously set callback function.
        name : str, optional
            The name of the event to be removed from the callback listener
            (the default is ALL, all events).


        Notes
        -----
        The callback listener is removed if all events are removed from the callback.
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

    def get_firefly_url(self, mode='minimal', channel=None):
        """
        Get URL to Firefly Tools viewer and the channel set. Normally this method
        will be called without any parameters.

        Parameters
        -------------
        mode : {'full', 'minimal'}, optional
            Url mode (the default is 'minimal').
        channel : str, optional
            A different channel string than the default.

        Returns
        -------
        out : str
            url string.
        """

        if not channel:
            channel = self.channel

        url = 'http://' + self.this_host + '/firefly/firefly.html?id=Loader&channelID='
        if mode.lower() == "full":
            url = self.url_bw
        return url + channel

    def launch_browser(self, url=None, channel=None, force=False):
        """
        Launch a browser with the Firefly Tools viewer and the channel set.
        The page is launched when `force` is true or the page is not opened yet.
        Normally this method will be called without any parameters.

        Parameters
        ----------
        url : str, optional
            An url overriding the default (the default is set as self.url_bw).
        channel : str, optional
            A different channel than the default (the default is set as self.channel).
        force : bool, optional
            If the browser page is forced to be opened (the default is false).

        Returns
        -------
        out : str
            The channel ID.
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
        """Keep WebSocket connected."""
        self.run()

    def disconnect(self):
        """Disconnect the WebSocket."""
        self.close()

    def upload_file(self, path, pre_load=True):
        """
        Upload a file to the Firefly Server.

        Parameters
        ----------
        path : str
            Path of uploaded file. It can be fits, region, and various types of table files.
        pre_load : bool
            This parameter is not used.

        Notes
        -----
        'pre_load' is not implemented in the server (will be removed later).

        Returns
        -------
            Path of file after the upload.
        """

        url = 'http://' + self.this_host + '/firefly/sticky/Firefly_FileUpload?preload=%s' % pre_load
        files = {'file': open(path, 'rb')}
        result = self.session.post(url, files=files)
        index = result.text.find('$')
        return result.text[index:]

    def upload_fits_data(self, stream):
        """
        Upload a FITS file like object to the Firefly server. The method should allow file like data
        to be streamed without using a actual file.

        Parameters
        ----------
        stream: file-like object
            An file like object containing fits data,
            such as if *f = open(<a_fits_path>)*, *f* is a file object.

        Returns
        -------
        out : dict
            Status, like {'success': true}.
        """

        url = 'http://' + self.this_host + '/firefly/sticky/Firefly_FileUpload?preload=true'
        data_pack = {'data': stream}
        result = self.session.post(url, files=data_pack)
        index = result.text.find('$')
        return result.text[index:]

    def upload_text_data(self, stream):
        """
        Upload a Text file like object to the Firefly server. The method should allow text file like data
        to be streamed without using a actual file.

        Parameters
        ----------
        stream : file-like object
            An file like object containing text data,
            such as if *f = open(<a_textfile_path>)*, *f* is a file object.


        Returns
        -------
        out : dict
            Status, like {'success': true}.
        """
        return self.upload_data(stream, 'UNKNOWN')

    def upload_data(self, stream, data_type):
        """
        Upload a file like object to the Firefly server. The method should allow either fits or
        non-fits file like data to be streamed without using a actual file.

        Parameters
        ----------
        stream : file-like object
            An file like object containing fits data or others.
        data_type : {'FITS', 'UNKNOWN'}
            Data type, fits or others.

        Returns
        -------
        out : dict
            Status, like {'success': true}.
        """

        url = 'http://' + self.this_host + '/firefly/sticky/Firefly_FileUpload?preload='
        url += 'true&type=FITS' if data_type.upper() == 'FITS' else 'false&type=UNKNOWN'
        data_pack = {'data': stream}
        result = self.session.post(url, files=data_pack)
        index = result.text.find('$')
        return result.text[index:]

    @staticmethod
    def create_image_url(image_source):
        """
        Create image url or data uri according to the image source.

        Parameters
        ----------
        image_source : str
            An image path or image url.

        Returns
        -------
         out : str
            Data URI or image url.
        """

        def is_url(url):
            return urllib.parse.urlparse(url).scheme != ''

        if not image_source.startswith('data:image') and not is_url(image_source):
            mime, _ = mimetypes.guess_type(image_source)
            with open(image_source, 'rb') as fp:
                data = fp.read()
                data_uri = b''.join(base64.encodestring(data).splitlines())
                return 'data:%s;base64,%s' % (mime, data_uri)

        return image_source

    def dispatch_remote_action(self, channel, action_type, payload):
        """
        Dispatch the action to the server by using 'GET' request.

        Parameters
        ----------
        channel : str
            WebSocket channel id.
        action_type : str
            Action type,  one of actions from FireflyClient's attribute,  ACTION_DICT.
        payload : dict
            Payload, the content varies among action types.

        Returns
        -------
        out : dict
            Status of remote dispatch, like {'success': true}.
        """

        action = {'type': action_type, 'payload': payload}
        url = self.url_root + '?channelID=' + channel + '&cmd=pushAction&action='
        url += json.dumps(action)
        return self._send_url_as_get(url)

    def dispatch_remote_action_by_post(self, channel, action_type, payload):
        """
        Dispatch the action to the server by using 'POST' request.

        Parameters
        ----------
        channel : str
            Websocket channel id.
        action_type : str
            Action type, one of actions from FireflyClient's attribute, ACTION_DICT.
        payload : dict
            Payload, the content varies among action types.

        Returns
        -------
        out : dict
            Status of remotely dispatch, like {'success': true}.
        """

        action = {'type': action_type, 'payload': payload}
        data = {'channelID': channel, 'cmd': 'pushAction', 'action': json.dumps(action)}

        return self._send_url_as_post(data)

    # -------------------------
    # dispatch actions
    # -------------------------

    # --------------------------------------------------------------------------
    # action on showing fits, tables, XYPlot, adding extension, and adding mask
    # -------------------------------------------------------------------------

    def show_fits(self, file_on_server=None, plot_id=None, **additional_params):
        """
        Show a fits image.

        Parameters
        ----------
        file_on_server : str, optional
            The is the name of the file on the server.  If you use upload_file()
            then it is the return value of the method. Otherwise it is a file that
            Firefly has direct access to.
        plot_id : str or list of str, optional
            The id you assign to the image plot. This is necessary to further control the plot.

        additional_params : dict, optional
            Dictionary of any valid fits viewer plotting parameters,
            see `fits plotting
            parameters<https://github.com/Caltech-IPAC/firefly/blob/dev/docs/fits-plotting-parameters.md>`_.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
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
        Show a table.

        Parameters
        ----------
        file_on_server : str, optional
            The is the name of the file on the server.  If you use upload_file()
            then it is the return value of the method. Otherwise it is a file that
            Firefly has direct access to.
        tbl_id : str, optional
            A table ID. It will be created automatically if not specified.
        title : str, optional
            Title associated with the table.
        page_size : int, optional
            The number of rows that are shown in the table page (the default is 100).
        is_catalog : bool, optional
            If the table file is a catalog (the default is true) or not.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
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
        Show a XY plot

        Parameters
        ----------
        file_on_server : str, optional
            The is the name of the file on the server.  If you use upload_file()
            then it is the return value of the method. Otherwise it is a file that
            Firefly has direct access to.
        **additional_params
            Additional parameters for XY Plot, please see the details in 'Other Parameters'.

        Other Parameters
        ----------------
        source: str
            location of the ipac table, url or file path; ignored when XY plot view is added to table.
        chartTitle : str
            title of the chart.
        xCol: str
            column or expression to use for x values, can contain multiple column names,
            ex. log(col) or (col1-col2)/col3.
        yCol: str
            column or expression to use for y values, can contain multiple column names,
            ex. sin(col) or (col1-col2)/col3.
        xyRatio : numeric types
            Aspect ratio (must be between 1 and 10).
        stretch : {'fit', 'fill'}
            Stretch method.
        xLabel : str
            label to use with x axis.
        yLabel : str
            label to use with y axis.
        xUnit : str
            unit for x axis.
        yUnit : str
            unit for y axis.
        xOptions : str
            Comma separated list of x axis options: grid,flip,log.
        yOptions : str
            Comma separated list of y axis options: grid,flip,log.

        Notes
        -----
            For the additional parameters, xCol and yCol are required, then all other
            parameters are valid.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
        """

        url = self.url_root + "?cmd=pushXYPlot"
        if additional_params:
            url += '&' + '&'.join(['%s=%s' % (k, v) for k, v in additional_params.items()])
        url += '&file=%s' % file_on_server
        return self._send_url_as_get(url)

    def add_extension(self, ext_type, plot_id=None, title='', tool_tip='',
                      extension_id=None, image_src=None):
        """
        Add an extension to the plot. Extensions are context menus that allows you to extend
        what Firefly can do when certain actions happen.

        Parameters
        ----------
        ext_type : {'AREA_SELECT', 'LINE_SELECT', or 'POINT'}
            Extension type. It can be one of the values in the list,
            or it will be reset to be 'NONE'.
        plot_id : str, optional
            Plot ID of the plot which the extension is added to, if not specified, then this request
            applied to all plots in the same group of the active plot.
        title : str, optional
            The title for the extension.
        tool_tip : str, optional
            Tooltip for the extension.
        extension_id : str, optional
            Extension ID. It will be created automatically if not specifed.
        image_src : str, optional
            Image source of an icon to be displayed on the toolbar instead of the title.
            Image source could be an image path or an image url.

        Notes
        -----
        If image_src is not specified, then no extension is added.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
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
        Zoom the image.

        Parameters
        ----------
        plot_id : str or a list of str
            ID of the plot to be zoomed. If plot_id is a list or tuple, then each plot in the list
            or the tuple is zoomed in order.
        factor : numeric type
            Zoom factor for the image.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
        """

        def zoom_oneplot(one_plot_id, f):
            payload = {'plotId': one_plot_id, 'userZoomType': 'LEVEL', 'level': f, 'actionScope': 'SINGLE'}
            return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['ZoomImage'], payload)

        if isinstance(plot_id, tuple) or isinstance(plot_id, list):
            return [zoom_oneplot(x, factor) for x in plot_id]
        else:
            return zoom_oneplot(plot_id, factor)

    def set_pan(self, plot_id, x=None, y=None, coord='image'):
        """
        Relocate the image to center on the given image coordinate or EQ_J2000 coordinate.
        If no (x, y) is given, the image is recentered at the center of the image.

        Parameters
        ----------
        plot_id : str or a list of str
            ID of the plot to be panned. If plot_id is a list or tuple, then each plot in the list
            or the tuple is panned in order.
        x, y : numeric type
            New center of x and y position to scroll to.
        coord: {'image', 'J2000'}, optional
            Coordinate system (the default is 'image').

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
        """

        payload = {'plotId': plot_id}
        if x and y:
            if coord.startswith('image'):
                payload.update({'centerPt': {'x': x, 'y': y, 'type': 'ImagePt'}})
            else:
                payload.update({'centerPt': {'x': x, 'y': y, 'type': 'J2000'}})

        return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['PanImage'], payload)

    def set_stretch(self, plot_id, stype=None, algorithm=None, **additional_params):
        """
        Change the stretch of the image (no band case).

        Parameters
        ----------
        plot_id : str or a list of str
            ID of the plot to be panned. If plot_id is a list or tuple, then each plot in the list
            or the tuple is stretched in order.
        stype : {'percent','maxmin','absolute','zscale', 'sigma'}, optional
            Stretch method (the default is 'percent').
        algorithm : {'linear', 'log','loglog','equal', 'squared', 'sqrt', 'asinh', 'powerlaw_gamma'}, optional
            Stretch algorithm (the default is 'linear').
        **additional_params
            Additional parameters for image stretch. Please see the details in 'Other Parameters'.

        Other Parameters
        ----------------
        zscale_contrast : numeric type
            zscale contrast (the default is 25).
        zscale_samples : int
            zscale samples, int (the default is 600).
        zscale_samples_perline : int
            zscale samples per line (the default is 120).

        lower_value : numeric type
            Lower end of stretch (the default is 1).
        uppler_value : numeric type
            Upper end of stretch (the default is 90).

        Notes
        -----
            `zscale_contrast`, `zscale_samples`, and `zscale_samples_perline` are for
            `stype = 'zscale'` case, and `lower_value`, and `upper_value` are for other type cases.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
        """

        if stype and stype.lower() == 'zscale':
            serialized_rv = self._create_rangevalues_zscale(algorithm, **additional_params)
        else:
            serialized_rv = self._create_rangevalues_standard(algorithm, stype, **additional_params)

        st_data = [{'band': 'NO_BAND', 'rv': serialized_rv, 'bandVisible': True}]
        payload = {'stretchData': st_data, 'plotId': plot_id}

        return self.dispatch_remote_action(self.channel, FireflyClient.ACTION_DICT['StretchImage'], payload)

    # -----------------------------------------------------------------
    # Region Stuff
    # -----------------------------------------------------------------

    def overlay_region_layer(self, file_on_server=None, region_data=None, title=None,
                             region_layer_id=None, plot_id=None):
        """
        Overlay a region layer on the loaded FITS images. The regions are defined either by a file or
        by text region description.

        Parameters
        ----------
        file_on_server : str, optional
            This is the name of the file on the server.  If you used upload_file()
            then it is the return value of the method. Otherwise it
            is a file that Firefly has direct read access to.
        region_data: str or list of str, optional
            Region description, either a list of strings or a string.
        title : str, optional
            Title of the region layer.
        region_layer_id : str, optional
            ID of the layer to be created. It is automatically created if not specified.
        plot_id : str or a list of str, optional
            ID of the plot that the region layer is created on.
            If None,  then overlay region(s) on all plots in the same group of the active plot.

        Notes
        -----
        file_on_server and region_data are exclusively required.
        If both are specified, file_on_server takes the priority.
        If none is specified, no region layer is created.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
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
        Delete region layer from the loaded FITS images.

        Parameters
        ----------
        region_layer_id : str
            Region layer with the region_layer_id to be removed.
        plot_id : str or a list of str
            Plot ID. The region layer is removed from the plot with the plot id.
            If not specified, then remove region layer from all plots in the same group of the active plot.

        Returns
        -------
         out : dict
            Status of the request, like {'success': true}.
        """

        payload = {'drawLayerId': region_layer_id}
        if plot_id:
            payload.update({'plotId': plot_id})

        return self.dispatch_remote_action(self.channel,
                                           FireflyClient.ACTION_DICT['DeleteRegionLayer'], payload)

    def add_region_data(self, region_data, region_layer_id, title=None, plot_id=None):
        """
        Add region entries to a region layer with the given ID.

        Parameters
        ----------
        region_data : str or a list of str
            Region entries to be added.
        region_layer_id : str
            ID of region layer where the entries are added to.
        title : str, optional
            Title of the region layer. If the layer exists, the original title is replaced.
            If the layer doesn't exist, a new layer with the given title is created.
        plot_id : str or a list of str, optional
            Plot ID. This is for the case that the region layer doesn't exist.
            If the region layer exists, this request applies to all plots attached to the layer.

        Notes
        -----
            If no region layer with the given ID exists, a new region layer will be created
            automatically just like how function 'overlay_region_layer' works.

        Returns
        -------
        out : dict
            Status of the request, like {'success': true}.
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
        Remove region entries from a region layer with the give ID.

        Parameters
        ----------
        region_data : str or a list of str
            Region entries to be removed.
        region_layer_id : str
            ID of the region layer where the region entries are removed from.

        Returns
        --------
        out : dict
            Status of the request, like {'success': true}.
        """
        payload = {'regionChanges': region_data, 'drawLayerId': region_layer_id}

        return self.dispatch_remote_action_by_post(self.channel,
                                                   FireflyClient.ACTION_DICT['RemoveRegionData'], payload)

    def add_mask(self,  bit_number, image_number, plot_id, mask_id=None, color=None, title=None,
                 file_on_server=None):
        """
        Add a mask layer.

        Parameters
        ----------
        image_number : int
            Image number of the mask layer, HDU extension in fits.
        bit_number : int
            Bit number of the mask to overlay.
        plot_id : str
            ID of the plot to overlay the mask on.
        mask_id : str, optional
            Mask ID. It will be created automatically if not specified.
        color : str, optional
            Color as an html color (eg. #FF0000 (red) #00FF00 (green). A color will be
            created in default if not specified.
        title : str, optional
            Title of the mask layer.
        file_on_server : str, optional
            File to get the mask from. The mask will be taken from the original file if not specified.

        Returns
        --------
        out : dict
            Status of the request, like {'success': true}.
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
        Remove a mask layer from the plot with the given plot ID.

        Parameters
        ----------
        plot_id : str
            ID of the plot where the mask layer to be removed from.
        mask_id: str
            ID of the mask layer to be removed.

        Returns
        --------
        out : dict
            Status of the request, like {'success': true}
        """

        payload = {'plotId': plot_id, 'imageOverlayId': mask_id}
        return self.dispatch_remote_action(self.channel,
                                           FireflyClient.ACTION_DICT['DeleteOverlayMask'], payload)

    # -----------------------------------------------------------------
    # Range Values
    # -----------------------------------------------------------------

    @staticmethod
    def _create_rv(stretch_type, lower_value, upper_value, algorithm,
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
        Create range values for non-zscale cases.

        Parameters
        -----------
        algorithm : {'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'}
            Stretch algorithm.
        stretch_type : {'Percent','Absolute','Sigma'}
            Stretch type.
        lower_value: numeric type
            Lower end of stretch.
        upper_value: numeric type
            Upper end of stretch

        Returns
        -------
        out : str
            a serialized range values string
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
        Create range values for zscale case.

        Parameters
        ----------
        algorithm: {'Linear', 'Log','LogLog','Equal','Squared', 'Sqrt'}
            Stretch algorithm.
        zscale_contrast: numeric type
            Zscale contrast.
        zscale_samples: int
            Zscale samples
        zscale_samples_perline: int
            Zscale samples per line

        Returns
        -------
        out : str
            a serialized range values string
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
        Generate an ID for table, region layer, or extension entity.

        Parameters
        ----------
        item : {'Table', 'RegionLayer', 'Extension'}
            Entity type.

        Returns
        -------
        out : str
            ID string.
        """

        if item in cls._item_id:
            cls._item_id[item] += 1
            return item + '-' + str(cls._item_id[item])
        else:
            return None
