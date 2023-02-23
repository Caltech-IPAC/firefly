/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {arrayOf, func, object, string} from 'prop-types';
import React, {memo} from 'react';
import CompleteButton from '../CompleteButton.jsx';
import {convertRequest, DynamicFieldGroupPanel, DynLayoutPanelTypes} from './DynamicUISearchPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {isSIAStandardID, makeFieldDefs} from './ServiceDefTools.js';

/**
 * @typedef {Object} SearchAreaInfo
 *
 * @prop {String} examples
 * @prop {String} moc
 * @prop {String} mocDesc
 * @prop {String} HiPS
 * @prop {String} hips_initial_fov
 * @prop {WorldPt} centerWp
 * @prop {CoordinateSys} coordinateSys
 */

const GROUP_KEY= 'ActivateMenu';

const titleStyle= {width: '100%', textAlign:'center', padding:'10px 0 5px 0', fontSize:'larger', fontWeight:'bold'};


export const ServiceDescriptorPanel= memo(({ serviceDefRef='none', serDefParams, setSearchParams,
                                               title, makeDropDown, sRegion,
                                               plotId= 'defaultHiPSTargetSearch'}) => {

    const fieldDefAry= makeFieldDefs(serDefParams, sRegion, undefined, true);

    const submitSearch= (r) => {
        const convertedR= convertRequest(r,fieldDefAry,isSIAStandardID(serDefParams.standardID));
        setSearchParams(convertedR);
    };

    return (
        <div key={serviceDefRef}>
            {makeDropDown?.()}
            <div style={{padding: '5px 5px 5px 5px'}}>
                <div style= {titleStyle}> {title} </div>
                <DynamicFieldGroupPanel groupKey={GROUP_KEY} keepState={false}
                                        DynLayoutPanel={DynLayoutPanelTypes.Simple}
                                        plotId={plotId}
                                        fieldDefAry={fieldDefAry} />
                <CompleteButton style={{padding: '20px 0 0 0'}}
                                   onSuccess={(r) => submitSearch(r)}
                                   onFail={() => showInfoPopup('Some field are not valid')}
                                   text={'Submit'} groupKey={GROUP_KEY} />
            </div>
        </div>
    );
});

ServiceDescriptorPanel.propTypes= {
    serDefParams: arrayOf(object),
    title: string,
    serviceDefRef: string,
    setSearchParams: func,
    makeDropDown: func,
    sRegion: string
};


