import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class QueueSystem {

    public float meanInterArrival, meanService;
    public int numDelaysRequired;
    public int q_limit;
    public String serverStatus;
    public int numberOfEvents;
    public int nextEventType, numberOfCustomersDelayed, numberInQ;
    public float areaNumInQ, areaServerStatus, simTime, timeLastEvent, totalDelays;
    public float[] timeArrival; // the queue
    public float[] timeNextEvent;

    // Log file
    int numberOfCustomersCame;
    int numberOfCustomersDeparted;
    int eventCount;
    File logFile;
    BufferedWriter logWriter;

    public QueueSystem(float meanInArrival, float meanService, int numDelaysRequired, int numberOfEvents, int q_limit, File logFile) {
        this.meanInterArrival = meanInArrival;
        this.meanService = meanService;
        this.numDelaysRequired = numDelaysRequired;
        this.numberOfEvents = numberOfEvents;
        this.q_limit = q_limit;
        timeArrival = new float[q_limit + 1];// it is the queue. Plus one for the extra person that come at last
        this.logFile = logFile;
        // Simulation clock
        simTime = 0;
        // State vars
        serverStatus = "idle";
        numberInQ = 0;
        timeLastEvent = 0;
        // Stats counter
        numberOfCustomersDelayed = 0;
        totalDelays = 0;
        areaNumInQ = 0;
        areaServerStatus = 0;
        // event list
        timeNextEvent = new float[numberOfEvents + 1]; // 1 -> arrival, 2 -> departure
        timeNextEvent[1] = simTime + exponential(meanInterArrival);
        timeNextEvent[2] = Float.MAX_VALUE; // so that the first event is an arrival and not a departure
        nextEventType = 0;
        //log
        numberOfCustomersCame = 0;
        numberOfCustomersDeparted = 0;
        eventCount = 0;
        try {
            logWriter = new BufferedWriter(new FileWriter(logFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean timing() { // returns false if the simulation ends
        float minTime = Float.MAX_VALUE;
        // determine the event that occurs in the nearest time
        for(int i = 1; i <= numberOfEvents; i++){
            if(timeNextEvent[i] < minTime){
                minTime = timeNextEvent[i];
                nextEventType = i; // 1 for arrival, 2 for departure
            }
        }
        if(nextEventType == 0){// next event is nothing (not updated)
            // There is no event to occur -> end the simulation
            return false;
        }
        // simulation continues
        simTime = minTime; // simulation clock is taken to the event time
        return true;
    }

    public boolean arrive() { // returns false if the simulation ends
        float delay;

        //log file
        eventCount++;
        numberOfCustomersCame++;
        try {
            logWriter.write(eventCount + ". Next event: Customer " + numberOfCustomersCame + " Arrival\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // generate next arrival time
        timeNextEvent[1] = simTime + exponential(meanInterArrival);
        // If the server is busy then to Q. Otherwise to service
        if(serverStatus.equalsIgnoreCase("busy")){ // server is busy
            numberInQ++;
            if(numberInQ > q_limit){ // overflow the queue capacity. Stop the simulation
                return false;
            }
            timeArrival[numberInQ] = simTime; // He came in current time. So in queue it has to be in order
        }
        else{// server is idle
            // there is no delay here
            delay = 0;
            totalDelays += delay;
            numberOfCustomersDelayed++; //using as total customers served
            try {
                logWriter.write("\n");
                logWriter.write("---------No. of customers delayed: " + numberOfCustomersDelayed + "---------\n");
                logWriter.write("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            serverStatus = "busy";
            timeNextEvent[2] = simTime + exponential(meanService); // departure time of the person arrived
        }
        return true;
    }

    public void depart() {
        float delay;

        // log file
        eventCount++;
        numberOfCustomersDeparted++;
        try {
            logWriter.write(eventCount + ". Next event: Customer " + numberOfCustomersDeparted + " Departure\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(numberInQ == 0){ // none is waiting in the queue. So none is there to take service. make the server idle
            serverStatus = "idle";
            timeNextEvent[2] = Float.MAX_VALUE; // next event wont be a departure
        }
        else{ // There is at least one waiting in the queue. Send the 1st person to service
            numberInQ--;
            // calculate the delay
            delay = simTime - timeArrival[1]; // takes the 1st person from the queue for service
            totalDelays += delay;
            numberOfCustomersDelayed++;
            try {
                logWriter.write("\n");
                logWriter.write("---------No. of customers delayed: " + numberOfCustomersDelayed + "---------\n");
                logWriter.write("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            timeNextEvent[2] = simTime + exponential(meanService); // departure time for the person who entered into service

            // Rearrange the queue (now 2nd becomes the 1st, 3rd becomes 2nd .....)
            for(int i = 1; i <= numberInQ; i++){
                timeArrival[i] = timeArrival[i+1];
            }
        }
    }

    public float exponential(float meanInter) {
        return (float) (-meanInter * Math.log(Math.random()));
    }

    public void report(File file) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            float avgDelayInQ = totalDelays / numberOfCustomersDelayed;
            float avgNumInQ = areaNumInQ / simTime;
            float avgServerStatus = areaServerStatus / simTime;

            writer.write("Single-server queueing system\n");
            // input info
            String s1 = String.format("%-25s %10.3f minutes\n", "Mean interarrival time", meanInterArrival);
            String s2 = String.format("%-25s %10.3f minutes\n", "Mean service time", meanService);
            String s3 = String.format("%-25s %10d\n", "Number of customers", numDelaysRequired);

            writer.write(s1);
            writer.write(s2);
            writer.write(s3);

            // performance metrics
            String row1 = String.format("%-25s %10.3f minutes\n", "Average delay in queue", avgDelayInQ);
            String row2 = String.format("%-25s %10.3f\n", "Average number in queue", avgNumInQ);
            String row3 = String.format("%-25s %10.3f\n", "Server utilization", avgServerStatus);

            writer.write("\n");
            writer.write(row1);
            writer.write(row2);
            writer.write(row3);

            String lastRow = String.format("%-25s %10.3f minutes\n", "Time simulation ended", simTime);
            writer.write(lastRow);

            writer.flush();
            System.out.println("Writing successful");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTimeAvgStats() {
        float timeSinceLastEvent = simTime - timeLastEvent;
        timeLastEvent = simTime; // simulation clock is set to the last event time
        areaNumInQ += numberInQ*timeSinceLastEvent;

        if(serverStatus.equalsIgnoreCase("busy")){
            areaServerStatus += 1.0*timeSinceLastEvent;
        }
    }

}
