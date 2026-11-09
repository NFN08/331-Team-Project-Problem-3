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
    private HashMap<Integer, HashMap<Integer,Integer>> weight;
    private int[] from;
    private int[] timetracker;
    private int[] search;
    private int searchid=0;
    private HashMap<Integer,Double> usage;
    private HashMap<Integer,Integer> steps;
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
        this.weight=new HashMap<>();
        this.from= new int[this.graph.size()];
        this.timetracker= new int[this.graph.size()];
        this.search= new int[this.graph.size()];
    }
    static class runfeedback{
        HashMap<Integer,ArrayList<Integer>> routesfound=new HashMap<>();
        HashMap<Integer,ArrayList<int[]>> waitatnode=new HashMap<>();
        HashMap<Integer,Integer> routetime= new HashMap<>();
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
        this.steps= bfs(this.graph, this.clients);
        this.oldpaths= bfsPaths(this.graph, this.clients);
        this.usage= new HashMap<>();
        for (Client client : this.clients){
            double totaluse=Math.floor(client.alpha*steps.get(client.id));
            usage.put(client.id, totaluse);
        }

       int[] bandwidth= new int[this.graph.size()];

        for (int spot=0; spot<bandwidth.length; spot++){
            bandwidth[spot]=this.bandwidths.get(spot);
        }

        runfeedback run=buildroute(bandwidth);
        sol.paths=run.routesfound;

        for(int fill=0; fill<bandwidth.length; fill++){
            sol.bandwidths.add(bandwidth[fill]);
        }





        return sol;
    }





    runfeedback buildroute(int[] bandwith){
        runfeedback out= new runfeedback();
        for (int router :this.graph.keySet()){
            weight.put(router,new HashMap<>());

        }
        for (Client client :this.clients){
            ArrayList<Integer> route= moddedbfsPaths(this.graph,client,client, usage.get(client.id),oldpaths,bandwith,out);
            out.routesfound.put(client.id,route);
        }


        return out;
    }












    ArrayList<Integer> moddedbfsPaths(Graph graph, Client clients,Client target, double usage,HashMap<Integer, ArrayList<Integer>>  oldpath, int[] bandwidth, runfeedback runffeed){
        ArrayList<Integer> totalwalk= new ArrayList<>();
        this.searchid=this.searchid+1;
        PriorityQueue<Pair<Integer,Integer>> totaldist=new PriorityQueue<>(Comparator.comparingInt(Pair::getSecond));
        totaldist.add(new Pair<>(graph.contentProvider, 0));
        this.search[graph.contentProvider]=this.searchid;
        timetracker[graph.contentProvider]=0 ;
        from[graph.contentProvider]=-1;


        while (totaldist.isEmpty()==false){
            Pair<Integer,Integer> node= totaldist.poll();
            int id=node.getFirst();
            int traveltime=node.getSecond();
            int timetrack=timetracker[id];
            if (id==target.id){

                break;}
            if (traveltime>=usage){continue;}

            if(traveltime>timetracker[id]){continue;}



            ArrayList<Integer> nextspots=this.graph.get(id);
            int cashmax= bandwidth[id];


            while (this.weight.get(id).getOrDefault(timetrack,0)>=cashmax){
                timetrack=timetrack+1;
            }
            timetrack=timetrack+1;
            for (int nodes : nextspots){
                if (nodes== graph.contentProvider){continue;}
                if (timetrack<timetracker[nodes] || search[nodes]!=searchid){
                    totaldist.add(new Pair<>(nodes,timetrack));
                    from[nodes]=id;
                    timetracker[nodes]= timetrack;
                    search[nodes]=searchid;}
            }


        }
        Boolean check=false;
        int backend= target.id;
        if (search[backend]!=searchid || from[backend]==-1){
            totalwalk=oldpath.get(backend);
            check=true;
        }
        while(backend!= graph.contentProvider&& check==false){
            totalwalk.add(backend);
            backend=from[backend];
        }
        if (check==false){
            totalwalk.add(graph.contentProvider);
            Collections.reverse(totalwalk);}
        int exittime=0;
        ArrayList<int[]> waittime= new ArrayList<>();
        for (int idspot=0;idspot<totalwalk.size()-1;idspot++){
            int spot= totalwalk.get(idspot);
            int cashmax= bandwidth[spot];


            if (weight.get(spot)==null){
                weight.put(spot, new HashMap<>());
            }
            int optimaltime=exittime;
            int wait=0;



            while (weight.get(spot).getOrDefault(exittime,0)>=cashmax){
                exittime=exittime+1;
                wait=wait+1;
            }
            if(wait>0){
                waittime.add(new int[]{spot,optimaltime,wait});
            }

            weight.get(spot).put(exittime,weight.get(spot).getOrDefault(exittime,0)+ 1);
            exittime=exittime+1;
        }
        runffeed.waitatnode.put(target.id,waittime);
        runffeed.routetime.put(target.id,exittime);

        return totalwalk;

    }
}


























