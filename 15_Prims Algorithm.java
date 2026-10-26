/*algorithm
1. start from any source vertex
2. out of all active edges pick the one with smallest weight
*/


import java.util.*;

public class PrimMST {
    
    /**
     * Graph class using adjacency list representation.
     * Each entry adj[u] is a list of int[]{neighbor, weight}.
     */
    static class Graph {
        // Adjacency list: adj.get(u) = list of {neighbor, weight} pairs
        List<List<int[]>> adj;
        int v;
        
        public Graph(int n) {
            this.v = n;
            this.adj = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                adj.add(new ArrayList<>());
            }
        }
        
        /**
         * Add an undirected edge between x and y with weight wt.
         */
        public void addEdge(int x, int y, int wt) {
            adj.get(x).add(new int[]{y, wt});
            adj.get(y).add(new int[]{x, wt});  // undirected
        }
        
        /**
         * Prim's MST algorithm:
         * 1. Start from any source vertex (here vertex 0).
         * 2. Maintain a priority queue of active edges (edges from MST to outside).
         * 3. Repeatedly pick the minimum-weight active edge.
         * 4. If destination not yet in MST → add it, accumulate weight, add its edges to PQ.
         *    If already in MST → discard (would create cycle).
         *
         * @return Total weight of the MST
         */
        public int primMST() {
            // Priority queue: min-heap of int[]{weight, node}
            // Stored as {weight, node} so the heap orders by weight automatically
            PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
            
            // Track which vertices are already included in the MST
            boolean[] visited = new boolean[v];
            int totalWeight = 0;
            
            // Start from vertex 0 with weight 0
            pq.offer(new int[]{0, 0});  // {weight, node}
            
            while (!pq.isEmpty()) {
                // Pick the minimum-weight active edge
                int[] best = pq.poll();
                int weight = best[0];   // weight to reach this node
                int node = best[1];     // destination node
                
                // If already in MST, skip (would create cycle)
                if (visited[node]) {
                    continue;
                }
                
                // Otherwise, include this node in MST
                visited[node] = true;
                totalWeight += weight;
                
                // Add all edges from this node (to unvisited neighbors) to PQ
                for (int[] edge : adj.get(node)) {
                    int neighbor = edge[0];
                    int edgeWeight = edge[1];
                    if (!visited[neighbor]) {
                        pq.offer(new int[]{edgeWeight, neighbor});
                    }
                }
            }
            
            return totalWeight;
        }
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();  // number of vertices
        int m = scanner.nextInt();  // number of edges
        
        Graph g = new Graph(n);
        for (int i = 0; i < m; i++) {
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            int w = scanner.nextInt();
            // Convert to 0-indexed
            g.addEdge(x - 1, y - 1, w);
        }
        
        System.out.println(g.primMST());
        scanner.close();
    }
}



class graph {
	//adjacency list implementation
	vector < pair<int, int> >*l;//for array of vector
	int v;
public:
	graph(int n) {
		v = n;
		l = new vector < pair <int, int> >[n];
	}

	void addEdge(int x, int y, int wt) {
		l[x].push_back({y, wt});
		l[y].push_back({x, wt});
	}

	int prim_mst() {
		//most important stuff
		priority_queue<pair<int, int>, vector<pair<int, int>>, greater<pair<int, int>>> q;
		//another array
		//visited array that denotes whether a node has been included in mst or not
		bool *visited = new bool[v] {0};

		int ans = 0;

		//begin
		q.push({0, 0});//pushing weight,node

		while (!q.empty()) {
			//pick out the edge with min weight from all available active edges
			auto best = q.top();
			q.pop();

			int to = best.second;//destinatin node
			int wt = best.first; //weight for destination node

			if (visited[to]) {
				//discard the edge and continue
				continue;
			}
			//otherwise take the current edge and mark as visited
			ans += wt;
			visited[to] = 1;

			//add the new edge in the queue which contains all the active edge
			for (auto x : l[to]) {
				if (visited[x.first] == 0) {
					q.push({x.second, x.first});//pusing wt,node in the queue
				}
			}
		}
		return ans;
	}
};

int32_t main()
{
	ios_base::sync_with_stdio(false);
	cin.tie(NULL);
	cout.tie(NULL);
	int n, m;
	cin >> n >> m;

	graph g(n);
	for (int i = 0; i < m; i++) {
		int x, y, w;
		cin >> x >> y >> w;
		g.addEdge(x - 1, y - 1, w);
	}
	cout << g.prim_mst() << endl;
}
