package edu.caltech.ipac.visualize.plot;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by zhang on 7/7/15.
 */
public class LSSTMask {

    private static Map<Color, Class<? extends LSSTMask>> registry = new HashMap<Color, Class<? extends LSSTMask>>();

    private Color color; // name of this mask item
    private String name;
    private int index; // i.e. bit offset
    private int bit; // the bit mask itself
    private int inv; // the complement of _bit
    private int ignore; // mask of bits to ignore
    private int check; // bit with ignore removed
    public static final int MASK32 = 0xffffffff;
    /**
     * Create a mask definition with the given color, name and bit offset.
     *
     * @param  bitOffset  index of the mask
     * @param  color       of the mask
     * @throws IllegalArgumentException if the offset is less than 0 or more than 63
     */
    public LSSTMask(int bitOffset, Color color, String name) {
        if (bitOffset < 0 || bitOffset >= 32) {
            throw new IllegalArgumentException("bit offset must be >= 0 and < 64, not " + bitOffset);
        }

        init(1 << bitOffset, color, bitOffset, name);
    }

    public LSSTMask(int bitOffset, Color color) {
        if (bitOffset < 0 || bitOffset >= 32) {
            throw new IllegalArgumentException("bit offset must be >= 0 and < 64, not " + bitOffset);
        }

        init(1 << bitOffset, color, bitOffset, color.toString());
    }
    /**
     * Register a class containing mask definitions.
     * @param color to be used as a lookup by the {@link #get(Color)} method
     * @param c class extending this one containing the definitions
     */
    public static void register(Color color, Class<? extends LSSTMask> c) {
        registry.put(color, c);
    }

    /**
     * Create a mask object from the given integer mask. Both single and multi-bit masks may be
     * created in this way.
     * @param mask integer mask
     */
    public LSSTMask(int mask) {

        int index = -1;
        String name = "multi-bit";

        // Find the number of bits set and the index of the MSB:
        int[] r = countBits(mask);

        if (r[0] == 1) {
            index = r[1];
            name = "bit-" + index;
        }

        init(mask, null, index,  name);
    }

    /**
     * Get the registry of all classes defining masks.
     * @return the defensive copy of registry
     */
    protected static Map<Color, Class<? extends LSSTMask>> getRegistry() {
        return new HashMap<Color, Class<? extends LSSTMask>>(registry);
    }

    /**
     * Get the class registered with the given name.
     * @param color  the one the class was registered with
     * @return the class corresponding to that name
     */
    public static Class<? extends LSSTMask> get(Color color) {
        return registry.get(color);
    }

    /**
     * Get all the masks defined by the given subclass.
     * @param <T> extends FixedMask
     * @param c a subclass defining mask fields
     * @return all the mask objects defined by those fields
     */
    public static <T extends LSSTMask> Collection<T> getMasks(Class<T> c) {
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

    private void init(int bitOffset, Color maskColor, int maskIndex, String maskName) {
        bit = bitOffset;
        check = bit;
        inv = bit ^ MASK32;
        color = maskColor;
        index = maskIndex;
        name=maskName;
    }

    /**
     * Get the name of this mask.
     * @return a string identifying the mask
     */
    public Color getColor() {
        return color;
    }

    public String getName() {
        return name;
    }
    /**
     * Get the integer value of this mask.
     * @return the value
     */
    public long getValue() {
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
     * Set the behaviour of the <code>isSet</code> methods to <em>ignore</em> the bit being set.
     * Note that this <em>does not</em> change the behaviour of the <code>set</code> or
     * <code>unset</code> methods. The purpose of this is to be able to turn off the applicability
     * of the bit during data processing without modifying the mask itself.
     * <p>
     * If set to <code>true</code>, then this is equivalent to setting the ignore mask for a single bit
     * only. It has no effect if the mask is multi-bit. If set to <code>false</code>, then the effect
     * is to clear the ignore mask.
     * @param ignore  if set to true, then the isSet methods will return false
     *
     */
    public void setIgnore(boolean ignore) {
        if (!ignore) {
            setIgnoreMask(0);
        } else if (index >= 0) {
            setIgnoreMask(1 << index);
        }
    }

    /**
     * The purpose of this is to be able to turn off the applicability of the bits during data processing
     * without modifying the mask itself.
     * @param mask of bits to be ignored
     */
    public void setIgnoreMask(int mask) {
        ignore = mask;
       check = bit & (ignore ^ MASK32);
    }

    /**
     * Get the mask of bits ignored by the <code>isSet</code> method.
     * @return mask of bits to be ignored
     */
    public int getIgnoreMask() {
        return ignore;
    }

    /**
     * Get the "ignore" attribute of this mask. This is true if an ignore mask is set.
     * @return the ignore attribute
     * @see #setIgnore(boolean)
     */
    public boolean isIgnore() {
        return ignore != 0;
    }

    /**
     * Set the bits of this mask in the given argument.
     * @param  mask  value in which to set the bits
     * @return the updated value
     */
    public final int set(int mask) {
        return mask | bit;
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
    public final boolean isSet(final short mask) {
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
     * If both bit are set, using the lowest bit Color
     * Logic inclusive bitwise or
     * example:
     *       0101 (decimal 5)            0010 (decimal 2)
     *  OR   0011 (decimal 3)         OR 1000 (decimal 8)
     *     = 0111 (decimal 7)         = 1010 (decimal 10)
     * Return a combined (or) mask
     * @param  rhs mask to apply
     * @return combined mask
     */
    public final LSSTMask or(LSSTMask rhs) {
        Color usingColor = bit<rhs.bit? color: rhs.color;
        return new LSSTMask(bit | rhs.bit, usingColor, name + " | " + rhs.name);
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
    public final LSSTMask xor(LSSTMask rhs) {
        Color usingColor = bit<rhs.bit? color: rhs.color;
        return new LSSTMask( bit ^ rhs.bit, usingColor,name + " | " + rhs.name);
    }

    /**
     * Check for equality. Masks are equal if their bit representation
     * is the same.
     * @param  mask to compare with
     * @return <code>true</code> if they are equal
     */

    public boolean equals(Object mask) {
        return bit == ((LSSTMask ) mask).bit;
    }


    public String toString() {
         return getName() + " @ " + index + " = " + getValue();
    }

    /**
     * Convenience method for creating a combination mask by <em>or</em>ing the input masks. This mask can
     * then be used to test if <em>any</em> of the mask bits it contains are set.
     * @param masks individual input masks
     * @return a combined mask with all the input bits sets
     * @throws NullPointerException if the input is <code>null</code>.
     */
    public static LSSTMask combine(LSSTMask... masks) {
        if (masks == null) {
            throw new NullPointerException("Please input valid FixedMask, not null.");
        } else if (masks.length == 0) {
            return new LSSTMask(0);
        } else {
            LSSTMask mask = masks[0];
            for (int i = 1; i < masks.length; i++) {
                mask = mask.or(masks[i]);
            }
            return mask;
        }
    }

    /**
     * Convenience method for creating a combination mask by specifying which masks to
     * include and which to exclude. If the same mask is present in both it is excluded.
     * This mask can then be used to test if <em>any</em> of the mask bits included are set.
     * @param masksToInclude individual input masks to include
     * @param masksToExclude individual input masks to exclude
     * @return a combined mask with all the included bits sets
     * @throws NullPointerException if any input is <code>null</code>.
     */
    public static LSSTMask combine(LSSTMask[] masksToInclude,LSSTMask[] masksToExclude) {

        if (masksToInclude == null || masksToExclude == null) {
            throw new NullPointerException("All inputs should be FixedMask arrays, not null.");
        }

        Set<LSSTMask> masks = new HashSet<LSSTMask>();
        for (LSSTMask mask : masksToInclude) {
            masks.add(mask);
        }
        for (LSSTMask mask : masksToExclude) {
            masks.remove(mask);
        }

        return combine(masks.toArray(new LSSTMask[0]));
    }



    /**
     * Change the bit value to 1
     * @param bitOffset
     * @param mask
     * @return
     */
    public static int set(int bitOffset, int mask) {
        return mask | (1 << bitOffset);
    }

    /**
     * Toggle a bit in mask value. If the bit value is 1, the toggled result is 0,
     * if the bit value is 0, the tagged value is 1.
     * @param  bitOffset position in mask to toggle
     * @param  mask      value to update
     * @return the updated value
     */
    public static int toggle(int bitOffset, int mask) {
        return mask ^ (1 << bitOffset);
    }

    /**
     * Change the bit value to 0
     * Unset a bit in mask value.
     * @param  bitOffset position in mask to set
     * @param  mask      value to update
     * @return the updated value
     */
    public static int unset(int bitOffset, int mask) {
        return (mask & ((1 << bitOffset) ^ MASK32));
    }


    /**
     * Count the bits that are set in an integer/mask. This method also returns the index of the MSB.
     * @param n the input
     * @return a two-element array containing:
     * <ol>
     * <li>The number of bits set.</li>
     * <li>The index of the MSB set. This is -1 if no bits are set.</li>
     * </ol>
     */
    public static int[] countBits(int n) {

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
