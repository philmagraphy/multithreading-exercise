import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class KioskClerk extends Thread {
    public static long time = System.currentTimeMillis();
    private Semaphore generalKioskWaiting; // the general line feeds each clerk's individual line
    private Semaphore clerkReady; // semaphore to signal whether clerk is ready to process next in it's line
    private Semaphore passengerReady;
    private Semaphore passProcessed;
    private Semaphore line;
    private Semaphore waitBoarding;
    private int passengersProcessed;
    private Semaphore clerkMutex;
    private int numPassengers;
    private Passenger currentCustomer;
    private ArrayList<Integer> seatNumbers;

    public KioskClerk(int num, ArrayList<Integer> seatNumbers, Semaphore generalKioskWaiting, Semaphore clerkReady, Semaphore passengerReady,
                      Semaphore passProcessed, Semaphore line, Semaphore waitBoarding, int passengersProcessed, Semaphore clerkMutex, int numPassengers) {
        super("Clerk-" + num);
        this.seatNumbers = seatNumbers;
        this.generalKioskWaiting = generalKioskWaiting;
        this.clerkReady = clerkReady;
        this.passengerReady = passengerReady;
        this.passProcessed = passProcessed;
        this.line = line; // default line capacity of 3
        this.waitBoarding = waitBoarding;
        this.passengersProcessed = passengersProcessed;
        this.clerkMutex = clerkMutex;
        this.numPassengers = numPassengers;
    }

    public void msg(String m) {
        System.out.println("["+(System.currentTimeMillis()-time)+"] "+getName()+": "+m);
    }

    public Semaphore getClerkReady() {
        return clerkReady;
    }

    public Semaphore getPassengerReady() {
        return passengerReady;
    }

    public Semaphore getPassProcessed() {
        return passProcessed;
    }

    public Semaphore getLine() {
        return line;
    }

    public void setCurrentCustomer(Passenger currentCustomer) {
        this.currentCustomer = currentCustomer;
    }

    // checks if there's an opening in this clerk's line
    private void generateBoardingPass() {
        int seatNumber = seatNumbers.remove(0);
        int zone = ((seatNumber-1)/10) + 1;
        currentCustomer.updateBoardingPass(seatNumber, zone);
        msg(currentCustomer.getName() + " received boarding pass. Seat " + seatNumber + ", Zone " + zone + ".");
    }

    public void run() {
        msg(" waiting for passengers.");
        while(passengersProcessed < numPassengers || waitBoarding.getQueueLength() > 0) {
            // wait for passenger
            try {
                passengerReady.acquire();
            }
            catch(InterruptedException e) {e.printStackTrace();}

            // generate boarding pass
            try {
                clerkMutex.acquire();
                try {
                    generateBoardingPass();
                    passengersProcessed++;
                }
                finally {
                    clerkMutex.release();
                }
            }
            catch(InterruptedException e) {e.printStackTrace();}
            // let passenger proceed to boarding
            // signal next passenger in line
            passProcessed.release();
            clerkReady.release();
        }
        // no more passengers to process or the plane has departed, ok to terminate
        msg("terminating.");
    }
}
