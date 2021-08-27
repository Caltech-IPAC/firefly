/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import {makeMouseStatePayload, fireMouseCtxChange, MouseState} from '../VisMouseSync.js';
import PropTypes from 'prop-types';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import { dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {pvEqualExScroll} from '../PlotViewUtil.js';
import shallowequal from 'shallowequal';
import DELETE from 'images/blue_delete_10x10.png';


const rS= {
    width: '100% - 2px',
    position: 'relative',
    verticalAlign: 'top',
    whiteSpace: 'nowrap',
    display:'inline-flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    zIndex : 1
};

export const VisInlineToolbarView = memo( (props) => {
        const {pv, showDelete,show, topOffset=0}= props;
        if (!pv) return undefined;
        const deleteClick= () => {
            const mouseStatePayload= makeMouseStatePayload(undefined,MouseState.EXIT,undefined,0,0);
            fireMouseCtxChange(mouseStatePayload);  // this for anyone listening directly to the mouse
            dispatchDeletePlotView({plotId:pv.plotId});
        };

        const topStyle= {
            visibility: show ? 'visible' : 'hidden',
            opacity: show ? 1 : 0,
            transition: show ? 'opacity .15s linear' : 'visibility 0s .15s, opacity .15s linear',
            top: topOffset
        };

        return (
            <div style={topStyle} className='iv-decorate-inline-toolbar-container'>
                <div style={rS}>
                    <ToolbarButton icon={DELETE} tip='Delete Image'
                                   style={{alignSelf:'flex-start'}}
                                   horizontal={true} visible={showDelete} onClick={deleteClick}/>
                </div>
            </div>
        );
    },
    (p,nP) => shallowequal({...p, pv:undefined}, {...nP,pv:undefined}) && pvEqualExScroll(p.pv, nP.pv)
);

VisInlineToolbarView.propTypes= {
    pv : PropTypes.object,
    showDelete : PropTypes.bool,
    show : PropTypes.bool,
    help_id : PropTypes.string,
    topOffset: PropTypes.number
};
