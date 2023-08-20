public class Toy {

    public static void main(String[] args) {

        Box a = new Box();
        a.name = "A";
        Box b = new Box();
        b.name = "B";

        Machine machine = new Machine(new JobImpl(a, b) {
            @Override
            public void doJob() { // cartridge
                for (int i = 0; i < 10; i++) {
                    a.set(i);
                    b.set(i * 10);
                }

            }
        });
        Person person = new Person(new JobImpl(a, b) {

            @Override
            public void doJob() {
                Box.get();
                System.out.println("start");
                for (int i = 0; i < 10; i++) { // emulTransfer
                    System.out.print("emul A ");
                    a.setVal(i * -1);
                    System.out.print("emul B ");
                    b.setVal(i * -10);
                }
                System.out.println("end");
                Box.free();
            }
        });

        Thread t1 = new Thread(machine);
        Thread x = new Thread(person);

        t1.start();
        x.start();
    }

}

abstract class JobImpl implements Job { // SerialPort

    static final Object sharedLock = new Object(); // Any object's instance

    Box a, b;

    JobImpl(Box a, Box b) {
        this.a = a;
        this.b = b;
    }
}

class Machine implements Runnable { // Emulator A

    Job job;

    Machine(Job job) {
        this.job = job;
    }

    @Override
    public void run() { // cartridge code
        job.doJob();
    }
}

class Person implements Runnable { // Emulator B

    Job job;

    Person(Job job) {
        this.job = job;
    }

    @Override
    public void run() {
        job.doJob();
    }
}

interface Job {         // SerialEndPoint

    void doJob();           // transfer or setByte
}

class Box { // AddressSpace

    int val;
    String name;

    static private final Object obj = new Object();

    static volatile boolean beingUsed;

    static void get() {
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

    static synchronized void free() {
        synchronized (obj) {
            beingUsed = false;
            obj.notify();
        }
    }

    void set(int val) {
        get();
        System.out.print("      ");
        setVal(val);
        free();
    }

    void setVal(int val) {
        // cartridge가 어떻게 block 안 되고.. 누락 안 되고..
            System.out.printf("[%s val:%d]%n", name, val);
            this.val = val;
    }
}
