package loci.exploratorium;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import ij.gui.*;
import inra.ijpb.morphology.Strel;
/***
 * Morphological filters ImageJ plugin is needed.
 * http://imagej.net/MorphoLibJ
 * @author Bing Dai
 *
 */
public class testMorphologicalFilter {
	public static void main(String[] args) {

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		String path = System.getProperty("user.dir") + "/images/junkfinder_25x_dic_frame1801.jpg";
		//		String path = System.getProperty("user.dir") + "/images/junkfinder_25x_dic_frame1801.jpg";
		ImagePlus image = IJ.openImage(path);
		image.show();

		IJ.run("8-bit");
//		IJ.run("Smooth");
//		IJ.run("Find Edges");
		//	IJ.run("Auto Threshold", "method=Default white");  // this plugin cannot be called in Java
		IJ.run("Size...", "width=200 height=129 constrain average interpolation=Bilinear");
//		IJ.setAutoThreshold(image,"Default dark");
//		IJ.run(image, "Convert to Mask", "");

		// Creates a r=2 disk structuring element
		Strel strel = Strel.Shape.DISK.fromRadius(5);
		// applies dilation on current image
		ImageProcessor image1 = IJ.getImage().getProcessor();
		ImageProcessor eroded = strel.erosion(image1);
		// Display results
		new ImagePlus("eroded", eroded).show();
	}

}
