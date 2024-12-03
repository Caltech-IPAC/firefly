import React from 'react';
import {Stack, Typography} from '@mui/joy';
import {ObjectIDSearch} from './ObjectIDSearch';
import {ObsCoreSearch} from './ObsCore';
import {ExposureDurationSearch} from './ObsCoreExposureDuration';
import {ALL_FALLBACK_META_OPTIONS, getServiceMetaOptions} from './SiaUtil';
import {SpatialSearch} from './SpatialSearch';
import {ObsCoreWavelengthSearch} from './WavelengthPanel';


export function SiaUI({initArgs, serviceUrl, serviceLabel, siaMeta, sx={}}) {

    const useSIAv2= true;
    const fallbackMetaOptions= getServiceMetaOptions(serviceLabel) ?? [];
    const obsCoreMetadataModel= makeObsCoreMetadataModel(siaMeta, fallbackMetaOptions);
    const showNoMetaError= !hasSomeImportantMeta(siaMeta);

    return (
        <Stack spacing={1} width={1} maxWidth='75rem' sx={sx}>
            {showNoMetaError && <Typography color='warning' pl={3}>
                {`Warning: The ${serviceLabel} SIAv2 service does not return any meta data to configure search panel`}
            </Typography>}
            <SpatialSearch {...{cols:undefined, serviceUrl, serviceLabel, columnsModel:undefined, initArgs, obsCoreEnabled:false,
                tableName:undefined, useSIAv2,
                capabilities: {canUsePoint:true, canUseCircle:true, canUsePolygon:true, canUseContains:true}
            }}/>
            <ObsCoreSearch {...{obsCoreMetadataModel, serviceLabel, initArgs, useSIAv2,
                slotProps:{innerStack: {width:1}} }} />
            <ExposureDurationSearch {...{initArgs, useSIAv2}} />
            <ObsCoreWavelengthSearch {...{initArgs, serviceLabel,useSIAv2}} />
            <ObjectIDSearch {...{useSIAv2}}/>
        </Stack>
    );
}

const metaNameMap= {
    BAND: 'band',
    CALIB:'calib',
    COLLECTION: 'obs_collection',
    DPTYPE: 'dataproduct_type',
    EXPTIME: 'exptime',
    FACILITY: 'facility',
    FORMAT: 'format',
    FOV: 'fov',
    ID:'ID',
    INSTRUMENT:'instrument_name',
    POL: 'POL',
    POS:'POS',
    SPATRES: 'spatres',
    SPECRP: 'specrp',
    TARGET:'target',
    TIME:'time',
    TIMERE:'timere'
};

const toColName= (name) => metaNameMap[name]??name;

const importantMetaParams= [ 'DPTYPE', 'BAND', 'CALIB', 'COLLECTION', 'FACILITY', 'INSTRUMENT' ];

const hasSomeImportantMeta= (siaMeta) =>
    Boolean(siaMeta?.params?.some( (entry) => importantMetaParams.includes(entry.name.toUpperCase())) );

function makeObsCoreMetadataModel(siaMeta,fallbackMetaOptions=[]) {

    const rows= siaMeta?.params
        ?.map( ({name,options}) => {
            if (!options) return [];
            return options .split(',') .filter(Boolean).map( (v) => [ toColName(name), v]);
        })
        .flat() ?? [];

    fallbackMetaOptions.forEach( (entry) => {
        const name= toColName(entry.name);
        if (rows.some( (row) => row[0]===name)) return;
        const values= entry.options.split(',').map( (s) => s.trim());
        values.forEach( (v) => rows.push([name,v]));
    });

    ALL_FALLBACK_META_OPTIONS.forEach( (entry) => {
        const name= toColName(entry.name);
        if (rows.some( (row) => row[0]===name)) return;
        const values= entry.options.split(',').map( (s) => s.trim());
        values.forEach( (v) => rows.push([name,v]));
    });

    ALL_FALLBACK_META_OPTIONS.forEach( (entry) => {
        const name= toColName(entry.name);
        if (!entry.optionNames && !entry.values) return;
        const optionValueAry= entry.options.split(',').map( (s) => s.trim());
        const optionNameAry= entry.optionNames.split(',').map( (s) => s.trim());
        if (optionNameAry.length!==optionValueAry.length) return;

        const optionRowsWithoutNames= rows.filter( (r) => r[0]===name && r[1] && !r[2]);

        optionRowsWithoutNames.forEach( (r) => {
            const idx= optionValueAry.findIndex( (v) => v===r[1]);
            if (idx===-1) return;
            r[2]= optionNameAry[idx];
        });
    });

    return {
        tableMeta: siaMeta.params.reduce( (obj, {name,desc}) => ( {...obj, [name]: desc}), {}),
        tableData: {
            columns: [
                {name:'column_name', type:'char'},
                {name:'column_options', type:'char'},
                {name:'column_labels', type:'char'}
            ],
            data: rows,
        },
        title: 'loadObsCoreMetadata',
        type: 'table',
        totalRows: rows.length
    };
}