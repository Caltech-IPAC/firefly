
import React from 'react';
import PropTypes from 'prop-types';
import {WcsMatchType, dispatchWcsMatch} from '../ImagePlotCntlr.js';


const tStyle= {
    display:'inline-block',
    whiteSpace: 'nowrap',
    minWidth: '3em',
    paddingLeft : 5
};

export function WcsMatchOptions({wcsMatchType, activePlotId}) {

    return (
        <div style={{alignSelf:'center', paddingLeft:25}}>
            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                           checked={wcsMatchType===WcsMatchType.Standard}
                           onChange={(ev) => wcsMatchStandard(ev.target.checked, activePlotId) }
                    />
                </div>
                <div style={tStyle}>WCS Match</div>
            </div>

            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                           checked={wcsMatchType===WcsMatchType.Target}
                           onChange={(ev) => wcsMatchTarget(ev.target.checked, activePlotId) }
                    />
                </div>
                <div style={tStyle}>Target Match</div>
            </div>
        </div>

    );
}

WcsMatchOptions.propTypes= {
    wcsMatchType : PropTypes.any,
    activePlotId: PropTypes.string,
};

function wcsMatchStandard(doWcsStandard, plotId) {
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Standard:false, plotId});
}


function wcsMatchTarget(doWcsTarget, plotId) {
    dispatchWcsMatch({matchType:doWcsTarget?WcsMatchType.Target:false, plotId});
}
