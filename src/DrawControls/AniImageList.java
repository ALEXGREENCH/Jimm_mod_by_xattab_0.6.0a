/*
 * GifImageList.java
 *
 * Created on 4 Апрель 2008 г., 18:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

//#sijapp cond.if modules_ANISMILES is "true"#
package DrawControls;

import java.io.InputStream;
import java.util.Vector;

import jimm.*;

//import jimm.chat.ChatTextList;
//import jimm.ui.CanvasEx;
//import jimm.ui.NativeCanvas;
//import jimm.ui.Selector;

/**
 *
 * @author vladimir
 */
public class AniImageList extends ImageList implements Runnable {
    
    private AniIcon[] icons;
    private Thread thread;

    //! Return image by index
    public Icon elementAt(int index) { //!< Index of requested image in the list
        if (index < size() && index >= 0) {
            return icons[index];
        }
        return null;
    }
    public int size() {
        return icons != null ? icons.length : 0;
    }

    /** Creates a new instance of GifImageList */
    public AniImageList() {
    }
    private String getSmileFile(String resName, int i) {
        return resName + "/" + (i + 1) + ".png";
    }

    public void load(String resName, int w, int h) {
        Vector tmpIcons = new Vector();
        try {
            InputStream is = getClass().getResourceAsStream(resName + "/animate.bin");
            int smileCount = is.read();

            icons = new AniIcon[smileCount];
            ImageList imgs = new ImageList();
            for (int smileNum = 0; smileNum < smileCount; smileNum++) {
                int imageCount = is.read();
                int frameCount = is.read();
                imgs.load(getSmileFile(resName, smileNum), imageCount);
                boolean loaded = (0 < imgs.size());
                AniIcon icon = loaded ? new AniIcon(imgs.elementAt(0), frameCount) : null;
                for (int frameNum = 0; frameNum < frameCount; frameNum++) {
                    int iconIndex = is.read();
                    int delay = is.read() * TIME;
                    if (loaded) {
                        icon.addFrame(frameNum, imgs.elementAt(iconIndex), delay);
                    }
                }
                icons[smileNum] = icon;
                if (loaded) {
                    width = Math.max(width, icon.getWidth());
                    height = Math.max(height, icon.getHeight());
                }
            }
        } catch (Exception e) {
        }
        if (size() > 0) {
            thread = new Thread(this);
            thread.start();
        }
    }

    private long time;
    private static final int TIME = 100;
    public void run() {
        time = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(TIME);
            } catch (Exception e) {
            }
            long newTime = System.currentTimeMillis();
            Object screen = JimmUI.getCurrentScreen();
            boolean animationWorked = (screen instanceof VirtualList);
            if (animationWorked) {
                boolean update = false;
                for (int i = 0; i < size(); i++) {
                    if (icons[i] != null) {
                        update |= icons[i].nextFrame(newTime - time);
                    }
                }
                if (update) {
                    ((VirtualList)screen).invalidate();
                }
            }
            time = newTime;
        }
    }
}
//#sijapp cond.end#
