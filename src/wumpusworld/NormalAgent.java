/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wumpusworld;

import java.util.ArrayList;

/**
 *
 * @author Tristan
 */
public class NormalAgent implements Agent
{

    public World w;

    NaiveWorldSolver solver;
    
    public NormalAgent(World w)
    {
        this.w = w;
    }
    
    public void setValues(double[] inputs, double[] outputs)
    {
        // visible map
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.isVisited(x + 1, y + 1))
            {
                inputs[i] = 1.0;
            } else
            {
                inputs[i] = 0.0;
            }
        }

        //stench map
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.hasStench(x + 1, y + 1))
            {
                inputs[16 + i] = 1.0;
            } else
            {
                inputs[16 + i] = 0.0;
            }
        }
        
        solver = new NaiveWorldSolver(w);
        
        for(int i = 0; i < outputs.length; i++)
            outputs[i] = 0.0;
        
        if(solver.shootDirY == 1)
            outputs[16 + 0] = 1.0;
        else if(solver.shootDirX == 1)
            outputs[16 + 1] = 1.0;
        else if(solver.shootDirY == -1)
            outputs[16 + 2] = 1.0;
        else if(solver.shootDirX == -1)
            outputs[16 + 3] = 1.0;
        else if(solver.targetTile > 0 && solver.targetTile <= outputs.length)
            outputs[solver.targetTile] = 1.0;
    }

    @Override
    public void doAction()
    {
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
        
        solver = new NaiveWorldSolver(w);
        
        if (solver.shootDirX == 1)
        {
            shootEast();
        } else if (solver.shootDirX == -1)
        {
            shootWest();
        } else if (solver.shootDirY == 1)
        {
            shootNorth();
        } else if (solver.shootDirY == -1)
        {
            shootSouth();
        } else
        {
            // walk dir
            int dx = 0;
            int dy = 0;
            int target = solver.targetTile;
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
                        int steps = search(x, y, target);
                        if (steps <= minSteps)
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
        }
    }

    private int search(int sx, int sy, int target)
    {
        int cx = w.getPlayerX() - 1;
        int cy = w.getPlayerY() - 1;

        // x = i%4; y = i/4;
        ArrayList<Integer> open = new ArrayList<>();
        ArrayList<Integer> closed = new ArrayList<>();

        open.add(sx + sy * 4);
        closed.add(cx + cy * 4);

        int steps = 0;
        while (!open.isEmpty())
        {
            steps++;
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
                        return steps;
                    }
                    if (!open.contains(index) && !closed.contains(index) && w.isVisited(index % 4 + 1, index / 4 + 1))
                    {
                        open.add(index);
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
}
