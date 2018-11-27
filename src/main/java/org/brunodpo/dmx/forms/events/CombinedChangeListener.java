package org.brunodpo.dmx.forms.events;

import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class makes two different components to have the same value.
 * <p>
 * v.1.0.0 - Initial release
 * </p>
 * @author Bruno Di Prinzio de Oliveira
 * @version 1.0.0
 * @since 2018-05-01
 */
public class CombinedChangeListener implements ChangeListener {

	private JSlider slider;
	private JSpinner spinner;

	public CombinedChangeListener(JSlider slider, JSpinner spinner) {
		this.slider = slider;
		this.spinner = spinner;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (slider.getValue() == (int)spinner.getValue())
			return;
		if (e.getSource() instanceof JSlider)
			spinner.setValue(slider.getValue());
		if (e.getSource() instanceof JSpinner)
			slider.setValue((int)spinner.getValue());
	}
}
