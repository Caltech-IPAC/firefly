/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import * as TblUtil from '../tables/TableUtil.js';
import * as TableStatsCntlr from '../charts/TableStatsCntlr.js';
import {singleTraceUI, getNumericCols} from '../charts/ChartUtil.js';
import {ChartSelectPanel, CHART_ADDNEW} from './../charts/ui/ChartSelectPanel.jsx';
import {getActiveViewerItemId} from '../charts/ui/MultiChartViewer.jsx';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../visualize/MultiViewCntlr.js';

import CompleteButton from './CompleteButton.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

const dropdownName = 'ChartSelectDropDownCmd';


function hideSearchPanel() {
    dispatchHideDropDown();
}

export class ChartSelectDropdown extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
    }

    componentDidMount() {


        const tblId = TblUtil.getActiveTableId(this.props.tblGroup);
        const tblStatsData = tblId && flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY][tblId];
        const table= TblUtil.getTblById(tblId);
        if (!tblStatsData && table) {
            TableStatsCntlr.dispatchLoadTblStats(table['request']);
        }



        this.removeListener = flux.addListener(() => this.storeUpdate());
        this.iAmMounted= true;
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        this.removeListener && this.removeListener();
    }

    getNextState() {
        const tblId = TblUtil.getActiveTableId(this.props.tblGroup);
        const tblStatsData = tblId && flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY][tblId];
        return {tblId, tblStatsData};
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const {tblId, tblStatsData} = this.getNextState();
            if (tblId !== this.state.tblId || tblStatsData !== this.state.tblStatsData) {
                this.setState(this.getNextState());
            }
        }
    }

    render() {
        const {tblId} = this.state;

        let noChartReason='';
        if (tblId) {
            const {tableData, totalRows}= TblUtil.getTblById(tblId);
            if (!totalRows) {
                noChartReason = 'empty table';
            } else if (getNumericCols(tableData.columns).length < 1) {
                noChartReason = 'the table has no numeric columns';
            }
        } else {
            noChartReason = 'no active table';
        }

        if (!noChartReason) {
            return (
                <ChartSelectPanel {...{
                    tbl_id: tblId,
                    chartId: getActiveViewerItemId(DEFAULT_PLOT2D_VIEWER_ID),
                    chartAction: CHART_ADDNEW,
                    hideDialog: ()=>dispatchHideDropDown()}}/>
            );
        } else {
            const msg = `Charts are not available: ${noChartReason}.`;
            return (
                <div>
                    <div style={{padding:20, fontSize:'150%'}}>{msg}</div>
                    <CompleteButton style={{paddingLeft: 20, paddingBottom: 20}}
                                    onSuccess={hideSearchPanel}
                                    text = {'OK'}
                    />
                </div>
            );
        }
    }
}

ChartSelectDropdown.propTypes = {
    tblGroup: PropTypes.string, // if not present, default table group is used
    name: PropTypes.oneOf([dropdownName])
};


ChartSelectDropdown.defaultProps = {
    name: dropdownName
};

