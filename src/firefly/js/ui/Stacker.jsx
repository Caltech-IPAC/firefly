import {Sheet, Stack} from '@mui/joy';
import React from 'react';
import {node, shape, object, number, string, oneOf} from 'prop-types';

/*
 * A convenience component built around Sheet and Stack to provide a <start main end> layout
 * with support for variant and color.  Use slopProps to customize
 */
export function Stacker({variant, color, orientation='horizontal', endDecorator, startDecorator, gap=1, slotProps, children, ...props}) {

    const component = (variant || color) && Sheet;
    const dir = orientation === 'horizontal' ? 'row' : 'column';
    return (
        <Stack variant={variant} color={color} component={component}
               direction={dir} spacing={gap} alignItems='center'
               {...slotProps?.root}>

            {startDecorator}
            <Stack spacing={1} direction={dir} flexGrow={1} {...props}>
                {children}
            </Stack>
            {endDecorator}

        </Stack>
    );
}

Stacker.propTypes = {
    variant: string,
    color: string,
    orientation: oneOf(['horizontal', 'vertical']),
    endDecorator: node,
    startDecorator: node,
    gap: number,            // spacing between decorators and the main child
    slotProps: shape({
        root: object,
    })
};

