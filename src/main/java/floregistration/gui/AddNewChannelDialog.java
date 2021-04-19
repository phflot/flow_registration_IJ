package floregistration.gui;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JSlider;

import org.scijava.command.Interactive;

import floregistration.algorithm.RegistrationChannelOptions;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.math.Min;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Dialog that adds a new channel for the registration
 *
 */
public class AddNewChannelDialog extends GenericDialog {
		
	private static final long serialVersionUID = 2206943449958360921L;
	
	private ImagePlus currentImage;
	private List<ImagePlusImg> images;
	private final Choice impChoice;
	
	@SuppressWarnings("rawtypes")
	private RegistrationChannelOptions registrationChannelOptions;

	@SuppressWarnings("rawtypes")
	public RegistrationChannelOptions getRegistrationChannelOptions() {
		return registrationChannelOptions;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AddNewChannelDialog(String title, Frame parent, List<ImagePlusImg> images) {
		super(title, parent);
		
		this.images = images;
		
		setOKLabel("Add");
		setResizable(false);
		
		registrationChannelOptions = new RegistrationChannelOptions();
				
		int n_channels = images.size();
		String[] open_images = new String[n_channels];
		for (int i = 0; i < n_channels; i++) {
			open_images[i] = images.get(i).getImagePlus().getShortTitle();
		}
		
		currentImage = ((ImagePlusImg)images.get(0).copy()).getImagePlus();
		addImage(currentImage);
		
		// Selection of the input image:
		addChoice("Channel", open_images, open_images[0]);
		impChoice = (Choice)this.getChoices().lastElement();
		registrationChannelOptions.setImg(images.get(impChoice.getSelectedIndex()));
		impChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationChannelOptions.setImg(images.get(impChoice.getSelectedIndex()));
				repaint();
			}
		});
		repaint();

		
		// Selection of sigma x
		this.addSlider("sigma x", 0.05f, 5.0f, registrationChannelOptions.getSigma()[0]);
		final TextField sigmax_field = ((TextField)super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[0] = Float.parseFloat(sigmax_field.getText());
		sigmax_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[0] = Float.parseFloat(sigmax_field.getText());
				repaint();
			}
		});
		
		// Selection of sigma y
		this.addSlider("sigma y", 0.05f, 5.0f, registrationChannelOptions.getSigma()[1]);
		final TextField sigmay_field = ((TextField)super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[1] = Float.parseFloat(sigmay_field.getText());
		sigmay_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[1] = Float.parseFloat(sigmay_field.getText());
				repaint();
			}
		});
		
		// Selection of sigma z
		this.addSlider("sigma z", 0.05f, 5.0f, registrationChannelOptions.getSigma()[2]);
		final TextField sigmaz_field = ((TextField)super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[2] = Float.parseFloat(sigmaz_field.getText());
		sigmaz_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[2] = Float.parseFloat(sigmaz_field.getText());
				repaint();
			}
		});
		
		Vector sigmaValues = this.getNumericFields();
		
		addCheckbox("Register inplace", registrationChannelOptions.isInplace());
		final Checkbox isInplaceBox = (Checkbox)super.getCheckboxes().lastElement();
		// isInplaceBox.setEnabled(false);
		isInplaceBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationChannelOptions.setInplace(isInplaceBox.getState());
				repaint();
			}
		});	
	}
	
	public void repaint() {
		
		long[] dims = new long[images.get(impChoice.getSelectedIndex()).numDimensions()];
		assert dims.length == 3;
		images.get(impChoice.getSelectedIndex()).dimensions(dims);
		long[] viewDims = new long[] {dims[0]-1, dims[1]-1, Math.min(50, dims[2])-1};
		long[] newDims = new long[]{dims[0], dims[1], Math.min(50, dims[2])};
		
		RandomAccessibleInterval view = Views.interval(images.get(impChoice.getSelectedIndex()), 
				new long[]{0, 0, 0}, viewDims);
				
		ImagePlusImg<? extends ComplexType, ? extends ArrayDataAccess> img = 
				images.get(impChoice.getSelectedIndex()).factory().create(newDims);
		
		float[] s = registrationChannelOptions.getSigma();
		double[] sigma = new double[] {s[0], s[1], s[2]};
		Gauss3.gauss(sigma, Views.extendBorder(view), img);
		
		currentImage.setImage(img.getImagePlus());
		super.repaint();
	}
}
