import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

import {dispatchJobAdd} from './BackgroundCntlr.js';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {Logger} from '../../util/Logger.js';

const logger = Logger('BgMaskPanel');

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
    const parts = get(bgStatus, 'ITEMS.length', 0);
    const msg = 'Working...' + (parts > 1 ? ` part #${parts}` : '');

    logger.debug(inProgress ? 'show' : 'hide');
    if (inProgress) {
        return (
            <div style={maskStyle}>
                <div className='loading-mask'/>
                { bgStatus &&
                    <div style={{display: 'flex', alignItems: 'center'}}>
                        <div style={maskButton} >
                            <div style={{textAlign: 'center', margin: 5, fontSize: 'larger', fontStyle: 'italic'}}>{msg}</div>
                            <div className='button large' onClick={sendToBg}>Send to background</div>
                        </div>
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
    marginTop: '95px',
    position: 'relative',
    color: 'white'

};
