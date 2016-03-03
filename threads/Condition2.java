package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	   this.conditionLock = conditionLock;
       this.waitQueue = new LinkedList<KThread>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        conditionLock.release();
        boolean restoreState = Machine.interrupt().disable();
        
        waitQueue.add(KThread.currentThread());
        KThread.currentThread().sleep();
        
        Machine.interrupt().setStatus(restoreState);
        conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	   Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        boolean restoreStatus = Machine.interrupt().disable();
        
        if(!waitQueue.isEmpty()) {
            waitQueue.poll().ready();
        }
        Machine.interrupt().restore(restoreStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean restoreState = Machine.interrupt().disable();

        while(!waitQueue.isEmpty()) {
            waitQueue.poll().ready();
        }
        
        Machine.interrupt().restore(restoreState);
    }

    public static void selfTest() {
        
        testLock = new Lock();
        cTest = new Condition2(testLock);
        
        p = new KThread(new Runnable() {
            @Override
            public void run() {
                testLock.acquire();
                System.out.println("Now Begining thread P.");
                System.out.println("sleeping P on condition variable");
                cTest.sleep();
                System.out.println("returned to thread p");
                testLock.release();
            }
        });
        
        q = new KThread(new Runnable() {
            @Override
            public void run() {
                testLock.acquire();
                System.out.println("Now Beginning thread Q.");
                for(int x = 1; x <= 5; x++)
                    System.out.println("Q counting " + x);
                System.out.println("Now waking all...");
                cTest.wakeAll();
                testLock.release();
            }
        });
        
        r = new KThread(new Runnable() {
            @Override
            public void run() {
                testLock.acquire();
                System.out.println("Now starting Thread R. Sleeping Thread R");
                q.fork();
                cTest.sleep();
                System.out.println("returned to thread r");
                testLock.release();
            }
        });
        
        s = new KThread(new Runnable() {
            @Override
            public void run() {
                testLock.acquire();
                System.out.println("Now starting Thread S. Sleeping Thread S");
                cTest.sleep();
                System.out.println("returned to thread s");
                testLock.release();
            }
        });
        
        p.fork();
        s.fork();
        r.fork();
    }

    private Lock conditionLock;
    private LinkedList<KThread> waitQueue = null;

    //begin test fields
    private static KThread p = null;
    private static KThread q = null;
    private static KThread r = null;
    private static KThread s = null;
    private static Condition2 cTest = null;
    private static Lock testLock = null;
    //end test fields
}
