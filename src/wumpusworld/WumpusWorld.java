package wumpusworld;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
/**
 * Starting class for the Wumpus World program. The program
 * has three options: 1) Run a GUI where the Wumpus World can be
 * solved step by step manually or by an agent, or 2) run
 * a simulation with random worlds over a number of games,
 * or 3) run a simulation over the worlds read from a map file.
 *
 * @author Johan Hagelbäck
 */
public class WumpusWorld {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        WumpusWorld ww = new WumpusWorld();
    }

    /**
     * Starts the program.
     *
     */
    public WumpusWorld()
    {
        String option = Config.getOption();

        if (option.equalsIgnoreCase("gui"))
        {
            showGUI();
        }
        if (option.equalsIgnoreCase("sim"))
        {
            runSimulator();
        }
        if (option.equalsIgnoreCase("simdb"))
        {
            runSimulatorDB();
        }
        if(option.equalsIgnoreCase("train"))
        {
            runTrainer();
        }
    }

    /**
     * Starts the program in GUI mode.
     */
    private void showGUI()
    {
        GUI g = new GUI();
    }

    /**
     * Starts the program in simulator mode with
     * maps read from a data file.
     */
    private void runSimulatorDB()
    {
        MapReader mr = new MapReader();
        Vector<WorldMap> maps = mr.readMaps();

        double totScore = 0;
        for (int i = 0; i < maps.size(); i++)
        {
            World w = maps.get(i).generateWorld();
            totScore += (double)runSimulation(w);
        }
        totScore = totScore / (double)maps.size();
        System.out.println("Average score: " + totScore);
    }

    /**
     * Starts the program in simulator mode
     * with random maps.
     */
    private void runSimulator()
    {
        int numSims = 1000;
        double totScore = 0;
        for (int i = 0; i < numSims; i++)
        {
            WorldMap w = MapGenerator.getRandomMap(i);
            totScore += (double)runSimulation(w.generateWorld());
        }
        totScore = totScore / (double)numSims;
        System.out.println("Average score: " + totScore);
    }

    /**
     * Runs the solver agent for the specified Wumpus
     * World.
     *
     * @param w Wumpus World
     * @return Achieved score
     */
    private int runSimulation(World w)
    {
        int actions = 0;
        Agent a = new MyAgent(w);
        while (!w.gameOver() && actions < 50)
        {
            a.doAction();
            actions++;
        }
        int score = w.getScore();
        System.out.println("Simulation ended after " + actions + " actions. Score " + score);
        return score;
    }

    ExecutorService executor = Executors.newFixedThreadPool(16);
    
    private void runTrainer()
    {
        MapReader mr = new MapReader();
        final Vector<WorldMap> maps = mr.readMaps();
        
        ArrayList<MyAgent> agents = new ArrayList<>();

        final Random rand = new Random();
        // generate good starting agents
        int numStartAgents = 10;
        int maxTries = 2000;
        int numSims = 100;
        while (agents.size() < numStartAgents && maxTries-- > 0)
        {
            MyAgent a = new MyAgent((World)null);
            for (int i = 0; i < numSims; i++)
            {
                WorldMap wm = MapGenerator.getRandomMap(rand.nextInt());
                a.w = wm.generateWorld();
                runTrainingSim(a);
            }
            a.avgScore /= (double)numSims;
            System.out.println("score: " + Double.toString(a.avgScore));
            if(a.avgScore > -1100.0 || true)
            {
                agents.add(a);
                System.out.println("Adding agent with score: " + Double.toString(a.avgScore));
            }
            if (maxTries % 100 == 0)
                System.out.println("Tries left: " + Integer.toString(maxTries));
        }
        Collections.sort(agents);
        System.out.println("Starting with:");
        for (MyAgent a : agents)
        {
            System.out.print((int)a.avgScore);
            System.out.print(", ");
        }
        System.out.println();


        ArrayList<MyAgent> best = new ArrayList<>();
        for(int i = 0; i < 10; i++)
        {
            best.add(agents.get(i));
        }
        agents.clear();


        int numGeneration = 4000;
        for (int i = 0; i < numGeneration; i++)
        {
            Collections.sort(best);
            int numBest = 3;
            if(best.size() > numBest)
                best.subList(numBest, best.size()).clear();
            for(int j = 0; j < best.size(); j++)
            {
                for(int k = 0; k < 2; k++)
                {
                    agents.add(new MyAgent(best.get(j)));
                }
            }
            


            System.out.println("All time best:");
            for(MyAgent a : best)
            {
                System.out.print(a.avgScore);
                System.out.print(", ");
            }
            System.out.println();

            
            Vector<FutureTask> tasks = new Vector<>();

            for (MyAgent a : agents)
            {
                tasks.add(new FutureTask(new Callable() {
                    MyAgent a;
                    @Override
                    public Object call() throws Exception {
                        int numMutations = 10;
                        for(int j = 0; j < numMutations; j++)
                        {
                            int layer = rand.nextInt(a.weights.size());
                            int weightIndex = rand.nextInt(a.weights.get(layer).size());
                            double current = a.weights.get(layer).get(weightIndex);
                            double change = (rand.nextInt(10) - 5) / 100;
                            change = 0.25*(rand.nextDouble()*2.0-1.0);
                            //a.weights.get(layer).set(weightIndex, current + change);
                            a.weights.get(layer).set(weightIndex, rand.nextDouble()*2.0 - 1);

                            layer = rand.nextInt(a.biases.size());
                            int biasIndex = rand.nextInt(a.biases.get(layer).size());
                            current = a.biases.get(layer).get(biasIndex);
                            //change = (rand.nextInt(10) - 5) / 100;
                            change = 0.25*(rand.nextDouble()*2.0-1.0);
                            //a.biases.get(layer).set(biasIndex, current+change);
                            a.biases.get(layer).set(biasIndex, rand.nextDouble()*2.0 - 1);
                        }
                        int numSims = maps.size();
                        for (int i = 0; i < numSims; i++)
                        {
                            //WorldMap w = MapGenerator.getRandomMap(rand.nextInt());
                            //a.w = w.generateWorld();
                            a.w = maps.get(i).generateWorld();
                            runTrainingSim(a);
                        }
                        a.avgScore /= (double)numSims;
                        return null;
                    }
                    private Callable init(MyAgent a)
                    {
                        this.a = a;
                        return this;
                    }
                }.init(a)));
                executor.execute(tasks.lastElement());
            }
            
            try {
                for(FutureTask t : tasks)
                {
                    while(!t.isDone())
                    {
                        Thread.sleep(50L);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(WumpusWorld.class.getName()).log(Level.SEVERE, null, ex);
            }

            Collections.sort(agents);
            System.out.println("Best scores generation " + Integer.toString(i) + ":");
            for(int j = 0; j < numBest; j++)
            {
                MyAgent a = agents.get(j);
                best.add(a);
                System.out.print(a.avgScore);
                System.out.print(", ");
            }
            System.out.println();
            agents.clear();
        }
        executor.shutdown();
        try {
            best.get(0).saveNetwork(numGeneration);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WumpusWorld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private void runTrainingSim(MyAgent agent)
    {
        int actions = 0;
        while(!agent.w.gameOver() && actions < 50)
        {
            agent.doAction();
            actions++;
        }
        double score = (double)agent.w.getScore();
        if(actions==100 || !agent.w.hasGold())
        {
            score -= 100000;
        }
        if(agent.w.hasGold())
            score += 10000;
        agent.avgScore += score;
    }
}
