import java.util.ListIterator;
import java.util.concurrent.Semaphore;

public class FlightAttendant extends Thread {
    public static long time = System.currentTimeMillis();
    private Semaphore signalBoarding;
    private Semaphore waitBoarding;
    private Semaphore signalDeboard;
    private Semaphore deboardComplete;
    private Semaphore announceBoarding;
    private int groupNum;
    private Semaphore zone1;
    private Semaphore zone2;
    private Semaphore zone3;
    private Semaphore planeZone1;
    private Semaphore planeZone2;
    private Semaphore planeZone3;

    public FlightAttendant(int groupNum, Semaphore signalBoarding, Semaphore announceBoarding,
                           Semaphore waitBoarding, Semaphore signalDeboard, Semaphore deboardComplete) {
        super("Flight Attendant");
        this.signalBoarding = signalBoarding;
        this.announceBoarding = announceBoarding;
        this.waitBoarding = waitBoarding;
        this.signalDeboard = signalDeboard;
        this.deboardComplete = deboardComplete;
        this.groupNum = groupNum;
        // passengers will board by zone
        zone1 = new Semaphore(0, true);
        zone2 = new Semaphore(0, true);
        zone3 = new Semaphore(0, true);
        planeZone1 = new Semaphore(0, true);
        planeZone2 = new Semaphore(0, true);
        planeZone3 = new Semaphore(0, true);
    }

    public void msg(String m) {
        System.out.println("["+(System.currentTimeMillis()-time)+"] "+getName()+": "+m);
    }

    public Semaphore getZone(int zone) {
        if(zone == 1) return zone1;
        if(zone == 2) return zone2;
        if(zone == 3) return zone3;
        else return null;
    }

    public Semaphore getPlaneZone(int zone) {
        if(zone == 1) return planeZone1;
        if(zone == 2) return planeZone2;
        if(zone == 3) return planeZone3;
        else return null;
    }

    public void run() {
        msg("standing by.");

        // wait for clock to signal boarding
        try {
            signalBoarding.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // boarding
        // signal passengers
        announceBoarding.release();

        msg("Now boarding zone 1.");
        // wait for zone passengers to arrive
        // set a time before we move on to boarding next zone
        long waitTime = System.currentTimeMillis();
        while(zone1.getQueueLength() < 10) {
            if(System.currentTimeMillis() == waitTime + 1000L) break;
        }
        // release 4 passengers to board at a time
        while(zone1.getQueueLength() > 0) {
            for(int i = 0; i < groupNum; i++) {
                zone1.release();
            }
        }

        msg("Now boarding zone 2.");
        // wait for zone passengers to arrive
        // set a time before we move on to boarding next zone
        while(zone2.getQueueLength() < 10) {
            if(System.currentTimeMillis() == waitTime + 1000L) break;
        }
        // release 4 passengers to board at a time
        while(zone2.getQueueLength() > 0) {
            for(int i = 0; i < groupNum; i++) {
                zone2.release();
            }
        }

        msg("Now boarding zone 3.");
        // wait for zone passengers to arrive
        // set a time before we move on to boarding next zone
        while(zone3.getQueueLength() < 10) {
            if(System.currentTimeMillis() == waitTime + 1000L) break;
        }
        // release 4 passengers to board at a time
        while(zone3.getQueueLength() > 0) {
            for(int i = 0; i < groupNum; i++) {
                zone3.release();
            }
        }

        // signal clock that boarding is complete
        // flight is now able to take off
        msg("Plane doors are now closed.");
        waitBoarding.release();

        // wait for clock to signal landing, time to de-board
        try {
            signalDeboard.acquire();
        }
        catch(InterruptedException e) {e.printStackTrace();}

        // signal passengers to deboard
        msg("Plane has landed.");
        msg("Now deboarding zone 1.");
        while(planeZone1.getQueueLength() > 0) {
            for(int i = 0; i < 10; i++) {
                planeZone1.release();
            }
        }

        msg("Now deboarding zone 2.");
        while(planeZone2.getQueueLength() > 0) {
            for(int i = 0; i < 10; i++) {
                planeZone2.release();
            }
        }

        msg("Now deboarding zone 3.");
        while(planeZone3.getQueueLength() > 0) {
            for(int i = 0; i < 10; i++) {
                planeZone3.release();
            }
        }

        // signal clock de-boarding complete
        // cleans and leaves airplane
        while(!(planeZone1.getQueueLength() == 10 && planeZone2.getQueueLength() == 10 && planeZone3.getQueueLength() == 10)) {}
        deboardComplete.release();
        msg("Exiting plane.");
    }

}
