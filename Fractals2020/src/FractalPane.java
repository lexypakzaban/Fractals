import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class FractalPane extends JPanel implements MouseListener, MouseMotionListener
{

    private BufferedImage workspace; // a window-sized chunk of memory where we'll draw the fractal. Periodically, we'll
                                     //        copy this to the window.
    private Object workspaceMutex;   // a "mutex" (a.k.a. "traffic control") that ensures only one thing uses/changes
                                     //        the workspace at a time. (Definitely not on the AP.)

    ScalingManager scaleManager;

    private boolean isDragging;  // is the user currently dragging a selection box?
    private DoublePoint dragStart, dragEnd; // the location in the window where the drag started and where it is currently
                                            //      (or where it just ended).

    private CalculationThread calculationThread;  // An instance of a class that will execute a run() function
                                                  //      simultaneously with the code in this class. (Definitely not on the AP.)

    private Font displayFont;

    private Point mousepos;

    public FractalPane()
    {
        super();
        scaleManager = new ScalingManager();

        workspace = new BufferedImage((int)scaleManager.getWindowBounds().getWidth(),(int)scaleManager.getWindowBounds().getHeight(),BufferedImage.TYPE_INT_RGB);
        workspaceMutex = new Object();

        isDragging = false;

        calculationThread = new CalculationThread();
        calculationThread.start();

        dragStart = new DoublePoint();
        dragEnd = new DoublePoint();

        this.addMouseListener(this);
        this.addMouseMotionListener(this);

    }

    public void paintComponent(Graphics g)
    {
        // wait for the workspace to be available, then "lock" it so that we can use it briefly.
        synchronized (workspaceMutex)
        {
            g.drawImage(workspace,0,0,null);
        } // (ok, we're done with it.)

        // if the user is dragging a box, we can now draw the box on top of the copied workspace.
        if (isDragging)
        {
            //pick a random color black, red, green, blue, yellow, cyan, magenta, or white.
            g.setColor(new Color(255*(int)(Math.random()+0.5),255*(int)(Math.random()+0.5),255*(int)(Math.random()+0.5)));

            DoubleRectangle box = new DoubleRectangle(dragStart,dragEnd);
            g.drawRect((int)box.getX(), (int)box.getY(),(int)box.getWidth(), (int)box.getHeight());
        }

        // if the calculation thread is doing a calculation pass, draw a little red dot in the upper left corner.
        if (calculationThread.isScanning())
        {
            g.setColor(Color.RED);
            g.fillOval(2,2,10,10);
            g.setColor(Color.BLACK);
            g.drawOval(2,2,10,10);
        }





    }

//    /**
//     * find the mathematical point that is found at the given screen locations. Rather than making a new point and
//     * returning it, it will modify the one that is given to it, a slight speed improvement since we don't need to
//     * allocate memory
//     * @param cp - the mathematical point that this method will CHANGE to update with the output info
//     * @param sp - the location in the window of interest.
//     */
//    public void mathPointForScreenPoint(DoublePoint cp, DoublePoint sp)
//    {
//        cp.setX(map(sp.getX(), 0,windowBounds.getWidth(), mathBounds.getX(), mathBounds.getX()+mathBounds.getWidth()));
//        cp.setY(map(sp.getY(), 0,windowBounds.getHeight(), mathBounds.getY(), mathBounds.getY()+mathBounds.getHeight()));
//    }
//
//
//    /**
//     * considers a given sourceValue within a range from sourceMin to sourceMax and determines a destinationVaule at the
//     * same relative position within a destMin - destMax range.
//     * For example:
//     *     sourceMin            sourceValue                                 sourceMax
//     *     destMin              destValue                                   destMax
//     *
//     * if source Value is about 30% of the way between sourcemin and source max, we want to find a destinationValue that
//     * is also 30% of the way between destmin and destmax.
//     * prerequisite: sourceMin != sourceMax
//     * Note: sourceValue does <i>not</i> need to fall between sourceMin and sourceMax.
//     * @param sourceValue
//     * @param sourceMin
//     * @param sourceMax
//     * @param destMin
//     * @param destMax
//     * @return the destination value.
//     */
//    public double map(double sourceValue, double sourceMin, double sourceMax, double destMin, double destMax)
//    {
//        if (sourceMin == sourceMax)
//            throw new RuntimeException("Cannot map a point within a zero range - source min and max are both "+sourceMin+".");
//        return destMin + (destMax-destMin) * (sourceValue-sourceMin)/(sourceMax-sourceMin);
//    }





    @Override
    /**
     * the user has the mouse button pressed and the mouse has just changed position. This is the response.
     * Since we are in the midst of a drag operation, update the drag area on the screen.
     */
    public void mouseDragged(MouseEvent e) //overriding method in MouseMotionListener interface
    {
        dragEnd.setXY(e.getX(),e.getY());
        repaint();
    }

    @Override
    /**
     * The user has the mouse button unpressed and the mouse just changed position. This is the response.
     */
    public void mouseMoved(MouseEvent e) //overriding method in MouseMotionListener interface
    {
        // Nothing in this one - we don't have any plans to do anything when the mouse moves and is not dragged, but we
        //    are committed to having this method.

    }

    @Override
    /**
     * The user has just released the mousebutton in the same place as where it was pressed. This is the response.
     */
    public void mouseClicked(MouseEvent e) //overriding method in MouseListener interface
    {
        // Nothing in this one, either. We aren't particularly interested in the user releasing at the same place as
        //    he/she pressed the mouse, but (again) we are committed to having this method.
    }

    @Override
    /**
     * The user has just changed the mouse button from unpressed to pressed. This is the response.
     * The user is starting a drag... so start handling the drag.
     */
    public void mousePressed(MouseEvent e) //overriding method in MouseListener interface
    {
        isDragging = true;
        dragStart.setXY(e.getX(),e.getY());
        dragEnd.setXY(e.getX(),e.getY());
        repaint();
    }

    @Override
    /**
     * The user has just changed the mouse button from pressed to unpressed. This is the response.
     * Since this would suggest that the user has been dragging a zoom box, and now it is done, we want to perform the
     * zoom.
     */
    public void mouseReleased(MouseEvent e) //overriding method in MouseListener interface
    {
        dragEnd.setXY(e.getX(),e.getY());
        isDragging = false;
        if (dragEnd.getX() != dragStart.getX() && dragStart.getY() != dragEnd.getY())
        {
            if (e.isShiftDown())
                zoomOut();
            else
                zoomIn();
            repaint();
        }
    }

    @Override
    /**
     * the user has just moved the mouse into this panel. This is the response
     */
    public void mouseEntered(MouseEvent e) //overriding method in MouseListener interface
    {
        // Nope, not interested in this one, either.
    }

    @Override
    /**
     * the user has just moved the mouse out of this panel. This is the response.
     * In this case, we want to cancel the drag.
     */
    public void mouseExited(MouseEvent e) //overriding method in MouseListener interface
    {
        isDragging = false;
        repaint();
    }

    /**
     * The user has just released the mouse while the shift key was down, and we want to change the mathematical bounds
     * so that the corners of the window appear to "contract" into the selected area.
     */
    public void zoomOut()
    {
        // make a DoubleRectangle of dragStart and dragEnd locations. This will automatically make it think in terms of
        //      (x,y) of top left corner and (width/length) - even if the dragEnd isn't below and to the right of dragStart.
        DoubleRectangle dragRect = new DoubleRectangle(dragStart,dragEnd);

        // Calculate the new mathBounds we would need at the window's corners to shrink the current mathbounds
        //     (currently at the window's corners)  to get shrunk in so that they are found at the locations in the
        //     window set by the dragged rect.
        //     Note: we will use the original mathBounds for all four computations, we don't want to change mathBounds
        //     until we are done with all four.
        //     Also note: this is similar to zoomIn, but is different.
        DoubleRectangle mathBounds = scaleManager.getMathBounds();
        DoubleRectangle windowBounds = scaleManager.getWindowBounds();
        double tempMinX = scaleManager.map(0,dragRect.getLeft(),dragRect.getRight(),mathBounds.getLeft(),mathBounds.getRight());
        double tempMaxX = scaleManager.map(windowBounds.getRight(),dragRect.getLeft(),dragRect.getRight(),mathBounds.getLeft(),mathBounds.getRight());
        double tempMinY = scaleManager.map(0,dragRect.getTop(),dragRect.getBottom(),mathBounds.getTop(),mathBounds.getBottom());
        double tempMaxY = scaleManager.map(windowBounds.getBottom(),dragRect.getTop(),dragRect.getBottom(),mathBounds.getTop(),mathBounds.getBottom());
        scaleManager.setMathBounds(new DoubleRectangle(tempMinX,tempMinY,tempMaxX-tempMinX,tempMaxY-tempMinY));



        // ---- The rest of this is just fancy. When we zoom out a step, instead of just restarting our scan with a blank
        //          screen, we are going to copy a scalled-down version of the window into the dragged area, so we can
        //          see the relationship between the previous view and this one as it loads.

        // find the ratio between the size of the dragged box and the size of the window. This will be used to determine
        //     the spacing of the pixels we'll be copying.
        double horizontalRatio = windowBounds.getWidth()/dragRect.getWidth();
        double verticalRatio = windowBounds.getHeight()/dragRect.getHeight();

        // wait until the workspace is available, and then lock it so that others can't mess with it for a while.
        synchronized ((workspaceMutex))
        {
            //Make a copy of the workspace.
            BufferedImage tempImage = new BufferedImage(workspace.getWidth(),workspace.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics temp_g = tempImage.getGraphics();
            temp_g.drawImage(workspace,0,0,null);

            // for each pixel in the dragged rect (which is smaller than the window) copy a pixel from the workspace copy
            // into the shrunk-down area in the real workspace.
            for (int i=(int)dragRect.getLeft(); i<dragRect.getRight(); i++)
                for (int j = (int)dragRect.getTop(); j<dragRect.getBottom(); j++)
                {
                    workspace.setRGB(i,j,tempImage.getRGB((int)((i-dragRect.getLeft())*horizontalRatio),(int)((j-dragRect.getTop())*verticalRatio)));

                }

        }//release the workspace lock!
        // -----

        // Tell the calculation thread that it should stop the current scan (if any) and start on a new one.
        calculationThread.resetScan();

        // we've made changes to the workspace - make sure they get drawn to the screen the next chance we get!
        repaint();
    }

    /**
     * The user just released the mouse to select a drag area - we want the computer to "expand" this area to the size
     * of the window and scan that area in more detail.
     */
    public void zoomIn()
    {
        // make a DoubleRectangle of dragStart and dragEnd locations. This will automatically make it think in terms of
        //      (x,y) of top left corner and (width/length) - even if the dragEnd isn't below and to the right of dragStart.
        DoubleRectangle dragRect = new DoubleRectangle(dragStart,dragEnd);

        // Calculate the mathematical equivalences of the left and right "x" values and the top and bottom "y" values.
        //     Since these calculations depend on mathBounds for all four computations, we don't want to change mathBounds
        //     until we are done with all four. NOTE: this is similar to ZoomOut, but different.
        DoubleRectangle mathBounds = scaleManager.getMathBounds();
        DoubleRectangle windowBounds = scaleManager.getWindowBounds();
        double tempMinX = scaleManager.map(dragRect.getX(), 0, windowBounds.getWidth(), mathBounds.getX(),mathBounds.getX()+mathBounds.getWidth());
        double tempMaxX = scaleManager.map(dragRect.getX()+dragRect.getWidth(), 0, windowBounds.getWidth(), mathBounds.getX(),mathBounds.getX()+mathBounds.getWidth());
        double tempMinY = scaleManager.map(dragRect.getY(), 0, windowBounds.getHeight(), mathBounds.getY(),mathBounds.getY()+mathBounds.getHeight());
        double tempMaxY = scaleManager.map(dragRect.getY()+dragRect.getHeight(), 0, windowBounds.getHeight(), mathBounds.getY(),mathBounds.getY()+mathBounds.getHeight());
        scaleManager.setMathBounds(new DoubleRectangle(tempMinX,tempMinY,tempMaxX-tempMinX,tempMaxY-tempMinY));
        System.out.println(mathBounds);


        // ---- The rest of this is just fancy. When we zoom in a step, instead of just restarting our scan with a blank
        //          screen, we are going to copy a blown-up version of the image in the drag window into the window's
        //          workspace, so we can see the relationship between the previous view and this one as it loads.

        // find the ratio between the size of the dragged box and the size of the window. This will be used to determine
        //     the sise of the boxes we'll be drawing.
        double horizontalRatio = windowBounds.getWidth()/dragRect.getWidth();
        double verticalRatio = windowBounds.getHeight()/dragRect.getHeight();


        // make a blank BufferedImage the size of the draggedRect we're going to copy that portion of the workspace into
        //     it in a moment.
        BufferedImage tempImage = new BufferedImage((int)(dragRect.getWidth()),(int)dragRect.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics temp_g = tempImage.getGraphics();

        // wait until the workspace is available, and then lock it so that others can't mess with it for a while.
        synchronized (workspaceMutex)
        {
            // copy the dragRectangle area from the current workspace into the temp BI we just created.
            temp_g.drawImage(workspace.getSubimage((int)dragRect.getX(), (int)dragRect.getY(), (int)dragRect.getWidth(), (int)dragRect.getHeight()),0,0,null);
            // draw boxes of color into the workspace that correspond to the pixels in the temp image.
            Graphics work_g = workspace.getGraphics();
            for (int i=(int)dragRect.getWidth()-1; i>-1; i--)
                for (int j=(int)dragRect.getHeight()-1; j>-1; j--)
                {
                    work_g.setColor(new Color(tempImage.getRGB(i,j)));
                    work_g.fillRect((int)(i*horizontalRatio),(int)(j*verticalRatio),(int)horizontalRatio+1,(int)verticalRatio+1);
                }
        } //release the workspace lock!
        // -----

        // Tell the calculation thread that it should stop the current scan (if any) and start on a new one.
        calculationThread.resetScan();

        // we've made changes to the workspace - make sure they get drawn to the screen the next chance we get!
        repaint();
    }


    //------------------------------------------------------------------------- Calculation Thread
    // This is using two things that definitely aren't on the AP test - an internal class that extends Thread so that we
    //     can multitask.
    // The fact that this class is defined INSIDE the FractalPane class means that a) only FractalPane knows about this
    //     class, and b) It has access to the private variables and methods of FractalPane, as if they were its own.
    // The fact that it extends Thread means that the run() method can be operating at the same time as the other things
    //     in FractalPane (and the rest of the program) are happening. So we can do an extended set of calculations and
    //     updating the screen at the same time - this is what makes the screen seem to "live update" instead of just
    //     popping a finished fractal on the screen at the end of the scan. We can also interrupt the calculation mid-loop.
    //     Note: we never call "run()" directly - the thread is activated by calling the Thread method "start()" - which
    //           calls run(), itself.
    class CalculationThread extends Thread
    {
        private int x, y;
        private ComplexNumber c;
        private boolean needsReset;
        private boolean isScanning;

        public CalculationThread()
        {
            super();
            x = 0;
            y = 0;
            needsReset = false;
            isScanning = false;
        }

        /**
         * stops the current scan, if there is one active, and starts a new scan, from the top.
         */
        public void resetScan()
        {
            needsReset = true;
        }

        public boolean isScanning()
        {
            return isScanning;
        }

        /**
         * This is the method that will start running simultaneously with the main program when we say start().
         */
        public void run()
        {
            DoublePoint cp = new DoublePoint();
            DoublePoint sp = new DoublePoint();
            DoubleRectangle windowBounds = scaleManager.getWindowBounds();
            ColorConverter converter = new ColorConverter();
            FractalCalculator calculator = new FractalCalculator();
            Color pixelColor;
            int count;
            isScanning = false;
            needsReset = true;

            while (true)
            {
                if (needsReset)
                {
                    needsReset = false;

                    for (y = 0; y < windowBounds.getHeight(); y++)
                    {
                        isScanning = true;
                        for (x = 0; x < windowBounds.getWidth(); x++)
                        {
                            // find the complex number corresponding to this pixel.
                            sp.setXY(x, y);
                            scaleManager.mathPointForScreenPoint(cp, sp);
                            c = new ComplexNumber(cp);

                            count = calculator.getCountForComplexNumber(c); // bounce around until you go out of range...
                            pixelColor = converter.colorMap(count); // convert the number of bounces to a color...

                            // draw that color at this location.
                            synchronized (workspaceMutex) // wait until you can "lock" the workspace image - this prevents simultaneous use.
                            {
                                workspace.setRGB(x, y, pixelColor.getRGB()); // put the color in for this pixel location.
                            } // ok, I'm done with the workspace for the moment.

                            if (needsReset) // if this scan is canceled, leave this loop early.
                                break;
                        }

                        if (needsReset) // if this scan is canceled, leave this loop, too!
                            break;

                        // The next time it gets a chance, the main thread should update the screen. We only do this once
                        //   per line, because the program runs too slow if it is drawing after (almost) every pixel.
                        repaint();
                    }
                }
                if (isScanning) // if we got here, we're not scanning anymore. If we just finished scanning, update the indicator
                {
                    isScanning = false; //(This deactivates the little red dot in the corner)
                    repaint();
                }
                try
                {
                Thread.sleep(250); // chill out for 1/4 second. This means that we will only be considering whether
                                    // to recalculate the screen that often, if the scan is complete.
                } catch (InterruptedException iExp)
                {
                    iExp.printStackTrace();
                }
            }
        }
    }
}
