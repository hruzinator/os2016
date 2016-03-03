package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        communicatorLock = new Lock();
        listeningCondition = new Condition(communicatorLock);
        speakingCondition = new Condition(communicatorLock);
        mailboxCondition = new Condition(communicatorLock);
        hasSpeakerInMailbox = false;
        hasListenerInMailbox = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word) {
        communicatorLock.acquire();
        if(hasSpeakerInMailbox) { //someone is already waiting to speak
            speakingCondition.sleep();
        }
        Lib.assertTrue(!hasSpeakerInMailbox);
        hasSpeakerInMailbox = true;
        this.word = word;
        
        if(!hasListenerInMailbox) { //no active listeners
            listeningCondition.wake(); //we could just have sleeping listeners
            mailboxCondition.sleep();
            //we then speak, leave condition block, etc.
        }
        else{ //there is an active listener, and since we can guarantee atomicity, we need to wake them up!
            mailboxCondition.wake();
        }
        
        hasSpeakerInMailbox = false;
        speakingCondition.wake();
        communicatorLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */    
    public int listen() {
        communicatorLock.acquire();
        if(hasListenerInMailbox) { //someone is already waiting to listen
            listeningCondition.sleep();
        }
        Lib.assertTrue(!hasListenerInMailbox);
        hasListenerInMailbox = true;
        
        if(!hasSpeakerInMailbox) { //no active speakers
            speakingCondition.wake(); //we could just have sleeping speakers
            mailboxCondition.sleep(); //continues execution once there is a speaker
        }
        else{ //there is an active speaker, and since we can guarantee atomicity, we need to wake them up!
            mailboxCondition.wake();
        }
        int wordTransfer = this.word;
        
        hasListenerInMailbox = false;
        listeningCondition.wake();
        communicatorLock.release();
        
        return wordTransfer;
    }

    public static void selfTest() {

        Communicator testCommunicator = new Communicator();
        
        pListener = new KThread(new ListenTester(testCommunicator));
        pListener.fork();
        qListener = new KThread(new ListenTester(testCommunicator));
        qListener.fork();
        rListener = new KThread(new ListenTester(testCommunicator));
        rListener.fork();

        pSpeaker = new KThread(new SpeakTester(testCommunicator, 1));
        pSpeaker.fork();
        qSpeaker = new KThread(new SpeakTester(testCommunicator, 2));
        qSpeaker.fork();
        rSpeaker = new KThread(new SpeakTester(testCommunicator, 3));
        rSpeaker.fork();
        

        KThread.yield();
    }

    private Condition listeningCondition = null;
    private Condition speakingCondition = null;
    private Condition mailboxCondition = null;
    private Lock communicatorLock = null;
    int word;
    private boolean hasMessage;
    private boolean hasListenerInMailbox;
    private boolean hasSpeakerInMailbox;

    //begin test variables
    private static KThread pSpeaker = null;
    private static KThread qSpeaker = null;
    private static KThread rSpeaker = null;
    private static KThread pListener = null;
    private static KThread qListener = null;
    private static KThread rListener = null;
    //end test variables
}

class SpeakTester implements Runnable {

    public SpeakTester(Communicator conversation, int word) {
        this.conversation = conversation;
        this.word = word;
    }

    public void run() {
        System.out.println("Speaker prepared to speak the word: " + word);
        conversation.speak(word);
        System.out.println("Speaker has spoken the word: " + word);
    }

    public Communicator getConversation() {return conversation; }

    private Communicator conversation = null;
    private int word;
}

class ListenTester implements Runnable {

    ListenTester(Communicator conversation) {
        this.conversation = conversation;
    }

    public void run() {
        System.out.println("Beginning a listener");
        int theSpokenWord = conversation.listen();
        System.out.println("Listener recieved the word: " + theSpokenWord);
    }

    public Communicator getConversation() {return conversation;}

    private Communicator conversation = null;
}