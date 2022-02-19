//#sijapp cond.if target is "MOTOROLA"#
package DrawControls;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import jimm.Options;

public final class TPropFont {

  public static TPropFont font = null;

    public TPropFont(String s) {
        try {
            InputStream inputstream;
            (inputstream = getClass().getResourceAsStream(s)).read();
            e = inputstream.read();
            f = inputstream.read();
            a = new int[e];
            b = new int[e];
            c = new int[e];
            for(int i = 0; i < e; i++) {
                a[i] = inputstream.read();
                b[i] = inputstream.read();
                c[i] = inputstream.read() + 1;
            }
            return;
        }
        catch(IOException ex) {}
    }

    public final void setImage(String s) {
        try{
          d[3] = Image.createImage(s);
        }catch(IOException ex) {}
        createColor();
    }

    public final void setImage(Image image) {
        d[3] = image;
        createColor();
    }

    public final int getWidth(char c1) {
        int i;
        if((i = c1) > '\037' && i < 127)
            i -= 32;
        if(i > 1039 && i < 1104)
            i -= 937;
        if(i == 1025)
            i -= 929;
        if(i < 0 || i >= e || i == 13 || i == 10)
            return 0;
        else
            return c[i];
    }

    public final int getStringWidth(String s) {
        int i = 0;
        for(int j = 0; j < s.length(); j++)
            i += getWidth(s.charAt(j));
        return i;
    }

    public final int getHeight() {
        return f;
    }

    public final void drawString(Graphics g, int i, int j, String s, int color) {
        int k = g.getClipX();
        int l = g.getClipY();
        int i1 = g.getClipWidth();
        int j1 = g.getClipHeight();
        int k1 = i;
        int index = 0;
        if(color == 0xFF0000){
            index = 1;
        }else if(color == blue){
          index = 2;
        }
        for(int l1 = 0; l1 < s.length(); l1++) {
            int i2;
            if((i2 = s.charAt(l1) - 1) + 1 == 13 || i2 + 1 == 10)
                continue;
            if((i2 = (i2 = i2 != 1024 ? i2 != 1104 ? i2 : 183 : 167) <= 1024 ? i2 : i2 - 848) > 30 && i2 < 127)
                i2 -= 31;
            if(i2 > 166 && i2 < 172)
                i2 -= 70;
            if(i2 == 183)
                i2 -= 82;
            if(i2 > 190)
                i2 -= 88;
            if(i2 < 0 || i2 >= e)
                continue;
            int j2 = c[i2];
            if(i2 > 0) {
                int k2 = a[i2];
                int l2 = b[i2];
                int i3 = k1 - k2;
                int j3 = j - l2;
                if(k1 < k)
                    if(k1 + j2 > k) {
                        j2 -= k - k1;
                        k1 = k;
                    } else {
                        k1 += j2;
                        continue;
                    }
                if(k1 + j2 > k + i1) {
                    if(k1 >= k + i1)
                        break;
                    j2 = (k + i1) - k1;
                }
                g.setClip(k1, j, j2, f);
                g.drawImage(d[index], i3, j3, Graphics.TOP | Graphics.LEFT);
            }
            k1 += j2;
        }
        g.setClip(k, l, i1, j1);
    }

    private void createColor() {
      blue = Options.getInt(Options.OPTION_COLOR_MY_NICK);
      text = Options.getInt(Options.OPTION_COLOR_TEXT);
      int h = d[3].getHeight();
      int w = d[3].getWidth();
      int[] buf = new int[h * w];
      d[3].getRGB(buf, 0, w, 0, 0, w, h);
      for(int x = 0;x < w * h; x++) {
        if(buf[x] == 0xff000000) {
          buf[x] = 0xff000000 | text;
        }
      }
      d[0] = Image.createRGBImage(buf, w, h, true);
      d[3].getRGB(buf, 0, w, 0, 0, w, h);
      for(int x = 0;x < w * h; x++) {
        if(buf[x] == 0xff000000) {
          buf[x] = 0xffff0000;
        }
      }
      d[1] = Image.createRGBImage(buf, w, h, true);
      d[3].getRGB(buf, 0, w, 0, 0, w, h);
      for(int x = 0;x < w * h; x++) {
        if(buf[x] == 0xff000000) {
          buf[x] = 0xff000000 | blue;
        }
      }
      d[2] = Image.createRGBImage(buf, w, h, true);
      buf = null;
      d[3] = null;
     System.gc();
    }

    private int blue = 0xff0000;
    private int text = 0x000000;
    private int a[];
    private int b[];
    private int c[];
    private Image[] d = new Image[4];
    private int e;
    private int f;
}
//#sijapp cond.end#