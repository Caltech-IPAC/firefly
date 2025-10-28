import {number, shape, string, object} from 'prop-types';
import React, {Suspense, use} from 'react';
import {Box, Sheet, Skeleton, Stack, Typography} from '@mui/joy';
import * as ifWorking from '../InputFieldView';

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


export function IfWorkingMask({promise, message, gridSize, sx}) {
    const Empty= ({promise}) => void (promise && use(promise));
    return (
        <Suspense fallback={<WorkingAnimation {...{message, gridSize, sx}}/>}>
            <Empty promise={promise}/>
        </Suspense>
    );
}

IfWorkingMask.propTypes = {
    message: string,
    promise: object,
    gridSize: shape({rows: number, cols: number}),
    sx: object,
};

const WorkingAnimation= ({message, gridSize, sx}) => (
    <Box sx={{ position:'absolute', left:0, right:0, top:0, bottom:0, ...sx}}>
        {(gridSize?.rows || gridSize?.cols)
            ? <GridMask rows={gridSize?.rows} cols={gridSize?.cols}/>
            : <Skeleton variant='rectangular' sx={{width:1, height:1}}/>
        }
        <Typography {...{level:'h4', width:1, position:'absolute', top:'3rem', textAlign:'center', color:'neutral'}}>
            {message}
        </Typography>
    </Box>
);
