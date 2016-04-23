/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {keyMap, panelKey, computeCurrentCatalogId, rgbFieldGroup,
        IRAS, TWOMASS, WISE, MSX, DSS, SDSS, FITS, URL, NONE,
        PLOT_NO, RED, GREEN, BLUE, PLOT_CREATE, PLOT_CREATE3COLOR, rgb} from './ImageSelectPanel.jsx';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchPlotImage, visRoot } from '../ImagePlotCntlr.js';
import {dispatchAddImages} from '../MultiViewCntlr.js';
import {parseWorldPt} from '../Point.js';
import {panelCatalogs} from './ImageSelectPanelProp.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {sizeFromDeg} from '../../ui/SizeInputField.jsx';
import {get} from 'lodash';
import {dispatchHideDropDownUi} from '../../core/LayoutCntlr.js';
import {getPlotViewById} from '../PlotViewUtil.js';

const loadErrorMsg = {
    'nosize': 'no valid size is specified',
    'nopixelsize': 'no valid pixel size is specified',
    'notarget': 'no valid target name or position is specified',
    'nourl': 'no valid url of fits file is specified',
    'nofits': 'no fits file uploaded',
    'failplot': 'fail to make plot request',
    'noplotid': 'no plot id or group id found',
    'noplot': 'no plot to replace or create',
    'norgbselect': 'no image is selected'
};

function *newPlotIdMaker() {
    var imgNo = 0;
    const plotName = 'ImPanel_';

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
    var size = request[keyMap['sizefield']];

    return WebPlotRequest.makeBlankPlotRequest(
        parseWorldPt(request.UserTargetWorldPt), sizeFromDeg(size, 'arcsec'),
                     sizeV, sizeV);
}

// image plot on IRAS, 2MASS, WISE, MSX, DSS, SDSS
function imagePlotOnSurvey(crtCatalogId, request) {

    var wp = parseWorldPt(request.UserTargetWorldPt);
    var sym = panelCatalogs[crtCatalogId].Symbol.toLowerCase();

    var t = `${sym}types`;
    var b = `${sym}bands`;
    var survey = get(request, keyMap[t]);
    var band = get(request, keyMap[b]);
    var sizeInDeg = parseFloat(request[keyMap['sizefield']]);

    var wpr = null;

    switch(crtCatalogId) {
        case IRAS:
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
        var crtCatalogId = computeCurrentCatalogId(request[panelKey],
                [request[rgbFieldGroup[RED]], request[rgbFieldGroup[GREEN]], request[rgbFieldGroup[BLUE]]]);
        var isThreeColor = !request[panelKey].hasOwnProperty(keyMap['catalogtab']);

        var validate = (fg, cId, rgbNote = '') => {
            var errMsg = '';
            var msg = (code, note = '') => `${note}${loadErrorMsg[code]}`;
            const skey = 'sizefield';

            // error message is for the validation made by complete button callback
            // error code is for checking the empty entry passed by request
            switch (cId) {
                case URL:
                    if (fromFail) {
                        errMsg = 'invalid url';
                    } else {
                        if (!get(fg, keyMap['urlinput'])) {
                            errMsg = msg('nourl', rgbNote);
                        }
                    }
                    break;

                case FITS:
                    if (fromFail) {
                        errMsg = 'invalid file upload';
                    } else {
                        if (!get(fg, keyMap['fitsupload'])) {
                            errMsg = msg('nofits', rgbNote);
                        }
                    }
                    break;
/*
                case BLANK:
                    if (fromFail) {
                        errMsg = 'invalid name or position or invalid pixel size or invalid size';
                    } else {
                        if (!fg.hasOwnProperty('UserTargetWorldPt') || !fg.UserTargetWorldPt) {
                            errMsg = msg('notarget');
                        } else if (!get(fg, keyMap['blankinput'])) {
                            errMsg = msg('nopixelsize', rgbNote);
                        } else if (!get(fg, keyMap[skey])) {
                            errMsg = msg('nosize');
                        }
                    }
                    break;
*/


                case NONE:
                    break;
                default:
                    if (fromFail) {
                        errMsg = 'invalid name or position or invalid size';
                    } else {
                        if (!fg.hasOwnProperty('UserTargetWorldPt') || !fg.UserTargetWorldPt) {
                           errMsg = msg('notarget');
                        } else if (!get(fg, keyMap[skey])) {
                            errMsg = msg('nosize');
                        }
                    }
            }

            return errMsg;
        };

        var chkResult = '';

        // validate the entry for 3 color or non 3 color cases
        if (isThreeColor) {

            rgbFieldGroup.find((item, index) => {
                chkResult = '';

                if (crtCatalogId[index] !== NONE) {
                    var fg = Object.assign({}, request[item],
                        {
                            UserTargetWorldPt: get(request[panelKey], 'UserTargetWorldPt'),
                            [keyMap['sizefield']]: get(request[panelKey], keyMap['sizefield'])
                        });

                    chkResult = validate(fg, crtCatalogId[index], `${rgb[index].toUpperCase()}: `);
                }
                return chkResult;
            });
        } else {
            chkResult = validate(request[panelKey], crtCatalogId[0]);
        }
        outputMessage(chkResult);
        return chkResult;
    };
}

/*
 * onSucess callback for 'load' button on image select panel
 */
export function resultSuccess(plotInfo, hideDropdown = false) {
    return (request) => {

        if (plotInfo.addPlot === PLOT_NO) {
            outputMessage(loadErrorMsg['noplot']);
            return;
        }

        var errMsg = resultFail(false);

        if (errMsg(request)) {
            return;
        }

        var wpSet = [];
        var wpr = null;

        var webRequest = (cId, rq) => {

            switch (cId) {
                case URL:
                    wpr = imagePlotOnURL(rq);
                    break;
                case FITS:
                    wpr = imagePlotOnFITS(rq);
                    break;
                //case BLANK:
                //    wpr = imagePlotOnBlank(rq);
                //    break;
                default:
                    wpr = imagePlotOnSurvey(cId, rq);
            }
            return wpr;
        };

        var crtCatalogId = computeCurrentCatalogId(request[panelKey],
            [request[rgbFieldGroup[RED]], request[rgbFieldGroup[GREEN]], request[rgbFieldGroup[BLUE]]]);

        // send web request for either 3 color or not 3 color cases
        if (!plotInfo.isThreeColor) {
            wpr = webRequest(crtCatalogId[0], request[panelKey]);
            if (!wpr) {
                return outputMessage(loadErrorMsg['failplot']);
            }
            wpSet.push(wpr);
        } else {
            rgbFieldGroup.map((item, index) => {
                wpr = null;
                if (crtCatalogId[index] !== NONE) {
                    // add target and size data into r, g, b field group

                    var fgRequest = Object.assign({}, request[item],
                        {
                            UserTargetWorldPt: get(request[panelKey], 'UserTargetWorldPt'),
                            [keyMap['sizefield']]: get(request[panelKey], keyMap['sizefield'])
                        });

                    wpr = webRequest(crtCatalogId[index], fgRequest);
                    if (!wpr) {
                        return outputMessage(`${loadErrorMsg['failplot']} on ${rgb[index]} image`);
                    }
                }
                wpSet.push(wpr);
            });
            if (wpSet.every((wpr)=>( !wpr ))) {
                return outputMessage(`${loadErrorMsg['norgbselect']}`);
            }
        }
        const create = (PLOT_CREATE|PLOT_CREATE3COLOR);
        var nPlotId = null;
        var groupId = null;

        if ((plotInfo.addPlot&create) && plotInfo.viewerId) { // create & with viewerId
            groupId = plotInfo.viewerId;
            nPlotId = plotidGen.next().value;
            dispatchAddImages(plotInfo.viewerId, [nPlotId]);
        } else {                                            // replace and with plotId
            nPlotId = plotInfo.plotId;
            if (nPlotId) {
                groupId = getPlotViewById(visRoot(), nPlotId).plotGroupId;
            }
        }

        if (!groupId || !nPlotId) {
            return outputMessage(loadErrorMsg['noplotid']);
        } else {
            wpSet.forEach((item) => {
                if (item) {
                    item.setPlotGroupId(groupId);
                    item.setTitle('3-Color Image');
                }
            });
        }


        if (hideDropdown) {
            dispatchHideDropDownUi();
        }

        if (plotInfo.isThreeColor) {
            dispatchPlotImage(nPlotId, wpSet, true);
        } else {
            dispatchPlotImage(nPlotId, wpSet[0]);
        }

    };
}