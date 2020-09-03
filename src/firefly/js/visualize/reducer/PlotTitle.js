/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {TitleOptions} from '../WebPlotRequest.js';
import {RequestType} from '../RequestType.js';
import {PlotAttribute} from '../PlotAttribute';
/**
 *
 * @param {WebPlot} plot
 * @param {WebPlotRequest} r the request
 * @return {String} the title
 */
export function makePostPlotTitle(plot,r){


    if (plot.plotState.isThreeColor()) return '3-color';

    let title= r.getTitle();
    const titleOps= r.getTitleOptions();
    const {attributes}= plot;
    let preTitle= attributes[PlotAttribute.PRE_TITLE];
    let postTitle= attributes[PlotAttribute.POST_TITLE];

    preTitle= preTitle ? `${preTitle}: `: '';
    if (titleOps===TitleOptions.FILE_NAME) {
        title= computeFileNameBaseTitle(r,plot.plotState, plot.plotState.firstBand(),preTitle);
    }
    else if (!title ||
        titleOps===TitleOptions.PLOT_DESC ||
        titleOps===TitleOptions.PLOT_DESC_PLUS ||
        titleOps===TitleOptions.SERVICE_OBS_DATE ) {
        title = preTitle + plot.plotDesc;
    }
    else if (preTitle) {
        title= preTitle+title;
    }

    postTitle= postTitle ? `: ${postTitle}` : '';
    title= title + postTitle;

    return title;
}

function computeFileNameBaseTitle(r,state, band, preTitle) {
    let retval= '';
    const rt= r.getRequestType();

    switch (rt) {
        case RequestType.WORKSPACE:
            if (r.getFileName()) retval= computeTitleFromFile(r.getFileName(), preTitle);
            else if (state.getUploadFileName(band)) retval= computeTitleFromFile(state.getUploadFileName(band), preTitle);
            break;
        case RequestType.FILE:
        case RequestType.TRY_FILE_THEN_URL:
            if (state.getUploadFileName(band)) retval= computeTitleFromFile(state.getUploadFileName(band), preTitle);
            else if (r.getFileName()) retval= computeTitleFromFile(r.getFileName(), preTitle);
            break;
        case RequestType.URL:
            retval= computeTitleFromURL(r.getURL(),r,preTitle);
            break;

    }

    if (!retval) retval= 'FITS';
    return retval;
}


function computeTitleFromURL(urlStr, r, preTitle='') {
    if (!urlStr || !r) return '';
    var retval= '';
    var qIdx=urlStr.indexOf('?');
    if (qIdx>-1 && urlStr.length>qIdx+1) {
        var prepend= preTitle;
        var workStr= urlStr.substring(qIdx+1);
        var fLoc= workStr.toLowerCase().indexOf('.fit');
        if (fLoc>-1) {
            workStr= workStr.substring(0,fLoc);
            if (workStr.lastIndexOf('=')>0)  {
                workStr= workStr.substring(workStr.lastIndexOf('='));
                retval= prepend +stripFilePath(workStr);
            }
            else {
                retval= prepend +stripFilePath(workStr);
            }
        }
        else {
            fLoc= urlStr.toLowerCase().indexOf('.fit');
            if (fLoc>-1) {
                workStr= urlStr.substring(0,fLoc);
                retval= prepend +stripFilePath(workStr);
            }
            else {
                retval= prepend+ workStr;
            }
        }
    }
    else {
        retval= preTitle+computeTitleFromFile(urlStr);
    }
    return retval;
}

function computeTitleFromFile(fileStr) {
    return fileStr ? getFileBase(stripFilePath(fileStr)) : '';
}

function stripFilePath(path) {
    if (!path) return '';
    const i = path.lastIndexOf('/');
    return (i<0) ? path : path.substring(i+1, path.length);
}

function getFileBase(s) {
    if (!s) return '';
    const i = s.lastIndexOf('.');
    return (i===-1 || i===0) ? s : s.substring(0, i);
}

