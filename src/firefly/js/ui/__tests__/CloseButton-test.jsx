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

    it('renders the default label', () => {
        render(<CloseButton/>);
        expect(screen.getByRole('button', {name: /close/i})).toBeInTheDocument();
    });

    // The visible label and the accessible name come from different props: Tooltip puts
    // `tip` on the button as aria-label, which wins over the rendered text.
    it('renders a caller-supplied label', () => {
        render(<CloseButton text='Dismiss' tip='Dismiss this panel'/>);

        expect(screen.getByText('Dismiss')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Dismiss this panel'})).toBeInTheDocument();
    });

    it('invokes onClick when pressed', async () => {
        const onClick = jest.fn();
        render(<CloseButton onClick={onClick}/>);

        await userEvent.click(screen.getByRole('button', {name: /close/i}));

        expect(onClick).toHaveBeenCalledTimes(1);
    });
});
