package ub.cse.algo;

import ub.cse.algo.util.Pair;

import java.sql.Array;
import java.util.*;

import static ub.cse.algo.Traversals.*;

public class Solution {

    private Info info;
    private Graph graph;
    private ArrayList<Client> clients;
    private ArrayList<Integer> bandwidths;
    private HashMap<Integer, HashMap<Integer, Integer>> weight;
    private int[][] newpq = new int[100][];
    private int[] pqneededsize = new int[100];
    private int[] from;
    private int[] timetracker;
    private int[] search;
    private int searchid = 0;
    private HashMap<Integer, Double> usage;
    private HashMap<Integer, Integer> steps;
    private HashMap<Integer, ArrayList<Integer>> oldpaths;

    /**
     * Basic Constructor
     *
     * @param info: data parsed from input file
     */
    public Solution(Info info) {
        this.info = info;
        this.graph = info.graph;
        this.clients = info.clients;
        this.bandwidths = info.bandwidths;
        this.weight = new HashMap<>();
        this.from = new int[this.graph.size()];
        this.timetracker = new int[this.graph.size()];
        this.search = new int[this.graph.size()];
    }

    static class runfeedback {
        HashMap<Integer, ArrayList<Integer>> routesfound = new HashMap<>();
        HashMap<Integer, ArrayList<int[]>> waitatnode = new HashMap<>();
        HashMap<Integer, Integer> routetime = new HashMap<>();
    }

    /**
     * Method that returns the calculated
     * SolutionObject as found by your algorithm
     *
     * @return SolutionObject containing the paths, priorities and bandwidths
     */
    public SolutionObject outputPaths() {
        SolutionObject sol = new SolutionObject();
        /* TODO: Your solution goes here */
        this.steps = bfs(this.graph, this.clients);
        this.oldpaths = bfsPaths(this.graph, this.clients);
        this.usage = new HashMap<>();
        for (Client client : this.clients) {
            double totaluse = Math.floor(client.alpha * steps.get(client.id));
            usage.put(client.id, totaluse);
        }

        int[] bandwidth = new int[this.graph.size()];

        for (int spot = 0; spot < bandwidth.length; spot++) {
            bandwidth[spot] = this.bandwidths.get(spot);
        }

        runfeedback run = buildroute(bandwidth);
        sol.paths = run.routesfound;

        for (int fill = 0; fill < bandwidth.length; fill++) {
            sol.bandwidths.add(bandwidth[fill]);
        }


        sol.bandwidths = chooseBandwidths(sol, run);


        return sol;
    }


    runfeedback buildroute(int[] bandwith) {
        runfeedback out = new runfeedback();
        for (int router : this.graph.keySet()) {
            weight.put(router, new HashMap<>());

        }
        for (Client client : this.clients) {
            ArrayList<Integer> route = moddedbfsPaths(this.graph, client, client, usage.get(client.id), oldpaths, bandwith, out);
            out.routesfound.put(client.id, route);
        }


        return out;
    }


    ArrayList<Integer> moddedbfsPaths(Graph graph, Client clients, Client target, double usage, HashMap<Integer, ArrayList<Integer>> oldpath, int[] bandwidth, runfeedback runffeed) {
        ArrayList<Integer> totalwalk = new ArrayList<>();
        this.searchid = this.searchid + 1;
        this.search[graph.contentProvider] = this.searchid;
        timetracker[graph.contentProvider] = 0;
        from[graph.contentProvider] = -1;
        int sport = 0;
        int spotsize = 0;
        addtopq(0, graph.contentProvider);
        Boolean breaker = false;
        while (breaker == false) {
            while (sport <= spotsize && pqneededsize[sport] == 0) {
                sport = sport + 1;
            }
            if (sport > spotsize) {
                break;
            }
            int holding = sport;
            int id = newpq[sport][--pqneededsize[sport]];
            int timetrack = timetracker[id];
            if (id == target.id) {

                break;
            }
            if (holding >= usage) {
                continue;
            }

            if (sport > timetracker[id]) {
                continue;
            }


            boolean noCapacity = bandwidth[id] == 0;
            if (noCapacity) {
                continue;
            }
            ArrayList<Integer> nextspots = this.graph.get(id);
            int cashmax = bandwidth[id];


            while (this.weight.get(id).getOrDefault(timetrack, 0) >= cashmax) {
                timetrack = timetrack + 1;
            }
            timetrack = timetrack + 1;
            for (int nodes : nextspots) {
                if (nodes == graph.contentProvider) {
                    continue;
                }
                if (timetrack < timetracker[nodes] || search[nodes] != searchid) {
                    addtopq(timetrack, nodes);
                    if (timetrack > spotsize) {
                        spotsize = timetrack;
                    }

                    from[nodes] = id;
                    timetracker[nodes] = timetrack;
                    search[nodes] = searchid;
                }

            }


        }
        for (int iter = 0; iter <= spotsize; iter++) {
            pqneededsize[iter] = 0;
        }
        Boolean check = false;
        int backend = target.id;
        if (search[backend] != searchid || from[backend] == -1) {
            totalwalk = oldpath.get(backend);
            check = true;
        }
        while (backend != graph.contentProvider && check == false) {
            totalwalk.add(backend);
            backend = from[backend];
        }
        if (check == false) {
            totalwalk.add(graph.contentProvider);
            Collections.reverse(totalwalk);
        }
        int exittime = 0;
        ArrayList<int[]> waittime = new ArrayList<>();
        for (int idspot = 0; idspot < totalwalk.size() - 1; idspot++) {
            int spot = totalwalk.get(idspot);
            boolean needsCapacity = bandwidth[spot] == 0 && spot != graph.contentProvider;
            if (needsCapacity) {
                bandwidth[spot] = 1;
            }
            int cashmax = bandwidth[spot];


            if (weight.get(spot) == null) {
                weight.put(spot, new HashMap<>());
            }
            int optimaltime = exittime;
            int wait = 0;


            while (weight.get(spot).getOrDefault(exittime, 0) >= cashmax) {
                exittime = exittime + 1;
                wait = wait + 1;
            }
            if (wait > 0) {
                waittime.add(new int[]{spot, optimaltime, wait});
            }

            weight.get(spot).put(exittime, weight.get(spot).getOrDefault(exittime, 0) + 1);
            exittime = exittime + 1;
        }
        runffeed.waitatnode.put(target.id, waittime);
        runffeed.routetime.put(target.id, exittime);

        return totalwalk;

    }

    private ArrayList<Integer> chooseBandwidths(SolutionObject base, runfeedback run) {

        SolutionObject trial = new SolutionObject();
        trial.paths = base.paths;
        trial.bandwidths = new ArrayList<>(base.bandwidths);

        HashMap<Integer, Integer> before = Simulator.run(graph, clients, base);

        for (Client client : clients) {
            boolean noComplaint = !complains(client, before);
            if (noComplaint) {

                continue;
            }
            for (int[] wait : run.waitatnode.get(client.id)) {
                int node = wait[0];
                boolean canUpgrade = node != graph.contentProvider;
                if (canUpgrade) {


                    trial.bandwidths.set(node, base.bandwidths.get(node) + 3);
                }
            }
        }

        HashMap<Integer, Integer> after = Simulator.run(graph, clients, trial);
        float original = netRevenue(base, before);
        float upgraded = netRevenue(trial, after);

        boolean earnsMore = upgraded > original;
        if (earnsMore) {
            return trial.bandwidths;
        }
        return base.bandwidths;
    }

    private boolean complains(Client client, Map<Integer, Integer> delays) {

        int distance = info.shortestDelays.get(client.id);
        return delays.get(client.id) > client.beta * distance;
    }

    public float legalPenalty(Map<Integer, Integer> delays) {
        int allComplaints = 0;
        int fccComplaints = 0;
        int fccSize = 0;


        for (Client client : clients) {
            boolean fcc = client.isFcc;
            if (fcc) {

                fccSize++;
            }
            boolean complained = complains(client, delays);
            if (complained) {
                allComplaints++;
            }
            boolean fccComplaint = fcc && complained;
            if (fccComplaint) {
                fccComplaints++;
            }
        }
        float lawsuit = penalty(allComplaints, clients.size(), info.rho1, info.lawsuit);

        float fccFine = penalty(fccComplaints, fccSize, info.rho2, info.fccFine);
        return lawsuit + fccFine;
    }

    private float penalty(int count, int size, float ratio, float fine) {
        int limit = (int) (ratio * size);
        boolean fineDue = count >= limit;
        if (fineDue) {
            return -fine;
        }
        return 0;
    }

    private float netRevenue(SolutionObject plan, HashMap<Integer, Integer> delays) {


        float income = Revenue.revenue(info, plan, delays, false, false, true);
        return income + legalPenalty(delays);
    }

    private void addtopq(int time, int node) {

        if (time >= newpq.length) {
            newpq = Arrays.copyOf(newpq, newpq.length * 15);
            pqneededsize = Arrays.copyOf(pqneededsize, pqneededsize.length * 15);


        }
        if (newpq[time] == null) {
            newpq[time] = new int[30];
        }


        if (pqneededsize[time] == newpq[time].length) {
            newpq[time] = Arrays.copyOf(newpq[time], pqneededsize[time] * 15);
        }

        newpq[time][pqneededsize[time]++] = node;


    }

}


























