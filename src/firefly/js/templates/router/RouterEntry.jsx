
import React, {useCallback, useEffect} from 'react';
import {RouterProvider, useNavigate, redirect} from 'react-router-dom';
import {dispatchNotifyRemoteAppReady, dispatchOnAppReady, dispatchSetMenu} from '../../core/AppDataCntlr.js';
import {dispatchSetLayoutInfo, getLayouInfo} from '../../core/LayoutCntlr.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import {TABLE_SEARCH} from '../../tables/TablesCntlr.js';
import {MenuItem} from '../../ui/Menu.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';

export const ROUTER = 'router';

/**
 * React Router entry point.  See https://reactrouter.com/ for full details.
 * @param root      React root's component.
 * @param props     Firefly application's properties.
 * @param props.getRouter   function returning one of react-router's create*Router.  Have tested with  'createBrowserRouter' and 'createHashRouter'.
 *                          There's a limitation when using createBrowserRouter.  Firefly cannot support more than 1 level of subpath.
 *                          This is due to loading firefly_loader.js at the base path and we don't want to resolve it, yet.
 * @param props.menu        Will setup menu if exists.
 */
export function routerEntry(root, props) {
    const {getRouter} = props;

    window.firefly.ignoreHistory = true;
    dispatchSetMenu({menuItems: props.menu});
    dispatchNotifyRemoteAppReady();

    const view = (
        <RouterProvider router={getRouter(props)} />
    );

    root.render(view);
}


export function redirectOnMatch(pattern, url, {redirectTo, dropDown=true}) {

    pattern = pattern instanceof RegExp ? pattern : new RegExp(pattern);

    const {pathname, search} = new URL(url);
    if (pattern.test(pathname+search)) {
        dispatchSetLayoutInfo({dropDown:{visible: dropDown}});
        return redirect(redirectTo);
    }
    return null;
}

/**
 * Custom hook to mimic Firefly's current data flow
 * @param p
 * @param p.actions     actions to watch.  When encountered, hide dropdown then navigate to 'submitTo' path.
 * @param p.submitTo    path to navigate to when one of the given actions are caught.  This is analogous to the application's results view.
 *                      if submitTo is a function, it will be called with (action, navigate) instead.
 */
export function useSubmitActions({actions=[TABLE_SEARCH,ImagePlotCntlr.PLOT_IMAGE], submitTo}) {
    const navigate = useNavigate();
    const dropDownVisible = useStoreConnector(() => getLayouInfo()?.dropDown?.visible);

    useEffect(() => {
        if (!dropDownVisible) {
            submitTo && navigate(submitTo);
        }
    }, [dropDownVisible]);

    useEffect(()=> {
        dispatchOnAppReady(() => {
            dispatchAddActionWatcher({actions,
                callback: (action) => {
                    if (submitTo instanceof Function) {
                        submitTo(action, navigate);
                    }else {
                        dispatchSetLayoutInfo({dropDown:{visible: false}});
                    }
                }
            });
        });
    },[]);
}

export function RouteMenuItem({menuItem}) {
    const navigate = useNavigate();
    const clickHandler = useCallback(() => {
        const {path, options} = menuItem.action;
        navigate(path, options);
        dispatchSetLayoutInfo({dropDown:{visible: true}});
    });
    return <MenuItem menuItem={menuItem} clickHandler={clickHandler}/>;
}
