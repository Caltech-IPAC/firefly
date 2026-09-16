/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*eslint-env node, jest */

import React from 'react';
import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {CloseButton} from '../CloseButton.jsx';

describe('CloseButton', () => {

    it('renders a button labeled Close', () => {
        render(<CloseButton/>);
        expect(screen.getByRole('button')).toHaveTextContent('Close');
    });

    it('invokes onClick when pressed', async () => {
        const onClick = jest.fn();
        render(<CloseButton onClick={onClick}/>);

        await userEvent.click(screen.getByRole('button', {name: /close/i}));

        expect(onClick).toHaveBeenCalledTimes(1);
    });
});
