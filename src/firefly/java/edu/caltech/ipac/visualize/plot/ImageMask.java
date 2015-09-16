package edu.caltech.ipac.visualize.plot;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by zhang on 7/7/15.
 */
public class ImageMask {


    private Color color; // name of this mask item
    private int index; // i.e. bit offset
    private int bit; // the bit mask itself
    private int inv; // the complement of _bit
    private int check; // bit with ignore removed
    public static final int MASK32 = 0xffffffff;

    /**
     * contrusctor to createa new ImageMask
     * Example:
     *   ImageMask m = new ImageMask(0, Color.RED)
     *   it creates a new ImageMask object which the bit (bitoffste=0) set, the Color is read
     *   when the mask data has the first bit set, the color of the pixel will be red
     * @param index: the bit offset
     * @param color
     */
    public ImageMask(int index, Color color) {
        if (index < 0 || index >= 32) {
            throw new IllegalArgumentException("bit offset must be >= 0 and < 64, not " + index);
        }

        init(1 << index, color, index);
    }
    /**
     * Create a mask definition with the given color, name and bit offset.
     * @param  bit  the bit mask
     * @param  color  the color of the mask
     * @throws IllegalArgumentException if the offset is less than 0 or more than 63
     */
    public ImageMask(Color color, int bit) {
        init(bit, color, -1);
    }


    /**
     * Create a mask object from the given integer mask. Both single and multi-bit masks may be
     * created in this way.
     * @param mask integer mask
     */
    public ImageMask(int mask) {

        int index = -1;
        String name = "multi-bit";

        // Find the number of bits set and the index of the MSB:
        int[] r = countBits(mask);

        if (r[0] == 1) { //only one bit is set
            index = r[1];
            name = "bit-" + index;
        }

        init(mask, null, index);
    }


    /**
     * Get all the masks defined by the given subclass.
     * @param <T> extends FixedMask
     * @param c a subclass defining mask fields
     * @return all the mask objects defined by those fields
     */
    public static <T extends ImageMask> Collection<T> getMasks(Class<T> c) {
        Collection<T> list = new ArrayList<T>();

        for (Field field : c.getDeclaredFields()) {
            Object o = null;
            try {
                o = field.get(null);
            } catch (IllegalAccessException x) {
                continue; // not a publicly accessible field
            } catch (NullPointerException x) {
                continue; // non-static field
            }

            if (o == null || !(c.isInstance(o))) {
                continue;
            }
            list.add(c.cast(o));
        }
        return list;
    }

    private void init(int bit, Color maskColor, int maskIndex) {
        this.bit = bit;
        check = this.bit;
        inv = this.bit ^ MASK32;
        color = maskColor;
        index = maskIndex;

    }

    /**
     * Get the name of this mask.
     * @return a string identifying the mask
     */
    public Color getColor() {
        return color;
    }

    /**
     * Get the integer value of this mask.
     * @return the value
     */
    public int getValue() {
        return bit;
    }

    /**
     * Get the index of this mask bit ie the bit offset. This index is defined to be -1 for
     * multi-bit masks.
     * @return  the bit offset, -1 if multi-bit
     */
    public int getIndex() {
        return index;
    }

    /**
     * Does this mask object have multiple bits set? This is true if it doesn't refer to a single
     * bit offset.
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    public boolean isMultiBit() {
        return index == -1;
    }

    /**
     *
     * @param mask
     * @return
     */
    public  int set(int  mask) {
        return  bit|mask;
    }

    /**
     * Set the bits of this mask in each element of an array.
     * @param  mask  array in which to set the bits
     * @return the updated array
     */
    public int[] set(int[] mask) {
        for (int i = 0; i < mask.length; i++) {
            mask[i]=set(mask[i]);

        }
        return mask;
    }


    /**
     * Toggle the bits of this mask in the given argument.
     * @param  mask  value in which to toggle the bits
     * @return the updated value
     */
    public final int toggle(int mask) {
        return mask^bit;
    }

    /**
     * Toggle the bits of this mask in each element of an array.
     * @param  mask  array in which to toggle the bits
     * @return the updated array
     */
    public int[] toggle(int[] mask) {
        for (int i = 0; i < mask.length; i++) {
            mask[i]=toggle(mask[i]);
        }
        return mask;
    }


    /**
     * Clear the bits of this mask in the given argument.
     * @param  mask  value in which to unset the bits
     * @return the updated value
     */
    public final int unset(int mask) {
        return mask & inv;
    }

    /**
     * Clear the bits of this mask in each element of an array.
     * @param  mask  array in which to unset the bits
     * @return the updated array
     */
    public int[] unset(int[] mask) {
        for (int i = 0; i < mask.length; i++) {
            mask[i]=unset(mask[i]);
        }
        return mask;
    }




    /**
     * Check if any of the bits in this mask are set in the given argument.
     * @param  mask value to check
     * @return <code>true</code> if any are set
     */
    public final boolean isSet(final int mask) {
        return (check & mask) != 0;
    }

    /**
     * Return a boolean array indicating where any mask bits are set.
     * @param  mask  array of values in which to test the mask
     * @return <code>true</code> where any bit is set, for each element
     */
    public Boolean[] isSet(final short[] mask) {
        Boolean[] b = new Boolean[mask.length];
        for (int i = 0; i < mask.length; i++) {
            b[i]=isSet(mask[i]);
        }
        return b;
    }

       /**
     * Return a combined (and) mask, the color is the lowest index's color
     * @param  rhs mask to apply
     * @return combined mask
     */
    public final ImageMask and(ImageMask rhs) {
        Color usingColor = index<rhs.index? color: rhs.color;
        return new ImageMask(usingColor , bit & rhs.bit);
    }

    /**
     * Convenience method for creating a combination mask by <em>and</em>ing the input masks. This mask can
     * then be used to test if all of the mask bits it contains are set.
     * @param masks individual input masks
     * @return a combined mask with all the input bits sets
     * @throws NullPointerException if the input is <code>null</code>.
     */
    public static ImageMask combineWithAnd(ImageMask... masks) {
        if (masks == null) {
            throw new NullPointerException("Please input valid FixedMask, not null.");
        } else if (masks.length == 0) {
            return new ImageMask(0);
        } else {
            ImageMask mask = masks[0];
            for (int i = 1; i < masks.length; i++) {
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
    public final ImageMask or(ImageMask rhs) {
        Color usingColor = index<rhs.index? color: rhs.color;
        return new ImageMask( usingColor, bit | rhs.bit);
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
    public final ImageMask xor(ImageMask rhs) {
        Color usingColor = index<rhs.index? color: rhs.color;
        return new ImageMask( usingColor,bit ^ rhs.bit);
    }

    /**
     * Check for equality. Masks are equal if their bit representation
     * is the same.
     * @param  mask to compare with
     * @return <code>true</code> if they are equal
     */

    public boolean equals(Object mask) {
        return bit == ((ImageMask) mask).bit;
    }


    /**
     * Convenience method for creating a combination mask by <em>or</em>ing the input masks. This mask can
     * then be used to test if <em>any</em> of the mask bits it contains are set.
     * @param masks individual input masks
     * @return a combined mask with all the input bits sets
     * @throws NullPointerException if the input is <code>null</code>.
     */
    public static ImageMask combineWithOr(ImageMask... masks) {
        if (masks == null) {
            throw new NullPointerException("Please input valid FixedMask, not null.");
        } else if (masks.length == 0) {
            return new ImageMask(0);
        } else {
            ImageMask mask = masks[0];
            for (int i = 1; i < masks.length; i++) {
                mask = mask.or(masks[i]);
            }
            return mask;
        }
    }





    /**
     * Toggle a bit in mask value. If the bit value is 1, the toggled result is 0,
     * if the bit value is 0, the tagged value is 1.
     * @param  index position in mask to toggle
     * @param  mask      value to update
     * @return the updated value
     */
    public static int toggle(int index, int mask) {
        return mask ^ (1 << index);
    }

    /**
     * Change the bit value to 0
     * Unset a bit in mask value.
     * @param  index position in mask to set
     * @param  mask      value to update
     * @return the updated value
     */
    public static int unset(int index, int mask) {
        return (mask & ((1 << index) ^ MASK32));
    }


    /**
     * Count the bits that are set in an integer/mask. This method also returns the index of the MSB.
     * @param n the input
     * @return a two-element array containing:
     */
    private static int[] countBits(int n) {

        int count = 0;
        int index = -1;

        while (n != 0) {
            index++;
            if ((n & 1) == 1) {
                count++;
            }
            n >>>= 1;
        }

        return new int[] { count, index };
    }


}
