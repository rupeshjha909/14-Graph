
class graph
{
public:
	/**
	 * Cycle Detection using Kahn's Algorithm (Topological Sort)
	 *
	 * This algorithm detects cycles in a directed graph by attempting to perform
	 * topological sorting. If a topological sort is impossible (some nodes remain
	 * unvisited), then the graph contains a cycle.
	 *
	 * Approach:
	 * 1. Calculate in-degree (incoming edges) for each node
	 * 2. Start with nodes having 0 in-degree (no dependencies)
	 * 3. Process each node, decrement in-degree of its neighbors
	 * 4. When a neighbor's in-degree becomes 0, add it to the queue
	 * 5. If all nodes are processed, no cycle exists
	 * 6. If some nodes remain unprocessed, a cycle exists
	 *
	 * Time Complexity: O(V + E) where V is vertices and E is edges
	 * Space Complexity: O(V) for storing in-degree and queue
	 *
	 * @return true if the graph contains a cycle, false otherwise
	 */
	bool isCyclic(vector<vector<int>> &adjList, int v)
	{
		// Initialize visited vector - no nodes have been visited yet
		vector<bool> visited(v, false);
		vector<int> indegree(v, 0);
		queue<int> q; // Queue for BFS
		// Calculate in-degree (incoming edges) for each node
		for (int x = 0; x < v; x++)
		{
			for (auto v : adjList[x])
			{
				indegree[v]++;
			}
		}
		// Add all nodes with in-degree 0 to the queue (these have no dependencies)
		for (int x = 0; x < v; x++)
		{
			if (indegree[x] == 0)
			{
				q.push(x);
				visited[x] = true; // Mark as visited
			}
		}

		// Process nodes in BFS order
		while (!q.empty())
		{
			auto cur = q.front();
			q.pop();

			// Process all neighbors of the current node
			for (auto nbr : adjList[cur])
			{
				indegree[nbr]--;			// Remove the edge from cur to nbr
				if (indegree[nbr] == 0) // If nbr has no more dependencies
				{
					q.push(nbr);			// Add to queue for processing
					visited[nbr] = true; // Mark as visited
				}
			}
		}

		// If any node remains unvisited, it must be part of a cycle
		// (because nodes in cycles never get their in-degree reduced to 0)
		for (int x = 0; x < v; x++)
		{
			if (visited[x] == false)
				return true; // Cycle detected
		}
		return false; // No cycle found
	}
};

int main()
{
	// Redirect input and output
	freopen("input.txt", "r", stdin);
	freopen("output.txt", "w", stdout);

	// Define edges directly as a vector of vectors
	vector<vector<int>> edges = {
		 {0, 1},
		 {0, 2},
		 {0, 3},
		 {1, 2},
		 {3, 2}};

	int n = 5;							  // Number of nodes (0-4)
	vector<vector<int>> adjList(n); // n is number of nodes
	for (auto &edge : edges)
	{
		adjList[edge[0]].push_back(edge[1]);
		adjList[edge[1]].push_back(edge[0]); // For undirected graph
	}
	graph g;
	cout << "Is cycle present? " << (g.isCyclic(adjList, n) ? "Yes" : "No") << endl;

	return 0;
}
