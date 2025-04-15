import {Stack, Typography} from '@mui/joy';
import React from 'react';
import {ObjectIDSearch} from './ObjectIDSearch';
import {ObsCoreSearch} from './ObsCore';
import {ExposureDurationSearch} from './ObsCoreExposureDuration';
import {getServiceMetaOptions, getSiaServiceId, makeObsCoreMetadataModel} from './SiaUtil';
import {SpatialSearch} from './SpatialSearch';
import {ObsCoreWavelengthSearch} from './WavelengthPanel';


export function SiaUI({initArgs, serviceUrl, serviceLabel, siaMeta, sx={}}) {

    const useSIAv2= true;
    const fallbackMetaOptions= getServiceMetaOptions(serviceLabel) ?? [];
    const obsCoreMetadataModel= makeObsCoreMetadataModel(siaMeta, fallbackMetaOptions);
    const showNoMetaError= !hasSomeImportantMeta(siaMeta);
    const serviceId= getSiaServiceId(serviceUrl);

    return (
        <Stack spacing={1} width={1} maxWidth='75rem' sx={sx}>
            {showNoMetaError && <Typography color='warning' pl={3}>
                {`Warning: The ${serviceLabel} SIAv2 service does not return any meta data to configure search panel`}
            </Typography>}
            <SpatialSearch {...{cols:undefined, serviceUrl, serviceLabel, serviceId, columnsModel:undefined, initArgs, obsCoreEnabled:false,
                tableName:undefined, useSIAv2,
                capabilities: {canUsePoint:true, canUseCircle:true, canUsePolygon:true, canUseContains:true}
            }}/>
            <ObsCoreSearch {...{obsCoreMetadataModel, serviceId, initArgs, useSIAv2,
                slotProps:{innerStack: {width:1}} }} />
            <ExposureDurationSearch {...{initArgs, useSIAv2}} />
            <ObsCoreWavelengthSearch {...{initArgs, serviceId, useSIAv2}} />
            <ObjectIDSearch {...{useSIAv2}}/>
        </Stack>
    );
}

const importantMetaParams= [ 'DPTYPE', 'BAND', 'CALIB', 'COLLECTION', 'FACILITY', 'INSTRUMENT' ];

const hasSomeImportantMeta= (siaMeta) =>
    Boolean(siaMeta?.params?.some( (entry) => importantMetaParams.includes(entry.name.toUpperCase())) );

