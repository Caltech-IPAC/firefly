import React from 'react';
import PropTypes from 'prop-types';

import {dispatchJobAdd} from './BackgroundCntlr.js';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';


/**
 * This component uses ComponentCntlr state persistence.  It is keyed by the given props's componentKey.
 * The data structure is described below.
 * @typedef {Object} data BackgroundablePanel's data structure
 * @prop {boolean}  data.inProgress   true when a download is in progress
 * @prop {boolean}  data.bgStatus    the bgStatus given to this background request
 */


export const BgMaskPanel = React.memo(({componentKey, style={}}) => {

    const [{inProgress, bgStatus}] = useStoreConnector(() => getComponentState(componentKey));

    const sendToBg = () => {
        bgStatus && dispatchJobAdd(bgStatus);
    };
    const maskStyle = {...defMaskStyle, ...style};

    if (inProgress) {
        return (
            <div style={maskStyle}>
                <div className='loading-mask'/>
                {bgStatus &&
                <div style={{display: 'flex', alignItems: 'center'}}>
                    <button type='button' style={maskButton} className='button std' onClick={sendToBg}>Send to background</button>
                </div>
                }
            </div>
        );
    } else {
        return null;
    }
});

BgMaskPanel.propTypes = {
    componentKey: PropTypes.string.isRequired,  // key used to identify this background job
    style: PropTypes.object                     // used for overriding default styling
};

const defMaskStyle = {
            position: 'relative',
            width: '100%',
            height: '100%',
            boxSizing: 'border-box',
            border: '1px solid rgba(0, 0, 0, 0.3)',
            display: 'flex',
            justifyContent: 'center'
        };

const maskButton =  {
            zIndex: 1,
            marginTop: '80px',
            border: '1px solid rgb(125,125,125)',
            boxSizing: 'content-box',
            backgroundColor: 'rgb(220,220,220)',
            borderRadius: 4
        };
