/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

export const APP_LOAD = 'APP_LOAD';
export const APP_UPDATE = 'APP_UPDATE';



function getInitState() {
    return {
        isReady : false
    };
}

export function reducer(state=getInitState(), action={}) {

    switch (action.type) {
        case (APP_LOAD)  :
            // adding a dummy sideEffect to test feature
            action.sideEffect( (dispatch) => fetchAppData(dispatch, 'fftools_v1.0.1 Beta WITH SIDE-EFFECT added.', 4) );
            return getInitState();

        case (APP_UPDATE)  :
            return Object.assign({}, state, action.payload);

        default:
            return state;
    }

}

export function loadAppData() {

    return function (dispatch) {
        dispatch({ type : APP_LOAD });
        fetchAppData(dispatch, 'fftools_v1.0.1 Beta', 2);
    };
}

/**
 *
 * @param appData {Object} The partial object to merge with the appData branch under root
 * @returns {{type: string, payload: object}}
 */
export function updateAppData(appData) {
    return { type : APP_UPDATE, payload: appData };
}

function fetchAppData(dispatch, version, waitSec) {

    setTimeout(function () {
        var mockData = {
            isReady: true,
            props: {
                version
            }
        };
        dispatch( updateAppData(mockData) );
    }, waitSec * 1000);

    //ServerApi.loadAppData()
    //    .then((data) => {
    //        dispatch(updateAppData(data));
    //    })
    //    .catch((errorMessage) => {
    //        var data = {
    //            isReady: false,
    //            errorMessage: errorMessage
    //        };
    //        dispatch(updateAppData(data));
    //    });
}
