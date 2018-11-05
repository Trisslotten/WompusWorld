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
 * Contains starting code for creating your own Wumpus World agent.
 * Currently the agent only make a random decision each turn.
 *
 * @author Johan Hagelbäck
 */
public class MyAgent implements Agent, Comparable
{
    World w = null;

    
    double avgScore = 0;

    Random rand  = new Random();

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

    // stench -> binary
    // breeze -> binary
    // pos -> 4*4 = 16
    // prevPos -> (dx,dy)
    // canShoot -> binary
    // nearPit -> binary
    // breezeMap -> 4*4 = 16 (-1 no breeze, 0 unknown, 1 breeze)
    // stenchMap -> 4*4 = 16 (-1 no stench, 0 unknown, 1 stench)
    // valid walk dir -> 4
    // time -> 1
    int numInputs = 1 + 1 + 16 + 2 + 1 + 1 + 16 + 16 + 4 + 1;
    // walk dir -> 4 (north, east, south, west)
    // shoot dir -> 4 (north, east, south, west)
    int numOutputs = 4 + 4;
    //layout of NN. Fitst value is size of input layer, last is output layer. Arbitrary number of hidden layers and their sizes.
    int[] layerSizes = {numInputs, numInputs-16, numOutputs};


    /**
     * Creates a new instance of your solver agent.
     *
     * @param world Current world state
     */
    public MyAgent(World world)
    {
        w = world;

        
        //initialize layer activation values
        for(int i = 0; i < layerSizes.length; i++)
        {
            layers.add(new ArrayList<Double>());
            for(int j = 0; j < layerSizes[i]; j++)
            {
                layers.get(i).add(0.0);
            }
        }


        //initialize weights
        for(int i = 0; i < layers.size()-1; i++)
        {
            weights.add(new ArrayList<Double>());
            for(int j = 0; j < layers.get(i).size() * layers.get(i+1).size(); j++)
            {
                //randomly generated weights for each connection between left side and right side
                weights.get(i).add(rand.nextDouble()*2.0 +1);
                //System.out.println("weight[ " + i + "][" + j + "] = " + weights.get(i).get(j).toString());
            }
        }

        //initialize biases
        for(int i = 0; i < layers.size()-1; i++)
        {
            biases.add(new ArrayList<Double>());
            for(int j = 0; j < layers.get(i+1).size(); j++)
            {
                //randomly generated bias for each node in in each layer
                biases.get(i).add(rand.nextDouble()*2.0 +1);
                //biases.get(i).add(0.0);
                //System.out.println("biases[ " + i + "][" + j + "] = " + biases.get(i).get(j).toString());
            }
        }

        /*
        try {
            this.loadNetwork(5000);
        } catch (IOException ex) {
            Logger.getLogger(MyAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
        
    }

    public MyAgent(MyAgent agent)
    {
        layerSizes = agent.layerSizes.clone();
        for(ArrayList<Double> arr : agent.biases)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            biases.add(cpy);
            for(Double d : arr)
            {
                cpy.add(d);
            }
        }

        for(ArrayList<Double> arr : agent.weights)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            weights.add(cpy);
            for(Double d : arr)
            {
                cpy.add(d);
            }
        }

        for(ArrayList<Double> arr : agent.layers)
        {
            ArrayList<Double> cpy = new ArrayList<>();
            layers.add(cpy);
            for(Double d : arr)
            {
                cpy.add(d);
            }
        }

    }

    @Override
    public int compareTo(Object o)
    {
        if(o instanceof MyAgent)
        {
            MyAgent a = (MyAgent)o;
            if(a.avgScore > avgScore)
                return 1;
            else if(a.avgScore == avgScore)
                return 0;
            else
                return -1;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void saveNetwork(int generation) throws FileNotFoundException
    {
        try (PrintWriter out = new PrintWriter("gen_"+generation+".txt")) {
            //save network layout
            out.println(layerSizes.length);

            for(int i=0; i<layerSizes.length; i++) {
                out.println(layerSizes[i]);
            }
            //save weights
            for(int i=0; i<layers.size() - 1; i++) {
                for(int j=0; j<layers.get(i).size() * layers.get(i+1).size(); j++) {
                    out.println(weights.get(i).get(j));
                }
            }
            //save biases
            for(int i=0; i<layers.size() - 1; i++) {
                for(int j=0; j<layers.get(i+1).size(); j++) {
                    out.println(biases.get(i).get(j));
                }
            }
        }
    }

    public void loadNetwork(int generation) throws FileNotFoundException, IOException {
        try(BufferedReader br = new BufferedReader(new FileReader("gen_"+generation+".txt"))) {
            String line = br.readLine();
            int numLayers = Integer.parseInt(line);

            //read network layout
            int[] templs = new int[numLayers];
            for(int i=0; i<numLayers;i++) {
                    line = br.readLine();
                    templs[i] = Integer.parseInt(line);
                }
            layerSizes = templs;

            layers = new ArrayList<>();
            weights = new ArrayList<>();
            biases = new ArrayList<>();
            
            zs = new ArrayList<>();

            for(int i = 0; i < layerSizes.length; i++)
            {
                layers.add(new ArrayList<Double>());
                for(int j = 0; j < layerSizes[i]; j++)
                {
                    layers.get(i).add(0.0);
                }
            }
            for(int i = 0; i < layerSizes.length-1; i++)
            {
                zs.add(new ArrayList<Double>());
                for(int j = 0; j < layerSizes[i+1]; j++)
                {
                    zs.get(i).add(0.0);
                }
            }

            for(int i = 0; i < layers.size()-1; i++)
            {
                weights.add(new ArrayList<Double>());
                for(int j = 0; j < layers.get(i).size() * layers.get(i+1).size(); j++)
                {
                    line = br.readLine();
                    weights.get(i).add(Double.parseDouble(line));
                    System.out.println("weight[ " + i + "][" + j + "] = " + weights.get(i).get(j).toString());
                }
            }

            for(int i = 0; i < layers.size()-1; i++)
            {
                biases.add(new ArrayList<Double>());
                for(int j = 0; j < layers.get(i+1).size(); j++)
                {
                    line = br.readLine();
                    biases.get(i).add(Double.parseDouble(line));
                    System.out.println("biases[ " + i + "][" + j + "] = " + biases.get(i).get(j).toString());
                }
            }
        }
    }
    
    public void setInputs()
    {
        
        int cx = w.getPlayerX();
        int cy = w.getPlayerY();
        // stench -> binary
        // breeze -> binary
        // pos -> 4*4 = 16
        // prevPos -> (dx,dy)

        // set inputs
        if (w.hasBreeze(cx, cy))
        {
            //System.out.println("I am in a Breeze");
            layers.get(0).set(0,1.0);
        }
        else
        {
            layers.get(0).set(0,-1.0);
        }

        if (w.hasStench(cx, cy))
        {
            //System.out.println("I am in a Stench");
            layers.get(0).set(1,1.0);
        }
        else
        {
            layers.get(0).set(1,-1.0);
        }

        for(int i = 0; i < 16; i++)
        {
            layers.get(0).set(2+i, 0.0);
        }
        layers.get(0).set(2+(cx-1) + (cy-1)*4, 1.0);

        int dx = cx-px;
        int dy = cy-py;
        layers.get(0).set(2+16, (double)dx);
        layers.get(0).set(2+16+1, (double)dy);

        int canShoot = -1;
        if(w.hasArrow()) {
            canShoot = 1;
        }
        layers.get(0).set(2+16+2, (double)canShoot);


        //if in square with pit
        int hasPit = -1;
        if(w.hasPit(cx, cy)) {
            hasPit = 1;
        }
        layers.get(0).set(2+16+3, (double)hasPit);

        //breeze map
        for(int i = 0; i < 16; i++) {
            layers.get(0).set(2+16+4+i, 0.0);
            int x = (i%4)+1;
            int y = (i/4)+1;
            if(w.isUnknown(x,y))
            {
                layers.get(0).set(2+16+4+i, 0.0);
            } else if(w.hasBreeze(x, y)){
                layers.get(0).set(2+16+4+i, 1.0);
            }
        }

        //stench map
        for(int i = 0; i < 16; i++) {
            layers.get(0).set(2+16+4+16+i, 0.0);
            int x = (i%4)+1;
            int y = (i/4)+1;
            if(w.isUnknown(x,y))
            {
                layers.get(0).set(2+16+4+16+i, 0.0);
            } else if(w.hasStench(x, y)){
                layers.get(0).set(2+16+4+16+i, 1.0);
            }
        }

        // valid walk dir
        if(w.isValidPosition(cx, cy+1))
            layers.get(0).set(2+16+4+16 + 0, 1.0);
        else
            layers.get(0).set(2+16+4+16 + 0, -1.0);

        if(w.isValidPosition(cx+1, cy))
            layers.get(0).set(2+16+4+16 + 1, 1.0);
        else
            layers.get(0).set(2+16+4+16 + 1, -1.0);

        if(w.isValidPosition(cx, cy-1))
            layers.get(0).set(2+16+4+16 + 2, 1.0);
        else
            layers.get(0).set(2+16+4+16 + 2, -1.0);

        if(w.isValidPosition(cx-1, cy))
            layers.get(0).set(2+16+4+16 + 3, 1.0);
        else
            layers.get(0).set(2+16+4+16 + 3, -1.0);
        
        
        layers.get(0).set(2+16+4+16+4 + 0, (double)turns/50.0);
    }
    
    public void feedforward()
    {
        // for each layer
        for(int i = 0; i < layers.size()-1; i++)
        {
            for(int k = 0; k < layers.get(i+1).size(); k++)
            {
                layers.get(i+1).set(k, 0.0);
            }
            // for each node in current layer
            for(int j = 0; j < layers.get(i).size(); j++)
            {
                // for each node in next layer
                for(int k = 0; k < layers.get(i+1).size(); k++)
                {
                    //acummulate weighted values
                    Double weight = weights.get(i).get(k + j*layers.get(i+1).size());
                    Double prev = layers.get(i+1).get(k);
                    Double val =  prev + weight * layers.get(i).get(j);
                    layers.get(i+1).set(k, val);
                }
            }
            
            for(int k = 0; k < layers.get(i+1).size(); k++)
            {
                Double val = layers.get(i+1).get(k);
                Double bias = biases.get(i).get(k);
                //ReLU
                //layers.get(i+1).set(k, max(0.0, val + bias));
                // sigmoid
                layers.get(i+1).set(k,1.0/(1.0 + Math.exp(-val - bias)));
                //zs.get(i).set(k, val+bias);
            }
        }
    }
    

    void backpropagate(ArrayList<Double> y)
    {
        setInputs();
        feedforward();
        
        ArrayList<Double> cd = costDerivative(layers.get(layers.size()-1), y);
        ArrayList<Double> ReLUd = ReLUDerivative(zs.get(zs.size()-1));
        ArrayList<Double> delta = new ArrayList<>();
        for(int i = 0; i < cd.size(); i++)
        {
            delta.add(cd.get(i) * ReLUd.get(i));
        }
        ArrayList<ArrayList<Double>> nablaBias = new ArrayList<>();
        for(int i = 0; i < layers.size()-1; i++)
        {
            nablaBias.add(new ArrayList<Double>());
            for(int j = 0; j < layers.get(i+1).size(); j++)
            {
                nablaBias.get(i).add(0.0);
            }
        }
        
        ArrayList<Double> lastBias = nablaBias.get(nablaBias.size()-1);
        for(int i = 0; i < lastBias.size();i++)
        {
            
        }
        ArrayList<ArrayList<Double>> nablaWeight = new ArrayList<>();
        
        for(int i = 0; i < layers.size()-1; i++)
        {
            weights.add(new ArrayList<Double>());
            for(int j = 0; j < layers.get(i).size() * layers.get(i+1).size(); j++)
            {
                weights.get(i).add(0.0);
            }
        }
        
        for(int i = 0; i < delta.size(); i++)
        {
            
        }
        
        
    }
    
    ArrayList<Double> costDerivative(ArrayList<Double> activations, ArrayList<Double> y)
    {
        ArrayList<Double> result = new ArrayList<>();
        for(int i = 0; i < y.size(); i++)
        {
            result.add(activations.get(i) - y.get(i));
        }
        return result;
    }
    
    ArrayList<Double> ReLUDerivative(ArrayList<Double> z)
    {
        ArrayList<Double> result = new ArrayList<>();
        for(int i = 0; i < z.size(); i++)
        {
            double val = 0.0;
            if(z.get(i)>0)
                val = 1.0;
            result.add(val);
        }
        return result;
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

        
        setInputs();
        feedforward();

        
        int index = -1;
        Double largest = -1.0;
        for(int i = 0; i < layers.get(layers.size()-1).size(); i++)
        {
            Double val = layers.get(layers.size()-1).get(i);
            //System.out.print("[" + Integer.toString(i)+ "] = " + Double.toString(val) + ", ");
            if(val > largest)
            {
                largest = val;
                index = i;
            }
        }
        //System.out.println();
        
        
//        double sum = 0.0;
//        for(int i = 0; i < layers.get(layers.size()-1).size(); i++)
//        {
//            sum += layers.get(layers.size()-1).get(i);
//        }
//
//        double random = sum * rand.nextDouble();
//        sum = 0.0;
//        for(int i = 0; i < layers.get(layers.size()-1).size(); i++)
//        {
//            sum += layers.get(layers.size()-1).get(i);
//            if(random <= sum)
//            {
//                index = i;
//                break;
//            }
//        }
        
/*
        NaiveWorldSolver tiles = new NaiveWorldSolver(w);
        index = -1;
        if(tiles.shootDirX == 1)
            index = 5;
        if(tiles.shootDirX == -1)
            index = 7;
        if(tiles.shootDirY == 1)
            index = 4;
        if(tiles.shootDirY == -1)
            index = 6;
        if(tiles.walkDirX == -1)
            index = 3;
        if(tiles.walkDirX == 1)
            index = 1;
        if(tiles.walkDirY == -1)
            index = 2;
        if(tiles.walkDirY == 1)
            index = 0;
*/        

        //make decision based on "most activated" node in output layer
        switch(index)
        {
            case 0:
                //System.out.println("went North");
                goNorth();
                break;
            case 1:
                //System.out.println("went East");
                goEast();
                break;
            case 2:
                //System.out.println("went South");
                goSouth();
                break;
            case 3:
                //System.out.println("went West");
                goWest();
                break;
            case 4:
                //System.out.println("shoot");
                shootNorth();
                break;
            case 5:
                //System.out.println("shoot");
                shootEast();
                break;
            case 6:
                //System.out.println("shoot");
                shootSouth();
                break;
            case 7:
                //System.out.println("shoot");
                shootWest();
                break;
        }

        px = cx;
        py = cy;
    }

    private void goNorth() {
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

    private void goSouth() {
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

    private void goEast() {
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

    private void goWest() {
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

    private void shootNorth() {
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

    private void shootSouth() {
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

    private void shootEast() {
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

    private void shootWest() {
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
      return (int)(Math.random() * 4);
    }
}
