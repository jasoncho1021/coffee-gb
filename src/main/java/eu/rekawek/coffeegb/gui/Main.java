package eu.rekawek.coffeegb.gui;

import eu.rekawek.coffeegb.debug.Console;
import eu.rekawek.coffeegb.serial.SerialEndpoint;
import eu.rekawek.coffeegb.serial.SerialPort;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Main {

    static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    static SerialEndpoint onePoint;
    static SerialEndpoint twoPoint;

    static class Box {

        static int cnt;
        int idx;

        {
            idx = cnt;
            ++cnt;
        }

        SerialPort serialPort;
        SerialPort my;

        int transfer(int outgoing) {
            //SerialPort.get();
            LOGGER.info("master transfer start");
            int ingoing = serialPort.getByte(0xff01);
            my.setByte(0xff01, ingoing);
            my.setByte(0xff02, 0x01);
            my.interrupt();
            LOGGER.info("master transfer end");

            LOGGER.info("slave transfer start");
            serialPort.setByte(0xff01, outgoing);
            serialPort.setByte(0xff02, 0x00);
            serialPort.interrupt();
            LOGGER.info("slave transfer end");
            //SerialPort.free();

            /*
            jobQueue.add(() -> {
                serialPort.setByte(0xff01, outgoing);
                serialPort.setByte(0xff02, 0x00);
                serialPort.interrupt();
                LOGGER.info("slave transfer end");
                //}).start();
            });
             */

            //return ingoing;
            return 0;
        }
    }

    public static JobQueue jobQueue;

    public static void main(String[] args) throws Exception {

        //  LOGGER.info("This is an INFO level log message!");
        //   LOGGER.error("This is an ERROR level log message!");

        jobQueue = JobQueue.getInstace();

        Box oneBox = new Box();
        Box twoBox = new Box();
        onePoint = oneBox::transfer;
        twoPoint = twoBox::transfer;

        //Emulator one = load(args, SerialEndpoint.NULL_ENDPOINT);
        //Emulator two = load(args, SerialEndpoint.NULL_ENDPOINT);
        Emulator one = load(args, onePoint, oneBox.idx);
        Emulator two = load(args, twoPoint, twoBox.idx);//outgoing -> outgoing);

        oneBox.serialPort = two.getSerialPort();
        oneBox.my = one.getSerialPort();
        twoBox.serialPort = one.getSerialPort();
        twoBox.my = two.getSerialPort();

        IntBox intBox = new IntBox();
        new Thread(() -> {
            while (true) {
                intBox.clear();
                jobQueue.get(intBox);
                if (intBox.item != null) {
                    //System.out.println(Thread.currentThread().getId());
                    intBox.item.callBack();
                }
            }
        }).start();

        one.run();
        two.run();

    }

    private static Emulator load(String[] args, SerialEndpoint serialEndpoint, int idx) throws Exception {
        System.setProperty("apple.awt.application.name", "Coffee GB");
        return new Emulator(args, loadProperties(), serialEndpoint, idx);
    }

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        File propFile = new File(new File(System.getProperty("user.home")), ".coffeegb.properties");
        if (propFile.exists()) {
            try (FileReader reader = new FileReader(propFile)) {
                props.load(reader);
            }
        }
        return props;
    }

}
