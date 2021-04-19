package floregistration.gui;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JSlider;

import org.scijava.command.Interactive;

import floregistration.algorithm.RegistrationChannelOptions;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PlotCanvas;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
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
	
	private ImagePlusImg<? extends ComplexType, ? extends ArrayDataAccess> currentImageStack;
	private ImagePlus currentImageIMP;
	private long[] dims;
	private List<ImagePlusImg> images;
	private final Choice impChoice;
	private final Scrollbar timeSlider;
	private long gaussCooldown = 0;
	private long gaussCooldowmPeriod = 100;
	
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
		
		// currentImageIMP = ((ImagePlusImg<? extends ComplexType, ? extends ArrayDataAccess>)images.get(0).copy()).getImagePlus();
		dims = new long[images.get(0).numDimensions()];
		images.get(0).dimensions(dims);
		currentImageIMP = images.get(0).factory().create(new long[] {dims[0], dims[1], 1}).getImagePlus();
		setPreviewStack(images.get(0));
		updateGauss();
		addImage(currentImageIMP);
		
		timeSlider = new Scrollbar(Scrollbar.HORIZONTAL, 1, 5, 1, 10);
		timeSlider.setSize(new Dimension(512, 10));
		// this.addSlider("", 1, 1, 1);
		this.add(timeSlider);
		//final TextField timeSliderValue = ((TextField)super.getNumericFields().lastElement());
		//timeSliderValue.setVisible(false);
		// timeSlider = (Scrollbar)super.getSliders().lastElement();
		
		// Selection of the input image:
		addChoice("Channel:", open_images, open_images[0]);
		impChoice = (Choice)this.getChoices().lastElement();
		registrationChannelOptions.setImg(images.get(impChoice.getSelectedIndex()));
		setPreviewStack(images.get(impChoice.getSelectedIndex()));
		impChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationChannelOptions.setImg(images.get(impChoice.getSelectedIndex()));
				setPreviewStack(images.get(impChoice.getSelectedIndex()));
				timeSlider.setMaximum((int) dims[2]);
			}
		});
		repaint();
		
		/*
		timeSliderValue.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				currentImage.setSlice(Integer.parseInt(timeSliderValue.getText()));
				updateImg();
			}
		});*/
		
		addCheckbox("2D symmetric kernel", true);
		final Checkbox symKernelBox = (Checkbox)super.getCheckboxes().lastElement();

		
		// Selection of sigma x
		this.addSlider("sigma x", 0.05f, 5.0f, registrationChannelOptions.getSigma()[0]);
		final Scrollbar sigmaXSlider = (Scrollbar) this.getSliders().lastElement();
		final TextField sigmaXField = ((TextField)super.getNumericFields().lastElement());
		
		this.addSlider("sigma y", 0.05f, 5.0f, registrationChannelOptions.getSigma()[1]);
		final Scrollbar sigmaYSlider = (Scrollbar) this.getSliders().lastElement();
		final TextField sigmaYField = ((TextField)super.getNumericFields().lastElement());
		sigmaYSlider.setEnabled(false);
		sigmaYField.setEnabled(false);
		
		sigmaXField.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				synchronized (this) { 
					float sigmaX = Float.parseFloat(sigmaXField.getText());
					registrationChannelOptions.getSigma()[0] = sigmaX;
					if (symKernelBox.getState()) {
						sigmaYField.setText(sigmaXField.getText());
						registrationChannelOptions.getSigma()[1] = sigmaX;
					} 
					if (System.currentTimeMillis() - gaussCooldown > gaussCooldowmPeriod) {
						updateGauss();
						gaussCooldown = System.currentTimeMillis();
					}
				}
			}
		});
				
		// Selection of sigma y
		sigmaYField.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				float sigmaY = Float.parseFloat(sigmaYField.getText());
				registrationChannelOptions.getSigma()[1] = sigmaY;
				if (System.currentTimeMillis() - gaussCooldown > gaussCooldowmPeriod) {
					updateGauss();
					gaussCooldown = System.currentTimeMillis();
				}
			}
		});
		
		symKernelBox.addItemListener(new ItemListener() {	
			@Override
			public void itemStateChanged(ItemEvent evt) {
				if (symKernelBox.getState()) {
					sigmaYField.setEnabled(false);
					sigmaYSlider.setEnabled(false);
				}
				else {
					sigmaYField.setEnabled(true);
					sigmaYSlider.setEnabled(true);
				}
			}
		});		
		
		// Selection of sigma z
		this.addSlider("sigma z", 0.05f, 5.0f, registrationChannelOptions.getSigma()[2]);
		final Scrollbar sigmazSlider = (Scrollbar)this.getSliders().lastElement();
		final TextField sigmaz_field = ((TextField)super.getNumericFields().lastElement());
		sigmaz_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[2] = Float.parseFloat(sigmaz_field.getText());
				
				/*if (System.currentTimeMillis() - gaussCooldown > gaussCooldowmPeriod) {
					updateGauss();
					gaussCooldown = System.currentTimeMillis();
				}*/
			}
		});
		
		// setting the slider mouse up events to ensure that the filtered image is always updated:
		MouseListener sliderMouseUpListener = new MouseListener() {	
			@Override
			public void mouseReleased(MouseEvent e) {
				updateGauss();
			}

			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		};
		sigmaXSlider.addMouseListener(sliderMouseUpListener);
		sigmaYSlider.addMouseListener(sliderMouseUpListener);
		sigmazSlider.addMouseListener(sliderMouseUpListener);
		
		addCheckbox("Register inplace", registrationChannelOptions.isInplace());
		final Checkbox isInplaceBox = (Checkbox)super.getCheckboxes().lastElement();
		// isInplaceBox.setEnabled(false);
		isInplaceBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationChannelOptions.setInplace(isInplaceBox.getState());
			}
		});
	}
	
	private void updateImg() {
		super.repaint();
	}
	
	private void updateGauss() {
		float[] s = registrationChannelOptions.getSigma();
		double[] sigma = new double[] {s[0], s[1], s[2]};
		ImagePlusImg<? extends ComplexType, ? extends ArrayDataAccess> tmpImg = currentImageStack.factory().create(dims);
		Gauss3.gauss(sigma, Views.extendBorder(currentImageStack), tmpImg);
		currentImageIMP.setImage(tmpImg.getImagePlus());
		super.repaint();
	}
	
	private void setPreviewStack(ImagePlusImg img) {
		long[] dims = new long[img.numDimensions()];
		assert dims.length == 3;
		img.dimensions(dims);

		long[] viewDims = new long[] {dims[0]-1, dims[1]-1, Math.min(50, dims[2])-1};
		long[] newDims = new long[]{dims[0], dims[1], Math.min(50, dims[2])};
		
		RandomAccessibleInterval view = Views.interval(img, 
				new long[]{0, 0, 0}, viewDims);
		
		currentImageStack = img.factory().create(newDims);

		Cursor<? extends ComplexType> target = currentImageStack.localizingCursor();
		RandomAccess<? extends ComplexType> ra = view.randomAccess();
		while(target.hasNext()) {
			target.fwd();
			ra.setPosition(target);
			target.get().set(ra.get());
		}
		
		dims = newDims;
		updateGauss();
	}
	/*
	public void repaint() {
		/*
		long[] dims = new long[images.get(impChoice.getSelectedIndex()).numDimensions()];
		assert dims.length == 3;
		images.get(impChoice.getSelectedIndex()).dimensions(dims);
		long[] viewDims = new long[] {dims[0]-1, dims[1]-1, Math.min(50, dims[2])-1};
		long[] newDims = new long[]{dims[0], dims[1], Math.min(50, dims[2])};*/
		
		/*
		RandomAccessibleInterval view = Views.interval(images.get(impChoice.getSelectedIndex()), 
				new long[]{0, 0, 0}, viewDims);
				
		ImagePlusImg<? extends ComplexType, ? extends ArrayDataAccess> img = 
				images.get(impChoice.getSelectedIndex()).factory().create(newDims);
		
		float[] s = registrationChannelOptions.getSigma();
		double[] sigma = new double[] {s[0], s[1], s[2]};
		Gauss3.gauss(sigma, Views.extendBorder(currentImageStack), currentImageStack);
		
		currentImageIMP.setImage(currentImageStack.getImagePlus());*/
		//updateGauss();
	//}
}
