package eu.rekawek.coffeegb.serial;

import eu.rekawek.coffeegb.AddressSpace;
import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.cpu.InterruptManager;
import eu.rekawek.coffeegb.cpu.SpeedMode;
import eu.rekawek.coffeegb.gui.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SerialPort implements AddressSpace {

    private static final Logger LOG = LoggerFactory.getLogger(SerialPort.class);

    private final SerialEndpoint serialEndpoint;

    private final InterruptManager interruptManager;

    private final SpeedMode speedMode;

    private int sb;

    private int sc;

    private boolean transferInProgress;

    private int divider;

    public SerialPort(InterruptManager interruptManager, SerialEndpoint serialEndpoint, SpeedMode speedMode) {
        this.interruptManager = interruptManager;
        this.serialEndpoint = serialEndpoint;
        this.speedMode = speedMode;
    }

    public void tick() {
        if (!transferInProgress) {
            return;
        }
        /*
            internal clock : 8192Hz == 122 microseconds
            GameBoy : 4194304Hz

            4194304Hz : 8192Hz = ? : 1
            cpu에서 tick 512 번 돌아야 internal clock 한 번 도는 것과 같구나

            sc=0x81 --> wait 512 cycle

         */
        if (++divider >= Gameboy.TICKS_PER_SEC / 8192 / speedMode.getSpeedMode()) { // 8192hz 기준으로 현재 cpu hz 사용하기 위함
            transferInProgress = false;
            try {
                /*sb = serialEndpoint.transfer(sb); // 1byte, // should be 8 clock tick
                sc = 0x01;*/

                /*setByte(0xff01, serialEndpoint.transfer(sb));
                setByte(0xff02, 0x01);*/

                serialEndpoint.transfer(sb);

                //System.out.printf("--A_%s_ = sb:%s \n", Thread.currentThread().getId(), Integer.toString(sb, 16));
            } catch (IOException e) {
                //   LOG.error("Can't transfer byte", e);
                sb = 0;
            }
            /*
            interrupt(); // 0x01
            LOG.info("master transfer end");
             */
        }
    }

    public void interrupt() {
        interruptManager.requestInterrupt(InterruptManager.InterruptType.Serial);
    }

    @Override
    public boolean accepts(int address) {
        return address == 0xff01 || address == 0xff02;
    }

    @Override
    public void setByte(int address, int value) {
        get();
        set(address, value);
        free();
    }

    public void set(int address, int value) {
        if (address == 0xff01) {
            LOG.info("SerialPort {} sb setByte:{}", this.hashCode(), value);
            sb = value;
        } else if (address == 0xff02) {
            LOG.info("SerialPort {} sc setByte:{}", this.hashCode(), value);
            sc = value;

            if (sc == 0x81) { // master
                //if ((sc & 0x81) != 0) {
                startTransfer();
            }
        }
    }

    @Override
    public int getByte(int address) {
        get();
        int val = get(address);
        free();
        return val;
    }

    public int get(int address) {
        if (address == 0xff01) {
            LOG.info("SerialPort {} sb getByte:{}", this.hashCode(), sb);
            return sb;
        } else if (address == 0xff02) {
            LOG.info("SerialPort {} sc getByte:{}", this.hashCode(), sc);
            //return sc;
            return sc | 0b01111110;         // bgb debugger 찍어보니 이게 맞음.
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void startTransfer() {
        transferInProgress = true;
        divider = 0;
    }

    static private final Object obj = new Object();

    static volatile boolean beingUsed;

    public static void get() {
        synchronized (obj) {
            while (beingUsed) {
                try {
                    obj.wait(); // this 못 쓰는 와중에 wait을 쓰기 위해 일단 obj 두긴했는데.. 이게 맞나..
                } catch (Exception ignored) {
                }
            }
            beingUsed = true;
        }
    }

    public static synchronized void free() {
        synchronized (obj) {
            beingUsed = false;
            obj.notify();
        }
    }

}
