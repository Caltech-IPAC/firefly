/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {keyMap, computeCurrentCatalogId,
        IRSA, TWOMASS, WISE, MSX, DSS, SDSS, FITS, URL, BLANK} from './ImageSelectPanel.jsx';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchPlotImage, visRoot } from '../ImagePlotCntlr.js';
import {dispatchAddImages} from '../MultiViewCntlr.js';
import {parseWorldPt} from '../Point.js';
import {panelCatalogs} from './ImageSelectPanelProp.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {sizeFromDeg} from '../../ui/sizeInputFields.jsx';
import {get} from 'lodash';
import {dispatchHideDropDownUi} from '../../core/LayoutCntlr.js';

const loadErrorMsg = {
    'nosize': 'no valid size is specified',
    'nopixelsize': 'no valid pixel size is specified',
    'notarget': 'no valid target name or position is specified',
    'nourl': 'no valid url of fits file is specified',
    'nofits': 'no fits file uploaded',
    'failplot': 'fail to make plot request',
    'noplot': 'no active plot folund to replace'
};

function *newPlotIdMaker() {
    var imgNo = 0;
    const plotName = 'SELECTIMAGEPANEL_imgPlot';

    while (true) {
        yield `${plotName}${(++imgNo)}`;
    }
}

var plotidGen = newPlotIdMaker();


var outputMessage = (errMsg) => errMsg&&showInfoPopup(errMsg, 'Load Selected Image Error');

// image plot on specified url
function imagePlotOnURL(request) {
    var url = get(request, keyMap['urlinput']);
    var wpr = WebPlotRequest.makeURLPlotRequest(url);


    if (wpr && request[keyMap['urllist']] === 'loadOne' && request.hasOwnProperty(keyMap['urlextinput'])) {
        wpr.setMultiImageIdx(request[keyMap['urlextinput']]);
    }
    return wpr;
}

// image plot on specified upload FITS
function imagePlotOnFITS(request) {
    var fits = get(request, keyMap['fitsupload']);
    var wpr = WebPlotRequest.makeFilePlotRequest(fits);


    if (wpr && request[keyMap['fitslist']] === 'loadOne' && request.hasOwnProperty(keyMap['fitsextinput'])) {
          wpr.setMultiImageIdx(request[keyMap['fitsextinput']]);
    }
    return wpr;
}



// image plot on blank image
function imagePlotOnBlank(request) {
    var sizeV = request[keyMap['blankinput']];
    var size = request[keyMap['radiusfield']];

    return WebPlotRequest.makeBlankPlotRequest(
        parseWorldPt(request.UserTargetWorldPt), sizeFromDeg(size, 'arcsec'),
                     sizeV, sizeV);
}

// image plot on IRSA, 2MASS, WISE, MSX, DSS, SDSS
function imagePlotOnSurvey(crtCatalogId, request) {

    var wp = parseWorldPt(request.UserTargetWorldPt);
    var sym = panelCatalogs[crtCatalogId].Symbol.toLowerCase();

    var t = `${sym}types`;
    var b = `${sym}bands`;
    var survey = get(request, keyMap[t]);
    var band = get(request, keyMap[b]);
    var sizeInDeg = parseFloat(request[keyMap['radiusfield']]);

    var wpr = null;

    switch(crtCatalogId) {
        case IRSA:
            var s = (survey) ? survey.split('-')[1]: '';

            if (survey.includes('issa')) {
                wpr = WebPlotRequest.makeISSARequest(wp, s, sizeInDeg);
            } else if (survey.includes('iris')) {
                wpr = WebPlotRequest.makeIRISRequest(wp, s, sizeInDeg);
            }

            break;

        case TWOMASS:
            wpr = WebPlotRequest.make2MASSRequest(wp, survey, sizeInDeg);
            break;

        case WISE:
            if (band) {
                wpr = WebPlotRequest.makeWiseRequest(wp, survey, band, sizeInDeg);
            }
            break;

        case MSX:
            wpr = WebPlotRequest.makeMSXRequest(wp, survey, sizeInDeg);
            break;

        case DSS:
            wpr = WebPlotRequest.makeDSSRequest(wp, survey, sizeInDeg);
            break;

        case SDSS:
            wpr = WebPlotRequest.makeSloanDSSRequest(wp, survey, sizeInDeg);
            break;
        default:
            break;
    }

    return wpr;
}

/*
 * onFail callback and blank input checker for request sent to onSuccess
 */
export function resultFail(fromFail = true) {
    return (request) => {
        var crtCatalogId = computeCurrentCatalogId(request);
        var errCode = '';
        var errMsg = '';
        const skey = 'radiusfield';

        switch (crtCatalogId) {
            case URL:
                if (fromFail) {
                    errMsg = 'invalid url';
                } else {
                    if (!get(request, keyMap['urlinput'])) {
                        errCode = 'nourl';
                    }
                }
                break;

            case FITS:
                if (fromFail) {
                    errMsg = 'invalid file upload';
                } else {
                    if (!get(request, keyMap['fitsupload'])) {
                        errCode = 'nofits';
                    }
                }
                break;

            case BLANK:
                if (fromFail) {
                    errMsg = 'invalid name or position or invalid pixel size or invalid size';
                } else {
                    if (!request.hasOwnProperty('UserTargetWorldPt') || !request.UserTargetWorldPt) {
                        errCode = 'notarget';
                    } else if (!get(request, keyMap['blankinput'])) {
                        errCode = 'nopixelsize';
                    } else if (!get(request, keyMap[skey])) {
                        errCode = 'nosize';
                    }
                }
                break;

            default:
                if (fromFail) {
                    errMsg = 'invalid name or position or invalid size';
                } else {
                    if (!request.hasOwnProperty('UserTargetWorldPt') || !request.UserTargetWorldPt) {
                        errCode = 'notarget';
                    } else if (!get(request, keyMap[skey])) {
                        errCode = 'nosize';
                    }
                }
        }

        if (errCode) {
            errMsg = loadErrorMsg[errCode];
        }
        if (errMsg) {
            outputMessage(errMsg);
        }

        return errMsg;
    };
}

/*
 * onSucess callback for 'load' button on image select panel
 */
export function resultSuccess(isAddNewPlot, viewerId, hideDropdown = false) {
    return (request) => {
        var wpr = null;
        const crtCatalogId = computeCurrentCatalogId(request);

        var errMsg = resultFail(false);
        if (errMsg(request)) {
            return;
        }

        switch (crtCatalogId) {
            case URL:
                wpr = imagePlotOnURL(request);
                break;
            case FITS:
                wpr = imagePlotOnFITS(request);
                break;
            case BLANK:
                wpr = imagePlotOnBlank(request);
                break;
            default:
                wpr = imagePlotOnSurvey(crtCatalogId, request);
        }

        if (wpr) {
            var nPlotId;

            if (isAddNewPlot && viewerId) {
                nPlotId = plotidGen.next().value;
                dispatchAddImages(viewerId, [nPlotId]);
            } else {
                nPlotId = get(visRoot(), 'activePlotId');
                if (!nPlotId) {
                    outputMessage('noplotid');
                }
            }

            dispatchPlotImage(nPlotId, wpr);

            if (hideDropdown) {
                dispatchHideDropDownUi();
            }
        } else {
            outputMessage('failplot');
        }
    };
}

