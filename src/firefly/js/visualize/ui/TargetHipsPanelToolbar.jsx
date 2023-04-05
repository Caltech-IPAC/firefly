
import SELECT_NONE from 'images/icons-2014/28x28_Rect_DD.png';
import React from 'react';
import PropTypes from 'prop-types';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {CONE_CHOICE_KEY} from './CommonUIKeys.js';
import {SelectAreaButton} from './SelectAreaDropDownView.jsx';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {getActivePlotView} from '../PlotViewUtil.js';




const toolsStyle= {
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    width:'100%',
    height: 30,
    justifyContent: 'space-between',
    marginTop: -2,
    paddingBottom: 2
};

const imageStyle={width:16, height:16};

export const HelpLines= ({whichOverlay}) => {
    const modalEndInfo = useStoreConnector(() => getComponentState('ModalEndInfo', {}));
    const setModalEndInfo= (info) => dispatchComponentStateChange('ModalEndInfo',  {...{}, ...info});

    const selectButton= (
        <SelectAreaButton {...{pv:getActivePlotView(visRoot()),modalEndInfo,setModalEndInfo,
            tip:'Reselect an area for search', imageStyle, style:{paddingTop:3}}}/>
    );
    return (
        whichOverlay===CONE_CHOICE_KEY ?
        ( <div style={{paddingLeft:5, display:'flex', alignItems:'center'}}>
            <span>Click to choose a search center, or use the Selection Tools (</span>
            {selectButton}
            <span>) to choose a search center and radius.</span>
        </div> ) :
        ( <div style={{paddingLeft:5, display:'flex', alignItems:'center'}}>
            <span>Use the Selection Tools (</span>
            {selectButton}
            <span>)  to choose a search polygon. Click to change the center. </span>
        </div> ));

}


export function TargetHipsPanelToolbar({visRoot, viewerPlotIds, toolbarStyle={}, whichOverlay= CONE_CHOICE_KEY, viewerId }) {

    const leftImageStyle= {
        verticalAlign:'bottom',
        cursor:'pointer',
        flex: '0 0 auto',
        paddingLeft: 10
    };
    if (viewerPlotIds.length===2) {
        leftImageStyle.visibility='hidden';
    }


    const style= {...toolsStyle, ...toolbarStyle};
    const {showImageToolbar=true}= getActivePlotView(visRoot)?.plotViewCtx.menuItemKeys ?? {};
    if (!showImageToolbar) return <div/>;

    return (
        <div style={style}>
            <HelpLines whichOverlay={whichOverlay}/>
            <div style={{display:'flex', justifyContent:'flex-start'}}>
                <VisMiniToolbar style={{width:'unset'}} viewerId={viewerId}
                                tips={{selectArea:'Select an area to search'}}/>
                <HelpIcon style={{alignSelf:'center', paddingLeft:5}} helpId={'hips.VisualSelection'} />
            </div>
        </div>
    );
}


TargetHipsPanelToolbar.propTypes= {
    dlAry : PropTypes.arrayOf(PropTypes.object),
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    makeDropDownFunc: PropTypes.func,
    makeDropDown: PropTypes.bool,
    toolbarStyle: PropTypes.object,
};

