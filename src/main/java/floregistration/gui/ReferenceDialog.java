package floregistration.gui;

import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.ui.rangeslider.RangeSlider;
import floregistration.algorithm.RegistrationChannelOptions;
import floregistration.algorithm.RegistrationJob;
import floregistration.algorithm.Util;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.view.Views;

public class ReferenceDialog extends GenericDialog {
	
	private final RegistrationJob channels;
	private ImagePlusImg<? extends ComplexType, FloatArray> meanImage;
	private ImagePlus meanImageIMP;
	private final RangeSlider rangeSlider;
	long nFrames = 0;

	/**
	 * 
	 */
	private static final long serialVersionUID = 2321714362453637841L;

	public ReferenceDialog(String title, final Frame parent, RegistrationJob channels) {
		super(title, parent);
		
		this.channels = channels;
		
		final long dims[] = new long[channels.getFirst().getImg().numDimensions()];
		channels.getFirst().getImg().dimensions(dims);
		nFrames = dims[2];
		dims[2] = channels.size();
		
		meanImage = channels.getFirst().getImg().factory().create(dims);
		meanImageIMP = meanImage.getImagePlus();
		addImage(meanImageIMP);

		//RandomAccessibleInterval view = Views.interval(images.get(impChoice.getSelectedIndex()), 
		//		new long[]{0, 0, 0}, viewDims);
		
		Panel rangeSliderPanel = new Panel();
		rangeSlider = new RangeSlider(0, (int) nFrames - 1);
		rangeSlider.setMinimum(0);
		rangeSlider.setValue(0);
		rangeSlider.setUpperValue(Math.min(10, (int)nFrames));
		rangeSliderPanel.add(rangeSlider);
		addPanel(rangeSliderPanel);
		
		rangeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateMeanImage();
			}
		});
		
		updateMeanImage();
	}
	
	private void updateMeanImage() {
		for (int i = 0; i < channels.size(); i++) {
			PlanarImg<? extends ComplexType, FloatArray> tmp = 
					(PlanarImg<? extends ComplexType, FloatArray>)
					 Util.getMean3(channels.get(i).getImg(), 
							 rangeSlider.getValue(), rangeSlider.getUpperValue());
			meanImage.setPlane(i, tmp.getPlane(0));
		}
		meanImageIMP.setImage(meanImage.getImagePlus());
		super.repaint();
	}
}
