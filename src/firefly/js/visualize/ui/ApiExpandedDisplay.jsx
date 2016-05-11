/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {VisHeaderView} from './VisHeaderView.jsx';
import {ExpandedModeDisplay} from '../iv/ExpandedModeDisplay.jsx';
// import {currMouseState} from '../VisMouseCntlr.js';
import {addMouseListener, lastMouseCtx} from '../VisMouseSync.js';



export class ApiExpandedDisplay extends Component {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), currMouseState:lastMouseCtx()};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        if (this.removeMouseListener) this.removeMouseListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.removeMouseListener= addMouseListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (visRoot()!==this.state.visRoot || lastMouseCtx() !==this.state.currMouseState) {
            this.setState({visRoot:visRoot(), currMouseState:lastMouseCtx()});
        }
    }

    /**
     *
     * @return {XML}
     */
    render() {
        const {closeFunc}= this.props;
        var {visRoot,currMouseState}= this.state;
        return (
            <div style={{width:'100%', height:'100%', display:'flex', flexWrap:'nowrap',
                         alignItems:'stretch', flexDirection:'column'}}>
                <div style={{position: 'relative', marginBottom:'6px'}} className='banner-background'>
                    <VisHeaderView visRoot={visRoot} currMouseState={currMouseState}/>
                </div>
                <div style={{flex: '1 1 auto', display:'flex'}}>
                    <ExpandedModeDisplay   {...{key:'results-plots-expanded', closeFunc, insideFlex:true}}/>
                </div>
            </div>
            );
    }
}

ApiExpandedDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func
};

ApiExpandedDisplay.defaultProps= {
    closeFunc:null
};
