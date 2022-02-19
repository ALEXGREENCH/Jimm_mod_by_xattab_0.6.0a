/*
 * GifImageList.java
 *
 * Created on 4 Апрель 2008 г., 18:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls;

import java.io.InputStream;
import java.util.Vector;

import jimm.*;

//import jimm.chat.ChatTextList;
//import jimm.ui.CanvasEx;
//import jimm.ui.NativeCanvas;
//import jimm.ui.Selector;
// #sijapp cond.if modules_GIFSMILES is "true" #

/**
 *
 * @author vladimir
 */
public class GifImageList extends ImageList implements Runnable {
    
    private GifIcon[] icons;
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
    public GifImageList() {
    }
    private String getSmileFile(String resName, int i) {
        return resName + "/" + (i + 1) + ".gif";
    }

    private static final int MAX_ICONS = 100;
    public void load(String resName, int w, int h) {
        Vector tmpIcons = new Vector();
        try {
            for (int i = 0; i < MAX_ICONS; i++) {
                GifDecoder gd = new GifDecoder();
                if (GifDecoder.STATUS_OK != gd.read(getSmileFile(resName, i))) {
                    break;
                }
                tmpIcons.addElement(new GifIcon(gd));
                width = Math.max(width, gd.getImage().getWidth());
                height = Math.max(height, gd.getImage().getHeight());
            }
        } catch (Exception e) {
        }
        icons = new GifIcon[tmpIcons.size()];
        tmpIcons.copyInto(icons);
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
            boolean animationWorked = false;
            animationWorked |= (screen instanceof VirtualList);
            //animationWorked |= (screen instanceof ChatTextList);
            //animationWorked |= (screen instanceof Selector);
            if (animationWorked) {
                boolean update = false;
                for (int i = 0; i < size(); i++) {
                    update |= icons[i].nextFrame(newTime - time);
                }
                if (update) {
                    ((VirtualList)screen).invalidate();
                }
            }
            time = newTime;
        }
    }
}
// #sijapp cond.end #
