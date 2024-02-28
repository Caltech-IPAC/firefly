import {Sheet, Stack} from '@mui/joy';
import React from 'react';
import {node, shape, object, number, string} from 'prop-types';

/*
 * A convenience component built around Sheet and Stack to provide a <start main end> layout
 * with support for variant and color.  Use slopProps to customize
 */
export function Stacker({variant, color, endDecorator, startDecorator, gap=1, slotProps, children, ...props}) {

    return (
        <Sheet variant={variant} color={color} component={Stack}
               direction='row' justifyContent='space-between' alignItems='center' spacing={gap}
               {...slotProps?.root}>

            {startDecorator}
            <Stack spacing={1} direction='row' alignItems='center' flexGrow={1} {...props}>
                {children}
            </Stack>
            {endDecorator}

        </Sheet>
    );
}

Stacker.propTypes = {
    variant: string,
    color: string,
    endDecorator: node,
    startDecorator: node,
    gap: number,            // spacing between decorators and the main child
    slotProps: shape({
        root: object,
    })
};

