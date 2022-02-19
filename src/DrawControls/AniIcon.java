/*
 * GifIcon.java
 *
 * Created on 4 Апрель 2008 г., 19:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

//#sijapp cond.if modules_ANISMILES is "true"#
package DrawControls;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 *
 * @author vladimir
 */
public class AniIcon extends Icon {
    private Icon[] frames;
    private int[] delays;
    private int currentFrame = 0;

    /** Creates a new instance of AniIcon */
    public AniIcon(Icon icon, int frameCount) {
        super(icon.getImage(), 0, 0, icon.getWidth(), icon.getHeight());
        frames = new Icon[frameCount];
        delays = new int[frameCount];
    }
    protected Image getImage() {
        return frames[currentFrame].getImage();
    }
    void addFrame(int num, Icon icon, int dalay) {
        frames[num] = icon;
        delays[num] = dalay;
    }
    public void drawImage(Graphics g, int x, int y) {
        frames[currentFrame].drawImage(g, x, y);
        painted = true;
    }
    private boolean painted = false;
    private long sleepTime = 0;
    boolean nextFrame(long deltaTime) {
        sleepTime -= deltaTime;
        if (sleepTime <= 0) {
            currentFrame = (currentFrame + 1) % frames.length;
            sleepTime = delays[currentFrame];
            boolean needReepaint = painted;
            painted = false;
            return needReepaint;
        }
        return false;
    }
}
//#sijapp cond.end#
