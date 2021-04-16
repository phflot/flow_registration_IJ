package floregistration.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import ij.ImageJ;

public class CompensationProgress extends JDialog implements PropertyChangeListener {

	private static final long serialVersionUID = 1123685531693361454L;
	private JProgressBar progressbar;
	private JTextArea output;
	private int nFrames;
	private int nProcessedFrames = 0;
	
	public static void main(String[] args) {
		ImageJ ij = new ImageJ();
		
		new CompensationProgress(ij, 123).setVisible(true);
	}
	
	public CompensationProgress(Frame parent, int nFrames) {
		super(parent);
		//setModal(false);
		
		setResizable(false);
		
		setBounds(100, 100, 220, 160);
		getContentPane().setLayout(new BorderLayout());
		
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		this.nFrames = nFrames;
		
		progressbar = new JProgressBar(0, nFrames);
		progressbar.setValue(0);
		progressbar.setStringPainted(true);
		
		output = new JTextArea(5, 15);
        output.setMargin(new Insets(5,5,5,5));
        output.setEditable(false);
        
        contentPanel.add(progressbar);
        contentPanel.add(output);
        output.append("Initializing motion compensation...");
	} 

	@Override
	public void propertyChange(PropertyChangeEvent event) {

		String[] out = new String[2];
	    for (int i = 0; i < 2; i++) {
	    	out[i] = "";
	    }
	    String[] out_current = output.getText().split("\n");
	    for (int i = 0; i < out_current.length && 
	    		i < out.length; i++) {
	    	out[i] = out_current[i];
	    }
		
		switch (event.getPropertyName()) {
		case "n_processed_frames":
			progressbar.setValue((int)event.getNewValue());
			break;
		case "frames_left":
			int frames_left = (int)event.getNewValue(); 
			out[0] = frames_left + " frames of " + nFrames + " left\n";
			nProcessedFrames = nFrames - frames_left;
			break;
		case "elapsed":
			double elapsed_time = (double)event.getNewValue();
			double time_left = (1.0d/((double)nProcessedFrames+0.01)) * elapsed_time * 
					(double)(nFrames - nProcessedFrames);
			out[1] = String.format ("%.2f", time_left) + "s left\n";
			break;
		case "final_time":
			String tmp = "Registered " + nFrames + " frames in " + 
					String.format("%.2f", (double)event.getNewValue()) + "s\n";
			out[0] = tmp;
			out[1] = "";
			break;
		}
		
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < out.length; i++) {
		   result.append(out[i]);
		   result.append("\n");
		}
		
		output.selectAll();
		output.replaceSelection("");
		output.append(result.toString());
	}

}
