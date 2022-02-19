package DrawControls;
//#sijapp cond.if modules_GIFSMILES is "true"#
//import java.net.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;
import java.util.*;

import jimm.*;

/**
 * Class GifDecoder - Decodes a GIF file into one or more frames.
 * <br><pre>
 * Example:
 *    GifDecoder d = new GifDecoder();
 *    d.read("sample.gif");
 *    int n = d.getFrameCount();
 *    for (int i = 0; i < n; i++) {
 *       BufferedImage frame = d.getFrame(i);  // frame i
 *       int t = d.getDelay(i);  // display duration of frame in milliseconds
 *       // do something with frame
 *    }
 * </pre>
 * No copyright asserted on the source code of this class.  May be used for
 * any purpose, however, refer to the Unisys LZW patent for any additional
 * restrictions.  Please forward any corrections to kweiner@fmsware.com.
 *
 * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's ImageMagick.
 * @version 1.03 November 2003
 *
 * @ Updated for use with J2ME by Tom Thompson, KPI Consulting, Inc. 15-Aug-2005
 */

 class GifDecoder {
     
     /**
      * File read status: No errors.
      */
     public static final int STATUS_OK = 0;
     
     
     /**
      * File read status: Error decoding file (may be partially decoded)
      */
     public static final int STATUS_FORMAT_ERROR = 1;
     
     /**
      * File read status: Unable to open source.
      */
     public static final int STATUS_OPEN_ERROR = 2;
     
     protected DataInputStream in;
     protected int status;
     
     protected int width; // full image width
     protected int height; // full image height
     protected boolean gctFlag; // global color table used
     protected int gctSize; // size of global color table
     protected int loopCount = 1; // iterations; 0 = repeat forever
     
     protected int[] globalColorTable; // global color table
     
     protected int bgIndex; // background color index
     protected int bgColor; // background color
     protected int lastBgColor; // previous bg color
     protected int pixelAspect; // pixel aspect ratio
     
     protected boolean lctFlag; // local color table flag
     protected boolean interlace; // interlace flag
     protected int lctSize; // local color table size
     
     protected int ix, iy, iw, ih; // current image rectangle
     protected int lx, ly, lw, lh; // last image rect
     protected static Image currentFrame; // current frame
     protected static Image previousFrame; // previous frame
     
     protected byte[] block = new byte[256]; // current data block
     protected int blockSize = 0; // block size
     
     // last graphic control extension info
     protected int dispose = 0;
     // 0=no action; 1=leave in place; 2=restore to bg; 3=restore to prev
     protected int lastDispose = 0;
     protected boolean transparency = false; // use transparent color
     protected int delay = 0; // delay in milliseconds
     protected int transIndex; // transparent color index
     
     protected static final int MaxStackSize = 4096;
     // max decoder pixel stack size
     
     // LZW decoder working arrays
     protected short[] prefix;
     protected byte[] suffix;
     protected byte[] pixelStack;
     
     protected int [] dest;
     
     protected Vector frames; // For J2ME - frames read from current file
     protected int frameCount;
     
	public static class GifFrame {
        public GifFrame(Image image, int del) {
            this.image = image;
            this.delay = del;
        }
        public Image image;
        public int delay;
    }

	/**
	 * Gets display duration for specified frame.
	 *
	 * @param n int index of frame
	 * @return delay in milliseconds
	 */
	public int getDelay(int n) {
		if ((n >= 0) && (n < frameCount)) {
			return ((GifFrame) frames.elementAt(n)).delay;
		}
		return -1;
	}

	/**
	 * Gets the number of frames read from file.
	 * @return frame count
	 */
	public int getFrameCount() {
		return frameCount;
	}

	/**
	 * Gets the first (or only) image read.
	 *
	 * @return BufferedImage containing first frame, or null if none.
	 */
	public Image getImage() {
		return getFrame(0);
	}

	/**
	 * Gets the "Netscape" iteration count, if any.
	 * A count of 0 means repeat indefinitiely.
	 *
	 * @return iteration count if one was specified, else 1.
	 */
	public int getLoopCount() {
		return loopCount;
	}

    public GifFrame[] getFrames() {
        // from last to second(!) frame
        for (int i = frames.size() - 1; i > 0; i--) {
            final GifFrame frame = (GifFrame)frames.elementAt(i);
            if (frame.delay <= 10) {
                frames.removeElementAt(i);
                frameCount--;
            }
        }
        GifFrame[] res = new GifFrame[frames.size()];
        frames.copyInto(res);
        return res;
    }
	/**
	 * Creates new frame image from current data (and previous
	 * frames as specified by their disposition codes).
	 */
	protected Image setPixels(byte[] pixels, int[] activeColorTable) {

        final int bgThemeColor = Options.getInt(Options.OPTION_COLOR_BACK);
        // expose destination image's pixels as int array
        dest = new int[width * height];
        for (int i = 0; i < dest.length; i++) {
            dest[i] = bgThemeColor;
        }

        // Fill in starting image's contents based on last image's dispose code
        if (lastDispose > 0) {
            if (lastDispose == 3) {     // use image before last
                int n = frameCount - 2;
                if (n > 0) {
                    previousFrame = getFrame(n - 1);
                } else {
                    previousFrame = null;
                }
            }
            
            if (previousFrame != null) {
                try {
                    previousFrame.getRGB(dest, 0, width, 0, 0, width, height);
                } catch (Exception ex) {
                }
            }
            // fill last image rect area with background color
            if (lastDispose == 2) {
                int c = transparency ? bgThemeColor : lastBgColor;
                for (int i = 0; i < lh; i++) {  // Use previous image dimensions
                    int fromX = (ly + i) * width + lx;
                    int toX = fromX + lw;
                    for (int k = fromX; k < toX; k++) { // Copy background color
                        dest[k] = c;
                    }
                }
            }
        }
	
        // copy each source line to the appropriate place in the destination
		int pass = 1;
		int inc = 8;
		int iline = 0;
		for (int i = 0; i < ih; i++) {
			int line = i;
			if (interlace) {
				if (iline >= ih) {
					pass++;
					switch (pass) {
						case 2 :
							iline = 4;
							break;
						case 3 :
							iline = 2;
							inc = 4;
							break;
						case 4 :
							iline = 1;
							inc = 2;
					}
				}
				line = iline;
				iline += inc;
			}
			line += iy;
			if (line < height) {
				int k = line * width;
				int destStartX = k + ix; // start of line in dest
				int destEndX = destStartX + iw; // end of dest line
				if ((k + width) < destEndX) {
					destEndX = k + width; // past dest edge
				}
				int sx = i * iw; // start of line in source
				while (destStartX < destEndX) {
					// map color and insert in destination
					int index = ((int) pixels[sx++]) & 0xff;
                    if (!transparency || (index != transIndex)) {
						dest[destStartX] = activeColorTable[index];
					}
					destStartX++;
				} // end while
			} // end if
		} // end for
        return Image.createRGBImage(dest, width, height, true);
	}

	/**
	 * Gets the image contents of frame n.
	 *
	 * @return BufferedImage representation of frame, or null if n is invalid.
	 */
	public Image getFrame(int n) {
		if ((n >= 0) && (n < frameCount)) {
			return ((GifFrame) frames.elementAt(n)).image;
        }
		return null;
	}

	/**
	 * Gets image size.
	 *
	 * @return GIF image dimensions
	 */
//	public Dimension getFrameSize() {
//		return new Dimension(width, height);
//	}

	/**
	 * Reads GIF image from stream
	 *
	 * @param BufferedInputStream containing GIF file.
	 * @return read status code (0 = no errors)
	 */
    public int read(DataInputStream is) {
        init();
        if (is != null) {
            in = is;
            readHeader();
            if (!err()) {
                readContents();
                if (frameCount < 0) {
                    status = STATUS_FORMAT_ERROR;
                }
            }
        } else {
            status = STATUS_OPEN_ERROR;
        }
        try {
            is.close();
        } catch (IOException e) {
        }
        return status;
    }

	/**
	 * Reads GIF image from stream
	 *
	 * @param InputStream containing GIF file.
	 * @return read status code (0 = no errors)
	 */
    public int read(InputStream is) {
        init();
        if (is != null) {
            if (!(is instanceof DataInputStream)) {
                in = new DataInputStream(is);
            }
            readHeader();
            if (!err()) {
                readContents();
                if (frameCount < 0) {
                    status = STATUS_FORMAT_ERROR;
                }
            }
        } else {
            status = STATUS_OPEN_ERROR;
        }
        try {
            is.close();
        } catch (IOException e) {
        }
        return status;
    }

	/**
	 * Reads GIF file from specified file/URL source  
	 * (URL assumed if name contains ":/" or "file:")
	 *
	 * @param name String containing source
	 * @return read status code (0 = no errors)
	 */
	public int read(String name) {
		status = STATUS_OK;
		try {
			status = read(getClass().getResourceAsStream(name));
		} catch (Exception e) {
			status = STATUS_OPEN_ERROR;
		}

		return status;
	} // end read

	/**
	 * Decodes LZW image data into pixel array.
	 * Adapted from John Cristy's ImageMagick.
	 */
	protected byte[] decodeImageData() {
		int NullCode = -1;
		int npix = iw * ih;
		int in_code;
		int code;
		int data_size;

//		if ((pixels == null) || (pixels.length < npix)) {
		byte[] pixels = new byte[npix]; // allocate new pixel array
//		}
		if (prefix == null) prefix = new short[MaxStackSize];
		if (suffix == null) suffix = new byte[MaxStackSize];
		if (pixelStack == null) pixelStack = new byte[MaxStackSize + 1];

		//  Initialize GIF data stream decoder.

		data_size = read();
		int clear = 1 << data_size;
		int end_of_information = clear + 1;
		int available = clear + 2;
		int old_code = NullCode;
		int code_size = data_size + 1;
		int code_mask = (1 << code_size) - 1;
		for (code = 0; code < clear; code++) {
			prefix[code] = 0;
			suffix[code] = (byte) code;
		}

		//  Decode GIF pixel stream.
		int bits = 0;
		int datum = 0;
		int count = 0;
		int first = 0;
		int top = 0;
		int bi = 0;
		int pi = 0;

		for (int i = 0; i < npix;) {
			if (top == 0) {
				if (bits < code_size) {
					//  Load bytes until there are enough bits for a code.
					if (count == 0) {
						// Read a new data block.
						count = readBlock();
						if (count <= 0)
							break;
						bi = 0;
					}
					datum += (((int) block[bi]) & 0xff) << bits;
					bits += 8;
					bi++;
					count--;
					continue;
				}

				//  Get the next code.

				code = datum & code_mask;
				datum >>= code_size;
				bits -= code_size;

				//  Interpret the code

				if ((code > available) || (code == end_of_information))
					break;
				if (code == clear) {
					//  Reset decoder.
					code_size = data_size + 1;
					code_mask = (1 << code_size) - 1;
					available = clear + 2;
					old_code = NullCode;
					continue;
				}
				if (old_code == NullCode) {
					pixelStack[top++] = suffix[code];
					old_code = code;
					first = code;
					continue;
				}
				in_code = code;
				if (code == available) {
					pixelStack[top++] = (byte) first;
					code = old_code;
				}
				while (code > clear) {
					pixelStack[top++] = suffix[code];
					code = prefix[code];
				}
				first = ((int) suffix[code]) & 0xff;

				//  Add a new string to the string table,

				if (available >= MaxStackSize)
					break;
				pixelStack[top++] = (byte) first;
				prefix[available] = (short) old_code;
				suffix[available] = (byte) first;
				available++;
				if (((available & code_mask) == 0)
					&& (available < MaxStackSize)) {
					code_size++;
					code_mask += available;
				}
				old_code = in_code;
			}

			//  Pop a pixel off the pixel stack.

			top--;
			pixels[pi++] = pixelStack[top];
			i++;
		}

		for (int i = pi; i < npix; i++) {
			pixels[i] = (byte)transIndex; // clear missing pixels
		}
        
        return pixels;
	}

	/**
	 * Returns true if an error was encountered during reading/decoding
	 */
	protected boolean err() {
		return status != STATUS_OK;
	}

	/**
	 * Initializes or re-initializes reader
	 */
	protected void init() {
		status = STATUS_OK;
		frameCount = 0;
		frames = new Vector();
		globalColorTable = null;
	}

	/**
	 * Reads a single byte from the input stream.
	 */
	protected int read() {
		int curByte = 0;
		try {
			curByte = in.read();
		} catch (IOException e) {
			status = STATUS_FORMAT_ERROR;
		}
		return curByte;
	}

	/**
	 * Reads next variable length block from input.
	 *
	 * @return number of bytes stored in "buffer"
	 */
	protected int readBlock() {
		blockSize = read();
		int n = 0;
		if (blockSize > 0) {
			try {
				int count = 0;
				while (n < blockSize) {
					count = in.read(block, n, blockSize - n);
					if (count == -1) 
						break;
					n += count;
				}
			} catch (IOException e) {
			}

			if (n < blockSize) {
				status = STATUS_FORMAT_ERROR;
			}
		}
		return n;
	}

	/**
	 * Reads color table as 256 RGB integer values
	 *
	 * @param ncolors int number of colors to read
	 * @return int array containing 256 colors (packed ARGB with full alpha)
	 */
	protected int[] readColorTable(int ncolors) {
 		int nbytes = 3 * ncolors;
		int[] tab = null;
		byte[] c = new byte[nbytes];
		int n = 0;
        // Read color table with this loop. J2ME's DataInputStream read() method
        // throws an exception if you try to read more than 255 bytes
        try {
            for (n = 0; n < nbytes; ++n) {
                c[n] = in.readByte();
            }
        } catch (IOException oe) {
        }
		if (n < nbytes) {
			status = STATUS_FORMAT_ERROR;
		} else {
			tab = new int[256]; // max size to avoid bounds checks
			int i = 0;
			int j = 0;
			while (i < ncolors) {
				int r = ((int) c[j++]) & 0xff;
				int g = ((int) c[j++]) & 0xff;
				int b = ((int) c[j++]) & 0xff;
				tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
			}
		}
		return tab;
	}

	/**
	 * Main file parser.  Reads GIF content blocks.
	 */
	protected void readContents() {
		// read GIF file content blocks
		boolean done = false;
		while (!(done || err())) {
			int code = read();
			switch (code) {

				case 0x2C : // image separator
					readImage();
					break;

				case 0x21 : // extension
					code = read();
					switch (code) {
						case 0xf9 : // graphics control extension
							readGraphicControlExt();
							break;

						case 0xff : // application extension
							readBlock();
							String app = "";
							for (int i = 0; i < 11; i++) {
								app += (char) block[i];
							}
							if (app.equals("NETSCAPE2.0")) {
								readNetscapeExt();
							}
							else {
								skip(); // don't care
                            }
							break;

						default : // uninteresting extension
							skip();
					}
					break;

				case 0x3b : // terminator
					done = true;
					break;

				case 0x00 : // bad byte, but keep going and see what happens
					break;

				default :
					status = STATUS_FORMAT_ERROR;
			}
		}
	}

	/**
	 * Reads Graphics Control Extension values
	 */
	protected void readGraphicControlExt() {
		read(); // block size
		int packed = read(); // packed fields
		dispose = (packed & 0x1c) >> 2; // disposal method
		if (dispose == 0) {
			dispose = 1; // elect to keep old image if discretionary
		}
		transparency = (packed & 1) != 0;
		delay = readShort() * 10; // delay in milliseconds
		transIndex = read(); // transparent color index
		read(); // block terminator
	}

	/**
	 * Reads GIF file header information.
	 */
	protected void readHeader() {
		String id = "";
		for (int i = 0; i < 6; i++) {
			id += (char) read();
		}
		if (!id.startsWith("GIF")) {
			status = STATUS_FORMAT_ERROR;
			return;
		}

		readLSD();
		if (gctFlag && !err()) {
			globalColorTable = readColorTable(gctSize);
			bgColor = globalColorTable[bgIndex];
		}
	}

	/**
	 * Reads next frame image
	 */
	protected void readImage() {
		ix = readShort(); // (sub)image position & size
		iy = readShort();
		iw = readShort();
		ih = readShort();

		int packed = read();
		lctFlag = (packed & 0x80) != 0; // 1 - local color table flag
		interlace = (packed & 0x40) != 0; // 2 - interlace flag
		// 3 - sort flag
		// 4-5 - reserved
		lctSize = 2 << (packed & 7); // 6-8 - local color table size

        int[] activeColorTable; // active color table

		if (lctFlag) {
			int[] localColorTable = readColorTable(lctSize); // read table
			activeColorTable = localColorTable; // make local table active
		} else {
			activeColorTable = globalColorTable; // make global table active
		}
        if (bgIndex == transIndex) {
            bgColor = Options.getInt(Options.OPTION_COLOR_BACK);
        } else {
            bgColor = activeColorTable[bgIndex];
        }

		if (activeColorTable == null) {
			status = STATUS_FORMAT_ERROR; // no color table defined
		}

		if (err()) return;

		byte[] pixels = decodeImageData(); // decode pixel data
		skip();

		if (err()) return;

		frameCount++;

		currentFrame = setPixels(pixels, activeColorTable); // transfer pixel data to image

		frames.addElement(new GifFrame(currentFrame, delay)); // add image to frame list

		resetFrame();

	}

	/**
	 * Reads Logical Screen Descriptor
	 */
	protected void readLSD() {

		// logical screen size
		width = readShort();
		height = readShort();

		// packed fields
		int packed = read();
		gctFlag = (packed & 0x80) != 0; // 1   : global color table flag
		// 2-4 : color resolution
		// 5   : gct sort flag
		gctSize = 2 << (packed & 0x07); // 6-8 : gct size

		bgIndex = read(); // background color index
		pixelAspect = read(); // pixel aspect ratio
	}

	/**
	 * Reads Netscape extenstion to obtain iteration count
	 */
	protected void readNetscapeExt() {
		do {
			readBlock();
			if (block[0] == 1) {
				// loop count sub-block
				int b1 = ((int) block[1]) & 0xff;
				int b2 = ((int) block[2]) & 0xff;
				loopCount = (b2 << 8) | b1;
			}
		} while ((blockSize > 0) && !err());
	}

	/**
	 * Reads next 16-bit value, LSB first
	 */
	protected int readShort() {
		// read 16-bit value, LSB first
		return read() | (read() << 8);
	}

	/**
	 * Resets frame state for reading next image.
	 */
	protected void resetFrame() {
		lastDispose = dispose;
		lx = ix;  ly = iy;  lw = iw;  lh = ih;
		previousFrame = currentFrame;
		lastBgColor = bgColor;
		int dispose = 0;
		boolean transparency = false;
		int delay = 0;
	}

	/**
	 * Skips variable length blocks up to and including
	 * next zero length block.
	 */
	protected void skip() {
		do {
			readBlock();
		} while ((blockSize > 0) && !err());
	}
}
//#sijapp cond.end#