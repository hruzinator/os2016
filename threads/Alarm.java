package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
    	Machine.timer().setInterruptHandler(new Runnable() {
    		public void run() { timerInterrupt(); }
    	});
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        interruptLock.acquire();
        boolean restoreState = Machine.interrupt().disable();
        long currentTime = Machine.timer().getTime();
        while(waitingThreads.size() > 0 && currentTime >= waitingThreads.get(0).getWakeTime()) {
            WaitingThread wakingUp = waitingThreads.poll();
            wakingUp.getContainingThread().ready();
        }

        Machine.interrupt().restore(restoreState);
        interruptLock.release();

        KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        boolean restoreState = Machine.interrupt().disable();
        long wakeTime = Machine.timer().getTime() + x;
        WaitingThread newElement = new WaitingThread(KThread.currentThread(), wakeTime);
        int i = 0;
        for(; i < waitingThreads.size() && wakeTime > waitingThreads.get(i).getWakeTime(); i++){
            //a tree would probably be more ideal, but whatever.
            //just advancing the the pointer to the right place
        }
        waitingThreads.add(i, newElement);
        KThread.currentThread().sleep();
        Machine.interrupt().restore(restoreState);
    }

        public static void selfTest() {

        KThread a = new KThread(new Runnable() {
            @Override
            public void run() {
                final int WAIT_TICKS = 500;
                System.out.println("Hello! I am thread A!");
                for(int x = 1; x <= 70; x++) {
                    System.out.println("Thread A counting " + x);
                    if(x == 10) {
                        System.out.println("***Thread A waiting at least " + WAIT_TICKS + " ticks");
                        ThreadedKernel.alarm.waitUntil(200);
                        System.out.println("Thread A returned from wait");
                    }
                    KThread.yield();
                }
            }
        });

        KThread b = new KThread(new Runnable() {
            @Override
            public void run() {
                final int WAIT_TICKS = 500;
                System.out.println("Hello! I am thread B!");
                for(int x = 1; x <= 70; x++) {
                    System.out.println("Thread B counting " + x);
                    if(x == 10) {
                        System.out.println("***Thread B waiting at least " + WAIT_TICKS + " ticks");
                        new Alarm().waitUntil(WAIT_TICKS);
                        System.out.println("Thread B returned from wait");
                    }
                    KThread.yield();
                }
            }
        });

        KThread c = new KThread(new Runnable() {
            @Override
            public void run() {
                System.out.println("I am thread C!");
                for(int x = 1; x <= 200; x++) {
                    System.out.println("Thread C counting " + x);
                    KThread.yield();
                }
            }
        });

        a.fork();
        b.fork();
        c.fork();
    }

    private static LinkedList<WaitingThread> waitingThreads = new LinkedList<WaitingThread>();
    private Lock interruptLock = new Lock();

    private class WaitingThread {

        private KThread containingThread = null;
        private long wakeTime;

        public WaitingThread(KThread containingThread, long wakeTime) {
            this.containingThread = containingThread;
            this.wakeTime = wakeTime;
        }

        public long getWakeTime() { return wakeTime; }
        public KThread getContainingThread() { return containingThread; }

    }
}
