/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {HiPSId, updateHiPSTblHighlightOnUrl, URL_COL} from '../visualize/HiPSListUtil.js';
import {HiPSSurveyListSelection, HiPSPopupMsg, getTblModelOnPanel, sourcesPerChecked} from './HiPSSurveyListDisplay.jsx';
import {ValidationField} from './ValidationField.jsx';
import {getCellValue} from '../tables/TableUtil.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {parseWorldPt} from '../visualize/Point.js';
import {useFieldGroupValues} from 'firefly/ui/SimpleComponent.jsx';

import './ImageSelect.css';

const hipsPanelId = HiPSId;
const urlWrapperStyle= {height: 35, width: 'calc(100% - 6pt)', alignItems: 'center'} ;
const listWrapperStyle= {height:'100%', display: 'flex', flexDirection:'column', alignItems: 'center'};

export const HiPSImageSelect= ({style={}, groupKey}) => {
    const {imageSource}= useFieldGroupValues(groupKey,'imageSource');
    return (
        <div style={{height:'100%',...style}} className='ImageSelect'>
            {imageSource === 'url' ?
                <SelectUrl style={urlWrapperStyle}/> :
                <HiPSSurveyListSelection surveysId={hipsPanelId} wrapperStyle={ listWrapperStyle } />
            }
        </div>
    );
};

HiPSImageSelect.propTypes = {
    groupKey: PropTypes.string.isRequired,
    style: PropTypes.object
};


function SelectUrl({style}) {
    return (
        <div className='ImageSearch__section' style={style} title={'enter url of HiPS image'}>
            <div className='ImageSearch__section--title'>Enter URL</div>
            <ValidationField labelWidth={150} style={{width: 475}} fieldKey='txURL' />
        </div>
    );
}

SelectUrl.propTypes = {
    style: PropTypes.object
};

/**
 * create webPlotRequest for HiPS image request
 * @param {WebPlotRequest} request
 * @param {String} plotId
 * @param {String} groupId
 */
export function makeHiPSWebPlotRequest(request, plotId, groupId= DEFAULT_FITS_VIEWER_ID) {
    let url;
    const sources = sourcesPerChecked();

    if ( (request?.imageSource ?? 'archive') === 'url') {
        url = request.txURL.trim();
        updateHiPSTblHighlightOnUrl(url, hipsPanelId, sources);
    } else {
        if (!sources) {
            HiPSPopupMsg('No HiPS source selected', 'HiPS search');
            return null;
        }
        const tableModel = getTblModelOnPanel(hipsPanelId);
        if (!tableModel) {
            HiPSPopupMsg('No HiPS information found', 'HiPS search');
            return null;
        }
        const {highlightedRow=0} = tableModel;
        url = getCellValue(tableModel, highlightedRow, URL_COL);
        if (url) {
            url = url.trim();
        }
    }

    if (!url) {
        HiPSPopupMsg('No HiPS URL found', 'HiPS search');
        return null;
    }

    const fov = request?.sizeFov ?? 180;
    const wp = parseWorldPt(request.UserTargetWorldPt) || null;
    const wpRequest = WebPlotRequest.makeHiPSRequest(url, wp, Number(fov) || NaN);
    wpRequest.setPlotGroupId(groupId);
    wpRequest.setPlotId(plotId);
    return wpRequest;
}
