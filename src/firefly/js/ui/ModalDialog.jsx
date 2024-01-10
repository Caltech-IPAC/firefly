import {Stack} from '@mui/joy';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import './ModalDialog.css';

export class ModalDialog extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {
            width: window.innerWidth,
            height: window.innerHeight,
        };
        this.browserResizeCallback = () => {
            if (!this.isUnmounted) {
                this.setState({width: window.innerWidth, height: window.innerHeight});
            }
        };
    }

    componentDidMount() {
        window.addEventListener('resize', this.browserResizeCallback);
    }

    componentWillUnmount() {
        this.isUnmounted = true;
        window.removeEventListener('resize', this.browserResizeCallback);
    }

    render() {
        const {width, height} = this.state;
        // make sure the modal fits into the viewport
        const wrapperStyle = {maxWidth: width, maxHeight: height, overflow: 'auto'};
        return (
            <Stack sx={{direction:'row', alignItems:'center', justifyContent:'center', zIndex: this.props.zIndex,
                       position: 'fixed', top: 0, left: 0, bottom: 0, right: 0, backgroundColor: 'rgba(0, 0, 0, 0.2)'
                   }}>
                <Stack {...{alignItems:'center', justifyContent:'space-between'}}>
                    <div className='ModalDialog__content' style={wrapperStyle}>
                        {this.props.children}
                    </div>
                </Stack>
            </Stack>
        );
    }
}

ModalDialog.propTypes= {
    zIndex : PropTypes.number
};