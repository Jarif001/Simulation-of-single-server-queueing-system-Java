import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        float meanInterArrival = 0, meanService = 0;
        int numDelaysRequired = 0;

        // Read the input file
        String filePath = "input.txt";
        File inputFile = new File(filePath);
        Scanner scanner;
        try{
            scanner = new Scanner(inputFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] theNums = line.split(" ");
                meanInterArrival = Float.parseFloat(theNums[0]);
                meanService = Float.parseFloat(theNums[1]);
                numDelaysRequired = Integer.parseInt(theNums[2]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Initialization
        int qLimit = 100;
        int numberOfEvents = 2;
        File logFile = new File("log.txt");
        QueueSystem system = new QueueSystem(meanInterArrival, meanService, numDelaysRequired, numberOfEvents, qLimit, logFile);

        // Run the simulation until the stopping condition is met
//        int itn = 0;
        System.out.println("Simulation starts");
        while(system.numberOfCustomersDelayed < system.numDelaysRequired){
//            System.out.println("Iteration - " + ++itn);
            // Generate the next event
            boolean willContinue = system.timing();
            if(!willContinue){ // event list empty -> simulation ends
                //write to report and the simulation ends
                System.out.println("Timing function --> event list empty. Simulation ends");
                break;
            }
            // update stats
            system.updateTimeAvgStats();
            // Process the event
            if(system.nextEventType == 1){
                willContinue = system.arrive();
                if(!willContinue){ // queue is overflown
                    //write to report and simulation ends
                    System.out.println("Arrive function --> queue is overflown. simulation ends");
                    break;
                }
            }
            else if(system.nextEventType == 2){
                system.depart();
            }
        }

        // after finishing the simulation, generate report
        System.out.println("Simulation ends. Writing report!");
        File reportFile = new File("report.txt");
        system.report(reportFile);

    }

}