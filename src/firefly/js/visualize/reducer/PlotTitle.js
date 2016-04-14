/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {TitleOptions} from '../WebPlotRequest.js';
import {RequestType} from '../RequestType.js';

/**
 *
 * @param plot
 * @param r the request
 * @param title starting title
 * @return {*}
 */
export function makePostPlotTitle(plot,r) {
    var title= r.getTitle();
    var titleOps= r.getTitleOptions();
    var preTitle= r.getPreTitle() ? r.getPreTitle()+': ': '';

    if (titleOps===TitleOptions.FILE_NAME) {
        title= computeFileNameBaseTitle(r,plot.plotState, plot.plotState.firstBand(),preTitle);
    }
    else if (!title ||
        titleOps===TitleOptions.PLOT_DESC ||
        titleOps===TitleOptions.PLOT_DESC_PLUS ||
        titleOps===TitleOptions.SERVICE_OBS_DATE ) {
        title = preTitle + plot.plotDesc;
    }

    var postTitle= r.getPostTitle() ? ': '+r.getPostTitle() : '';
    title= title + postTitle;

    return title;
}

function computeFileNameBaseTitle(r,state, band, preTitle) {
    var retval= '';
    var rt= r.getRequestType();
    if (rt===RequestType.FILE || rt===RequestType.TRY_FILE_THEN_URL) {
        if (state.getUploadFileName(band)) {
            retval= preTitle + computeTitleFromFile(state.getUploadFileName(band));
        }
        else {
            retval= preTitle + computeTitleFromFile(r.getFileName());
        }
    }
    else if (r.getRequestType()== RequestType.URL) {
        retval= computeTitleFromURL(r.getURL(),r,preTitle);
    }
    return retval;
}


function computeTitleFromURL(urlStr, r, preTitle='') {
    if (!urlStr || !r) return '';
    var retval= '';
    var qIdx=urlStr.indexOf('?');
    if (qIdx>-1 && urlStr.length>qIdx+1) {
        var prepend= r.getTitleFilenameModePfx() ? r.getTitleFilenameModePfx()+ ' ' : 'from ';
        prepend+= preTitle;
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
    var i = path.lastIndexOf('/');
    return (i===-1 || i=== 0)  ? path : path.substring(i+1, path.length);
}

function getFileBase(s) {
    if (!s) return '';
    var i = s.lastIndexOf('.');
    return (i==-1 || i==0) ? s : s.substring(0, i);
}

