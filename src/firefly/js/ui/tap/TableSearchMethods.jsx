import {Box} from '@mui/joy';
import React, {useState} from 'react';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {ConnectionCtx} from '../ConnectionCtx.js';
import {FieldGroup} from '../FieldGroup.jsx';
import {ObsCoreSearch} from './ObsCore.jsx';
import {ExposureDurationSearch} from './ObsCoreExposureDuration.jsx';
import {SpatialSearch} from './SpatialSearch.jsx';
import {TemporalSearch} from './TemporalSearch.jsx';
import {ObsCoreWavelengthSearch} from './WavelengthPanel.jsx';
import {ObjectIDSearch} from 'firefly/ui/tap/ObjectIDSearch';

const TAP_SEARCH_METHODS_GROUP= 'TAP_SEARCH_METHODS_GROUP';

export const TableSearchMethods = ({initArgs, obsCoreEnabled, columnsModel, serviceUrl, sx,
                                       serviceLabel, tableName, capabilities, obsCoreMetadataModel}) => {

    const [controlConnected, setControlConnected] = useState(false);

    return (
        <ConnectionCtx.Provider value={{controlConnected, setControlConnected}}>
            <Box sx={{...sx, height: '100%', overflow: 'auto'}} >
                <HelperComponents {...{initArgs,cols:getAvailableColumns(columnsModel), tableName,
                    columnsModel,serviceUrl,serviceLabel,obsCoreEnabled,capabilities,obsCoreMetadataModel}}/>
            </Box>
        </ConnectionCtx.Provider>
    );
};

const CompDivide= () => <Box sx={{my:1}}/>;

function HelperComponents({initArgs, cols, columnsModel, serviceUrl, serviceLabel, obsCoreEnabled, tableName,
                              capabilities, obsCoreMetadataModel}) {
    return obsCoreEnabled ?
        (
            <>
                <ObsCoreSearch {...{sx:{mt:1}, cols, obsCoreMetadataModel, serviceLabel, initArgs}} />
                <CompDivide/>
                <SpatialSearch {...{cols, serviceUrl, serviceLabel, columnsModel, initArgs, obsCoreEnabled, tableName, capabilities}} />
                <CompDivide/>
                <ExposureDurationSearch {...{initArgs}} />
                <CompDivide/>
                <ObsCoreWavelengthSearch {...{initArgs, serviceLabel}} />
                <CompDivide/>
                <ObjectIDSearch {...{cols, capabilities, tableName, columnsModel}}/>
            </>
        ) :
        (
            <>
                <SpatialSearch {...{sx:{mt:1}, cols, serviceUrl, serviceLabel, columnsModel, initArgs, obsCoreEnabled, tableName, capabilities}} />
                <CompDivide/>
                <TemporalSearch {...{cols, columnsModel}} />
                <CompDivide/>
                <ObjectIDSearch {...{cols, capabilities, tableName, columnsModel}}/>
            </>
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
