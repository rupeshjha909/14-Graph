== this is the algorithm for finding minimum spanning tree
== A spanning tree is a sub-graph of an undirected connected graph,   
   which includes all the vertices of the graph with a minimum possible number of edges. 
   If a vertex is missed, then it is not a spanning tree.
== A minimum spanning tree is a spanning tree in which
   the sum of the weight of the edges is as minimum as possible.



import java.util.*;

public class KruskalMST {
    
    /**
     * Disjoint Set Union (DSU) data structure with:
     *   - Path compression (in find)
     *   - Union by rank/size (in unite)
     */
    static class DSU {
        int[] parent;
        int[] rank;
        
        public DSU(int n) {
            parent = new int[n];
            rank = new int[n];
            // Initially each node is its own root (parent = -1) and rank = 1
            for (int i = 0; i < n; i++) {
                parent[i] = -1;
                rank[i] = 1;
            }
        }
        
        /**
         * Find the root of the set containing i, with path compression.
         */
        public int find(int i) {
            if (parent[i] == -1) {
                return i;
            }
            // Path compression: point i directly to root
            parent[i] = find(parent[i]);
            return parent[i];
        }
        
        /**
         * Union the sets containing x and y, using union by rank/size.
         */
        public void unite(int x, int y) {
            int set1 = find(x);
            int set2 = find(y);
            if (set1 != set2) {
                if (rank[set1] < rank[set2]) {
                    parent[set1] = set2;
                    rank[set2] += rank[set1];
                } else {
                    parent[set2] = set1;
                    rank[set1] += rank[set2];
                }
            }
        }
    }
    
    /**
     * Graph class with edge list representation (suitable for Kruskal's).
     */
    static class Graph {
        // Each edge stored as int[]{weight, x, y}
        // Weight first so natural sorting works on weight
        List<int[]> edgeList;
        int v;
        
        public Graph(int v) {
            this.v = v;
            this.edgeList = new ArrayList<>();
        }
        
        public void addEdge(int x, int y, int w) {
            edgeList.add(new int[]{w, x, y});
        }
        
        /**
         * Kruskal's MST algorithm:
         * 1. Sort all edges by weight.
         * 2. Initialize a DSU.
         * 3. For each edge in sorted order:
         *    - If the two endpoints are in different components → take edge.
         *    - Else (would create cycle) → skip.
         * 4. Return total weight of selected edges.
         */
        public int kruskalMST() {
            // Sort edges by weight (the first element of each int[])
            edgeList.sort((a, b) -> Integer.compare(a[0], b[0]));
            
            DSU dsu = new DSU(v);
            int totalWeight = 0;
            
            for (int[] edge : edgeList) {
                int w = edge[0];
                int x = edge[1];
                int y = edge[2];
                
                // Include edge only if it doesn't form a cycle
                if (dsu.find(x) != dsu.find(y)) {
                    dsu.unite(x, y);
                    totalWeight += w;
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
            // Convert to 0-indexed (1-based input → 0-based internal)
            g.addEdge(x - 1, y - 1, w);
        }
        
        System.out.println(g.kruskalMST());
        scanner.close();
    }
}




//dsu data structure
//path compression + union by rank
class dsu {
	int *parent;
	int *rank;
public:
	dsu(int n) {
		parent = new int[n];
		rank = new int[n];

		//parent -1 rank=1
		for (int i = 0; i < n; i++) {
			parent[i] = -1;
			rank[i] = 1;
		}
	}

	//find function
	int find(int i) {
		//base case
		if (parent[i] == -1)
			return i;
		return parent[i] = find(parent[i]);
	}

	//unite(union)
	void unite(int x, int y) {
		int set1 = find(x);
		int set2 = find(y);

		if (set1 != set2) {
			if (rank[set1] < rank[set2]) {
				parent[set1] = set2;
				rank[set2] += rank[set1];
			}
			else {
				parent[set2] = set1;
				rank[set1] += rank[set2];
			}
		}
	}
};

class graph {
	vector < vector<int> > edgelist;
	int v;
public:
	graph(int v) {
		this->v = v;
	}

	void addEdge(int x, int y, int w) {
		edgelist.push_back({w, x, y});
	}

	int krushkal_mst() {
		//main logic=easy
		//1.sort all the edges based upon weight
		sort(edgelist.begin(), edgelist.end());
		//initialize a dsu
		dsu s(v);
		int ans = 0;
		for (auto edge : edgelist) {
			int w = edge[0];
			int x = edge[1];
			int y = edge[2];

			//take that edge in mst if it doesnt form a cycle
			if (s.find(x) != s.find(y)) {
				s.unite(x, y);
				//add weight to the answer
				ans += w;
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
	for (int i = 0; i < m; ++i) {
		int x, y, w;
		cin >> x >> y >> w;
		g.addEdge(x - 1, y - 1, w);
	}

	cout << g.krushkal_mst() << endl;
}
