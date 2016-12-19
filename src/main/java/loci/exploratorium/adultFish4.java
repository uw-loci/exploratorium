package loci.exploratorium;

import ij.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.*;
import ij.process.*;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.Morphology;

public class adultFish4 implements PlugInFilter {
	public static ImagePlus originalImage,mask1;
	public static String imagePath;
	
	public int setup(String arg, ImagePlus imp) {
		//does nothing
		return DONE;
	}

	public void run(ImageProcessor ip) {
		//does nothing
	}

	public static void main(String[] args)
	{
		float ans[][]=new adultFish4().getCoordinates();
		for(int i=0;i<ans.length;i++)
		{
			System.out.printf("x=%f\ty=%f\tr=%f\n",ans[i][0],ans[i][1],ans[i][2]);
		}
	}
	
	public float[][] getCoordinates() {
		Class<?> clazz = adultFish2.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		//Path of image
		imagePath = System.getProperty("user.dir") + "/images/fry"+Integer.toString(1)+".jpg";
		originalImage = IJ.openImage(imagePath );
		originalImage.setTitle(new String("OriginalImage"));
		
		//Thresholding image to find dark regions
		int darkThreshold=50;
		mask1=originalImage.duplicate();
		IJ.run(mask1,"8-bit","");
		IJ.run(mask1,"Smooth","");
		IJ.setAutoThreshold(mask1, "Default");
		IJ.setThreshold(mask1, 0, darkThreshold);
		IJ.run(mask1,"Convert to Mask","");
		
		//Removing small black areas - smaller than circle of radius 10
		Strel openingDisk=Strel.Shape.DISK.fromRadius(10);
		ImageProcessor kip=Morphology.opening(mask1.getProcessor(), openingDisk);
		mask1.setImage(kip.createImage());
		
		int options=ParticleAnalyzer.SHOW_MASKS+ParticleAnalyzer.IN_SITU_SHOW;
		int measurements=ParticleAnalyzer.CIRCULARITY+ParticleAnalyzer.AREA+ParticleAnalyzer.CENTROID;
		int minArea=2000;
		int maxArea=Integer.MAX_VALUE;
		float minCircularity=(float) 0.3;//
		float maxCircularity=(float) 1.0;//
		ResultsTable rt=new ResultsTable();
		ParticleAnalyzer pa=new ParticleAnalyzer(options,measurements,rt,minArea,maxArea,minCircularity,maxCircularity);
		//ParticleAnalyzer pa=new ParticleAnalyzer(options,measurements,rt,minArea,maxArea);//Particle Analyzer without specifying circulatrity
		pa.analyze(mask1);
		//mask1.show();

		float coordinates[][]=new float[rt.size()][3];//format x,y,radius
		for(int x=0;x<rt.size();x++)
		{
			String temp=rt.getRowAsString(x);
			String array[]=temp.split("\t");
			float area=Float.parseFloat(array[1]);
			coordinates[x][0]=Float.parseFloat(array[2]);//x coordinate of centroid
			coordinates[x][1]=Float.parseFloat(array[3]);//y coordinate of centroid
			coordinates[x][2]=(float) Math.sqrt(area/Math.PI);//radius
		}
		return coordinates;
	}
}

/*
	ImageJ Macro - for referral
open("C:\\Users\\S.S. Mehta\\Desktop\\Exp2\\exploratorium\\images\\fry10.jpg");
run("8-bit");
run("Smooth");
setAutoThreshold("Default");
run("Threshold...");
setThreshold(0, 50);
run("Convert to Mask");
run("Close");
run("Morphological Filters", "operation=Opening element=Disk radius=10");
selectWindow("fry10-Opening");
run("Analyze Particles...", "size=2000-Infinity circularity=0.3-1.00 show=[Masks] display exclude");
*/