/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import ReactDOM from 'react-dom';
import {get} from 'lodash';

import {firefly} from './Firefly.js';
import {FireflyViewer} from './core/FireflyViewer.js';

/**
 * By default, firefly.js will startup in api mode.
 * If you want it to startup as an application, you need to at least
 * define a firefly.app options variable under window.
 *
 * Options will be passed into <Application>'s props.
 * By default, the application will be mounted to the 'app' div.
 * Use firefly.app.div to set it to something else.
 * Read Application's doc for more details the firefly.app properties.
 *
 *
 * Below is an example of how to startup FireflyViewer with 2 menu items in an 'image_table' view.
 *   <script type="text/javascript" language='javascript'>
 *      const menu = [{label:'Search', action:'TestSearches'},
 *                    {label:'Images', action:'ImageSelectDropDownCmd'}];
 *      window.firefly = {app: {views: 'image_table', menu}};
 *   </script>
 *
 */


firefly.bootstrap();

const app = get(window, 'firefly.app');

if (app) {
    const defProps = {
        appTitle: 'Firefly',
        views: 'tri_view',
        div: 'app',
        menu: [ {label:'Data Sets: Catalogs & Images', action:'AnyDataSetSearch'},
                {label:'Catalogs CLASSIC', action:'IrsaCatalogDropDown'},
                {label:'Test Searches', action:'TestSearches'},
                {label:'Images', action:'ImageSelectDropDownCmd'},
                {label:'Help', action:'overviewHelp', type:'COMMAND'},
                {label:'Example Js Dialog', action:'exampleDialog', type:'COMMAND'}
        ]
    };
    const props = Object.assign({}, defProps, app);

    ReactDOM.render(React.createElement(FireflyViewer, props),
        document.getElementById(props.div));
}


