/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
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
        const {pv, showDelete}= props;
        if (!pv) return undefined;
        const deleteClick= () => dispatchDeletePlotView({plotId:pv.plotId});

        return (
            <div style={rS}>
                <ToolbarButton icon={DELETE} tip='Delete Image'
                               style={{alignSelf:'flex-start'}}
                               horizontal={true} visible={showDelete} onClick={deleteClick}/>
            </div>
        );
    },
    (p,nP) => shallowequal({...p, pv:undefined}, {...nP,pv:undefined}) && pvEqualExScroll(p.pv, nP.pv)
);

VisInlineToolbarView.propTypes= {
    pv : PropTypes.object,
    showDelete : PropTypes.bool,
    help_id : PropTypes.string,
};
