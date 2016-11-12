package loci.exploratorium;

/**
 * This ImageJ plugin shows the Hough Transform Space and search for circles in
 * a binary image. The image must have been passed through an edge detection
 * module and have edges marked in white (background must be in black). Input
 * image should go through "8-bit", "Smooth", "Find Edges", "Auto Threshold -
 * method=Default white", and "Size... - width=200 height=129 constrain average
 * interpolation=Bilinear". MorphoLibJ ImageJ plugin should be included in the 
 * Java Build Path for morphological filters required in this plugin.
 * <p>
 * Work based on Hough_Circles10.java by Norbert Vischer (https://goo.gl/YR4m5A)
 * created on Feb 22, 2007, that originally based on the version by 
 * Hemerson Pistori and Eduardo Rocha Costa created on Mar 18 2004, 
 * based on Mark A. Schulze applet (http://www.markschulze.net/).
 * </p>
 * <p>
 * Latest version by Guneet Singh Mehta and Bing Dai created on Oct 27 2016.
 * </p>
 * 
 * @author Hemerson Pistori (pistori@ec.ucdb.br) and Eduardo Rocha Costa
 * @author Norbert Vischer
 * @author Guneet Singh Mehta (gmehta2@wisc.edu) and Bing Dai (bdai6@wisc.edu)
 */

//package sigus.templateMatching;
//import sigus.*;

import ij.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import inra.ijpb.morphology.Strel;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Stack;

import ij.gui.*;
import ij.measure.*;
import ij.plugin.frame.*;
import ij.process.*;


public class HoughAdultFish implements PlugInFilter {
	class circle
	{
		int centerX;
		int centerY;
		int radius;
		int houghValue;
		public circle(int x,int y,int r,int val)
		{
			this.centerX=x;
			this.centerY=y;
			this.radius=r;
			this.houghValue=val;
		}
		public boolean intersect(circle other)
		{
			double tolerance=0.80;//tolerance of overlap
			double dist=Math.sqrt(Math.pow(this.centerX-other.centerX, 2)+(Math.pow(this.centerY-other.centerY, 2)));
			if(dist<tolerance*(this.radius+other.radius))
			{
				return true;
			}
			return false;
		}
		public void resize(float factor)
		{
			this.centerX=(int) (this.centerX*factor);
			this.centerY=(int) (this.centerY*factor);
			this.radius=(int) (this.radius*factor);
		}
	}
	
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
	public circle circles[];//circles to be plotted
	private int vectorMaxSize = 500;
	boolean useThreshold = false;
	int lut[][][]; // LookUp Table for rsin e rcos values
	public static ImagePlus image,originalImage,edgeImage,originalImageFullSize;
	public static String imagePath;
	//private static RoiManager roiManager=new RoiManager();
	private static RoiManager roiManager=new RoiManager(false);
	public static PriorityQueue<circle> circlePriorityQueue;
	public static float downSizeFactor;

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
			Comparator<circle> comp=new Comparator<circle>()
					{
						public int compare(circle a,circle b)
						{
							if(a.houghValue>b.houghValue){return -1;}
							else if(a.houghValue<b.houghValue){return 1;}
							else
							{
								return 0;
							}
						}
					};
			circlePriorityQueue=new PriorityQueue<circle>(maxCircles,comp);
			houghTransform();//makes a width*height*depth array- "houghValues"
			createMiniMap();//reduces the houghValues' width and heightbya factor TEN
			// Create image View for Hough Transform.

			if (showOutputImage == 2) {
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
				createHoughPixels(newpixels, 0);//scales the houghValues acccumulator to show as a grayscale image
				ImageProcessor circlesip = new ByteProcessor(width, height);
				byte[] circlespixels = (byte[])circlesip.getPixels();
				removeIntersections();
				removeBubbles();
				drawCircles(circlespixels);
				//new ImagePlus("Hough Space [r="+radiusMin+"]", newip).show(); // Shows only the hough space for the minimun radius
				//new ImagePlus(maxCircles+" Circles Found", circlesip).show();
			}
			originalImage.show();
		}
	}

	public static void removeBubbles()
	{
		//Finds inner and outer boundaries and the average 8-bit value within them - if either is less than a threshold -50 => Remove the circle
		//inner boundary found by =>maskInCircle-MaskInCircleEroded
		//outer boundary found by =>maskInCircleDilated-maskInCircle
		Stack<circle> circleStack=new Stack<circle>();
		circle currentCircle;
		originalImage.show();
		int index=0;
		ImagePlus mask,dilatedMask,erodedMask,invertedMask;
		ImagePlus innerBoundary,outerBoundary;
		ImagePlus innerBoundaryMask,outerBoundaryMask;
		ImageCalculator imageCalculator=new ImageCalculator();
		
		while(!circlePriorityQueue.isEmpty())
		{
			mask=IJ.createImage("Mask","8-bit binary black",originalImage.getWidth(),originalImage.getHeight(),originalImage.getChannel());
			//mask=IJ.creat
			erodedMask=IJ.createImage("Mask","8-bit binary",originalImage.getWidth(),originalImage.getHeight(),originalImage.getChannel());
			dilatedMask=IJ.createImage("Mask","8-bit binary",originalImage.getWidth(),originalImage.getHeight(),originalImage.getChannel());
			invertedMask=IJ.createImage("Mask","8-bit binary",originalImage.getWidth(),originalImage.getHeight(),originalImage.getChannel());
			IJ.setForegroundColor(255, 255, 255);
			IJ.setBackgroundColor(0, 0, 0);
			//mask.show();//have to show to persist the image - and then close - find a way to avoid show
			mask.setTitle(new String("mask"));
			currentCircle=circlePriorityQueue.remove();
			
			int x,y,r;
			r=currentCircle.radius;
			x=currentCircle.centerX-r;
			y=currentCircle.centerY-r;
			OvalRoi currentROI=new OvalRoi(x,y,2*r,2*r);
			
			roiManager.addRoi(currentROI);
			originalImage.setRoi(currentROI);
			roiManager.select(index);
			roiManager.runCommand(mask,"Fill");
			IJ.run(mask, "Invert","");
			
			IJ.run(mask, "Erode","");
			IJ.run(mask, "Erode","");
			IJ.run(mask, "Erode","");
			mask.copy();
			erodedMask.paste();
			//erodedMask.show();
			erodedMask.setTitle(new String("Eroded Mask"));

			
			//One dilate to counter the Erosion and another dilation for actual dilation from the original mask
			IJ.run(mask,"Dilate","");
			IJ.run(mask,"Dilate","");
			IJ.run(mask,"Dilate","");
			IJ.run(mask,"Dilate","");
			IJ.run(mask,"Dilate","");
			IJ.run(mask,"Dilate","");
			mask.copy();
			dilatedMask.paste();
			//dilatedMask.show();
			dilatedMask.setTitle(new String("Dilated Mask"));
			
			IJ.run(mask, "Erode","");
			IJ.run(mask, "Erode","");
			IJ.run(mask, "Erode","");
			
			ImageCalculator ic = new ImageCalculator();
			innerBoundaryMask=ic.run("xor create",mask,erodedMask);
			//innerBoundaryMask.show();
			innerBoundaryMask.setTitle(new String("innerBoundaryMask"));
			
			
		    outerBoundaryMask=ic.run("subtract create",mask,dilatedMask);
		    //outerBoundaryMask.show();
		    outerBoundaryMask.setTitle(new String("outerBoundaryMask"));
		    
		    
		    ImagePlus innerBoundaryImage=ic.run("and create", innerBoundaryMask, originalImage);
		   //innerBoundaryImage.show();
		   innerBoundaryImage.setTitle(new String("innerBoundaryImage"));
		   
		   
		   ImagePlus outerBoundaryImage=ic.run("and create", outerBoundaryMask, originalImage);
		    //outerBoundaryImage.show();
		    outerBoundaryImage.setTitle(new String("outerBoundaryMask"));
		     
		    mask.copy();
		    invertedMask.paste();
		    IJ.run(invertedMask,"Invert","");
		    ImagePlus maskedImage=ic.run("and create", invertedMask, originalImage);
		    //maskedImage.show();
		    maskedImage.setTitle(new String("maskedImage"));

		    double w=mask.getWidth();
		    double h=mask.getHeight();
		    
		    double innerPixelsNum=getImageMeanValue(innerBoundaryMask)*w*h/255;//because each pixel has value of 255 in mask
		    double innerPixelsSum=getImageMeanValue(innerBoundaryImage)*w*h;
		    double meanInnerPixelValue=innerPixelsSum/innerPixelsNum;
		    
		    double outerPixelsNum=getImageMeanValue(outerBoundaryMask)*w*h/255;//because each pixel has value of 255 in mask
		    double outerPixelsSum=getImageMeanValue(outerBoundaryImage)*w*h;
		    double meanOuterPixelValue=outerPixelsSum/outerPixelsNum;
		    
			boolean isBubble=false;//true if currentCircle is a bubble
			isBubble=Math.abs(meanInnerPixelValue-meanOuterPixelValue)>10;//Bubble has low mean intensity - if either is less - we declare it as a bubble
			
			boolean falseFish=false;
			ImagePlus maskFishInterior=IJ.createImage("Mask","8-bit binary",originalImage.getWidth(),originalImage.getHeight(),originalImage.getChannel());
			erodedMask.copy();
			maskFishInterior.paste();
			//maskFishInterior.show();
			IJ.run(maskFishInterior, "Erode","");
			IJ.run(maskFishInterior, "Erode","");
			IJ.run(maskFishInterior,"Invert","");
			ImagePlus fishInteriorImage=ic.run("and create", maskFishInterior, image);
			//fishInteriorImage.show();
			//fishInteriorImage.setTitle("Fish Interior");
			
			ImagePlus edgeImageFishInterior=IJ.createImage("Mark","8-bit binary",edgeImage.getWidth(),edgeImage.getHeight(),edgeImage.getChannel());
			
			double edgeMeanValue=getImageMeanValue(fishInteriorImage);
			if(!isBubble&&!falseFish)
			{
				circleStack.push(currentCircle);
			}
			roiManager.runCommand(mask,"DeSelect");
			index++;

		}
		while(!circleStack.isEmpty())
		{
			circlePriorityQueue.add(circleStack.pop());
		}
		
	}
	
	public static double getImageMeanValue(ImagePlus image)
	{
		ResultsTable rt=new ResultsTable();
	    Analyzer an=new Analyzer(image,Analyzer.MEAN,rt);
	    an.measure();
	    rt.show("Results");
	    return rt.getValueAsDouble(1,0);
	}
	
	public static double getImageEntropy(ImagePlus image)
	{
		ResultsTable rt=new ResultsTable();
	    Analyzer an=new Analyzer(image,Analyzer.KURTOSIS,rt);
	    an.measure();
	    rt.show("Results");
	    return rt.getValueAsDouble(1,0);
	}
	
	private static void removeIntersections()
	{
		LinkedList<circle> list=new LinkedList<circle>();
		while(!circlePriorityQueue.isEmpty())
		{
			list.addLast(circlePriorityQueue.remove());
		}
		int i=0,j;
		while(i<list.size())
		{
			j=i+1;
			while(j<list.size())
			{
				if(list.get(i).intersect(list.get(j)))
				{
					list.remove(j);
				}
				else
				{
					j++;
				}
			}
			i++;
		}
		for(i=0;i<list.size();i++)
		{
			circlePriorityQueue.add(list.get(i));
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
		gd.addNumericField("Max_Radius (in pixels):", 55, 0);
		gd.addNumericField("Step_Radius (in pixels):", 2, 0);
		gd.addNumericField("Number_of_Circles (NC): (enter 0 if using threshold)", 10, 0);
		gd.addNumericField("Threshold: (not used if NC > 0)", 60, 0);
		gd.addNumericField("With_Output: (0, 1 or 2)", 1, 0);
		gd.addNumericField("Keep_previous_results: (0 or 1)", 0, 0);

		//gd.addCheckbox("Show output images", true); //n_ 

		//gd.showDialog();

		if (gd.wasCanceled()) {
			return(false);
		}

		radiusMin = (int) gd.getNextNumber();
		radiusMax = (int) gd.getNextNumber();
		radiusInc = (int) gd.getNextNumber();
		depth = ((radiusMax-radiusMin)/radiusInc)+1;
		maxCircles = (int) gd.getNextNumber();
		threshold = (int) gd.getNextNumber();
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
		for(int y = 1; y < l; y++) 
		{
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
		Stack<circle> circleStack;
		circle currentCircle,circleInPQ;
		int neighborhood=1;
		for(int y = neighborhood; y < height-neighborhood; y++) 
		{
			for(int x = neighborhood; x < width-neighborhood; x++) 
			{
				for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) 
				{
					int indexR=(radius-radiusMin)/radiusInc;
					if(houghValues[x][y][indexR]<100)//image should have atleast 50 points to be detected as a circle
					{
						continue;
					}
					boolean localMax=true;
					for(int a=-neighborhood;a<=neighborhood&&localMax==true;a++)
					{
						for(int b=-neighborhood;b<=neighborhood;b++)
						{
							if(houghValues[x][y][indexR]<houghValues[x-a][y-b][indexR])
							{
								localMax=false;
								break;
							}
						}
					}
					if(!localMax)
					{
						continue;
					}
					currentCircle=new circle(x,y,radius,houghValues[x][y][indexR]);
					//The code below is reached when the houghValue is greater than 50 , pixel is local maximum 
					//At all points the circlePriorityQueue must contain non overlapping circles
					if(circlePriorityQueue.size()==0)
					{
						circlePriorityQueue.add(currentCircle);
					}
					else
					{	
						circle intersectingCircle=null;		//if not null then the currentCircle intersects with a circle in PQ
						int numIntersections=0;
						circleStack=new Stack<circle>(); 	//a scratch stack used to store the circles in circlesPriorityQueue
						circle iteratorCircle;				//iterates through all circlesin circlesPriorityQueue
						
						//Finding Number of circles which intersect with the currentCircle and have HoughValues less than currentCircle's -
						//i.e. finding number of circles in the priority queue which are less prominent than the current circle
						while(!circlePriorityQueue.isEmpty())
						{
							iteratorCircle=circlePriorityQueue.remove();
							if(currentCircle.intersect(iteratorCircle)&&currentCircle.houghValue>iteratorCircle.houghValue)
							{
								numIntersections++;
								intersectingCircle=iteratorCircle;
							}
							if(currentCircle.intersect(iteratorCircle))
							{
								intersectingCircle=iteratorCircle;
							}
							circleStack.push(iteratorCircle);
						}
						while(!circleStack.isEmpty())
						{
							circlePriorityQueue.add(circleStack.pop());	//Restoring the priorityQueue from stack
						}
						
						//If there is a circle which has higher houghValue intersecting the circles in PQ -then we remove all such circles
						//if there is no intersection 
						if(numIntersections>0)
						{
							boolean circleRemoved=false;
							//removing all circles in PQ which intersect with currentCircle
							while(!circlePriorityQueue.isEmpty())
							{
								iteratorCircle=circlePriorityQueue.remove();
								if(currentCircle.intersect(iteratorCircle)&&currentCircle.houghValue>iteratorCircle.houghValue)
								{
									//Discard the circles in PQ which intersect with the currentCircle and have a lower HoughValue than currentCircle
									//Discarding done by not pushing the iterator circle in stack
									circleRemoved=true;
								}
								else
								{
									circleStack.push(iteratorCircle);
								}
							}
							while(!circleStack.isEmpty())
							{
								//transfering stack circles to the PQ
								circlePriorityQueue.add(circleStack.pop());
							}
							if(circleRemoved&&circlePriorityQueue.size()<maxCircles)
							{
								//if the currentCircle is more prominent it would have removed a circle from the PQ
								//if the currentCircle was not prominent and there is space in t
								circlePriorityQueue.add(currentCircle);	
							}
						}
						else if(intersectingCircle==null&&circlePriorityQueue.size()<maxCircles)
						{
							//No circle in circlesPriorityQueue intersects with the currentCircle and there is a space in the pq to push one more
							circlePriorityQueue.add(currentCircle);
						}
					}
				}			
			}
		}
	}
	
	public int findIntersectionNumberAndClosestCircle(circle intersectingCircle,circle currentCircle)
	{
		int ans=0;
		Stack<circle> circleStack=new Stack<circle>();
		circle iteratorCircle;
		while(!circlePriorityQueue.isEmpty())
		{
			iteratorCircle=circlePriorityQueue.remove();
			if(currentCircle.intersect(iteratorCircle))
			{
				ans++;
				intersectingCircle=iteratorCircle;
			}
			circleStack.push(iteratorCircle);
		}
		while(!circleStack.isEmpty())
		{
			circlePriorityQueue.add(circleStack.pop());
		}
		return ans;
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
		int kip=1;
		originalImage.show();
		originalImage.deleteRoi();
		image.deleteRoi();
		while(!circlePriorityQueue.isEmpty())
		{
			circle currentCircle=circlePriorityQueue.remove();
			//System.out.format("before %d %d %d\n",currentCircle.centerX,currentCircle.centerY,currentCircle.radius);
			int radius=currentCircle.radius;
			//image.setRoi(new OvalRoi(currentCircle.centerX-radius,currentCircle.centerY-radius,2*radius,2*radius));
			//IJ.run("Overlay Options...", "stroke=red width=3 fill=none apply");
			//IJ.run(image, "Add Selection...", "");
			
			currentCircle.resize(downSizeFactor);
			radius=currentCircle.radius;
			originalImage.setRoi(new OvalRoi(currentCircle.centerX-radius,currentCircle.centerY-radius,2*radius,2*radius));
	        IJ.run("Overlay Options...", "stroke=red width=3 fill=none apply");
	        IJ.run(originalImage, "Add Selection...", "");
	        System.out.format("circleNumber=%d centerX=%d centerY=%d radius=%d houghValue=%d\n",kip++,currentCircle.centerX,currentCircle.centerY,currentCircle.radius,currentCircle.houghValue);
		}
		originalImage.deleteRoi();
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
		circles=new circle[maxCircles];
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
				circles[c]=new circle(xMax,yMax,radiusMin+radiusInc*rMaxIndex,counterMax);
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
			for(int y = 0; y < height; y++) 
			{
				for(int x = 0; x < width; x++) 
				{
					int thisVal = houghValues[x][y][indexR];
					if(thisVal > threshold) 
					{
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
		rt.show("Results");
	}

	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = HoughAdultFish.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		//imagePath = System.getProperty("user.dir") + "/images/junkfinder_25x_dic_frame1.jpg";
		//imagePath = System.getProperty("user.dir") + "/images/junkfinder_25x_dic_frame1801.jpg";
		//imagePath = System.getProperty("user.dir") + "/images/egg1.jpg";
		//3 Fish
		//imagePath = System.getProperty("user.dir") + "/images/Kip1.jpg";
		
		//1 Partial Fish
		//imagePath = System.getProperty("user.dir") + "/images/Kip801.jpg";
		
		//1 Full Fish
		//imagePath = System.getProperty("user.dir") + "/images/Kip1201.jpg";
		
		//1 Bubble
		//imagePath = System.getProperty("user.dir") + "/images/Kip1401.jpg";
		
		//2 Bubbles - Incorrect bubble removal
		//imagePath = System.getProperty("user.dir") + "/images/Kip2801.jpg";
				
		//1 Partial fish and shell
		//imagePath = System.getProperty("user.dir") + "/images/Kip2401.jpg";
				
		//Nothing in image
		//imagePath = System.getPropertsy("user.dir") + "/images/Kip1601.jpg";
				
		//Adult fish
		imagePath = System.getProperty("user.dir") + "/images/18121.jpg";
		
		originalImageFullSize = IJ.openImage(imagePath );
		originalImage = IJ.openImage(imagePath );
		originalImageFullSize.show();
		//IJ.run(originalImage,"Size...", "width=200 height=129 constrain average interpolation=Bilinear");
		downSizeFactor=originalImage.getWidth()/(float)200;
		IJ.run(originalImage,"Size...", "width=200 constrain average interpolation=Bilinear");

		edgeImage=IJ.openImage(imagePath);
		//edgeImage.show();
		IJ.run(edgeImage,"8-bit","");
		IJ.run(edgeImage,"Smooth","");
		IJ.run(edgeImage,"Find Edges","");
		
		image = IJ.openImage(imagePath );
		//image.show();
		IJ.run(image,"8-bit","");
		IJ.run(image,"Smooth","");
		IJ.run(image,"Find Edges","");

		IJ.setAutoThreshold(image,"Default dark");
		IJ.run(image, "Convert to Mask", "");
		IJ.run(image,"Dilate","");
		IJ.run(image,"Dilate","");
		IJ.run(image,"Dilate","");
		IJ.run(image,"Fill Holes","");
		IJ.run(image,"Erode","");
		IJ.run(image,"Erode","");
		IJ.run(image,"Erode","");
		IJ.run(image,"Outline","");
		IJ.run(image,"Size...", "width=200 constrain average interpolation=Bilinear");
		IJ.run(edgeImage,"Size...", "width=200 constrain average interpolation=Bilinear");
		//image.show();
		// run the plugin
		IJ.runPlugIn(image,clazz.getName(), "");
	}
}