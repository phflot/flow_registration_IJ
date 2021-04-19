package floregistration;

import ij.IJ;
import ij.WindowManager;
import ij.gui.MessageDialog;
import ij.plugin.*;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.NativeType;
import java.util.ArrayList;
import java.util.List;
import floregistration.gui.OptionsDialog;

/**
 * Main PlugIn class
 */
public class FloRegistration implements PlugIn {

	public static void main(String[] args) {

	}
	
	public FloRegistration() {

	}
	
	@Override
	public void run(String arg) {
		
		final int[] imageIDX = WindowManager.getIDList();
		final int nImages = WindowManager.getImageCount();		
		
		final List<ImagePlusImg> images = new ArrayList<ImagePlusImg>(nImages);
		
		for (int i = 0; i < nImages; i++) {
			images.add(i, ImagePlusImgs.from(WindowManager.getImage(imageIDX[i])));
		}
		
		if (nImages == 0)
			new MessageDialog(IJ.getInstance(), "Error starting Plugin", 
					"Plugin needs at least one open Image!");
		else
			new OptionsDialog(IJ.getInstance(), images).showDialog();
	}
}
