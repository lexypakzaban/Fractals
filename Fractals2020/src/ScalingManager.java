public class ScalingManager
{

    private DoubleRectangle windowBounds; // ... essentially the size of the window.

    private DoubleRectangle mathBounds;   // The mathematical coordinates of the equivalent points in the corners of the
                                        //     window. For example, we might have an 800 x 800 pixel window that
                                        //     corresponds to a graph from -2 to +2 on the real axis and -1.5 to +1.5
                                        //     on the imaginary axis. (This would be a rect of
                                        //     (x, y, w, h) --> (-2, -1.5, 4, 3).

    /**
     * constructor - if either window or math is null, it will create a non-null DoubleRectangle with default values.
     * @param window
     * @param math
     */
    public ScalingManager(DoubleRectangle window, DoubleRectangle math)
    {
        if (window != null)
            windowBounds = window;
        else
            windowBounds = new DoubleRectangle(0,0,800,790);
        if (math != null)
            setMathBounds( math);
        else
            setMathBounds(new DoubleRectangle(-2,-1.5,3,3));
    }

    public ScalingManager()
    {
        this(null,null);
    }

    public DoubleRectangle getWindowBounds()
    {
        return windowBounds;
    }

    public void setWindowBounds(DoubleRectangle windowBounds)
    {
        this.windowBounds = windowBounds;
    }

    public DoubleRectangle getMathBounds()
    {
        return mathBounds;
    }

    public void setMathBounds(DoubleRectangle mathBounds)
    {
        this.mathBounds = mathBounds;
        System.out.println(mathBounds);
    }

    /**
     * find the mathematical point that is found at the given screen locations. Rather than making a new point and
     * returning it, it will modify the one that is given to it, a slight speed improvement since we don't need to
     * allocate memory
     * @param cp - the mathematical point that this method will CHANGE to update with the output info
     * @param sp - the location in the window of interest.
     */
    public void mathPointForScreenPoint(DoublePoint cp, DoublePoint sp)
    {
        //TODO - you need to write this. Note that you are not returning the point, you are changing the values in "cp."
        // Hint: I suggest you make use of "map()," the next method in this class.

        cp.setXY(map(sp.getX(), windowBounds.getLeft(), windowBounds.getRight(), mathBounds.getLeft(), mathBounds.getRight()),
                 map(sp.getY(), windowBounds.getTop(), windowBounds.getBottom(), mathBounds.getTop(), mathBounds.getBottom()));

    }



    /**
     * considers a given sourceValue within a range from sourceMin to sourceMax and determines a destinationVaule at the
     * same relative position within a destMin - destMax range.
     * For example:
     *     sourceMin            sourceValue                                 sourceMax
     *     destMin              destValue                                   destMax
     *
     * if source Value is about 30% of the way between sourcemin and source max, we want to find a destinationValue that
     * is also 30% of the way between destmin and destmax.
     * prerequisite: sourceMin != sourceMax
     * Note: sourceValue does <i>not</i> need to fall between sourceMin and sourceMax.
     * @param sourceValue
     * @param sourceMin
     * @param sourceMax
     * @param destMin
     * @param destMax
     * @return the destination value.
     */
    public double map(double sourceValue, double sourceMin, double sourceMax, double destMin, double destMax)
    {
        if (sourceMin == sourceMax)
            throw new RuntimeException("Cannot map a point within a zero range - source min and max are both "+sourceMin+".");
        return destMin + (destMax-destMin) * (sourceValue-sourceMin)/(sourceMax-sourceMin);
    }
}
