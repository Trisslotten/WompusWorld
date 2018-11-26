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
        double wompusProbability = 0;
    }
    World w;
    Tile[] tiles = new Tile[16];

    /*
    int walkDirX = 0;
    int walkDirY = 0;
     */
    int targetTile = -1;
    int shootDirX = 0;
    int shootDirY = 0;

    public NaiveWorldSolver(World w)
    {
        this.w = w;
        for (int i = 0; i < 16; i++)
        {
            tiles[i] = new Tile();
        }

        int visited = 0;
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.isVisited(x + 1, y + 1))
            {
                visited++;
            }
        }
        if (visited == 1 && w.hasStench(1, 1) && w.hasArrow())
        {
            shootDirX = 1;
        } else
        {
            solve();
        }
    }

    private void solve()
    {
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.hasStench(x + 1, y + 1))
            {
                addPossibleWompus(x + 1, y);
                addPossibleWompus(x - 1, y);
                addPossibleWompus(x, y + 1);
                addPossibleWompus(x, y - 1);
            }
            if (w.hasBreeze(x + 1, y + 1))
            {
                addPossiblePit(x + 1, y);
                addPossiblePit(x - 1, y);
                addPossiblePit(x, y + 1);
                addPossiblePit(x, y - 1);
            }
        }

        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            int ox = 1;
            int oy = 0;

            int stenchCount = 0;
            for (int j = 0; j < 4; j++)
            {
                int wx = x + ox;
                int wy = y + oy;
                if (w.hasStench(wx + 1, wy + 1))
                {
                    stenchCount++;
                }
                int temp = ox;
                ox = -oy;
                oy = temp;
            }

            if (!w.isVisited(x + 1, y + 1) && stenchCount >= 2)
            {
                tiles[i].hasWompus = true;
                tiles[i].wompusProbability = 1.0;
            }
        }
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.hasBreeze(x + 1, y + 1) || w.hasStench(x + 1, y + 1))
            {
                int ox = 1;
                int oy = 0;

                double numHidden = 0;

                for (int j = 0; j < 4; j++)
                {
                    int cx = x + ox;
                    int cy = y + oy;
                    if (w.isUnknown(cx + 1, cy + 1))
                    {
                        numHidden += 1.0;
                    }
                    int temp = ox;
                    ox = -oy;
                    oy = temp;
                }

                ox = 1;
                oy = 0;
                for (int j = 0; j < 4; j++)
                {
                    int cx = x + ox;
                    int cy = y + oy;
                    if (w.isValidPosition(cx + 1, cy + 1))
                    {
                        Tile t = tiles[cx + cy * 4];
                        if (w.hasBreeze(x + 1, y + 1))
                        {
                            if (w.isUnknown(cx + 1, cy + 1))
                            {
                                t.pitProbability = Math.max(1.0 / numHidden, t.pitProbability);
                            } else if (w.hasPit(cx + 1, cy + 1))
                            {
                                t.pitProbability = 1.0;
                            }
                        }
                        if (w.hasStench(x + 1, y + 1))
                        {
                            if (w.isUnknown(cx + 1, cy + 1))
                            {
                                t.wompusProbability = Math.max(1.0 / numHidden, t.wompusProbability);
                            }
                        }
                    }

                    int temp = ox;
                    ox = -oy;
                    oy = temp;
                }
            }
        }
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (w.isVisited(x + 1, y + 1))
            {
                tiles[i].visible = true;
                if (!w.hasStench(x + 1, y + 1))
                {
                    clearPossibleWompus(x + 1, y);
                    clearPossibleWompus(x - 1, y);
                    clearPossibleWompus(x, y + 1);
                    clearPossibleWompus(x, y - 1);
                }
                if (!w.hasBreeze(x + 1, y + 1))
                {
                    clearPossiblePit(x + 1, y);
                    clearPossiblePit(x - 1, y);
                    clearPossiblePit(x, y + 1);
                    clearPossiblePit(x, y - 1);
                }
            }
        }

        int px = w.getPlayerX() - 1;
        int py = w.getPlayerY() - 1;

        ArrayList<Integer> possibleTargets = new ArrayList<>();
        for (int i = 0; i < 16; i++)
        {
            int x = i % 4;
            int y = i / 4;
            if (isReachable(x, y) && !tiles[x + y * 4].visible)// && isSafe(x, y) )
            {
                possibleTargets.add(x + y * 4);
            }
        }

        double lowestPitProb = 2.0;
        int length = Integer.MAX_VALUE;
        int best = -1;
        for (int i = 0; i < possibleTargets.size(); i++)
        {
            int index = possibleTargets.get(i);
            int currLength = searchAvoidPit(index);
            Tile t = tiles[index];

            if (currLength < Integer.MAX_VALUE && t.wompusProbability <= 0.0)
            {
                if (t.pitProbability < lowestPitProb)
                {
                    lowestPitProb = t.pitProbability;
                    best = index;
                } else if (t.pitProbability == lowestPitProb && currLength < length)
                {
                    length = currLength;
                    best = index;
                }
            }
        }

        if (best == -1)
        {
            lowestPitProb = 2.0;
            for (int i = 0; i < 16; i++)
            {
                int x = i % 4;
                int y = i / 4;
                Tile t = tiles[i];
                if (isReachable(x, y) && !t.visible && t.wompusProbability <= 0.0)
                {
                    if (t.pitProbability < lowestPitProb)
                    {
                        lowestPitProb = t.pitProbability;
                        best = i;
                    }
                }
            }
        }
        if (best == -1)
        {
            lowestPitProb = 2.0;
            for (int i = 0; i < 16; i++)
            {
                int x = i % 4;
                int y = i / 4;
                Tile t = tiles[x + y * 4];
                if (isReachable(x, y) && !t.visible)
                {
                    if (t.pitProbability < lowestPitProb)
                    {
                        lowestPitProb = t.pitProbability;
                        best = i;
                    }
                }
            }
        }

        targetTile = best;

        if (w.hasArrow())
        {
            int ox = 1;
            int oy = 0;
            for (int i = 0; i < 4; i++)
            {
                int x = px + ox;
                int y = py + oy;
                if (w.isValidPosition(x + 1, y + 1))
                {
                    Tile t = tiles[x + y * 4];
                    if (t.hasWompus || t.wompusProbability == 1.0)
                    {
                        shootDirX = ox;
                        shootDirY = oy;
                        break;
                    }
                }
                // 90 degree rotate offset 
                int temp = ox;
                ox = -oy;
                oy = temp;
            }
        }
    }

    private int searchAvoidPit(int target)
    {
        int cx = w.getPlayerX() - 1;
        int cy = w.getPlayerY() - 1;

        // x = i%4; y = i/4;
        ArrayList<Integer> open = new ArrayList<>();
        ArrayList<Integer> closed = new ArrayList<>();

        open.add(cx + cy * 4);

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
                    if (!open.contains(index) && !closed.contains(index) && tiles[index].visible && !w.hasPit(x + 1, y + 1))
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

        if (w.isValidPosition(x + 1 + 1, y + 1))
        {
            result |= tiles[(x + 1) + (y) * 4].visible;
        }

        if (w.isValidPosition(x + 1 - 1, y + 1))
        {
            result |= tiles[(x - 1) + (y) * 4].visible;
        }

        if (w.isValidPosition(x + 1, y + 1 + 1))
        {
            result |= tiles[(x) + (y + 1) * 4].visible;
        }

        if (w.isValidPosition(x + 1, y + 1 - 1))
        {
            result |= tiles[(x) + (y - 1) * 4].visible;
        }

        return result;
    }

    private boolean isSafe(int x, int y)
    {
        Tile t = tiles[x + y * 4];
        return !t.possibleWompus && !t.possiblePit;
    }

    private void clearPossibleWompus(int x, int y)
    {
        if (w.isValidPosition(x + 1, y + 1))
        {
            tiles[x + y * 4].possibleWompus = false;
            tiles[x + y * 4].hasWompus = false;
            tiles[x + y * 4].wompusProbability = 0.0;
        }
    }

    private void clearPossiblePit(int x, int y)
    {
        if (w.isValidPosition(x + 1, y + 1))
        {
            tiles[x + y * 4].possiblePit = false;
            tiles[x + y * 4].pitProbability = 0.0;
        }
    }

    private void addPossibleWompus(int x, int y)
    {
        if (w.isValidPosition(x + 1, y + 1))
        {
            tiles[x + y * 4].possibleWompus = true;
        }
    }

    private void addPossiblePit(int x, int y)
    {
        if (w.isValidPosition(x + 1, y + 1))
        {
            tiles[x + y * 4].possiblePit = true;
        }
    }
}
