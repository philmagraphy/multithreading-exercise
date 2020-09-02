import java.util.Random;
import java.util.concurrent.Semaphore;

public class Passenger extends Thread {
    public static long time = System.currentTimeMillis();
    private Random seed = new Random();
    private Semaphore generalKioskWaiting;
    private KioskClerk[] clerks;
    private KioskClerk myClerk;
    private Semaphore line; // specific to a clerk thread
    private Semaphore clerkReady; // specific to a clerk thread
    private Semaphore passengerReady; // specific to a clerk thread
    private Semaphore passProcessed; // specific to a clerk thread
    private Semaphore announceBoarding;
    private FlightAttendant fa;
    private int seatNumber;
    private int zone;
    private Semaphore boardingZone;
    private Semaphore planeZone;

    public Passenger(int num, Semaphore generalKioskWaiting, KioskClerk[] clerks, Semaphore announceBoarding, FlightAttendant fa) {
        super("Passenger-" + num);
        this.generalKioskWaiting = generalKioskWaiting;
        this.announceBoarding = announceBoarding;
        this.clerks = clerks;
        this.fa = fa;
    }

    public void msg(String m) {
        System.out.println("["+(System.currentTimeMillis()-time)+"] "+getName()+": "+m);
    }

    public void updateBoardingPass(int seatNumber, int zone) {
        this.seatNumber = seatNumber;
        this.zone = zone;
    }

    public void run() {
        msg("arrived at the airport.");

        // wait in the general kiosk line
        // proceed if one of the clerk lines has room
        try {
            generalKioskWaiting.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // determine the line with room
        // if both have room, randomly decide on which line to queue in
        if(clerks[0].getLine().getQueueLength() < 3 && clerks[1].getLine().getQueueLength() < 3) {
            boolean pickLine = seed.nextBoolean();
            int numLine = pickLine ? 1 : 0;
            myClerk = clerks[numLine];
            line = myClerk.getLine();
            clerkReady = myClerk.getClerkReady();
            passengerReady = myClerk.getPassengerReady();
            passProcessed = myClerk.getPassProcessed();
            try {
                line.acquire();
            }
            catch(InterruptedException e) {e.printStackTrace();}
        }

        // otherwise join the line with room
        else if(clerks[0].getLine().getQueueLength() < 3) {
            myClerk = clerks[0];
            line = myClerk.getLine();
            clerkReady = myClerk.getClerkReady();
            passengerReady = myClerk.getPassengerReady();
            passProcessed = myClerk.getPassProcessed();
            try {
                line.acquire();
            }
            catch(InterruptedException e) {e.printStackTrace();}
        }

        else {
            myClerk = clerks[1];
            line = myClerk.getLine();
            clerkReady = myClerk.getClerkReady();
            passengerReady = myClerk.getPassengerReady();
            passProcessed = myClerk.getPassProcessed();
            try {
                line.acquire();
            }
            catch(InterruptedException e) {e.printStackTrace();}
        }

        // wait for the clerk to be ready
        try {
            clerkReady.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // signal clerk that you're ready
        // get boarding pass
        myClerk.setCurrentCustomer(this);
        passengerReady.release();

        // wait for clerk to finish making boarding pass
        try {
            passProcessed.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // proceed to boarding area
        try {
            announceBoarding.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // gather with fellow zone passengers
        boardingZone = fa.getZone(zone);
        try {
            boardingZone.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // board
        // sleep until flight over
        planeZone = fa.getPlaneZone(zone);
        try {
            planeZone.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // deboarding
        msg("Seat " + seatNumber + " deboarding.");
    }
}
