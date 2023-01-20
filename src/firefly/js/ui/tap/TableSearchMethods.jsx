import React, {useState} from 'react';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {ConnectionCtx} from '../ConnectionCtx.js';
import {FieldGroup} from '../FieldGroup.jsx';
import {ObsCoreSearch} from './ObsCore.jsx';
import {ExposureDurationSearch} from './ObsCoreExposureDuration.jsx';
import {SpatialSearch} from './SpatialSearch.jsx';
import {TemporalSearch} from './TemportalSearch.jsx';
import {ObsCoreWavelengthSearch} from './WavelengthPanel.jsx';

const TAP_SEARCH_METHODS_GROUP= 'TAP_SEARCH_METHODS_GROUP';

export const TableSearchMethods = ({initArgs, obsCoreEnabled, columnsModel, serviceUrl, serviceLabel}) => {

    const [controlConnected, setControlConnected] = useState(false);

    return (
        <ConnectionCtx.Provider value={{controlConnected, setControlConnected}}>
            <FieldGroup style={{height: '100%', overflow: 'auto'}} groupKey={TAP_SEARCH_METHODS_GROUP} keepState={true}>
                <HelperComponents {...{initArgs,cols:getAvailableColumns(columnsModel),
                    columnsModel,serviceUrl,serviceLabel,obsCoreEnabled}}/>
            </FieldGroup>
        </ConnectionCtx.Provider>
    );
};

function HelperComponents({initArgs, cols, columnsModel, serviceUrl, serviceLabel, obsCoreEnabled}) {
    return obsCoreEnabled ?
        (
            <React.Fragment>
                <ObsCoreSearch {...{cols, serviceLabel, initArgs}} />
                <SpatialSearch {...{cols, serviceUrl, columnsModel, initArgs, obsCoreEnabled}} />
                <ExposureDurationSearch {...{initArgs}} />
                <ObsCoreWavelengthSearch {...{initArgs, serviceLabel}} />
            </React.Fragment>
        ) :
        (
            <React.Fragment>
                <SpatialSearch {...{cols, serviceUrl, columnsModel, initArgs, obsCoreEnabled}} />
                <TemporalSearch {...{cols, columnsModel,}} />
            </React.Fragment>
        );
}


/**
 * Assembles an array of objects with column attributes in the format that ColumnFld accepts
 * @param columnsModel
 */
function getAvailableColumns(columnsModel){
    const attrIdx  = [
        //[<column name in columnModel>, <column attribute name ColumnFld wants>]
        ['column_name', 'name'],
        ['unit', 'units'],
        ['datatype', 'type'],
        ['ucd', 'ucd'],
        ['description', 'desc']
    ].map(([c,k])=>[k,getColumnIdx(columnsModel, c)]);

    const td= columnsModel?.tableData?.data ?? [] ;
    return td.map((r) => {
        const col = {};
        attrIdx.forEach(([k,i]) => { col[k] = r[i]; });
        return col;
    });
}
