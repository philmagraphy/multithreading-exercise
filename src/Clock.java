import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Semaphore;

public class Clock extends Thread {
    public static long time = System.currentTimeMillis();
    // default values, overridden if parameterized constructor used.
    private int numKioskClerks = 2;
    private int numPassengers = 30;
    private long timeUntilBoarding = 500;
    private long flightDuration = 500;
    private int groupNum = 4;
    private Semaphore waitOnClock;

    public Clock(Semaphore waitOnClock) {
        super("Clock");
        this.waitOnClock = waitOnClock;
    }

    public Clock(Semaphore waitOnClock, int numPassengers) {
        super("Clock");
        this.waitOnClock = waitOnClock;
        this.numPassengers = numPassengers;
    }

    public void msg(String m) {
        System.out.println("["+(System.currentTimeMillis()-time)+"] "+getName()+": "+m);
    }

    public void run() {
        // generating seat numbers for boarding passes
        ArrayList<Integer> seatNumbers = new ArrayList<>();
        for(int i = 1; i <= 30; i++) { // the flight only has 30 seats
            seatNumbers.add(i);
        }
        Collections.shuffle(seatNumbers);

        // blocking semaphore for the general line at check-in
        // counterNum = 3 in the specs is assumed to refer to the capacity of each clerk's line
        // passengers will wait in a general area until one of the lines has a free spot
        Semaphore generalKioskWaiting = new Semaphore(0, false); // false to simulate randomness.

        // mutex for clerks modifying counter passengersProcessed
        Semaphore clerkMutex = new Semaphore(1, true);
        int passengersProcessed = 0;

        // multiple binary semaphores between the clock and flight attendant are used here for more granular control on the steps
        // blocking semaphore to signal flight attendant that it's time to board
        Semaphore signalBoarding = new Semaphore(0, true);

        // blocking semaphore for flight attendant to signal passengers to line up in zones
        Semaphore announceBoarding = new Semaphore(0, true);

        // blocking semaphore to wait for flight attendant to complete boarding
        Semaphore waitBoarding = new Semaphore(0, true);

        // blocking semaphore to signal flight attendant to de-board
        Semaphore signalDeboard = new Semaphore(0, true);

        // blocking semaphore to wait for flight attendant to finish de-boarding before exiting.
        Semaphore deboardComplete = new Semaphore(0, true);

        // instantiate clerk threads
        // instantiate clerk array and semaphore array for the passengers to get divided between clerk lines
        // clerkReady - semaphore that represents whether clerk is ready to process next in their individual line
        // passengerReady - blocking semaphore that represents whether a passenger in the clerk's specific line is ready
        // passProcessed - blocking semaphore that represents whether a passenger has their boarding pass and can head to boarding
        KioskClerk[] clerks = new KioskClerk[numKioskClerks];
        Semaphore[] clerkLines = new Semaphore[numKioskClerks];
        for(int i = 1; i <= numKioskClerks; i++) {
            Semaphore clerkReady = new Semaphore(1, true); // specific to the clerk thread
            Semaphore passengerReady = new Semaphore(0, true); // specific to the clerk thread
            Semaphore passProcessed = new Semaphore(0, true); // specific to the clerk thread
            Semaphore line = new Semaphore(3, true); // default line capacity of 3, specific to the clerk thread
            KioskClerk kc = new KioskClerk(i, seatNumbers, generalKioskWaiting, clerkReady, passengerReady, passProcessed,
                                            line, waitBoarding, passengersProcessed, clerkMutex, numPassengers);
            clerkLines[i-1] = line;
            clerks[i-1] = kc;
            kc.start();
            for(int j = 1; j <= 3; j++) { // clerk's line is ready.
                generalKioskWaiting.release();
            }
        }

        // instantiate flight attendant thread
        FlightAttendant fa = new FlightAttendant(groupNum, signalBoarding, announceBoarding, waitBoarding, signalDeboard, deboardComplete);
        fa.start();

        // instantiate passenger threads
        for(int i = 1; i <= numPassengers; i++) {
            Passenger tempPassenger = new Passenger(i, generalKioskWaiting, clerks, announceBoarding, fa);
            tempPassenger.start();
        }

        // sleep until its time to board
        try {
            sleep(timeUntilBoarding);
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // signal flight attendant that it is time to board
        signalBoarding.release();

        // wait for flight attendant to board plane
        try {
            waitBoarding.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // boarding complete, plane is taking off
        // sleep for duration of the flight
        try {
            sleep(flightDuration);
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // flight landed.
        // signal flight attendant to de-board
        signalDeboard.release();

        // wait for flight attendant to signal it's finished (after it de-boards the plane).
        try {
            deboardComplete.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}
        msg("terminating.");

        // notify main that we are done.
        waitOnClock.release();
    }
}
