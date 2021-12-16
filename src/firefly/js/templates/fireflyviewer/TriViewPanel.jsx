/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {pick} from 'lodash';
import {flux} from '../../core/ReduxFlux.js';
import {LO_VIEW, LO_MODE, getLayouInfo, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {ResultsPanel} from './ResultsPanel.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {TriViewImageSection} from '../../visualize/ui/TriViewImageSection.jsx';
import {AppInitLoadingMessage} from '../../ui/AppInitLoadingMessage.jsx';
import {getExpandedChartProps} from '../../charts/ChartsCntlr.js';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';

export class TriViewPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            const nextState = pick(getLayouInfo(), ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'images']);
            this.setState(nextState);
        }
    }

    render() {
        const {showViewsSwitch, leftButtons, centerButtons, rightButtons, initLoadingMessage, initLoadCompleted} = this.props;
        const {title, mode, showTables, showImages, showXyPlots, images={}} = this.state;
        const {expanded, standard, closeable} = mode || {};
        const content = {};

        if (initLoadingMessage && !initLoadCompleted) return (<AppInitLoadingMessage message={initLoadingMessage}/>);

        if (showImages) {
            content.imagePlot = (<TriViewImageSection key='res-tri-img'
                                                      closeable={closeable}
                                                      imageExpandedMode={expanded===LO_VIEW.images}
                                                      {...images}  />);
        }
        if (showXyPlots) {
            const chartExpandedMode= expanded===LO_VIEW.xyPlots;
            const {expandedViewerId}= getExpandedChartProps();
            content.xyPlot = (<ChartsContainer key='res-xyplots'
                                               closeable={closeable}
                                               expandedMode={chartExpandedMode}
                                               viewerId={chartExpandedMode ? expandedViewerId : DEFAULT_PLOT2D_VIEWER_ID}
                                               tbl_group='main'
                                               useOnlyChartsInViewer={chartExpandedMode && expandedViewerId!==DEFAULT_PLOT2D_VIEWER_ID}
                                               addDefaultChart={true}/>);
        }
        if (showTables) {
            content.tables = (<TablesContainer key='res-tables'
                                               mode='both'
                                               closeable={closeable}
                                               expandedMode={expanded===LO_VIEW.tables}/>);
        }
        const viewSwitch = showViewsSwitch && showImages && showXyPlots && showTables;

        if (showImages || showXyPlots || showTables) {
            return (
                <ResultsPanel key='results'
                              title={title}
                              searchDesc ={searchDesc({viewSwitch, leftButtons, centerButtons, rightButtons})}
                              expanded={expanded}
                              standard={standard}
                    { ...content}
                />
            );
        } else {
            return <div/>;
        }
    }
}


TriViewPanel.propTypes = {
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func ),
    initLoadingMessage:  PropTypes.string,
    initLoadCompleted:  PropTypes.bool,
};
TriViewPanel.defaultProps = {
    showViewsSwitch: true
};


function searchDesc({viewSwitch, leftButtons, centerButtons, rightButtons}) {

    const hasContent = viewSwitch || leftButtons || centerButtons || rightButtons;
    return !hasContent ? <div/> :
    (
        <div style={{display: 'inline-flex', justifyContent: 'space-between'}}>
            <div>
                {leftButtons &&
                    leftButtons.map( (el) => el())
                }
            </div>
            <div>
                {centerButtons &&
                    centerButtons.map( (el) => el())
                }
            </div>
            <div style={{display: 'inline-flex'}}>
                {rightButtons &&
                    rightButtons.map( (el) => el())
                }
                <div style={{width: 20}}/>
                {viewSwitch &&
                    <div style={ {display: 'inline-block', float: 'right'} }>
                        <button type='button' className='button std'
                                onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | images | xyPlots'))}>tri-view</button>
                        <button type='button' className='button std'
                                onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | images'))}>img-tbl</button>
                        <button type='button' className='button std'
                                onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('images | xyPlots'))}>img-xy</button>
                        <button type='button' className='button std'
                                onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | xyPlots'))}>xy-tbl</button>
                    </div>
                }
            </div>
       </div>
    );
}

