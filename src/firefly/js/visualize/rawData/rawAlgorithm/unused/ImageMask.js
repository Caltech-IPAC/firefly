
import {isArray} from 'lodash';
import {isDefined} from '../../../../util/WebUtil.js';

const MASK32 = 0xffffffff;


/**
 * Created by zhang on 7/7/15.
 */
export class ImageMask {


    /**
     * constructor to create a new ImageMask
     * Example:
     *   ImageMask m = new ImageMask(0, Color.RED)
     *   it creates a new ImageMask object which the bit (bitoffste=0) set, the Color is read
     *   when the mask data has the first bit set, the color of the pixel will be red
     * @param obj
     * @param obj.index: the bit offset
     * @param obj.color
     * @param obj.bit
     * @param obj.mask
     */
    constructor({index = -1, color, bit, mask}) {
        if (isDefined(index)) {
            if ((index < 0 || index >= 32)) throw new Error('bit offset must be >= 0 and < 64, not ' + index);
            this.bit = 1 << index;
        } else if (isDefined(mask)) {
            this.bit = mask;
        } else {
            this.bit = bit;
        }

        if (isDefined(mask)) {
            let index = -1;
            const r = ImageMask.countBits(mask);
            if (r[0] === 1) index = r[1];
        }

        this.check = this.bit;
        this.inv = this.bit ^ MASK32;
        this.color = color;
        this.index = index;
    }

    /**
     * Get the name of this mask.
     * @return a string identifying the mask
     */
    getColor() {
        return this.color;
    }

    /**
     * Get the integer value of this mask.
     * @return the value
     */
    getValue() {
        return this.bit;
    }

    /**
     * Get the index of this mask bit ie the bit offset. This index is defined to be -1 for
     * multi-bit masks.
     * @return  the bit offset, -1 if multi-bit
     */
    getIndex() {
        return this.index;
    }

    /**
     * Does this mask object have multiple bits set? This is true if it doesn't refer to a single
     * bit offset.
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    isMultiBit() {
        return this.index === -1;
    }


    /**
     * Set the bits of this mask in each element of an array.
     * @param  mask  array in which to set the bits
     * @return the updated array
     */
    set(mask) {
        if (isArray(mask)) {
            for (let i = 0; i < mask.length; i++) {
                mask[i] = this.set(mask[i]);
            }
            return mask;
        } else {
            return this.bit | mask;
        }
    }

    /**
     * Toggle the bits of this mask in the given argument.
     * @param  mask  value in which to toggle the bits
     * @return the updated value
     */
    toggle(mask) {
        if (isArray(mask)) {
            for (let i = 0; i < mask.length; i++) {
                mask[i] = this.toggle(mask[i]);
            }
            return mask;
        } else {
            return mask ^ this.bit;
        }
    }

    /**
     * Clear the bits of this mask in the given argument.
     * @param  mask  value in which to unset the bits
     * @return the updated value
     */
    unset(mask) {
        if (isArray(mask)) {
            for (let i = 0; i < mask.length; i++) {
                mask[i] = this.unset(mask[i]);
            }
            return mask;
        } else {
            return mask & this.inv;
        }
    }


    /**
     * Return a boolean array indicating where any mask bits are set.
     * @param  mask  array of values in which to test the mask
     * @return <code>true</code> where any bit is set, for each element
     */
    isSet(mask) {
        if (isArray(mask)) {
            const b = [];
            for (let i = 0; i < mask.length; i++) {
                b[i] = this.isSet(mask[i]);
            }
            return b;
        } else {
            return (this.check & mask) !== 0;
        }
    }

    /**
     * Return a combined (and) mask, the color is the lowest index's color
     * @param  rhs mask to apply
     * @return combined mask
     */
    and(rhs) {
        const usingColor = this.index < rhs.index ? this.color : rhs.color;
        return new ImageMask({color: usingColor, bit: this.bit & rhs.bit});
    }

    /**
     * Convenience method for creating a combination mask by <em>and</em>ing the input masks. This mask can
     * then be used to test if all of the mask bits it contains are set.
     * @param masks individual input masks
     * @return a combined mask with all the input bits sets
     * @throws NullPointerException if the input is <code>null</code>.
     */
    static combineWithAnd(masks) {
        if (!masks) {
            throw new Error('input valid FixedMask, not null.');
        } else if (!masks.length) {
            return new ImageMask({mask: 0});
        } else {
            let mask = masks[0];
            for (let i = 1; i < masks.length; i++) {
                mask = mask.and(masks[i]);
            }
            return mask;
        }
    }

    /**
     * If both bit are set, using the lowest bit Color
     * Logic inclusive bitwise or
     * example:
     *       0101 (decimal 5)            0010 (decimal 2)
     *  OR   0011 (decimal 3)         OR 1000 (decimal 8)
     *     = 0111 (decimal 7)         = 1010 (decimal 10)
     * Return a combined (or) mask with the color at the lower bitoffset
     * @param  rhs mask to apply
     * @return combined mask
     */
    or(rhs) {
        const usingColor = this.index < rhs.index ? this.color : rhs.color;
        return new ImageMask({color: usingColor, bit: this.bit | rhs.bit});
    }

    /**Logic exclusive or
     * Examples
     *      0101 (decimal 5)                     0010 (decimal 2)
     *  XOR 0011 (decimal 3)                 XOR 1010 (decimal 10)
     *    = 0110 (decimal 6)                   = 1000 (decimal 8)
     * Return a combined (xor) mask
     * @param  rhs mask to apply
     * @return combined mask
     */
    xor(rhs) {
        const usingColor = this.index < rhs.index ? this.color : rhs.color;
        return new ImageMask({color: usingColor, bit: this.bit ^ rhs.bit});
    }

    /**
     * Check for equality. Masks are equal if their bit representation
     * is the same.
     * @param  mask to compare with
     * @return <code>true</code> if they are equal
     */
    equals(mask) {
        return this.bit === mask.bit;
    }


    /**
     * Convenience method for creating a combination mask by <em>or</em>ing the input masks. This mask can
     * then be used to test if <em>any</em> of the mask bits it contains are set.
     * @param masks individual input masks
     * @return a combined mask with all the input bits sets
     * @throws NullPointerException if the input is <code>null</code>.
     */
    static combineWithOr(masks) {
        if (!masks) {
            throw new Error('Please input valid FixedMask, not null.');
        } else if (!masks.length) {
            return new ImageMask({mask: 0});
        } else {
            let mask = masks[0];
            for (let i = 1; i < masks.length; i++) {
                mask = mask.or(masks[i]);
            }
            return mask;
        }
    }



    /**
     * Count the bits that are set in an integer/mask. This method also returns the index of the MSB.
     * @param n the input
     * @return a two-element array containing:
     */
    static countBits(n) {
        let count = 0;
        let index = -1;

        while (n !== 0) {
            index++;
            if ((n & 1) === 1) {
                count++;
            }
            n >>>= 1;
        }
        return [count, index];
    }
}


