
import React, {useEffect} from 'react';
import {oneOfType, string, func} from 'prop-types';
import {RouterProvider, useNavigate, redirect, useLocation} from 'react-router-dom';
import {isFunction} from 'lodash';
import {
    dispatchOnAppReady, dispatchSetMenu, FORM_CANCEL, FORM_SUBMIT, getMenu
} from '../../core/AppDataCntlr.js';
import {dispatchHideDropDown, dispatchSetLayoutInfo, getDropDownInfo} from '../../core/LayoutCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../../core/MasterSaga.js';
import {FireflyRoot} from '../../ui/FireflyRoot.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {dispatchComponentStateChange, getComponentState} from 'firefly/core/ComponentCntlr.js';

export const ROUTER = 'router';

const getMenuItems= () => getMenu()?.menuItems;

/**
 * React Router entry point.  See https://reactrouter.com/ for full details.
 * @param root      React root's component.
 * @param props     Firefly application's properties.
 * @param props.getRouter   function returning one of react-router's create*Router.  Have tested with  'createBrowserRouter' and 'createHashRouter'.
 *                          There's a limitation when using createBrowserRouter.  Firefly cannot support more than 1 level of subpath.
 *                          This is due to loading firefly_loader.js at the base path and we don't want to resolve it, yet.
 * @param props.menu        Will setup menu if exists.
 */
export function routeEntry(root, props) {
    const {getRouter} = props;

    window.firefly.ignoreHistory = true;

    const view = (
        <FireflyRoot ctxProperties={props}>
            <RouterProvider router={getRouter(props)} />
        </FireflyRoot>
    );

    root.render(view);
}

/*
  Used by a route's loader.  Not fully thought out, yet.  Avoid using it for now.
*/
export function redirectOnMatch(pattern, url, {redirectTo}) {

    pattern = pattern instanceof RegExp ? pattern : new RegExp(pattern);

    const {pathname, search} = new URL(url);
    const queryStr = search ? '\\'+search : '';     // adding '\' to escape '?' at the beginning of search string
    if (pattern.test(pathname+queryStr)) {
        return redirect(redirectTo);
    }
    return null;
}

/**
 * A wrapper component used to handle form actions, like submit and cancel
 * @param p             props
 * @param p.submitTo    path to navigate to when FORM_SUBMIT is dispatched.  This is analogous to the application's results view.
 *                      If submitTo is a function, it will be called with action passed in.  Function may return a path or null to do nothing.
 * @param p.onCancel    similar to submitTo, but for cancel
 * @param p.children
 * @return {object}
 * @constructor
 */
export function FormWatcher({submitTo, onCancel, children}) {
    useFormWatcher(submitTo, onCancel);
    return children;
}
FormWatcher.propTypes = {
    submitTo: oneOfType([string, func]),
    onCancel: oneOfType([string, func]),
};


/**
 * Custom hook to react to FORM_SUBMIT and FORM_CANCEL.
 * @param submitTo  path to navigate to when FORM_SUBMIT is dispatched.  This is analogous to the application's results view.
 *                  if submitTo is a function, it will be called with action passed in.  Function may return a path or null to do nothing.
 * @param onCancel  similar to submitTo, but for cancel.  Defaults to submitTo.
 */
function useFormWatcher(submitTo='/?results', onCancel) {
    const navigate = useNavigate();
    onCancel ??= submitTo;

    useEffect(()=> {
        const id = 'watchFormActions';
        dispatchOnAppReady(() => {
            dispatchAddActionWatcher({id, actions: [FORM_SUBMIT, FORM_CANCEL],
                callback: (action) => {
                    if (action?.type === FORM_SUBMIT) handleSubmit(submitTo, navigate, action);
                    if (action?.type === FORM_CANCEL) handleCancel(onCancel, navigate);
                }
            });
        });

        return (() => dispatchCancelActionWatcher(id));
    },[submitTo]);
}

/**
 * Custom hook to convert Firefly's drop down navigation as routes
 */
export function useDropdownRoute() {
    const navigate = useNavigate();
    const {pathname, search} = useLocation();
    const view = useStoreConnector(() => getDropDownInfo()?.view);
    const visible = useStoreConnector(() => getDropDownInfo()?.visible);

    useEffect(() => {
        const {path, visible} = getViewInfo();
        if (pathname !== path) {
            const menuItem = getMenuItem(pathname);
            if (menuItem) {     // the requested pathname is in the menu
                dispatchSetLayoutInfo({dropDown:{view: menuItem.action, menuItem, visible: true}});
                dispatchSetMenu({selected: menuItem.action});
            } else if (visible) {
                dispatchHideDropDown();     // it's not a menu path, hide dropdown
            }
        }
    }, [pathname]);         // when browser path changes, sync layout and menu state.

    useEffect(() => {
        const {path, visible} = getViewInfo();
        if (path && !pathname.startsWith(path)) {
            navigate(path);
        } else if( visible === false) {
            const lastResultPath = getResultsPath() || '/';
            navigate(lastResultPath);
        }
    }, [view]);             // when layout changes, navigate to that path

    return [visible, search];
}


/*---------------------------------------------------------------------------------------------
 Internal use only
----------------------------------------------------------------------------------------------*/

function handleSubmit(submitTo, navigate, action) {
    const path = isFunction(submitTo) ? submitTo?.(action?.payload, navigate) : submitTo;
    setResultsPath(path);
}

function handleCancel(onCancel, navigate) {
    const path = isFunction(onCancel) ? onCancel?.() : onCancel;
    setResultsPath(path);
}

function getViewInfo() {
    const {view, menuItem, visible} = getDropDownInfo();
    const path = menuItem?.path || getMenuItems()?.find((mi) => mi.action === view)?.path;
    return {path, visible};
}

function getMenuItem(path) {
    return getMenuItems()?.find((mi) => mi.path === path);
}

function setResultsPath(path) {
    if (path) dispatchComponentStateChange(ROUTER,{RESULTS:path});
}

function getResultsPath() {
    return getComponentState(ROUTER)?.RESULTS;
}