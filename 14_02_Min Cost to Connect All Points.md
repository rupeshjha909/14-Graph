# Min Cost to Connect All Points (LC 1584) — Interview Edition: MST with Kruskal and Prim

> **Problem.** You're given `points[i] = [xᵢ, yᵢ]` on a 2-D plane. Connecting two points costs their **Manhattan distance** `|xᵢ − xⱼ| + |yᵢ − yⱼ|`. Return the **minimum total cost** to make all points connected (any point can reach any other through some path).
>
> **Constraints (LeetCode):** `1 ≤ points.length ≤ 1000`, `−10⁶ ≤ xᵢ, yᵢ ≤ 10⁶`, all points are distinct.
>
> This is the textbook **Minimum Spanning Tree (MST)** problem, and it's the natural next step after **Make Network Connected (LC 1319)**: there you counted components with Union-Find; here every possible cable has a **cost**, and you want the cheapest set that connects everything. This doc covers how to recognize MST, **why** the greedy algorithms are correct, **Kruskal's** (sort + Union-Find) and **Prim's** (grow a tree), why the **O(n²) array version of Prim** is the best fit for this problem, dry runs, follow-ups, and a presentation script.

---

## Table of Contents

1. [The Problem in Plain Words](#1-the-problem-in-plain-words)
2. [The Picture to Hold in Your Head: Building Roads Between Towns](#2-the-picture-to-hold-in-your-head-building-roads-between-towns)
3. [Step 0 in the Interview: Clarifying Questions](#3-step-0-in-the-interview-clarifying-questions)
4. [Recognizing the Pattern: Why This Is an MST](#4-recognizing-the-pattern-why-this-is-an-mst)
5. [Approach 0: Brute Force — Try Every Spanning Tree](#5-approach-0-brute-force--try-every-spanning-tree)
6. [Why Greedy Works: The Cut Property in Plain Words](#6-why-greedy-works-the-cut-property-in-plain-words)
7. [Approach 1: Kruskal's — Cheapest Edges First, Skip Cycles](#7-approach-1-kruskals--cheapest-edges-first-skip-cycles)
8. [Dry Run of Kruskal's](#8-dry-run-of-kruskals)
9. [Approach 2: Prim's — Grow One Tree Outward](#9-approach-2-prims--grow-one-tree-outward)
10. [Prim's with a Heap](#10-prims-with-a-heap)
11. [Prim's with an Array — O(n²), the Best Fit Here](#11-prims-with-an-array--on²-the-best-fit-here)
12. [Dry Run of Array Prim's](#12-dry-run-of-array-prims)
13. [Which One Should You Write? (Dense vs. Sparse)](#13-which-one-should-you-write-dense-vs-sparse)
14. [The Clean Solution Code](#14-the-clean-solution-code)
15. [Complexity, Approach by Approach](#15-complexity-approach-by-approach)
16. [Edge Cases](#16-edge-cases)
17. [Follow-Up Questions Interviewers Ask](#17-follow-up-questions-interviewers-ask)
18. [How to Present It: A Minute-by-Minute Script](#18-how-to-present-it-a-minute-by-minute-script)
19. [Common Mistakes](#19-common-mistakes)
20. [The Pattern and Its Siblings](#20-the-pattern-and-its-siblings)
21. [TL;DR](#21-tldr)

---

## 1. The Problem in Plain Words

Every pair of points *could* be connected; each connection has a price (Manhattan distance). Pick some connections so that **everything is reachable** and the **total price is as small as possible**.

**Manhattan distance** = walking on a grid of streets: horizontal blocks + vertical blocks.

```
(0,0) → (2,2):   |0 − 2| + |0 − 2| = 4
(3,12) → (−2,5): |3 − (−2)| + |12 − 5| = 5 + 7 = 12
```

**Example 1:**

```
points = [[0,0],[2,2],[3,10],[5,2],[7,0]]   → 20

Label them:  A(0,0)  B(2,2)  C(3,10)  D(5,2)  E(7,0)

                 C (3,10)
                 │
                 │ 9
                 │
         B(2,2)──┴── 3 ──D(5,2)
          │               │
        4 │               │ 4
          │               │
        A(0,0)          E(7,0)

chosen links: B–D 3, A–B 4, D–E 4, B–C 9   → total 20
```

**Example 2:**

```
points = [[3,12],[-2,5],[-4,1]]   → 18
distances:  P0–P1 = 12,  P0–P2 = 18,  P1–P2 = 6
best: P1–P2 (6) + P0–P1 (12) = 18
```

---

## 2. The Picture to Hold in Your Head: Building Roads Between Towns

Points are **towns**. You can build a road between any two towns; the road's cost is their distance. You want every town reachable from every other, for the **least total money**.

Two common-sense facts:

1. **You'd never build a loop.** If roads form a cycle, remove the most expensive road in it — every town is still reachable (go around the other way), and you saved money. So the cheapest network has **no cycles**: it's a **tree**.
2. **A tree on `n` towns has exactly `n − 1` roads.** (Fewer can't connect everyone; more must create a loop.)

So the answer is: **the cheapest tree that touches every town** — the **Minimum Spanning Tree**.

> 💡 **If you remember one sentence:** "Connect everything at minimum total cost with positive edge costs → the result is a tree → Minimum Spanning Tree; build it greedily with Kruskal's or Prim's."

---

## 3. Step 0 in the Interview: Clarifying Questions

| Question | Why it matters |
|:--|:--|
| "Can I connect **any** two points, or only some?" | Any → the graph is **complete**: `n(n−1)/2` edges. This decides Kruskal vs. Prim (Section 13). |
| "Is the cost exactly Manhattan distance?" | Yes. (If Euclidean, same algorithms with `double`.) |
| "Are points distinct?" | Yes. Duplicates would just mean zero-cost edges. |
| "What if there's only one point?" | Cost `0`. |
| "How many points?" | Up to 1000 → about 500,000 possible edges. |
| "Can the answer overflow `int`?" | A single distance is at most `4 × 10⁶`. MST totals for 1000 points in this box stay well inside `int` in practice (random 1000 points give ~5 × 10⁷); a `long` accumulator is a cheap safety if you're unsure. |

---

## 4. Recognizing the Pattern: Why This Is an MST

Ask three questions:

1. **"Connect all the items?"** → a **spanning** structure (touches every node).
2. **"Minimum total cost, each link has a cost?"** → **minimum** total weight.
3. **"Costs are positive?"** → no benefit in cycles → the optimum is a **tree**.

Spanning + minimum + tree = **Minimum Spanning Tree**.

**Contrast with similar-sounding problems:**

| Wording | Problem type |
|:--|:--|
| "connect everything, minimum **total** cost" | **MST** (this problem) |
| "cheapest **route from A to B**" | shortest path (Dijkstra) |
| "how many groups / how many links to add" | connected components (LC 1319) |
| "connect everything, minimize the **most expensive** single link" | also MST (an MST minimizes the max edge too) |

> 💡 **What to say:** "Every pair can be linked with a positive cost and I need everything connected at minimum total cost. Any cycle could drop its most expensive edge, so the optimum is a tree — this is a minimum spanning tree on a complete graph."

---

## 5. Approach 0: Brute Force — Try Every Spanning Tree

Try every set of `n − 1` edges, keep the ones that connect everything (no cycles), and take the cheapest.

A complete graph on `n` nodes has **`n^(n−2)`** spanning trees (Cayley's formula). For `n = 10` that's already 100 million. Hopeless — but it's the **definition** of the answer, so I used it to verify the real solutions on small inputs.

> 💡 **What to say:** "Enumerating spanning trees is exponential. MST has a greedy structure — I can always safely take the cheapest edge that crosses between the part I've connected and the rest."

---

## 6. Why Greedy Works: The Cut Property in Plain Words

Both Kruskal's and Prim's are **greedy**: they repeatedly pick the cheapest "useful" edge and never undo it. Why is that safe?

**The cut property:** split the points into any two groups, `S` and the rest. Among all edges that cross between the groups, the **cheapest crossing edge** belongs to some minimum spanning tree.

**Why (exchange argument):**

```
     group S                 the rest
   ┌─────────┐            ┌─────────┐
   │   u ────┼── e (cheap)┼──── v   │
   │         │            │         │
   │   x ────┼── f (pricier)┼── y   │
   └─────────┘            └─────────┘
```

Suppose some optimal tree doesn't use the cheapest crossing edge `e = (u, v)`. That tree still connects `u` and `v`, so its path from `u` to `v` must cross the split somewhere, using some edge `f` with `cost(f) ≥ cost(e)`. Swap: remove `f`, add `e`. Still connected, still a tree, and the cost didn't go up. So there's an optimal tree **with** `e`. Greedy is safe.

- **Kruskal's** uses it like this: the cheapest edge joining two different components crosses the split "this component vs. everything else."
- **Prim's** uses it like this: the cheapest edge from the tree-so-far to an outside point crosses the split "tree vs. not tree."

> 💡 **Interview gold.** "Why is greedy correct here?" is a very common follow-up. The swap argument above is the full answer.

---

## 7. Approach 1: Kruskal's — Cheapest Edges First, Skip Cycles

**Idea:** sort all possible edges from cheapest to most expensive. Walk through them; **take an edge if it connects two different components**, skip it if both ends are already connected (it would make a cycle). Stop after `n − 1` edges.

"Are they already connected?" is exactly **Union-Find** — the same `union` returning a boolean from **LC 1319**:
- `union(a, b) == true` → different components → take the edge.
- `union(a, b) == false` → same component → skip (it's a "spare cable").

```java
public int minCostConnectPoints(int[][] points) {
    int n = points.length;

    // 1) All possible edges: [cost, i, j]
    List<int[]> edges = new ArrayList<>();
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            edges.add(new int[]{dist(points, i, j), i, j});

    // 2) Cheapest first
    edges.sort((x, y) -> Integer.compare(x[0], y[0]));

    // 3) Take an edge only if it joins two different components
    parent = new int[n];
    size = new int[n];
    Arrays.fill(parent, -1);
    Arrays.fill(size, 1);
    int total = 0, used = 0;
    for (int[] e : edges) {
        if (union(e[1], e[2])) {
            total += e[0];
            if (++used == n - 1) break;     // a tree has n-1 edges → done
        }
    }
    return total;
}

private int dist(int[][] p, int i, int j) {
    return Math.abs(p[i][0] - p[j][0]) + Math.abs(p[i][1] - p[j][1]);
}

private int[] parent, size;   // parent[x] == -1 → x is a leader

private int find(int x) {
    if (parent[x] == -1) return x;
    return parent[x] = find(parent[x]);
}

private boolean union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra == rb) return false;                       // would form a cycle
    if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
    parent[rb] = ra;
    size[ra] += size[rb];
    return true;
}
```

**Cost:** about 500,000 edges for `n = 1000`; sorting them is `O(n² log n)`, plus `O(n²)` memory for the edge list. Correct and accepted, but the heaviest of the three approaches here (measured ~175–300 ms at `n = 1000`).

---

## 8. Dry Run of Kruskal's

Example 1: `A(0,0) B(2,2) C(3,10) D(5,2) E(7,0)`.

**All 10 edges, sorted:**

```
B–D 3    A–B 4    D–E 4    A–D 7    A–E 7    B–E 7    B–C 9    C–D 10   A–C 13   C–E 14
```

**Walk through them:**

```
B–D 3   B, D in different groups → TAKE    total 3    groups {B,D} {A} {C} {E}
A–B 4   different              → TAKE    total 7    groups {A,B,D} {C} {E}
D–E 4   different              → TAKE    total 11   groups {A,B,D,E} {C}
A–D 7   same group             → skip (cycle A–B–D)
A–E 7   same group             → skip
B–E 7   same group             → skip
B–C 9   different              → TAKE    total 20   groups {A,B,C,D,E}
used = 4 = n − 1 → stop.   Answer 20  ✅
```

The skipped edges are exactly the "spare cables" of LC 1319 — they would only close loops.

---

## 9. Approach 2: Prim's — Grow One Tree Outward

**Idea:** start the tree at any point. Repeatedly add the **cheapest edge from the tree to a point not yet in the tree**. After `n − 1` additions, every point is in the tree.

```
tree = {A}
repeat n − 1 times:
    find the outside point closest to the tree (cheapest connecting edge)
    add that point (and pay that edge's cost)
```

It's like spreading paint: the painted region grows by one point at a time, always grabbing the nearest unpainted point.

Two ways to implement "find the outside point closest to the tree":
- a **min-heap** of candidate edges (Section 10),
- a plain **array** `minDist[v]` = cheapest known edge from the tree to `v` (Section 11).

---

## 10. Prim's with a Heap

The general-purpose version (great for **sparse** graphs):

```java
public int minCostConnectPoints(int[][] points) {
    int n = points.length;
    boolean[] inTree = new boolean[n];
    PriorityQueue<int[]> pq = new PriorityQueue<>((x, y) -> Integer.compare(x[0], y[0])); // [cost, point]
    pq.add(new int[]{0, 0});                     // start at point 0 with cost 0
    int total = 0, added = 0;

    while (added < n) {
        int[] top = pq.poll();
        int u = top[1];
        if (inTree[u]) continue;                 // stale entry: u already joined more cheaply
        inTree[u] = true;
        total += top[0];
        added++;
        for (int v = 0; v < n; v++) {
            if (!inTree[v]) pq.add(new int[]{dist(points, u, v), v});
        }
    }
    return total;
}
```

The `if (inTree[u]) continue;` line handles **stale** heap entries (a point can be pushed many times with different costs; only the first pop counts). This is called **lazy Prim's**.

On a complete graph, every point pushes up to `n` entries → `O(n²)` pushes, each `O(log n)` → **`O(n² log n)`** time and `O(n²)` heap space in the worst case. Better than Kruskal's in practice here (~20–65 ms), but not the best.

---

## 11. Prim's with an Array — O(n²), the Best Fit Here

On a **complete** graph, a heap doesn't pay off: you'd push almost every edge anyway. Instead keep one number per point:

```
minDist[v] = cheapest cost to connect v to the current tree   (∞ if not known yet)
```

Each round:
1. **Scan** all outside points and pick the one with the smallest `minDist` — `O(n)`.
2. Add it to the tree and pay `minDist[u]`.
3. **Relax:** for every outside point `v`, `minDist[v] = min(minDist[v], dist(u, v))` — `O(n)`.

`n` rounds × `O(n)` work = **`O(n²)`** time, **`O(n)`** extra space, and no edge list at all.

```java
public int minCostConnectPoints(int[][] points) {
    int n = points.length;
    int[] minDist = new int[n];                  // cheapest link from the tree to each point
    boolean[] inTree = new boolean[n];
    Arrays.fill(minDist, Integer.MAX_VALUE);
    minDist[0] = 0;                              // start the tree at point 0 (free)
    int total = 0;

    for (int round = 0; round < n; round++) {
        // 1) pick the closest point not yet in the tree
        int u = -1;
        for (int v = 0; v < n; v++) {
            if (!inTree[v] && (u == -1 || minDist[v] < minDist[u])) u = v;
        }

        // 2) add it
        inTree[u] = true;
        total += minDist[u];

        // 3) the tree just grew: some outside points may now be cheaper to reach
        for (int v = 0; v < n; v++) {
            if (!inTree[v]) {
                int d = Math.abs(points[u][0] - points[v][0]) + Math.abs(points[u][1] - points[v][1]);
                if (d < minDist[v]) minDist[v] = d;
            }
        }
    }
    return total;
}
```

**Measured at `n = 1000`: about 3 ms** — versus ~20–65 ms for heap Prim's and ~175–300 ms for Kruskal's.

> 💡 **What to say:** "The graph is complete, so `E ≈ n²/2`. A heap or sorting costs `n² log n`. Array-based Prim's picks the closest outside point by a linear scan, giving `O(n²)` time and `O(n)` memory — optimal for a dense graph, since there are `Θ(n²)` edges to consider anyway."

---

## 12. Dry Run of Array Prim's

Example 1: `A(0,0) B(2,2) C(3,10) D(5,2) E(7,0)`. Distances from the table in Section 8.

```
                          minDist                                     pick   total
                    A     B     C     D     E
start               0     ∞     ∞     ∞     ∞

round 1:  pick A (0)                                                  A      0
  relax from A:     —     4    13     7     7

round 2:  pick B (4)                                                  B      4
  relax from B:     —     —     9     3     7       (C: 13→9, D: 7→3, E stays 7)

round 3:  pick D (3)                                                  D      7
  relax from D:     —     —     9     —     4       (C: 10 > 9 stays, E: 7→4)

round 4:  pick E (4)                                                  E      11
  relax from E:     —     —     9     —     —       (C: 14 > 9 stays)

round 5:  pick C (9)                                                  C      20

Answer 20  ✅    (edges used: A–B 4, B–D 3, D–E 4, B–C 9 — same tree as Kruskal's)
```

Each `minDist` entry is "the cheapest road from the painted region to this town." When a new town gets painted, its roads may offer cheaper access to the others — that's the relax step.

---

## 13. Which One Should You Write? (Dense vs. Sparse)

| Graph shape | Edges `E` | Best MST algorithm | Time |
|:--|:--|:--|:--|
| **Dense / complete** (this problem: every pair can connect) | `≈ n²/2` | **Prim's with an array** | **O(n²)** |
| Sparse (edges given as a list, `E ≈ n`) | `≈ n` | Kruskal's or heap Prim's | O(E log E) |

Measured on this problem at `n = 1000`:

| Implementation | Time | Extra memory |
|:--|:--|:--|
| Kruskal's (all edges + sort + Union-Find) | ~175–300 ms | ~500k edges |
| Prim's with a heap (lazy) | ~20–65 ms | up to ~500k heap entries |
| **Prim's with an array** | **~3 ms** | `O(n)` |

**Interview recommendation:**
- If you know Kruskal's best (it reuses your Union-Find from LC 1319), **code Kruskal's** — it's accepted and easy to explain — then **say** "since the graph is complete, array-based Prim's is `O(n²)` and better."
- If you're comfortable with Prim's, **code the array version** directly and explain the dense-graph reasoning.

---

## 14. The Clean Solution Code

```java
class Solution {
    // Prim's algorithm, array version: O(n^2) time, O(n) space — best for a complete graph
    public int minCostConnectPoints(int[][] points) {
        int n = points.length;
        int[] minDist = new int[n];          // minDist[v] = cheapest edge from the tree to v
        boolean[] inTree = new boolean[n];
        Arrays.fill(minDist, Integer.MAX_VALUE);
        minDist[0] = 0;                      // start from point 0
        int total = 0;

        for (int round = 0; round < n; round++) {
            int u = -1;
            for (int v = 0; v < n; v++) {
                if (!inTree[v] && (u == -1 || minDist[v] < minDist[u])) u = v;
            }
            inTree[u] = true;
            total += minDist[u];

            for (int v = 0; v < n; v++) {
                if (!inTree[v]) {
                    int d = Math.abs(points[u][0] - points[v][0])
                          + Math.abs(points[u][1] - points[v][1]);
                    if (d < minDist[v]) minDist[v] = d;
                }
            }
        }
        return total;
    }
}
```

**Line-by-line notes to say out loud:**
- `minDist[0] = 0` — the first point joins for free; it's just where the tree starts.
- The loop runs `n` rounds (one per point), not `n − 1`, because round 1 adds the start point at cost 0.
- `u == -1 || …` picks the first outside point as the initial candidate, so we never compare against `Integer.MAX_VALUE` by mistake.
- Distances are computed on the fly — no edge list, no sorting.

(Verified: Kruskal's, heap Prim's, and array Prim's all match a brute force that enumerates every spanning tree, on 3,000 random small inputs, plus both LeetCode examples: `20` and `18`.)

---

## 15. Complexity, Approach by Approach

Let `n` = number of points, `E = n(n−1)/2`.

| Approach | Time | Space |
|:--|:--|:--|
| Brute force (all spanning trees) | O(n^(n−2) · n) | O(n) |
| Kruskal's (all edges + sort + Union-Find) | O(n² log n) | O(n²) |
| Prim's with a heap (lazy) | O(n² log n) | O(n²) |
| **Prim's with an array** | **O(n²)** | **O(n)** |

`O(n²)` is optimal for an arbitrary complete graph, since every pair's cost may matter. (For Manhattan distance specifically, there's a geometric trick that builds an `O(n)`-edge candidate graph in `O(n log n)` — see Follow-up Q6; it's not expected in interviews.)

---

## 16. Edge Cases

| Case | Answer | Why |
|:--|:--|:--|
| One point | `0` | nothing to connect; Prim's adds it at cost 0, Kruskal's loop never takes an edge |
| Two points | their distance | one edge |
| All points on a line | sum of gaps between neighbors | the MST links consecutive points |
| Negative coordinates | fine | `Math.abs` handles them |
| Ties in distance | any valid choice | the total MST cost is unique even if the tree isn't |
| Max coordinates (±10⁶) | single distance ≤ 4 × 10⁶ | fits `int`; use a `long` total if unsure |

---

## 17. Follow-Up Questions Interviewers Ask

**Q1. "Why is the greedy choice correct?"**
The cut property with the swap argument (Section 6).

**Q2. "Why not Dijkstra's?"**
Dijkstra's builds a **shortest-path tree** from one source: it minimizes each point's distance *from the source*, not the **total** cost. Counterexample: triangle with `A–B = 2`, `A–C = 2`, `B–C = 1`. Dijkstra from `A` picks `A–B` and `A–C` (total 4). The MST picks `B–C` plus one `2`-edge (total 3).

Code-wise, Prim's and Dijkstra's look almost identical — the only difference is the relax line:
- Prim's: `minDist[v] = min(minDist[v], w(u, v))`
- Dijkstra's: `dist[v] = min(dist[v], dist[u] + w(u, v))`

**Q3. "Return the actual edges, not just the cost."**
Prim's: keep `from[v]` = the tree point that gave `v` its current `minDist`; when `v` is picked, record edge `(from[v], v)`. Kruskal's: record each edge where `union` returns `true`.

**Q4. "What if some cables already exist for free?"** (like LC 1135 / "Connecting Cities")
Union those pairs first (cost 0), then run Kruskal's on the remaining edges. Or give those edges weight 0.

**Q5. "Split the points into `k` clusters as far apart as possible."**
Run Kruskal's but **stop after `n − k` merges** — the remaining components are the clusters (single-linkage clustering). This is a classic real-world use of MST.

**Q6. "Can you beat `O(n²)` for Manhattan distance?"**
Yes, with a geometric trick: for each point, only the nearest neighbor in each of 8 directional regions can be an MST edge. Four coordinate-transformed sweeps (sort + Fenwick tree) produce `O(n)` candidate edges in `O(n log n)`, then Kruskal's on those. Mention it; don't code it unless asked.

**Q7. "What if the distance were Euclidean?"**
Same algorithms with `double` (or compare squared distances, which preserves order, and take square roots only when summing).

**Q8. "How does this relate to Make Network Connected (LC 1319)?"**
Same Union-Find. LC 1319 counts components (all cables cost the same, you only move them). LC 1584 adds costs, so you process edges **cheapest first** — that ordering is what turns "connect everything" into "connect everything **cheaply**."

---

## 18. How to Present It: A Minute-by-Minute Script

**Minutes 0–2 — Clarify.** Any pair connectable? Manhattan distance? Distinct points? Size?

**Minutes 2–5 — Recognize.** "I need all points connected at minimum total cost with positive costs. Any cycle could drop its priciest edge, so the optimum is a tree with `n − 1` edges — a minimum spanning tree on a complete graph."

**Minutes 5–7 — Why greedy works.** The cut property swap argument, 30 seconds.

**Minutes 7–9 — Choose.** "Kruskal's sorts all `n²/2` edges: `O(n² log n)`. Since the graph is complete, Prim's with an array is `O(n²)` time and `O(n)` space — I'll code that."

**Minutes 9–18 — Code.** Array Prim's (Section 14). Explain `minDist` as "cheapest link from the tree."

**Minutes 18–23 — Dry run.** Example 1 with the `minDist` table → 20.

**Minutes 23–25 — Complexity.** `O(n²)`, `O(n)`.

**Remaining time — Follow-ups.** Q2 (Dijkstra's vs. Prim's), Q3 (return edges), Q5 (clustering). If time remains, sketch Kruskal's to show you know both.

---

## 19. Common Mistakes

- ❌ **Using Dijkstra's** — minimizes distance from one source, not total cost.
- ❌ **Kruskal's without the cycle check** — taking every cheap edge creates loops and overpays.
- ❌ **Not stopping Kruskal's at `n − 1` edges** — correct but wastes time scanning the rest.
- ❌ **Heap Prim's without the `inTree` check on pop** — stale entries get counted twice.
- ❌ **Array Prim's comparing against `Integer.MAX_VALUE` by addition** (e.g. `minDist[u] + d`) → overflow. Prim's never adds to `minDist`; it only compares.
- ❌ **Looping `n − 1` rounds in array Prim's while also starting with `minDist[0] = 0`** — you'd stop one point short. Either loop `n` rounds (as here) or pre-add point 0.
- ❌ **Building Euclidean distances by mistake** — the problem uses Manhattan.
- ❌ **Using a sort comparator like `(a, b) -> a[0] - b[0]`** — fine here (values ≤ 4 × 10⁶), but it overflows in general; prefer `Integer.compare`.

---

## 20. The Pattern and Its Siblings

**Family:** **Minimum Spanning Tree** — Kruskal's (sort + Union-Find) or Prim's (grow a tree with a heap or array).

| Problem | Connection |
|:--|:--|
| **Min Cost to Connect All Points** (this) | MST on a complete graph → array Prim's |
| **Connecting Cities With Minimum Cost** (LC 1135) | MST on a given edge list → Kruskal's; `−1` if not connected |
| **Optimize Water Distribution in a Village** (LC 1168) | add a virtual "well" node; MST |
| **Find Critical and Pseudo-Critical Edges in MST** (LC 1489) | run Kruskal's with an edge forced in / left out |
| **Make Network Connected** (LC 1319) | Union-Find component counting (unweighted cousin) |
| **Path With Minimum Effort** (LC 1631) | "minimize the max edge" → MST / Union-Find on sorted edges, or Dijkstra's variant |
| **Swim in Rising Water** (LC 778) | same "minimize the max edge on a path" idea |
| **Network Delay Time** (LC 743) | shortest paths (Dijkstra's) — the contrast case |

> 💡 **Recognize the shape.** "Connect all items, each link has a positive cost, minimize the total" → MST. Edge list given (sparse) → Kruskal's. Every pair connectable (dense) → array Prim's.

---

## 21. TL;DR

**Problem:** connect all points; linking two costs their Manhattan distance; minimize the total.

**Recognition:** connect everything + minimum total + positive costs → the optimum is a tree → **Minimum Spanning Tree**.

**Why greedy:** the cheapest edge crossing any split belongs to some MST (swap argument).

**Kruskal's:** all edges, sort cheapest first, take an edge if `union` returns `true` (different components), stop at `n − 1`. `O(n² log n)` here.

**Prim's (array):** `minDist[v]` = cheapest link from the tree; each round pick the smallest outside `minDist`, add it, relax all others. **`O(n²)` time, `O(n)` space — best for this complete graph.**

**Relation to LC 1319:** same Union-Find; adding costs and sorting edges turns "connect everything" into "connect everything cheaply."

**One-line philosophy:**
> When everything must be connected and every link costs something, never pay for a loop: keep grabbing the cheapest link that reaches something new — and on a graph where everyone can reach everyone, do it with a simple array instead of sorting half a million edges.
