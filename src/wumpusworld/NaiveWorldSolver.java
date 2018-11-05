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
public class NaiveWorldSolver
{
    public class Tile
    {
        boolean hasWompus = false;
        boolean possibleWompus = false;
        boolean possiblePit = false;
        boolean visible = false;
        double pitProbability = 0;
    }
    World w;
    Tile[] tiles = new Tile[16];

    int walkDirX = 0;
    int walkDirY = 0;
    int shootDirX = 0;
    int shootDirY = 0;

    public NaiveWorldSolver(World w) 
    {
        this.w = w;
        for(int i = 0; i < 16; i++)
        {
            tiles[i] = new Tile();
        }
        for(int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.hasStench(x+1, y+1))
            {
                addPossibleWompus(x+1, y);
                addPossibleWompus(x-1, y);
                addPossibleWompus(x, y+1);
                addPossibleWompus(x, y-1);
            }
            if(w.hasBreeze(x+1, y+1))
            {
                addPossiblePit(x+1, y);
                addPossiblePit(x-1, y);
                addPossiblePit(x, y+1);
                addPossiblePit(x, y-1);
            }
        }
        for(int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if(w.isVisited(x+1, y+1))
            {
                tiles[i].visible = true;
                if (!w.hasStench(x+1, y+1))
                {
                    clearPossibleWompus(x+1, y);
                    clearPossibleWompus(x-1, y);
                    clearPossibleWompus(x, y+1);
                    clearPossibleWompus(x, y-1);
                }
                if(!w.hasBreeze(x+1, y+1))
                {
                    clearPossiblePit(x+1, y);
                    clearPossiblePit(x-1, y);
                    clearPossiblePit(x, y+1);
                    clearPossiblePit(x, y-1);
                }
            }
        }
        for(int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.hasStench(x+1, y+1))
            {
                int ox = 1;
                int oy = 1;
                for(int j = 0; j < 4; j++)
                {
                    int wx = x + ox;
                    int wy = y + oy;
                    if(w.isValidPosition(wx+1, wy+1) && w.hasStench(wx+1, wy+1))
                    {
                        if(w.isVisited(wx+1, y+1))
                        {
                            tiles[x + wy*4].hasWompus = true;
                        }
                        if(w.isVisited(x+1, wy+1))
                        {
                            tiles[wx + y*4].hasWompus = true;
                        }
                    }
                    int temp = ox;
                    ox = -oy;
                    oy = temp;
                }
            }
        }
        for(int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if(w.hasBreeze(x+1, y+1))
            {
                int ox = 1;
                int oy = 0;

                double numHidden = 0;

                for(int j = 0; j < 4; j++)
                {
                    int cx = x + ox;
                    int cy = y + oy;
                    if(w.isUnknown(cx+1, cy+1))
                    {
                        numHidden += 1.0;
                    }
                    int temp = ox;
                    ox = -oy;
                    oy = temp;
                }

                ox = 1;
                oy = 0;
                for(int j = 0; j < 4; j++)
                {
                    int cx = x + ox;
                    int cy = y + oy;
                    if(w.isUnknown(cx+1, cy+1))
                    {
                        Tile t = tiles[cx + cy*4];
                        t.pitProbability = Math.max(1.0/numHidden, t.pitProbability);
                    }
                    int temp = ox;
                    ox = -oy;
                    oy = temp;
                }
            }
        }



        int px = w.getPlayerX()-1;
        int py = w.getPlayerY()-1;

        // target position
        int tx = -1;
        int ty = -1;
        int len = Integer.MAX_VALUE;
        for(int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if(isReachable(x, y) && isSafe(x, y) && !tiles[x+y*4].visible)
            {
                int dx = Math.abs(px-x);
                int dy = Math.abs(py-y);
                int newLen = dx + dy;
//                    System.out.println("x = " + Integer.toString(x));
//                    System.out.println("y = " + Integer.toString(y));
//                    System.out.println("Length = " + Integer.toString(newLen));
                if(newLen < len)
                {
                    len = newLen;
                    tx = x;
                    ty = y;
                }
            }
        }
//            System.out.println("////////////////////////////////////////");

        if(tx == -1 && ty == -1)
        {
            double lowestPitProb = 1.0;
            for(int i = 0; i < 16; i++)
            {
                int x = i % 4;
                int y = i / 4;
                Tile t = tiles[x+y*4];
                if(isReachable(x, y) && !t.visible)
                {
                    if(t.pitProbability < lowestPitProb)
                    {
                        lowestPitProb = t.pitProbability;
                        tx = x;
                        ty = y;
                    }
                }
            }
        }

        int target = tx + ty*4;

        /*
        System.out.println("Target:");
        System.out.println(tx);
        System.out.println(ty);
        */

        int ox = 1;
        int oy = 0;
        int bx = 0;
        int by = 0;
        int minSteps = Integer.MAX_VALUE;
        for(int i = 0; i < 4; i++) 
        {
            int x = px + ox;
            int y = py + oy;
            if(w.isValidPosition(x+1, y+1)) 
            {
                if(tiles[x+y*4].hasWompus)
                {
                    shootDirX = ox;
                    shootDirY = oy;
                    break;
                }

                if(x + y*4 == target)
                {
                   bx = ox;
                   by = oy;
                   minSteps = 1;
                   break;
                }
                if(tiles[x+y*4].visible)
                {
                    int steps = search(x, y, target);
                    if(steps < minSteps)
                    {
                       bx = ox;
                       by = oy;
                       minSteps = steps;
                    }
                }
            }
            // 90 degree rotate offset 
            int temp = ox;
            ox = -oy;
            oy = temp;
        }

        walkDirX = bx;
        walkDirY = by;
    }

    private int search(int sx, int sy, int target)
    {
        int cx = w.getPlayerX()-1;
        int cy = w.getPlayerY()-1;

        // x = i%4; y = i/4;
        ArrayList<Integer> open = new ArrayList<>();
        ArrayList<Integer> closed = new ArrayList<>();

        open.add(sx + sy*4);
        closed.add(cx + cy*4);

        int steps = 0;
        while(!open.isEmpty())
        {
            steps++;
            int pos = open.get(0);
            open.remove(0);
            closed.add(pos);
            int nx = pos%4;
            int ny = pos/4;
            int ox = 1;
            int oy = 0;
            for(int i = 0; i <= 4; i++)
            {
                int x = nx + ox;
                int y = ny + oy;
                if(w.isValidPosition(x+1, y+1))
                {
                    int index = x + y*4;
                    if(index == target)
                    {
                        return steps;
                    }
                    if(!open.contains(index) && !closed.contains(index) && tiles[index].visible)
                    {
                        open.add(index);
                    }
                }
                // 90 degree rotate offset 
                int temp = ox;
                ox = -oy;
                oy = temp;
            }
        }
        return Integer.MAX_VALUE;
    }

    private boolean isReachable(int x, int y)
    {
        boolean result = false;

        if(w.isValidPosition(x+1+1, y+1))
            result |= tiles[(x+1)+(y)*4].visible;

        if(w.isValidPosition(x+1-1, y+1))
            result |= tiles[(x-1)+(y)*4].visible;

        if(w.isValidPosition(x+1, y+1+1))
            result |= tiles[(x)+(y+1)*4].visible;

        if(w.isValidPosition(x+1, y+1-1))
            result |= tiles[(x)+(y-1)*4].visible;

        return result;
    }
    private boolean isSafe(int x, int y)
    {
        Tile t = tiles[x + y*4];
        return !t.possibleWompus && !t.possiblePit;
    }
    private void clearPossibleWompus(int x, int y) 
    {
        if(w.isValidPosition(x+1, y+1))
        {
            tiles[x+y*4].possibleWompus = false;
        }
    }
    private void clearPossiblePit(int x, int y) 
    {
         if(w.isValidPosition(x+1, y+1))
        {
            tiles[x+y*4].possiblePit = false;
        }
    }
    private void addPossibleWompus(int x, int y)
    {
        if(w.isValidPosition(x+1, y+1))
        {
            tiles[x+y*4].possibleWompus = true;
        }
    }
    private void addPossiblePit(int x, int y)
    {
        if(w.isValidPosition(x+1, y+1))
        {
            tiles[x+y*4].possiblePit = true;
        }
    }
}