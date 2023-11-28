
import React, {useEffect} from 'react';
import {RouterProvider, useNavigate, redirect, useLocation} from 'react-router-dom';
import {dispatchNotifyRemoteAppReady, dispatchOnAppReady, dispatchSetMenu, FORM_CANCEL, FORM_SUBMIT} from '../../core/AppDataCntlr.js';
import {dispatchSetLayoutInfo, getDropDownInfo} from '../../core/LayoutCntlr.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import {FireflyRoot} from '../../ui/FireflyRoot.jsx';
import {getMenuItems} from '../../ui/Menu.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';

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
export function routeEntry(root, props) {
    const {getRouter} = props;

    window.firefly.ignoreHistory = true;
    dispatchSetMenu({menuItems: props.menu});
    dispatchNotifyRemoteAppReady();

    const view = (
        <FireflyRoot>
            <RouterProvider router={getRouter(props)} />
        </FireflyRoot>
    );

    root.render(view);
}


export function redirectOnMatch(pattern, url, {redirectTo, showDropDown = true}) {

    pattern = pattern instanceof RegExp ? pattern : new RegExp(pattern);

    const {pathname, search} = new URL(url);
    const queryStr = search ? '\\'+search : '';     // adding '\' to escape '?' at the beginning of search string
    if (pattern.test(pathname+queryStr)) {
        dispatchSetLayoutInfo({dropDown:{visible: showDropDown}});
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


/**
 * Custom hook to react to FORM_SUBMIT and FORM_CANCEL.
 * @param submitTo  path to navigate to when FORM_SUBMIT is dispatched.  This is analogous to the application's results view.
 *                  if submitTo is a function, it will be called with action passed in.  Function may return a path or null to do nothing.
 * @param onCancel  similar to submitTo, but for cancel.  Defaults to submitTo.
 */
export function useFormWatcher(submitTo='/?view', onCancel) {
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
    },[]);
}

function handleSubmit(submitTo, navigate, action) {
    let path = submitTo;
    if (submitTo instanceof Function) {
        path = submitTo(action?.payload, navigate);
    }
    if (path) {
        navigate(path);
    }
}

function handleCancel(onCancel, navigate) {
    let path = onCancel;
    if (onCancel instanceof Function) {
        path = onCancel();
    }
    if (path) {
        navigate(path);
    }
}

/**
 * Custom hook to convert Firefly's drop down navigation as routes
 */
export function useDropdownRoute() {
    const navigate = useNavigate();
    const {pathname} = useLocation();
    const view = useStoreConnector(() => getDropDownInfo()?.view);

    useEffect(() => {
        let cview = getMenuItems()?.find((mi) => pathname === mi.path)?.action;         // look for exact match
        cview ??= getMenuItems()?.find((mi) => pathname.startsWith(mi.path))?.action;   // look for nested path
        if (cview && view !== cview) {
            dispatchSetLayoutInfo({dropDown:{view: cview}});
            dispatchSetMenu({selected: cview});
        }
    }, [pathname]);

    useEffect(() => {
        if (view) {
            const path = getMenuItems()?.find((mi) => mi.action === view)?.path;
            if (path && !pathname.startsWith(path)) navigate(path);
        }
    }, [view]);
}

