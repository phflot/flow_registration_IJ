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

import org.scijava.command.Interactive;

import floregistration.algorithm.RegistrationChannelOptions;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.img.imageplus.ImagePlusImg;

/**
 * Dialog that adds a new channel for the registration
 *
 */
public class AddNewChannelDialog extends GenericDialog {
		
	private static final long serialVersionUID = 2206943449958360921L;
	
	@SuppressWarnings("rawtypes")
	private RegistrationChannelOptions registrationChannelOptions;

	@SuppressWarnings("rawtypes")
	public RegistrationChannelOptions getRegistrationChannelOptions() {
		return registrationChannelOptions;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AddNewChannelDialog(String title, Frame parent, List<ImagePlusImg> images) {
		super(title, parent);
				
		setOKLabel("Add");
		setResizable(false);
		
		registrationChannelOptions = new RegistrationChannelOptions();
				
		int n_channels = images.size();
		String[] open_images = new String[n_channels];
		for (int i = 0; i < n_channels; i++) {
			open_images[i] = images.get(i).getImagePlus().getShortTitle();
		}
		
		images.get(0).getImagePlus().copy();
		ImagePlus currentImage = ImagePlus.getClipboard();
		
		// Selection of the input image:
		addChoice("Channel", open_images, open_images[0]);
		final Choice imp_choice = (Choice)this.getChoices().lastElement();
		registrationChannelOptions.setImg(images.get(imp_choice.getSelectedIndex()));
		imp_choice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationChannelOptions.setImg(images.get(imp_choice.getSelectedIndex()));
				currentImage.setImage(images.get(imp_choice.getSelectedIndex()).getImagePlus());
				repaint();
			}
		});
		addImage(currentImage);

		
		// Selection of sigma x
		addNumericField("sigma x", registrationChannelOptions.getSigma()[0], 2);
		final TextField sigmax_field = ((TextField)super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[0] = Float.parseFloat(sigmax_field.getText());
		sigmax_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[0] = Float.parseFloat(sigmax_field.getText());
			}
		});
		
		// Selection of sigma y
		addNumericField("sigma y", registrationChannelOptions.getSigma()[1], 2);
		final TextField sigmay_field = ((TextField)super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[1] = Float.parseFloat(sigmay_field.getText());
		sigmay_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[1] = Float.parseFloat(sigmay_field.getText());
			}
		});
		
		// Selection of sigma z
		addNumericField("sigma z", registrationChannelOptions.getSigma()[2], 2);
		final TextField sigmaz_field = ((TextField)super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[2] = Float.parseFloat(sigmaz_field.getText());
		sigmaz_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[2] = Float.parseFloat(sigmaz_field.getText());
			}
		});
		
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
}
