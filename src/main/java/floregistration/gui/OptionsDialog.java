package floregistration.gui;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

import bdv.ui.rangeslider.RangeSlider;

import floregistration.algorithm.MotionCompensationWorker;
import floregistration.algorithm.RegistrationChannelOptions;
import floregistration.algorithm.RegistrationJob;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.MessageDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.io.Opener;
import ij.io.SaveDialog;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;

/**
 * Main Dialog to select the solver and image parameters
 * @param parent
 * @param images
 */
public class OptionsDialog extends NonBlockingGenericDialog {
	
	private static final long serialVersionUID = -1425554761163870030L;
	private List<ImagePlusImg> images;
	private int registrationQualitySetting = 1;
	private int nChannels = 0;
	private RegistrationJob registrationJob = new RegistrationJob();
	private final TextField strideField;
	private int stride = 1;
	private final double[] registerTarget = new double[] {0, 1.0f};
	private final RangeSlider registerTargetSlider;
	

	public OptionsDialog(final Frame parent, List<ImagePlusImg> images) {
		super("Flow Registration");
		this.images = images;
		
		setResizable(false);
		
		setOKLabel("Start");
				
		Panel channel_panel = new Panel();
		Button add_channel = new Button("add channel");
		GridBagConstraints c = new GridBagConstraints();
		channel_panel.setLayout(this.getLayout());
		addPanel(channel_panel);

		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;
		channel_panel.add(add_channel, c);
		
		List<Label> labels = new LinkedList<Label>();
		List<JSpinner> weights = new LinkedList<JSpinner>();
		
		add_channel.addActionListener(new ActionListener() {
			@SuppressWarnings("rawtypes")
			@Override
			public void actionPerformed(ActionEvent e) {
				AddNewChannelDialog add_new_channel = new AddNewChannelDialog(
						"channel " + ++nChannels, parent, images);
				add_new_channel.showDialog();
				if (add_new_channel.wasOKed()) {

					OptionsDialog this_tmp = OptionsDialog.this;
					
					if (!registrationJob.add(add_new_channel.getRegistrationChannelOptions())) {
						new MessageDialog(parent, "Error adding channel", 
								"Numer of Frames and dimensions in all sequences need to be identical!");
						return;
					}
					
					String channel_name = add_new_channel.getRegistrationChannelOptions().getImg().getImagePlus().getTitle();
					if (channel_name.length() > 15)
						channel_name = "..." + channel_name.substring(channel_name.length()-15);
					labels.add(new Label(channel_name));
					SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.5f, 0.0f, 10.001f, 0.1);					
					JSpinner spinner = new JSpinner(spinnerModel);
					
					// get the spinner text field and make it fire for manual input:
					JFormattedTextField spinnerText = (JFormattedTextField) spinner.getEditor().getComponent(0);
				    ((DefaultFormatter) spinnerText.getFormatter()).setCommitsOnValidEdit(true);
					
					add_new_channel.getRegistrationChannelOptions().addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							spinnerModel.setValue(
									(double)add_new_channel.getRegistrationChannelOptions().getWeight());
						}
					});
					
					spinner.addChangeListener(new ChangeListener() {						
						@Override
						public void stateChanged(ChangeEvent arg0) {
							add_new_channel.getRegistrationChannelOptions().setWeight(
									spinnerModel.getNumber().floatValue());
						}
					});
					
					weights.add(spinner);
					
					int old_height = channel_panel.getHeight();
					
					channel_panel.removeAll();
					
					c.gridx = 0;
					c.gridy = 0;
					Iterator<JSpinner> weightIterator = weights.iterator();
					for (Label l : labels) {
						c.gridx = 0;
						channel_panel.add(l, c);
						c.gridx = 1;
						channel_panel.add(weightIterator.next(), c);

						c.gridy++;
					}
					c.gridy++;
					c.gridx = 0;
					channel_panel.add(add_channel, c);
					
					this_tmp.validate();
					this_tmp.setSize(this_tmp.getWidth(),
							this_tmp.getHeight() + channel_panel.getHeight() - old_height);
					this_tmp.validate();
										
					float weight = 1.0f/(float)registrationJob.size();
					for (RegistrationChannelOptions op : registrationJob) {
						op.setWeight(weight);
					}
					
					this_tmp.doLayout();
					this_tmp.validate();
				}
			}
		});
		
		addCheckbox("Symmetric smoothness weight", true);
		final Checkbox symSmoothBox = (Checkbox)super.getCheckboxes().lastElement();
		// Selection of alpha x:
		addSlider("Smoothness x", 0.1, 5, registrationJob.getAlpha()[0]);
		final TextField alphaXField = ((TextField)super.getNumericFields().lastElement());
		registrationJob.getAlpha()[0] = Float.parseFloat(alphaXField.getText());

		
		// Selection of alpha y:
		addSlider("Smoothness y", 0.1, 5, registrationJob.getAlpha()[1]);
		final TextField alphaYField = ((TextField)super.getNumericFields().lastElement());
		final Scrollbar alphaYSlider = (Scrollbar)getSliders().lastElement();
		alphaYField.setEnabled(false);
		alphaYSlider.setEnabled(false);
		
		registrationJob.getAlpha()[1] = Float.parseFloat(alphaYField.getText());
		
		
		alphaXField.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationJob.getAlpha()[0] = Float.parseFloat(alphaXField.getText());
				if (symSmoothBox.getState()) {
					alphaYField.setText(alphaXField.getText());
				}
			}
		});
		alphaYField.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationJob.getAlpha()[1] = Float.parseFloat(alphaYField.getText());
			}
		});
		
		symSmoothBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (symSmoothBox.getState()) {
					alphaYField.setEnabled(false);
					alphaYSlider.setEnabled(false);
				} else {
					alphaYField.setEnabled(true);
					alphaYSlider.setEnabled(true);
				}
			}
		});
		
		String[] speed = new String[] {"fast", "balanced", "quality"};
		addChoice("Registration quality", speed, speed[registrationQualitySetting]);
		Choice qualityChoice = (Choice)super.getChoices().lastElement();
		qualityChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationQualitySetting = qualityChoice.getSelectedIndex();
			}
		});
		
		Button referenceButton = new Button("select reference");
		Panel referenceButtonPanel = new Panel();
		referenceButtonPanel.setLayout(this.getLayout());
		addPanel(referenceButtonPanel);
		referenceButtonPanel.add(referenceButton);
		referenceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (registrationJob.isEmpty()) {
					new MessageDialog(IJ.getInstance(), "Error selecting reference", 
							"At least one channel needs to be selected before reference selection is possible!");
					return;
				}
				
				ReferenceDialog referenceDialog = new ReferenceDialog(
						"Define reference frames", parent, registrationJob);
				referenceDialog.showDialog();
				if (referenceDialog.wasOKed()) {

				}
			}
			
		});
		
		// Panel that contains the scroll pane for the registered frames
		Panel registerTargetPanel = new Panel();
		registerTargetPanel.setLayout(this.getLayout());
		addPanel(registerTargetPanel);
		registerTargetSlider = new RangeSlider(0, 999);
		registerTargetSlider.setValue(0);
		registerTargetSlider.setUpperValue(999);
		registerTargetPanel.add(new JLabel("Registration Target Frames:"));
		Panel rangeSliderPanel = new Panel();
		rangeSliderPanel.add(registerTargetSlider);
		addPanel(rangeSliderPanel);
		
		registerTargetSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				
				int lower = registerTargetSlider.getValue();
				int upper = registerTargetSlider.getUpperValue();
				
				if (lower == upper) {
					lower -= lower == registerTargetSlider.getMinimum() ? 0 : 1;
					upper += lower == registerTargetSlider.getMinimum() ? 1 : 0;
					
					registerTargetSlider.setValue(lower);
					registerTargetSlider.setUpperValue(upper);
				}
				
				registerTarget[0] = (double)lower / (double)(registerTargetSlider.getMaximum() - registerTargetSlider.getMinimum());
				registerTarget[1] = (double)upper / (double)(registerTargetSlider.getMaximum() - registerTargetSlider.getMinimum());
				
				/*
				int upper = registerTargetSlider.getUpperValue();
				lower = lower < 0 ? 0 : lower;
				
				if (registrationJob.getNslices() < 0) {
					registerTarget[0] = lower;
					registerTarget[1] = upper;
				}
				
				
				if (upper >= registrationJob.getNslices() && registrationJob.getNslices() > 0) { 
					upper = registrationJob.getNslices() - 1;
				}
				registerTarget[0] = lower;
				registerTarget[1] = upper;*/
			}
		});
		
		// Selection of stride
		addSlider("Stride: ", 1, 10, stride);
		strideField = ((TextField)super.getNumericFields().lastElement());
		strideField.addTextListener(new TextListener() {	
			@Override
			public void textValueChanged(TextEvent e) {
				int newStride = Integer.parseInt(strideField.getText());
				if (newStride < 1) {
					newStride = 1;
					strideField.setText("1");
				}
				if (newStride >= registrationJob.getNslices() && registrationJob.getNslices() > 0) {
					newStride = registrationJob.getNslices() - 1;
					strideField.setText(Integer.toString(registrationJob.getNslices()));
				}
				stride = newStride;
			}
		});

		JLabel projectLink = new JLabel("<html><hr>This plugin is based on a publication,<br />please visit our <a href='https://www.snnu.uni-saarland.de/flow-registration/'>project website</a><br /></html>");

		Panel projectLabelPanel = new Panel();
		projectLabelPanel.setLayout(this.getLayout());
		addPanel(projectLabelPanel);

		projectLabelPanel.add(projectLink);

		projectLink.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				try {

					Desktop.getDesktop().browse(new URI("https://www.snnu.uni-saarland.de/flow-registration/"));

				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		});
		
	}
	
	public synchronized void actionPerformed(final ActionEvent event) {
		super.actionPerformed(event);
		
		if (registrationJob.isEmpty() && this.wasOKed()) {
			new MessageDialog((Frame) this.getParent(), 
					"Need to select at least one channel", 
					"Need to select at least one channel!");
			return;
		}
		
		if (this.wasOKed()) {
			parseOptions();
			
			YesNoCancelDialog tmp_dialog = new YesNoCancelDialog(IJ.getInstance(), "Export options", "Export JSON options?");
			if (tmp_dialog.yesPressed()) {
				SaveDialog saveDialog = new SaveDialog("Save options", "options.json", ".json");
				registrationJob.saveOptions(saveDialog.getDirectory(), saveDialog.getFileName());
			}
			
			CompensationProgress progress_dialog = new CompensationProgress(IJ.getInstance(),
					registrationJob.getNRegistrationTargets());
			MotionCompensationWorker engine = 
					new MotionCompensationWorker(registrationJob);
			engine.addPropertyChangeListener(progress_dialog);
			progress_dialog.setVisible(true);
			engine.execute();
		}
	}
	
	private void parseOptions() {
		switch (registrationQualitySetting) {
			case 0:
				registrationJob.setIterations(50);
				//registrationJob.setEta(0.5f);
				//registrationJob.setLevels(20);
				registrationJob.setMinLevel(6);
				break;
			case 1:
				registrationJob.setIterations(50);
				registrationJob.setMinLevel(4);
				break;
			case 2:
				registrationJob.setIterations(50);
				//registrationJob.setEta(0.8f);
				registrationJob.setMinLevel(0);
				break;
		}
		
		int newStride = Integer.parseInt(strideField.getText());
		registrationJob.setRange(this.registerTarget[0], this.registerTarget[1]);
		registrationJob.setStride(newStride);
	}
}
