import java.util.*;

public class BipartiteMatrix {

    // =====================================================================
    // Step 1: convert the adjacency matrix into an adjacency list
    // graph[u][v] == 1 means an edge between u and v (0-indexed, V x V)
    // =====================================================================
    private static List<List<Integer>> toAdjList(int[][] graph, int V) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < V; i++) adj.add(new ArrayList<>());

        for (int u = 0; u < V; u++) {
            for (int v = 0; v < V; v++) {
                if (graph[u][v] == 1) adj.get(u).add(v);   // row u already holds both directions
            }
        }
        return adj;
    }

    // =====================================================================
    // DFS version
    // color[u]: 0 = not visited, 1 = color A, 2 = color B
    // =====================================================================
    public static boolean isBipartiteDFS(int[][] graph, int V) {
        List<List<Integer>> adj = toAdjList(graph, V);
        int[] color = new int[V];

        // Loop over every vertex so disconnected components are also checked
        for (int start = 0; start < V; start++) {
            if (color[start] == 0) {
                if (!dfs(adj, start, 1, color)) return false;
            }
        }
        return true;
    }

    private static boolean dfs(List<List<Integer>> adj, int u, int col, int[] color) {
        color[u] = col;

        for (int v : adj.get(u)) {
            if (color[v] == 0) {                          // unvisited -> give opposite color
                if (!dfs(adj, v, 3 - col, color)) return false;
            } else if (color[v] == col) {                 // neighbor has SAME color -> odd cycle
                return false;
            }
        }
        return true;
    }

    // =====================================================================
    // BFS version
    // =====================================================================
    public static boolean isBipartiteBFS(int[][] graph, int V) {
        List<List<Integer>> adj = toAdjList(graph, V);
        int[] color = new int[V];
        Deque<Integer> queue = new ArrayDeque<>();

        for (int start = 0; start < V; start++) {
            if (color[start] != 0) continue;              // already colored in an earlier component

            color[start] = 1;
            queue.offer(start);

            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v : adj.get(u)) {
                    if (color[v] == 0) {                  // unvisited -> opposite color, enqueue
                        color[v] = 3 - color[u];
                        queue.offer(v);
                    } else if (color[v] == color[u]) {    // same color on both ends -> not bipartite
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------------
    // Demo
    // ---------------------------------------------------------------------
    public static void main(String[] args) {
        // Even cycle 0-1-2-3-0  -> bipartite
        int[][] square = {
            {0, 1, 0, 1},
            {1, 0, 1, 0},
            {0, 1, 0, 1},
            {1, 0, 1, 0}
        };

        // Triangle 0-1-2-0 (odd cycle) -> not bipartite
        int[][] triangle = {
            {0, 1, 1},
            {1, 0, 1},
            {1, 1, 0}
        };

        // Disconnected: edge 0-1, plus triangle 2-3-4 -> not bipartite
        int[][] disconnected = {
            {0, 1, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {0, 0, 0, 1, 1},
            {0, 0, 1, 0, 1},
            {0, 0, 1, 1, 0}
        };

        System.out.println("square:       DFS=" + isBipartiteDFS(square, 4)       + "  BFS=" + isBipartiteBFS(square, 4));
        System.out.println("triangle:     DFS=" + isBipartiteDFS(triangle, 3)     + "  BFS=" + isBipartiteBFS(triangle, 3));
        System.out.println("disconnected: DFS=" + isBipartiteDFS(disconnected, 5) + "  BFS=" + isBipartiteBFS(disconnected, 5));
    }
}
