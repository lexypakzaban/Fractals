import java.awt.*;

public class ColorConverter
{

    /**
     * given an integer, generates a Color object. The rules are that if you repeat this method:
     * a) The same number should always return the same color.
     * b) Most of the time, nearby numbers should give nearby colors. (Occasional jumps are ok.) For example calling this
     * with 168 and 170 might give colors that are almost the same. But there might be a "jump" between 255 and 256.
     * c) if the number is negative, return black.
     * @param n an integer, of any size.
     * @return a color corresponding to n, and that color should be black if n<0.
     */
    public Color colorMap(int n)
    {
        //TODO - you need to write this.

        if (n > 0){
            Color col = new Color(((n*255)/1000)/2,(n*255)/1000,(n*255)/1000);
            return col;
        }

        else {
            return Color.BLACK;
        }

    }
}
