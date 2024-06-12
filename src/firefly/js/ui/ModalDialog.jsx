import {Alert, Stack} from '@mui/joy';
import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';


export function ModalDialog({zIndex, sx, children}) {
    const [dimensions, setDimensions] = useState({ width: window.innerWidth, height: window.innerHeight });
    const handleResize = () => setDimensions({ width: window.innerWidth, height: window.innerHeight });

    useEffect(() => {
        window.addEventListener('resize', handleResize);
        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, []);

    const {width, height} = dimensions;
    // make sure the modal fits into the viewport
    const wrapperSx = {maxWidth: width, maxHeight: height, overflow: 'auto'};

    return (
        <Stack sx={{direction:'row', alignItems:'center', justifyContent:'center', zIndex,
                   position: 'fixed', top: 0, left: 0, bottom: 0, right: 0, backgroundColor: 'background.backdrop'
               }}>
            <Alert color='primary' size='lg' variant='outlined' sx={{boxShadow: 'lg', ...wrapperSx, ...sx}}>
                {children}
            </Alert>
        </Stack>
    );
}

ModalDialog.propTypes= {
    zIndex : PropTypes.number,
    sx: PropTypes.object
};