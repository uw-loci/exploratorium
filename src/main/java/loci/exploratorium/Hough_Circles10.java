package loci.exploratorium;

/** Hough_Circles10.java:
22.2.2007
 by Norbert Vischer,
based on  work by:

Hemerson Pistori (pistori@ec.ucdb.br) and Eduardo Rocha Costa
and
 Mark A. Schulze applet (http://www.markschulze.net/)
 */

//package sigus.templateMatching;
//import sigus.*;

import ij.*;
import ij.plugin.*;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.*;
import ij.process.*;
import inra.ijpb.morphology.Strel;

import java.awt.*;
import ij.gui.*;
import ij.measure.*;

/**
 *   This ImageJ plugin shows the Hough Transform Space and search for
 *   circles in a binary image. The image must have been passed through
 *   an edge detection module and have edges marked in white (background
 *   must be in black).
 */
public class Hough_Circles10 implements PlugInFilter {

	public int radiusMin;  // Find circles with radius grater or equal radiusMin
	public int radiusMax;  // Find circles with radius less or equal radiusMax
	public int radiusInc;  // Increment used to go from radiusMin to radiusMax
	public int maxCircles; // Numbers of circles to be found
	public int threshold = -1; // An alternative to maxCircles. All circles with
	// a value in the hough space greater then threshold are marked. Higher thresholds
	// results in fewer circles. But it doesn't work as expected as too many circles
	// are found. Therefore maxCircles is the preferred method for now. 09/28/2016 BD
	public int showOutputImage = 0; // show houghspace window and center of circles
	public boolean keepPreviousResults = false;
	byte imageValues[]; // Raw image (returned by ip.getPixels())
	short houghValues[][][]; // Hough Space Values 12.9.2006
	short miniMap[][][]; // Hough Space Values 12.9.2006
	public int width; // Hough Space width (depends on image width)
	public int height;  // Hough Space height (depends on image height)
	public int miniWidth;  // MiniMap(depends on image height)
	public int miniHeight;  // MiniMap(depends on image height)
	public final int TEN = 10;

	public int depth;  // Hough Space depth (depends on radius interval)
	public int offset; // Image Width
	public int offx;   // ROI x offset
	public int offy;   // ROI y offset
	public Point centerPoint[]; // Center Points of the Circles Found.
	public double radiusList[]; // radius of the circles found.
	public int peakList[];      // maximum hough value of a circle found
	private int vectorMaxSize = 500;
	boolean useThreshold = false;
	int lut[][][]; // LookUp Table for rsin e rcos values
	public int bubbleThreshold = 80; // threshold to eliminate false positives (air bubbles, debris, etc.) from results


	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}
		return DOES_8G+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {

		imageValues = (byte[])ip.getPixels();
		Rectangle r = ip.getRoi();


		offx = r.x;
		offy = r.y;
		width = r.width;
		height = r.height;
		offset = ip.getWidth();

		miniWidth = (int) Math.ceil((double) width/TEN);
		miniHeight =(int) Math.ceil((double) height/TEN);


		if( readParameters() ) { // Show a Dialog Window for user input of
			// radius and maxCircles.


			houghTransform();
			createMiniMap();

			// Create image View for Hough Transform.

			if (showOutputImage == 2) {//n_
				ImageProcessor newip = new ByteProcessor(width, height);
				byte[] newpixels = (byte[])newip.getPixels();
				for (int kk=0; kk<newpixels.length; kk++)
					newpixels[kk] = 0;
				createHoughPixels(newpixels, 200);
				ImageProcessor circlesip = new ByteProcessor(width, height);
				byte[] circlespixels = (byte[])circlesip.getPixels();
				new ImagePlus("Hough Space [r="+radiusMin+"]", newip).show(); // Shows only the hough space for the minimun radius
			}

			// Mark the center of the found circles in a new image
			if(useThreshold)
				getCenterPointsByThreshold(threshold);
			else
				getCenterPoints(maxCircles);
			if (showOutputImage == 1) {//n_
				ImageProcessor newip = new ByteProcessor(width, height);
				byte[] newpixels = (byte[])newip.getPixels();
				createHoughPixels(newpixels, 0);
				ImageProcessor circlesip = new ByteProcessor(width, height);
				byte[] circlespixels = (byte[])circlesip.getPixels();
				drawCircles(circlespixels);
				new ImagePlus("Hough Space [r="+radiusMin+"]", newip).show(); // Shows only the hough space for the minimun radius
				new ImagePlus(maxCircles+" Circles Found", circlesip).show();
			}
		}
	}

	void showAbout() {
		IJ.showMessage("About Circles_...",
				"This plugin finds n circles\n" +
						"using a basic HoughTransform operator\n." +
						"For better results apply an Edge Detector\n" +
						"filter and a binarizer before using this plugin\n"+
						"\nAuthor: Hemerson Pistori (pistori@ec.ucdb.br)"
				);
	}

	boolean readParameters() {

		GenericDialog gd = new GenericDialog("Hough Parameters", IJ.getInstance());
		gd.addNumericField("Min_Radius (in pixels):", 35, 0);
		gd.addNumericField("Max_Radius (in pixels):", 45, 0);
		gd.addNumericField("Step_Radius (in pixels):", 2, 0);
		gd.addNumericField("Number_of_Circles (NC): (enter 0 if using threshold)", 3, 0);
		gd.addNumericField("Threshold: (not used if NC > 0)", 60, 0);
		gd.addNumericField("Bubble_Threshold: (1-99)", 80, 0);
		gd.addNumericField("With_Output: (0, 1 or 2)", 1, 0);
		gd.addNumericField("Keep_previous_results: (0 or 1)", 0, 0);

//		gd.addCheckbox("Show output images", true); //n_ 

		gd.showDialog();

		if (gd.wasCanceled()) {
			return(false);
		}

		radiusMin = (int) gd.getNextNumber();
		radiusMax = (int) gd.getNextNumber();
		radiusInc = (int) gd.getNextNumber();
		depth = ((radiusMax-radiusMin)/radiusInc)+1;
		maxCircles = (int) gd.getNextNumber();
		threshold = (int) gd.getNextNumber();
		bubbleThreshold = (int) gd.getNextNumber(); 
		showOutputImage = (int) gd.getNextNumber();
		keepPreviousResults = ( gd.getNextNumber() != 0);
		if (maxCircles > 0) {
			useThreshold = false;
			threshold = -1;
		} else {
			useThreshold = true;
			if(threshold < 0) {
				IJ.showMessage("Threshold must be greater than 0");
				return(false);
			}
		}
		return(true);

	}

	/** The parametric equation for a circle centered at (a,b) with
        radius r is:

    a = x - r*cos(theta)
    b = y - r*sin(theta)

    In order to speed calculations, we first construct a lookup
    table (lut) containing the rcos(theta) and rsin(theta) values, for
    theta varying from 0 to 2*PI with increments equal to
    1/(8*r). As of now, a fixed increment is being used for all
    different radius (1/(8*radiusMin)). This should be corrected in //n_
    the future.

    Return value = Number of angles for each radius

	 */
	private int buildLookUpTable() {

		int i = 0;
		int incDen = Math.round (8F * radiusMin);  // increment denominator

		lut = new int[2][incDen][depth];
		IJ.showStatus("Building sine-cosine LUT");
		for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
			i = 0;
			for(int incNun = 0; incNun < incDen; incNun++) {
				double angle = (2*Math.PI * (double)incNun) / (double)incDen;
				int indexR = (radius-radiusMin)/radiusInc;
				int rcos = (int)Math.round ((double)radius * Math.cos (angle));
				int rsin = (int)Math.round ((double)radius * Math.sin (angle));
				if((i == 0) | (rcos != lut[0][i][indexR]) & (rsin != lut[1][i][indexR])) {
					lut[0][i][indexR] = rcos;
					lut[1][i][indexR] = rsin;
					i++;
				}
			}
		}

		return i;
	}

	private void houghTransform () {

		int lutSize = buildLookUpTable();

		houghValues = new short[width][height][depth];//12.9.2006
		miniMap  = new short[miniWidth][miniHeight][depth];//12.9.2006

		int k = width - 1;
		int l = height - 1;
		int percentDone = 0;
		for(int y = 1; y < l; y++) {
			int nowDone = y*100/l +1;
			if (nowDone > percentDone){
				percentDone = nowDone;
				IJ.showStatus("Create Hough Space  "+percentDone+"%");
			}
			for(int x = 1; x < k; x++) {
				for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
					if( imageValues[(x+offx)+(y+offy)*offset] != 0 )  {// Edge pixel found
						int indexR=(radius-radiusMin)/radiusInc;
						for(int i = 0; i < lutSize; i++) {

							int a = x + lut[1][i][indexR]; 
							int b = y + lut[0][i][indexR]; 
							if((b >= 0) & (b < height) & (a >= 0) & (a < width)) {
								houghValues[a][b][indexR] += 1;
							}
						}

					}
				}
			}

		}

	}


	// Convert Values in Hough Space to an 8-Bit Image Space.
	private void createHoughPixels (byte houghPixels[], double factor) {

		double d = -1D;
		if (factor != 0)
			d = factor;

		else for(int j = 0; j < height; j++) {
			for(int k = 0; k < width; k++)
				if(houghValues[k][j][0] > d) {
					d = houghValues[k][j][0];
				}

		}

		for(int l = 0; l < height; l++) {
			for(int i = 0; i < width; i++) {
				houghPixels[i + l * width] = (byte) Math.round ((houghValues[i][l][0] * 255D) / d);
			}

		}

	}

	// Draw the circles found in the original image.
	public void drawCircles(byte[] circlespixels) {


		// Copy original image to the circlespixels image.
		// Changing pixels values to 100, so that the marked
		// circles appears more clear. Must be improved in
		// the future to show the resuls in a colored image.


		for(int i = 0; i < width*height ;++i ) {
			if(imageValues[i] != 0 )
				circlespixels[i] = 100;
			else
				circlespixels[i] = 0;
		}

		if(centerPoint == null) {
			if(useThreshold)
				getCenterPointsByThreshold(threshold);
			else
				getCenterPoints(maxCircles);
		}

		byte cor = -1;//=255 = white n_

		for(int l = 0; l < maxCircles; l++) {

			int i = centerPoint[l].x;
			int j = centerPoint[l].y;



			// Draw a gray cross marking the center of each circle.
			for( int k = -10 ; k <= 10 ; ++k ) {
				if(!outOfBounds(j+k+offy,i+offx))
					circlespixels[(j+k+offy)*offset + (i+offx)] = cor;
				if(!outOfBounds(j+offy,i+k+offx))
					circlespixels[(j+offy)*offset   + (i+k+offx)] = cor;
			}

			for( int k = -2 ; k <= 2 ; ++k ) {
				if(!outOfBounds(j-2+offy,i+k+offx))
					circlespixels[(j-2+offy)*offset + (i+k+offx)] = cor;
				if(!outOfBounds(j+2+offy,i+k+offx))
					circlespixels[(j+2+offy)*offset + (i+k+offx)] = cor;
				if(!outOfBounds(j+k+offy,i-2+offx))
					circlespixels[(j+k+offy)*offset + (i-2+offx)] = cor;
				if(!outOfBounds(j+k+offy,i+2+offx))
					circlespixels[(j+k+offy)*offset + (i+2+offx)] = cor;
			}
		}
	}


	private boolean outOfBounds(int y,int x) {
		if(x >= width)
			return(true);
		if(x <= 0)
			return(true);
		if(y >= height)
			return(true);
		if(y <= 0)
			return(true);
		return(false);
	}

	public Point nthMaxCenter (int i) {
		return centerPoint[i];
	}



	private void createMiniMap() {

		for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {

			int indexR = (radius-radiusMin)/radiusInc;
			for(int y = 0; y < height; y++) {
				int miniY = y/TEN;
				for(int x = 0; x < width; x++) {
					int miniX = x/TEN;
					if(houghValues[x][y][indexR] > miniMap[miniX][miniY][indexR])
						miniMap[miniX][miniY][indexR] = houghValues[x][y][indexR];
				}
			}
		}
	}









	/** Search for a fixed number of circles.

    @param maxCircles The number of circles that should be found.  
	 */
	private void getCenterPoints (int maxCircles) {


		centerPoint = new Point[maxCircles];
		radiusList = new double[maxCircles];
		peakList = new int[maxCircles];
		int xMiniMax = 0;
		int yMiniMax = 0;
		int xMax = 0;
		int yMax = 0;
		int rMax = 0;
		int rMaxIndex = 0;




		for(int c = 0; c < maxCircles; c++) {
			IJ.showStatus("Circle # (of " + maxCircles + "): " + c);
			int miniMapMax = 0;
			for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
				int indexR = (radius-radiusMin)/radiusInc;
				for(int y = 0; y < miniHeight; y++) {
					for(int x = 0; x < miniWidth; x++) {
						if(miniMap[x][y][indexR] > miniMapMax) {
							miniMapMax = miniMap[x][y][indexR];
							xMiniMax = x;
							yMiniMax = y;
							rMax = radius;
							rMaxIndex = indexR;
						}
					}
				}
			}

			//Now finetune...
			int counterMax = 0;
			if (miniMapMax > 0){
				for(int y = yMiniMax*TEN; y< yMiniMax*TEN+TEN; y++)
					for (int x = xMiniMax*TEN; x< xMiniMax*TEN+TEN; x++) try{
						if(houghValues[x][y][rMaxIndex] > counterMax) {//catch
							counterMax = houghValues[x][y][rMaxIndex];
							xMax = x;
							yMax = y;
						}
					} catch(Exception e) {/*IJ.showMessage("x="+x+" y="+y+ " r="+rMaxIndex +" width="+width+" height="+height+"  depth="+depth);*/}
				centerPoint[c] = new Point (xMax, yMax);
				radiusList[c]= rMax;//n__
				peakList[c]= counterMax;//n__

				clearNeighbours(xMax,yMax,rMax);
			}
		}
		centersToResultsTable(centerPoint, radiusList, peakList);//n_
	}


	/** Search circles having values in the hough space higher than a threshold
    @param threshold The threshold used to select the higher point of Hough Space
	 */


	private void getCenterPointsByThreshold (int threshold) {

		centerPoint = new Point[vectorMaxSize];
		centerPoint = new Point[vectorMaxSize];
		radiusList = new double[vectorMaxSize];
		peakList = new int[vectorMaxSize];

		int xMax = 0;
		int yMax = 0;
		int countCircles = 0;

		for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
			int indexR = (radius-radiusMin)/radiusInc;
			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {


					int thisVal = houghValues[x][y][indexR];
					if(thisVal > threshold) {


						if(countCircles < vectorMaxSize) {


							centerPoint[countCircles] = new Point (x, y);
							radiusList[countCircles]= radius;//n__
							peakList[countCircles]= thisVal;//n__

							clearNeighbours(xMax,yMax,radius);

							++countCircles;
						} else
							break;
					}
				}
			}
		}
		centersToResultsTable(centerPoint, radiusList, peakList);//n_
		maxCircles = countCircles;
	}

	/** Clear, from the Hough Space, all the counter that are near (radius/2) a previously found circle C.

    @param x The x coordinate of the circle C found.
    @param x The y coordinate of the circle C found.
    @param x The radius of the circle C found.
	 */
	private void clearNeighbours(int x,int y, int radius) {


		// The following code just clean the points around the center of the circle found.


		double halfRadius = radius / 2.0F;
		double halfSquared = halfRadius*halfRadius;


		int y1 = (int)Math.floor ((double)y - halfRadius);
		int y2 = (int)Math.ceil ((double)y + halfRadius) + 1;
		int x1 = (int)Math.floor ((double)x - halfRadius);
		int x2 = (int)Math.ceil ((double)x + halfRadius) + 1;



		if(y1 < 0)
			y1 = 0;
		if(y2 > height)
			y2 = height;
		if(x1 < 0)
			x1 = 0;
		if(x2 > width)
			x2 = width;



		for(int r = radiusMin;r <= radiusMax;r = r+radiusInc) {
			int indexR = (r-radiusMin)/radiusInc;
			for(int i = y1; i < y2; i++) {
				for(int j = x1; j < x2; j++) {	      	     
					if(Math.pow (j - x, 2D) + Math.pow (i - y, 2D) < halfSquared) {
						houghValues[j][i][indexR] = 0;//     12.9.2006
						miniMap[j/TEN][i/TEN][indexR] = 0;//     12.9.2006
					}
				}
			}
		}

	}

	private void centersToResultsTable(Point[] pointsUsed, double[] myRadiusList, int[]myPeakList){
		ResultsTable rt = ResultsTable.getResultsTable();
		if (!keepPreviousResults)
			rt.reset();


		int count = pointsUsed.length;
		for (int n=0; n< count; n ++) {
			rt.incrementCounter();
			rt.addValue("n", n);
			rt.addValue("xx", pointsUsed[n].x + offx);
			rt.addValue("yy", pointsUsed[n].y + offy);
			rt.addValue("rr", myRadiusList[n]);
			rt.addValue("peak", myPeakList[n]);
		}
		rt.show("results"); // not working if using "Results"
	}
	
	/**
	 * Eliminate false positives (air bubbles, debris, etc) from results.
	 * Basic idea is to get a binary mask around edge of circles and check
	 * if the inner boundary is dark - if dark then bubbles
	 * @param circlesIn
	 * @param RadiusListIn
	 * @param peakListin
	 */
	@SuppressWarnings("unused")
	private void checkforBubbles(Point[] circlesIn, double[] RadiusListIn, int[]peakListin){
		// create a dummy image BW of same size as original image imgIn
		ImagePlus BW = NewImage.createByteImage("Temporary Image Holder", width, height, 1, NewImage.FILL_BLACK);
		ImageProcessor BW_ip = BW.getProcessor();
		BW_ip.copyBits(ip, 0, 0, Blitter.COPY);
//		BW.show();
		// set all pixels of BW inside hough circle to 1, otherwise 0
		// Creates a r=2 disk structuring element
		// apply erosion on the BW and generate a tmp image
		// generate a ring (= BW - tmp) that only the pixels on the Hough circle edge are 1
		// apply ring as a binary mask on imgIn to get imageTmp
		// calculate the mean value of imageTmp
		// if meanValue > bubbleThreshold then it means features on the inner boundary are not black, then keep the data point; 
		// otherwise remove it
		// update the results
	}

	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Hough_Circles10.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
//		new ImageJ();

		// open the sample image
		String path = System.getProperty("user.dir") + "/images/junkfinder_25x_dic_frame1.jpg";
		//String path = System.getProperty("user.dir") + "/images/junkfinder_25x_dic_frame1801.jpg";
		ImagePlus image = IJ.openImage(path);
		image.show();

		IJ.run("8-bit");
		IJ.run("Smooth");
		IJ.run("Find Edges");
		//	IJ.run("Auto Threshold", "method=Default white");  // this plugin cannot be called in Java
		IJ.run("Size...", "width=200 height=129 constrain average interpolation=Bilinear");
		IJ.setAutoThreshold(image,"Default dark");
		IJ.run(image, "Convert to Mask", "");

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
		
		//		// Creates a r=2 disk structuring element
		//		Strel strel = Strel.Shape.DISK.fromRadius(2);
		//		// applies dilation on current image
		//		ImageProcessor image1 = IJ.getImage().getProcessor();
		//		ImageProcessor eroded = strel.erosion(image1);
		//		// Display results
		////		new ImagePlus("eroded", eroded).show();
	}


}