import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Semaphore;

public class Main {

    public static void main(String [] args) {
        // blocking semaphore to wait for clock to finish before exiting.
        Semaphore waitOnClock = new Semaphore(0, true);

        // the clock thread instantiates all other threads
        Clock c;
        if(args.length == 1) {
            int numPassengers = Integer.parseInt(args[0]);
            c = new Clock(waitOnClock, numPassengers);
        }
        else {
            c = new Clock(waitOnClock);
        }
        c.start();

        // wait for clock thread to signal that it's done before exiting.
        try {
            waitOnClock.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}
    }
}
