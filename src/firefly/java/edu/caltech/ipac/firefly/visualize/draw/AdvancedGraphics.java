/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 5/2/14
 * Time: 2:50 PM
 */


import edu.caltech.ipac.firefly.visualize.ScreenPt;

/**
 * @author Trey Roby
 */
public interface AdvancedGraphics extends Graphics {


    public void setShadowPerm(Shadow s);
    public void setShadowForNextDraw(Shadow s);
    public void clearShadow();

    public void setTranslationPerm(ScreenPt pt);
    public void setTranslationForNextDraw(ScreenPt pt);
    
    /**
     * Rotate next draw by an angle
     * @param radAngle angle to rotate in radians
     */
    public void setRotationForNextDraw(double ang0);
    public void clearTranslation();


    public void copyAsImage(AdvancedGraphics g);
    public CanvasPanel getCanvasPanel();


    public static class Shadow {
        private final double blur;
        private final double offX;
        private final double offY;
        private final String color;

        public Shadow(double blur, double offX, double offY, String color) {
            this.blur = blur;
            this.offX = offX;
            this.offY = offY;
            this.color = color;
        }

        public double getBlur() { return blur; }
        public double getOffX() { return offX; }
        public double getOffY() { return offY; }
        public String getColor() { return color; }
    }
}

