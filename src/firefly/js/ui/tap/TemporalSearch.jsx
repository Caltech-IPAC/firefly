import {Box, Stack} from '@mui/joy';
import React, {useContext, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {ColsShape, ColumnFld, getColValidator} from '../../charts/ui/ColumnOrExpression.jsx';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {useFieldGroupRerender, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {checkExposureTime} from '../TimeUIUtil.js';
import {ConstraintContext} from './Constraints.js';
import {TimeRangePanel} from './TimeRangePanel.jsx';
import {
    DebugObsCore, getPanelPrefix, LabelWidth, makeFieldErrorList,
    makePanelStatusUpdater, Width_Column, makeCollapsibleCheckHeader
} from './TableSearchHelpers.jsx';
import {getColumnAttribute, maybeQuote, tapHelpId} from './TapUtil.js';

const Temporal = 'Temporal';
const TemporalColumns = 'temporalColumns';
const TimeFrom = 'timeFrom';
const TimeTo = 'timeTo';
const FROM = 0;
const TO = 1;


const title = Temporal;
const panelValue = title;
const panelPrefix = getPanelPrefix(title);


function makeTemporalConstraints(columnsModel, fldObj) {
    let adqlConstraint = '';
    const errList= makeFieldErrorList();
    const {[TimeFrom]:timeFromField, [TimeTo]:timeToField, [TemporalColumns]:temporalColumnsField}= fldObj;

    if (timeFromField?.valid && timeToField?.valid && temporalColumnsField?.valid){
        const timeColumns = temporalColumnsField?.value?.split(',').map( (c) => c.trim()) ?? [];

        const {mjdRange, isoRange}=  checkExposureTime(timeFromField,timeToField);

        if (timeColumns.length === 1) {
            const timeColumn = timeColumns[0];

            // use MJD if time column has a numeric type, ISO otherwise
            const datatype = getColumnAttribute(columnsModel, timeColumn, 'datatype')?.toLowerCase() ?? '';
            // ISO types: char, varchar, timestamp, ?
            const useISO = !['double', 'float', 'real', 'int', 'long', 'short'].some((e) => datatype.includes(e));
            let timeRange = mjdRange;
            if (useISO) {
                // timeRange = mjdRange.map((value) => convertMJDToISO(value));
                timeRange = isoRange;
            }

            const timeConstraints = timeRange.map((oneRange, idx) => {
                if (!oneRange) return '';
                const limit  = useISO ? `'${oneRange}'` : oneRange;
                return idx === FROM ? `${maybeQuote(timeColumn)} >= ${limit}` : `${maybeQuote(timeColumn)} <= ${limit}`;
            });

            if (!timeConstraints[FROM] || !timeConstraints[TO]) {
                adqlConstraint = timeConstraints[FROM] + timeConstraints[TO];
            } else {
                adqlConstraint =`(${timeConstraints[FROM]} AND ${timeConstraints[TO]})`;
            }
        }
    }

    if (!timeFromField.value && !timeToField.value) errList.addError('Time is Required');
    if (!temporalColumnsField.value) errList.addError('Temporal Columns required');
    errList.checkForError(timeFromField);
    errList.checkForError(timeToField);
    errList.checkForError(temporalColumnsField);
    const errAry= errList.getErrors();
    return { valid: errAry.length===0, errAry, adqlConstraintsAry:[adqlConstraint], siaConstraints:[]};
}

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(Temporal));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldAry=[TimeTo,TimeFrom,TemporalColumns];

export function findTimeColumn(columnsTable) {
    const ucdIdx= getColumnIdx(columnsTable,'ucd',true);
    const nIdx= getColumnIdx(columnsTable,'column_name',true);
    const unitIdx= getColumnIdx(columnsTable,'unit',true);
    if (ucdIdx===-1 || nIdx===-1) return;
    const timeRows= columnsTable.tableData.data?.filter( (row) => row[ucdIdx]?.includes('time.epoch') && row[unitIdx]?.startsWith('d'));
    if (!timeRows?.length) return;
    const mainRows= timeRows?.filter( (row) => row[ucdIdx]?.includes('meta.main') && row[unitIdx]?.startsWith('d'));
    return mainRows?.length ? mainRows[0][nIdx] : timeRows[0][nIdx];
}


/**
 *
 * @param props
 * @param props.cols
 * @param props.columnsModel
 * @returns {JSX.Element}
 */
export function TemporalSearch({cols, columnsModel}) {
    const [constraintResult, setConstraintResult] = useState({});

    const {setFld,setVal,getVal,makeFldObj}= useContext(FieldGroupCtx);
    const {setConstraintFragment}= useContext(ConstraintContext);
    useFieldGroupRerender([...fldAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change
    const timeCol= getVal(TemporalColumns);

    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue, 'Error in temporal constraints');

    useFieldGroupWatch([TemporalColumns],
        ([tcVal],isInit) => {
            if (!isInit && !checkHeaderCtl.isPanelActive() && tcVal) {
                checkHeaderCtl.setPanelActive(true);
            }
            if (!checkHeaderCtl.isPanelActive()) return;
            if (!tcVal) return;
            const timeColumns = tcVal.split(',').map( (c) => c.trim()) ?? [];
            if (timeColumns.length > 1) {
                setFld(TemporalColumns, {value:tcVal, valid:false, message: 'you may only choose one column'});
            }
        }, [checkHeaderCtl.isPanelActive()]);

    useEffect(() => {
        const constraints= makeTemporalConstraints(columnsModel, makeFldObj([TimeFrom,TimeTo,TemporalColumns]));
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);


    useEffect(() => {
        const findTimeCol= findTimeColumn(columnsModel) ?? '';
        const errMsg= 'Temporal searches require identifying a table column containing a time in MJD.  Please provide a column name.';
        let existingTimeCol = timeCol; //get current val of TemporalColumns
        let timeColExists = false;
        //check if user has a previously selected Temporal Column, and if it exists in the currently selected table's cols
        if (existingTimeCol) timeColExists = cols.some((c) => c.name === existingTimeCol);
        if (!timeColExists) existingTimeCol = findTimeCol;
        setVal(TemporalColumns, existingTimeCol, {validator: getColValidator(cols, true, false, errMsg), valid: true});
        if (Boolean(findTimeCol)) checkHeaderCtl.setPanelOpen(true);
    }, [columnsModel]);


    const temporalColumns = (
                <ColumnFld fieldKey={TemporalColumns} cols={cols}
                           name='temporal column' // label that appears in column chooser
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                           label='Temporal Column' tooltip={'Column for temporal search'}
                           labelWidth={LabelWidth}
                           validator={getColValidator(cols, true, false)}
                />
        );

    const timeColStr= timeCol ? `(${timeCol})` : '';
    const timeRange= (//  radio field is styled with padding right in consistent with the label part of 'temporal columns' entry
        <TimeRangePanel {...{turnOnPanel: () => checkHeaderCtl.setPanelActive(true),
            panelActive:checkHeaderCtl.isPanelActive(),
            minKey:TimeFrom, maxKey:TimeTo, columnsForTip:[getVal(TemporalColumns)],
            fromTip:`'from' time ${timeColStr}`,
            toTip:`'to' time ${timeColStr}`,
            labelStyle:{width:'5rem'},
            style:{marginTop: 16, marginBottom: 16}}}/>
    );

    return (
        <CollapsibleCheckHeader title={title} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <Box sx={{mt:1/2}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    <Stack spacing={2}>
                        {temporalColumns}
                        {timeRange}
                    </Stack>
                </ForceFieldGroupValid>
            </Box>
            <DebugObsCore {...{constraintResult}}/>
        </CollapsibleCheckHeader>
    );
}

TemporalSearch.propTypes = {
    cols: ColsShape,
    columnsModel: PropTypes.object,
};
