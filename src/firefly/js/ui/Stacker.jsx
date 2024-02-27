import {Stack} from '@mui/joy';
import React from 'react';
import {node, shape, object, number} from 'prop-types';


export function Stacker({endDecorator, startDecorator, gap=1, slotProps, children, ...props}) {

    return (
        <Stack direction='row' justifyContent='space-between' alignItems='center' spacing={gap} {...slotProps?.root}>
            {startDecorator}
            <Stack spacing={1} direction='row' alignItems='center' flexGrow={1} {...props}>
                {children}
            </Stack>
            {endDecorator}
        </Stack>
    );
}

Stacker.propTypes = {
    endDecorator: node,
    startDecorator: node,
    gap: number,            // spacing between decorators and the main child
    slotProps: shape({
        root: object,
    })
};

