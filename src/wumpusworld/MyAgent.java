package wumpusworld;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains starting code for creating your own Wumpus World agent. Currently
 * the agent only make a random decision each turn.
 *
 * @author Johan Hagelbäck
 */
public class MyAgent implements Agent, Comparable
{

    World w = null;

    double avgScore = 0;

    Random rand = new Random();

    int px = 1, py = 1;

    int turns = 0;
    // layers[0] => input layer
    // layers[0][0] => value of first node of input layer
    // weights[0][0] => first weight from first node in input layer to first node in second layers
    // weights[0][1] => second weight from first node in input layer to second node in second layers

    // layer[j][i] => node i of layer j
    // weights[j][i + n*size] => n:th node left side to i:th node right side from layer j to j+1, size is number of nodes in right side layer
    // biases[j][i] => biases in i:th node in layer j+1
    ArrayList<ArrayList<Double>> layers = new ArrayList<ArrayList<Double>>();
    ArrayList<ArrayList<Double>> weights = new ArrayList<ArrayList<Double>>();
    ArrayList<ArrayList<Double>> biases = new ArrayList<ArrayList<Double>>();

    // for backpropagation 
    ArrayList<ArrayList<Double>> zs = new ArrayList<ArrayList<Double>>();

    // player map  -> 16
    // visible map -> 16
    // breeze map  -> 16
    // pit map     -> 16
    // stench map  -> 16
    static public int numInputs = 16 + 16 + 16 + 16 + 16;
    // target map -> 16
    // shoot dir -> 4
    static public int numOutputs = 16 + 4;
    //layout of NN. First value is size of input layer, last is output layer. Arbitrary number of hidden layers and their sizes.
    int[] layerSizes =
    {
        numInputs, numInputs, numOutputs
    };

    boolean shouldLoadNetwork = true;
    int generationToLoad = 548599;
    double trainingSpeed = 0.01;

    /**
     * Creates a new instance of your solver agent.
     *
     * @param world Current world state
     */
    public MyAgent(World world)
    {
        w = world;

        //initialize layer activation values
        for (int i = 0; i < layerSizes.length; i++)
        {
            layers.add(new ArrayList<Double>());
            for (int j = 0; j < layerSizes[i]; j++)
            {
                layers.get(i).add(0.0);
            }
        }

        for (int i = 0; i < layerSizes.length - 1; i++)
        {
            zs.add(new ArrayList<Double>());
            for (int j = 0; j < layerSizes[i + 1]; j++)
            {
                zs.get(i).add(0.0);
            }
        }

        //initialize weights
        for (int i = 0; i < layers.size() - 1; i++)
        {
            weights.add(new ArrayList<Double>());
            for (int j = 0; j < layers.get(i).size() * layers.get(i + 1).size(); j++)
            {
                //randomly generated weights for each connection between left side and right side
                weights.get(i).add(rand.nextDouble() * 2.0 - 1);
                //System.out.println("weight[ " + i + "][" + j + "] = " + weights.get(i).get(j).toString());
            }
        }

        //initialize biases
        for (int i = 0; i < layers.size() - 1; i++)
        {
            biases.add(new ArrayList<Double>());
            for (int j = 0; j < layers.get(i + 1).size(); j++)
            {
                //randomly generated bias for each node in in each layer
                biases.get(i).add(rand.nextDouble() * 2.0 - 1);
                //biases.get(i).add(0.0);
                //System.out.println("biases[ " + i + "][" + j + "] = " + biases.get(i).get(j).toString());
            }
        }
        if (shouldLoadNetwork)
        {
            try
            {
                this.loadNetwork(generationToLoad);
            } catch (IOException ex)
            {
                Logger.getLogger(MyAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public MyAgent(MyAgent agent)
    {
        layerSizes = agent.layerSizes.clone();
        for (ArrayList<Double> arr : agent.biases)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            biases.add(cpy);
            for (Double d : arr)
            {
                cpy.add(d);
            }
        }

        for (ArrayList<Double> arr : agent.weights)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            weights.add(cpy);
            for (Double d : arr)
            {
                cpy.add(d);
            }
        }

        for (ArrayList<Double> arr : agent.layers)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            layers.add(cpy);
            for (Double d : arr)
            {
                cpy.add(d);
            }
        }

    }

    @Override
    public int compareTo(Object o)
    {
        if (o instanceof MyAgent)
        {
            MyAgent a = (MyAgent) o;
            if (a.avgScore > avgScore)
            {
                return 1;
            } else if (a.avgScore == avgScore)
            {
                return 0;
            } else
            {
                return -1;
            }
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void saveNetwork(int generation) throws FileNotFoundException
    {
        try (PrintWriter out = new PrintWriter("gen_" + generation + ".txt"))
        {
            //save network layout
            out.println(layerSizes.length);

            for (int i = 0; i < layerSizes.length; i++)
            {
                out.println(layerSizes[i]);
            }
            //save weights
            for (int i = 0; i < layers.size() - 1; i++)
            {
                for (int j = 0; j < layers.get(i).size() * layers.get(i + 1).size(); j++)
                {
                    out.println(weights.get(i).get(j));
                }
            }
            //save biases
            for (int i = 0; i < layers.size() - 1; i++)
            {
                for (int j = 0; j < layers.get(i + 1).size(); j++)
                {
                    out.println(biases.get(i).get(j));
                }
            }
        }
    }

    public void loadNetwork(int generation) throws FileNotFoundException, IOException
    {
        try (BufferedReader br = new BufferedReader(new FileReader("gen_" + generation + ".txt")))
        {
            String line = br.readLine();
            int numLayers = Integer.parseInt(line);

            //read network layout
            int[] templs = new int[numLayers];
            for (int i = 0; i < numLayers; i++)
            {
                line = br.readLine();
                templs[i] = Integer.parseInt(line);
            }
            layerSizes = templs;

            layers = new ArrayList<>();
            weights = new ArrayList<>();
            biases = new ArrayList<>();

            zs = new ArrayList<>();

            for (int i = 0; i < layerSizes.length; i++)
            {
                layers.add(new ArrayList<Double>());
                for (int j = 0; j < layerSizes[i]; j++)
                {
                    layers.get(i).add(0.0);
                }
            }
            for (int i = 0; i < layerSizes.length - 1; i++)
            {
                zs.add(new ArrayList<Double>());
                for (int j = 0; j < layerSizes[i + 1]; j++)
                {
                    zs.get(i).add(0.0);
                }
            }

            for (int i = 0; i < layers.size() - 1; i++)
            {
                weights.add(new ArrayList<Double>());
                for (int j = 0; j < layers.get(i).size() * layers.get(i + 1).size(); j++)
                {
                    line = br.readLine();
                    weights.get(i).add(Double.parseDouble(line));
                    //System.out.println("weight[ " + i + "][" + j + "] = " + weights.get(i).get(j).toString());
                }
            }

            for (int i = 0; i < layers.size() - 1; i++)
            {
                biases.add(new ArrayList<Double>());
                for (int j = 0; j < layers.get(i + 1).size(); j++)
                {
                    line = br.readLine();
                    biases.get(i).add(Double.parseDouble(line));
                    //System.out.println("biases[ " + i + "][" + j + "] = " + biases.get(i).get(j).toString());
                }
            }
        }
    }

    public void setInputs()
    {
        // player map  -> 16
        // visible map -> 16
        // breeze map  -> 16
        // pit map     -> 16
        // stench map  -> 16

        for(int i = 0; i < layers.get(0).size(); i++)
            layers.get(0).set(i, 0.0);
        
        int px = w.getPlayerX()-1;
        int py = w.getPlayerY()-1;
        
        layers.get(0).set(px+py*4, 1.0);
        
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            
            // visible
            if (w.isVisited(x + 1, y + 1))
                layers.get(0).set(16+i, 1.0);

            // breeze
            if (w.hasBreeze(x + 1, y + 1))
                layers.get(0).set(16+16 + i, 1.0);

            // pit
            if (w.hasPit(x + 1, y + 1))
                layers.get(0).set(16+16+16+ i, 1.0);
            
            // stench
            if (w.hasStench(x + 1, y + 1))
                layers.get(0).set(16+16+16+16+ i, 1.0);
        }
    }

    double ReLU(double x)
    {
        return Math.max(x, 0.0);
    }

    double ReLUPrime(double x)
    {
        double res = 1.0;
        if (x < 0)
        {
            res = 0;
        }
        return res;
    }

    double leakyReLU(double x)
    {
        if (x < 0)
        {
            x *= 0.01;
        }
        return x;
    }

    double leakyReLUPrime(double x)
    {
        double res = 1.0;
        if (x < 0)
        {
            res = 0.01;
        }
        return res;
    }

    double sigmoid(double x)
    {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    double sigmoidPrime(double x)
    {
        double sig = sigmoid(x);
        return sig * (1.0 - sig);
    }

    public void feedforward()
    {
        // for each layer
        for (int i = 0; i < layers.size() - 1; i++)
        {
            for (int k = 0; k < layers.get(i + 1).size(); k++)
            {
                layers.get(i + 1).set(k, 0.0);
            }
            // for each node in current layer
            for (int j = 0; j < layers.get(i).size(); j++)
            {
                // for each node in next layer
                for (int k = 0; k < layers.get(i + 1).size(); k++)
                {
                    //acummulate weighted values
                    Double weight = weights.get(i).get(k + j * layers.get(i + 1).size());
                    Double prev = layers.get(i + 1).get(k);
                    Double val = prev + weight * layers.get(i).get(j);
                    layers.get(i + 1).set(k, val);
                }
            }

            for (int k = 0; k < layers.get(i + 1).size(); k++)
            {
                Double activation = layers.get(i + 1).get(k);
                Double bias = biases.get(i).get(k);
                double val = 0.0;
                //val = ReLU(activation + bias);
                //val = leakyReLU(activation + bias);
                val = sigmoid(activation + bias);
                layers.get(i + 1).set(k, val);
            }
        }
    }

    ArrayList<Double> costDerivative(ArrayList<Double> activations, ArrayList<Double> y)
    {
        ArrayList<Double> result = new ArrayList<>();
        for (int i = 0; i < y.size(); i++)
        {
            result.add(activations.get(i) - y.get(i));
        }
        return result;
    }

    ArrayList<Double> activationDerivative(ArrayList<Double> z)
    {
        ArrayList<Double> result = new ArrayList<>();
        for (int i = 0; i < z.size(); i++)
        {
            double val;
            //val = leakyReLUPrime(z.get(i));
            //val = ReLUPrime(z.get(i));
            val = sigmoidPrime(z.get(i));
            result.add(val);
        }
        return result;
    }

    void backpropagate(double[][] inputs, double[][] outputs, int numData)
    {
        ArrayList<ArrayList<Double>> dw = new ArrayList<>();
        ArrayList<ArrayList<Double>> db = new ArrayList<>();

        for (ArrayList<Double> arr : weights)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            dw.add(cpy);
            for (Double d : arr)
                cpy.add(0.0);
        }
        for (ArrayList<Double> arr : biases)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            db.add(cpy);
            for (Double d : arr)
                cpy.add(0.0);
        }

        for (int e = 0; e < numData; e++)
        {
            for (int i = 0; i < numInputs; i++)
            {
                layers.get(0).set(i, inputs[e][i]);
            }
            ArrayList<Double> y = new ArrayList<>();
            for (int i = 0; i < numOutputs; i++)
            {
                y.add(outputs[e][i]);
            }

            feedforward();

            ArrayList<Double> cd = costDerivative(layers.get(layers.size() - 1), y);
            ArrayList<Double> actiD = activationDerivative(layers.get(layers.size() - 1));
            ArrayList<Double> delta = new ArrayList<>();
            for (int i = 0; i < cd.size(); i++)
            {
                delta.add(cd.get(i) * actiD.get(i));
            }
            for (int i = dw.size() - 1; i >= 0; i--)
            {
                ArrayList<Double> currDw = dw.get(i);
                for (int j = 0; j < currDw.size(); j++)
                {
                    int rightNode = j % delta.size();
                    int leftNode = j / delta.size();
                    double left = layers.get(i).get(leftNode);
                    double right = delta.get(rightNode);
                    double weight = left * right;
                    currDw.set(j, weight + currDw.get(j));
                }
                ArrayList<Double> currDb = db.get(i);
                for (int j = 0; j < currDb.size(); j++)
                {
                    double bias = delta.get(j);
                    currDb.set(j, bias + currDb.get(j));
                }
                if (i > 0)
                {
                    actiD = activationDerivative(layers.get(i));
                    ArrayList<Double> nextDelta = new ArrayList<>();
                    for (int j = 0; j < layerSizes[i]; j++)
                    {
                        double val = 0.0;
                        for (int k = 0; k < delta.size(); k++)
                        {
                            double d = delta.get(k);
                            double w = weights.get(i).get(k + j * delta.size());
                            val += d * w;
                        }
                        val *= actiD.get(j);
                        nextDelta.add(val);
                    }
                    delta = nextDelta;
                }
            }
        }

        for (int i = 0; i < dw.size(); i++)
        {
            for (int j = 0; j < dw.get(i).size(); j++)
            {
                double curr = weights.get(i).get(j);
                double dweight = dw.get(i).get(j);
                double result = curr - trainingSpeed * dweight / (double) numData;
                weights.get(i).set(j, result);
            }
        }

        for (int i = 0; i < db.size(); i++)
        {
            for (int j = 0; j < db.get(i).size(); j++)
            {
                double curr = biases.get(i).get(j);
                double dbias = db.get(i).get(j);
                double result = curr - trainingSpeed * dbias / (double) numData;
                biases.get(i).set(j, result);
            }
        }

    }

    int actionIndex()
    {
        int index = -1;
        Double largest = -1.0;
        for (int i = 0; i < layers.get(layers.size() - 1).size(); i++)
        {
            Double val = layers.get(layers.size() - 1).get(i);
            //System.out.print("[" + Integer.toString(i)+ "] = " + Double.toString(val) + ", ");
            if (val > largest)
            {
                largest = val;
                index = i;
            }
            //System.out.println(val);
        }
        return index;
    }

    /**
     * Asks your solver agent to execute an action.
     */
    @Override
    public void doAction()
    {
        //layers[1][i] = layers[0][1] * weights[0][i + 1*layers[1].length]

        turns++;

        //Location of the player
        int cx = w.getPlayerX();
        int cy = w.getPlayerY();

        //Basic action:
        //Grab Gold if we can.
        if (w.hasGlitter(cx, cy))
        {
            w.doAction(World.A_GRAB);
            return;
        }
        //Basic action:
        //We are in a pit. Climb up.
        if (w.isInPit())
        {
            w.doAction(World.A_CLIMB);
            return;
        }

        int index = -1;

        setInputs();
        feedforward();

        index = actionIndex();

        //System.out.println("Index " + Integer.toString(index));
        if (index < 16)
        {
            // walk dir
            int dx = 0;
            int dy = 0;
            int target = index;
            int ox = 1;
            int oy = 0;
            int minSteps = Integer.MAX_VALUE;
            for (int i = 0; i < 4; i++)
            {
                int x = cx - 1 + ox;
                int y = cy - 1 + oy;
                if (w.isValidPosition(x + 1, y + 1))
                {
                    if (x + y * 4 == target)
                    {
                        //System.out.println("TARGET");
                        dx = ox;
                        dy = oy;
                        break;
                    }
                    //if(tiles[x+y*4].visible)
                    if (w.isVisited(x + 1, y + 1))
                    {
                        int steps = search(x, y, target, true);
                        if (steps < minSteps)
                        {
                            dx = ox;
                            dy = oy;
                            minSteps = steps;
                        }
                    }
                }
                // 90 degree rotate offset 
                int temp = ox;
                ox = -oy;
                oy = temp;
            }
            if (dx == 0 && dy == 0)
            {
                for (int i = 0; i < 4; i++)
                {
                    int x = cx - 1 + ox;
                    int y = cy - 1 + oy;
                    if (w.isValidPosition(x + 1, y + 1))
                    {
                        if (x + y * 4 == target)
                        {
                            //System.out.println("TARGET");
                            dx = ox;
                            dy = oy;
                            break;
                        }
                        //if(tiles[x+y*4].visible)
                        if (w.isVisited(x + 1, y + 1))
                        {
                            int steps = search(x, y, target, false);
                            if (steps < minSteps)
                            {
                                dx = ox;
                                dy = oy;
                                minSteps = steps;
                            }
                        }
                    }
                    // 90 degree rotate offset 
                    int temp = ox;
                    ox = -oy;
                    oy = temp;
                }
            }

            if (dx == 1)
            {
                goEast();
            }
            if (dx == -1)
            {
                goWest();
            }
            if (dy == 1)
            {
                goNorth();
            }
            if (dy == -1)
            {
                goSouth();
            }
        } else
        {
            switch (index - 16)
            {
            case 0:
                shootNorth();
                break;
            case 1:
                shootEast();
                break;
            case 2:
                shootSouth();
                break;
            case 3:
                shootWest();
                break;
            }
        }

        px = cx;
        py = cy;
    }

    private int search(int sx, int sy, int target, boolean avoidPit)
    {
        int cx = w.getPlayerX() - 1;
        int cy = w.getPlayerY() - 1;

        // x = i%4; y = i/4;
        ArrayList<Integer> open = new ArrayList<>();
        ArrayList<Integer> openSteps = new ArrayList<>();
        ArrayList<Integer> closed = new ArrayList<>();

        open.add(sx + sy * 4);
        openSteps.add(1);
        closed.add(cx + cy * 4);

        if (avoidPit && w.hasPit(sx + 1, sy + 1))
        {
            return Integer.MAX_VALUE;
        }

        while (!open.isEmpty())
        {
            int steps = openSteps.get(0);
            openSteps.remove(0);

            int pos = open.get(0);
            open.remove(0);

            closed.add(pos);
            int nx = pos % 4;
            int ny = pos / 4;
            int ox = 1;
            int oy = 0;
            for (int i = 0; i <= 4; i++)
            {
                int x = nx + ox;
                int y = ny + oy;
                if (w.isValidPosition(x + 1, y + 1))
                {
                    int index = x + y * 4;
                    if (index == target)
                    {
                        return steps + 1;
                    }
                    if (!open.contains(index) && !closed.contains(index) && w.isVisited(x + 1, y + 1) && (!w.hasPit(x + 1, y + 1) || !avoidPit))
                    {
                        open.add(index);
                        openSteps.add(steps + 1);
                    }
                }
                // rotate offset by 90 degrees
                int temp = ox;
                ox = -oy;
                oy = temp;
            }
        }
        return Integer.MAX_VALUE;
    }

    private void goNorth()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
    }

    private void goSouth()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_MOVE);
            return;
        }
    }

    private void goEast()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
    }

    private void goWest()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_MOVE);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_MOVE);
            return;
        }
    }

    private void shootNorth()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
    }

    private void shootSouth()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_SHOOT);
            return;
        }
    }

    private void shootEast()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
    }

    private void shootWest()
    {
        if (w.getDirection() == World.DIR_RIGHT)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_UP)
        {
            w.doAction(World.A_TURN_LEFT);
            w.doAction(World.A_SHOOT);
            return;
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            w.doAction(World.A_TURN_RIGHT);
            w.doAction(World.A_SHOOT);
            return;
        }
    }

    /**
     * Genertes a random instruction for the Agent.
     */
    public int decideRandomMove()
    {
        return (int) (Math.random() * 4);
    }
}
