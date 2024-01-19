/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {LinearProgress, Sheet, Stack, Typography} from '@mui/joy';

export function AppInitLoadingMessage({message= 'Loading...'}) {
    return (
        <Stack position='relative' width={1} height={1}>
            <Stack m='auto' spacing={1} width={300} height={200}>
                <Sheet variant='soft'
                       sx={{flexGrow:1, display:'flex', justifyContent:'center', alignItems:'center'}}>
                    <Typography color='warning' level='h4'>
                        {message}
                    </Typography>
                </Sheet>
                <LinearProgress sx={{flexGrow:0}}/>
            </Stack>
        </Stack>
    );
}

