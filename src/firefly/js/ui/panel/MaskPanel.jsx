import React from 'react';
import {Sheet, Skeleton, Stack} from '@mui/joy';

export function TableMask({cols=5, withToolbar=true, ...props}) {
    return (
        <Stack component={Sheet} spacing={1/2} height={1} {...props}>
            {withToolbar && <Skeleton variant='rectangular' height={30}/>}
            <Stack direction='row' spacing={1/2} height={30}>
                {[...Array(cols).keys()].map((value) => (
                    <Skeleton key={value} variant='rectangular'/>
                ))}
            </Stack>
            <Stack direction='row' spacing={1/2} flexGrow={1}>
                {[...Array(cols).keys()].map((value) => (
                    <Skeleton key={value} variant='rectangular'/>
                ))}
            </Stack>
        </Stack>
    );
}

export function GridMask({rows=3, cols=3, ...props}) {
    return (
        <Stack component={Sheet} spacing={1/2} height={1} {...props}>
            {[...Array(rows).keys()].map((idx) => (
                <Stack key={idx} direction='row' spacing={1/2} flexGrow={1}>
                    {[...Array(cols).keys()].map((idx) => (
                        <Skeleton key={idx} variant='rectangular'/>
                    ))}
                </Stack>
            ))}
        </Stack>
    );
}

export function FormMask({rows=10, sx, ...props}) {
    return (
        <Stack component={Sheet} direction='row' spacing={1/2} height={1} sx={sx} {...props}>
            <Stack spacing={1/2} flexGrow={1}>
                {[...Array(rows).keys()].map((value) => (
                    <Skeleton key={value} variant='rectangular' sx={{flexGrow:1}}/>
                ))}
            </Stack>
            <Stack spacing={1/2} flexGrow={3}>
                {[...Array(rows).keys()].map((value) => (
                    <Skeleton key={value} variant='rectangular'sx={{flexGrow:1}}/>
                ))}
            </Stack>
        </Stack>
    );
}

