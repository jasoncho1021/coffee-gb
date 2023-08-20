package eu.rekawek.coffeegb.gui;

import eu.rekawek.coffeegb.serial.SerialEndpoint;

public class SystemOutSerialEndpoint implements SerialEndpoint {

    @Override
    public int transfer(int b) {
        //System.out.print((char) b);
        System.out.println( Integer.toString(b, 2));

        /*  send  & recv */

        return 0; //return recv
    }

}
