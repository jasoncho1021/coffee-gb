package eu.rekawek.coffeegb.controller;


import eu.rekawek.coffeegb.AddressSpace;
import eu.rekawek.coffeegb.cpu.InterruptManager;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Joypad implements AddressSpace {
    private static final Logger LOG = LoggerFactory.getLogger(Joypad.class);

    private Set<ButtonListener.Button> buttons = new HashSet<>();

	private int p1;


    public Joypad(InterruptManager interruptManager, Controller controller) {
        controller.setButtonListener(new ButtonListener() {
            @Override
            public void onButtonPress(Button button) {
                interruptManager.requestInterrupt(InterruptManager.InterruptType.P10_13);
                buttons.add(button);
                LOG.info("pressed {}",button);
            }

			@Override
			public void onButtonRelease(Button button) {
				buttons.remove(button);
			}
		});
	}

	@Override
	public boolean accepts(int address) {
		return address == 0xff00;
	}

	@Override
	public void setByte(int address, int value) {
		p1 = value & 0b00110000;					// set col
		//System.out.println("KEY SET BYTE: " + Integer.toHexString(value) + " "+ Integer.toHexString(p1));
	}

    @Override
    public int getByte(int address) {
        int result = p1 | 0b11001111;
        for (ButtonListener.Button b : buttons) {
            if ((b.getLine() & p1) == 0) {
                LOG.info("Joypad {}",b);
                result &= 0xff & ~b.getMask();
            }
        }
        return result;
    }
}
