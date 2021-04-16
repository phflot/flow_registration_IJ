package floregistration.algorithm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;

/**
 * 
 * Class that stores information about an image channel for registration
 *
 */
public class RegistrationChannelOptions<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> {

	private ImagePlusImg<T, A> imp;
	
	private float aData = 0.5f;
	private final float[] sigma = new float[] {3.0f, 3.0f, 0.1f};
	private float weight = 1.0f;
	private boolean isInplace = false;
	
	
	public boolean isInplace() {
		return isInplace;
	}

	public void setInplace(boolean isInplace) {
		this.isInplace = isInplace;
	}

	public float getAdata() {
		return aData;
	}

	public void setAdata(float aData) {
		this.aData = aData;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
		for (ActionListener l : actionListeners) {
			l.actionPerformed(new ActionEvent(this, 0, new String()));
		}
	}

	public float[] getSigma() {
		return sigma;
	}
	
	public RegistrationChannelOptions() {
		
	}
	
	public RegistrationChannelOptions(ImagePlusImg<T, A> imp) {
		this.imp = imp;
	}
	
	public RegistrationChannelOptions(ImagePlusImg<T, A> imp, float weight) {
		this.imp = imp;
		this.weight = weight;
	}

	public ImagePlusImg<T, A> getImg() {
		return imp;
	}

	public void setImg(ImagePlusImg<T, A> imp) {
		this.imp = imp;
	}
	
	private final List<ActionListener> actionListeners = new LinkedList<ActionListener>();
	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}
}
