/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {getConnectionStatus} from '../core/AppDataCntlr';
import {useStoreConnector} from './SimpleComponent';
import {Alert, Stack, Typography} from '@mui/joy';

/**
 * When connection is lost, a warning indicator will appear.
 */

export function LostConnection () {

    const {lost, reason} = useStoreConnector(() => getConnectionStatus());
    if (!lost) return null;
    return (
        <Alert variant='soft' color='danger' size='sm' sx={{justifyContent:'center', borderRadius: 'unset'}}>
            <Stack>
                <Typography color='danger' level='h4'>{reason}</Typography>
                <Typography color='danger' level='data-sm'>Try reloading the page to reconnect. If the problem persists, please contact support.</Typography>
            </Stack>
        </Alert>
    );}
