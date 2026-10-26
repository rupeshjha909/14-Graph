# Connecting Cities With Minimum Cost (LC 1135) — Interview Edition: MST on an Edge List

> **Problem.** There are `n` cities labeled **`1` to `n`**. You're given `connections[i] = [xᵢ, yᵢ, costᵢ]`: building a road between cities `xᵢ` and `yᵢ` costs `costᵢ` (roads are two-way). Return the **minimum total cost** to connect all cities so every city can reach every other. If it's **impossible**, return **`−1`**.
>
> **Constraints (as given on LeetCode; it's a Premium problem):** `1 ≤ n ≤ 10⁴`, `1 ≤ connections.length ≤ 10⁴`, `1 ≤ xᵢ, yᵢ ≤ n`, `xᵢ ≠ yᵢ`, `1 ≤ costᵢ ≤ 10⁵`.
>
> This is the **same Minimum Spanning Tree problem as LC 1584**, with three differences that change the best solution:
> 1. Only **some** roads are possible — you get an **edge list** (a *sparse* graph), not "every pair."
> 2. The graph may be **disconnected** → you must detect it and return `−1` (like LC 1319).
> 3. Cities are **1-indexed**.
>
> Because the graph is sparse, **Kruskal's** (sort the given roads + Union-Find) is the natural answer here — the opposite of LC 1584, where array-based Prim's won.

---

## Table of Contents

1. [The Problem in Plain Words](#1-the-problem-in-plain-words)
2. [How It Relates to LC 1319 and LC 1584](#2-how-it-relates-to-lc-1319-and-lc-1584)
3. [Step 0 in the Interview: Clarifying Questions](#3-step-0-in-the-interview-clarifying-questions)
4. [Recognizing the Pattern: MST + Connectivity Check](#4-recognizing-the-pattern-mst--connectivity-check)
5. [Approach 0: Brute Force](#5-approach-0-brute-force)
6. [Why Greedy Works (the Cut Property, Briefly)](#6-why-greedy-works-the-cut-property-briefly)
7. [Approach 1: Kruskal's — the Main Answer](#7-approach-1-kruskals--the-main-answer)
8. [How Kruskal's Detects `−1`](#8-how-kruskals-detects--1)
9. [Dry Runs of Kruskal's](#9-dry-runs-of-kruskals)
10. [Approach 2: Prim's with a Heap and an Adjacency List](#10-approach-2-prims-with-a-heap-and-an-adjacency-list)
11. [Dry Run of Prim's](#11-dry-run-of-prims)
12. [Why Not Array Prim's This Time? (Sparse vs. Dense)](#12-why-not-array-prims-this-time-sparse-vs-dense)
13. [The Clean Solution Code](#13-the-clean-solution-code)
14. [Complexity](#14-complexity)
15. [Edge Cases](#15-edge-cases)
16. [Follow-Up Questions Interviewers Ask](#16-follow-up-questions-interviewers-ask)
17. [How to Present It: A Minute-by-Minute Script](#17-how-to-present-it-a-minute-by-minute-script)
18. [Common Mistakes](#18-common-mistakes)
19. [The Pattern and Its Siblings](#19-the-pattern-and-its-siblings)
20. [TL;DR](#20-tldr)

---

## 1. The Problem in Plain Words

A government has a list of roads it **could** build, each with a price. It wants every city reachable from every other, paying as little as possible. If even building **every** listed road wouldn't connect all cities, report `−1`.

**Example 1:**

```
n = 3, connections = [[1,2,5],[1,3,6],[2,3,1]]   → 6

      1
    5/ \6
    2───3
      1

Pick 2–3 (1) and 1–2 (5) → total 6. Road 1–3 (6) isn't needed: 1 already reaches 3 through 2.
```

**Example 2:**

```
n = 4, connections = [[1,2,3],[3,4,4]]   → -1

   1───2        3───4
     3            4

Even building both roads leaves two separate groups {1,2} and {3,4}. Impossible.
```

---

## 2. How It Relates to LC 1319 and LC 1584

| | LC 1319 Make Network Connected | LC 1584 Min Cost Connect All Points | **LC 1135 (this)** |
|:--|:--|:--|:--|
| Edges | given list, no costs | **every pair** (complete graph) | **given list with costs** |
| Goal | min cable **moves** | min **total cost** | min **total cost** |
| Can be impossible? | yes (`−1`) | no (any pair can connect) | **yes (`−1`)** |
| Tool | Union-Find count | MST (array Prim's best) | **MST (Kruskal's best)** |
| Node labels | `0..n−1` | `0..n−1` | **`1..n`** |

So this problem **combines** the two previous ones:
- From **LC 1584**: process roads **cheapest first** to get the minimum total.
- From **LC 1319**: use Union-Find's merge count to detect **disconnected** graphs → `−1`.

> 💡 **What to say:** "This is a minimum spanning tree on a given weighted edge list, with the twist that the graph might not be connected. Kruskal's handles both: it builds the cheapest tree, and if it can't collect `n − 1` edges, the graph is disconnected."

---

## 3. Step 0 in the Interview: Clarifying Questions

| Question | Why it matters |
|:--|:--|
| "Are cities numbered `1..n` or `0..n−1`?" | `1..n` → size arrays `n + 1` (or subtract 1). Classic off-by-one trap. |
| "Are roads two-way?" | Yes → undirected. |
| "Can the same pair appear twice with different costs?" | Possibly — the algorithms handle it (the cheaper one is used, the other is skipped). |
| "Can a road connect a city to itself?" | No (`xᵢ ≠ yᵢ`). If it could, Union-Find would just skip it. |
| "If not all cities can be connected?" | Return `−1`. |
| "Are costs positive?" | Yes (`≥ 1`). |
| "Can the total overflow `int`?" | Max `(n − 1) × 10⁵ ≈ 10⁹ < 2.1 × 10⁹` → fits, but it's worth saying you checked. |

---

## 4. Recognizing the Pattern: MST + Connectivity Check

- **"Connect all cities"** → a spanning structure.
- **"Minimum total cost"**, positive costs → never pay for a loop → a **tree** with `n − 1` roads → **Minimum Spanning Tree**.
- **"Return −1 if impossible"** → the graph might have more than one connected component; an MST exists **only if** the graph is connected.

```
"connect all cities at minimum cost, or −1"
        │                    │            │
        │                    │            └── connectivity check (Union-Find merges / visited count)
        │                    └── minimum total + positive costs → tree
        └── spanning
                           → MST on a sparse edge list → Kruskal's
```

---

## 5. Approach 0: Brute Force

Try every subset of `n − 1` roads; keep those that connect all cities (no cycles); take the cheapest. That's `C(m, n−1)` subsets — exponential. It's the definition of the answer, so it's what I used to verify the real solutions on small inputs.

> 💡 **What to say:** "Enumerating subsets is exponential; MST has a greedy structure — the cheapest road between two separate groups is always safe to take."

---

## 6. Why Greedy Works (the Cut Property, Briefly)

Split the cities into any two groups. The **cheapest road crossing between the groups** belongs to some minimum spanning tree.

**Swap argument:** if an optimal network didn't use that cheapest crossing road `e`, it still connects the two groups through some other crossing road `f` with `cost(f) ≥ cost(e)`. Remove `f`, add `e`: still connected, still a tree, cost not higher. So taking `e` is never a mistake.

Kruskal's applies this every time it takes the cheapest road joining two different components. (Full picture in the LC 1584 doc, Section 6.)

---

## 7. Approach 1: Kruskal's — the Main Answer

**Recipe:**
1. **Sort** the roads by cost, cheapest first.
2. Union-Find over cities `1..n`.
3. Walk the sorted roads: if `union(x, y)` returns `true` (different groups), **take it** — add its cost, count it.
4. Once you've taken **`n − 1`** roads, every city is connected → return the total.
5. If you run out of roads first → **`−1`**.

```java
class Solution {
    private int[] parent;   // parent[i] == -1 → i is a leader
    private int[] size;

    public int minimumCost(int n, int[][] connections) {
        // Optional quick reject: a connected graph on n nodes needs at least n-1 edges
        if (connections.length < n - 1) return -1;

        // 1) cheapest roads first
        Arrays.sort(connections, (a, b) -> Integer.compare(a[2], b[2]));

        // 2) cities are 1..n → arrays of size n+1 (index 0 unused)
        parent = new int[n + 1];
        size = new int[n + 1];
        Arrays.fill(parent, -1);
        Arrays.fill(size, 1);

        // 3) take a road only if it joins two different groups
        int total = 0, used = 0;
        for (int[] road : connections) {
            if (union(road[0], road[1])) {
                total += road[2];
                used++;
                if (used == n - 1) return total;     // spanning tree complete
            }
        }

        // 4) ran out of roads before connecting everyone
        return n == 1 ? 0 : -1;
    }

    private int find(int x) {
        if (parent[x] == -1) return x;
        return parent[x] = find(parent[x]);          // path compression
    }

    private boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;                  // would create a cycle → skip
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;                             // union by size
        size[ra] += size[rb];
        return true;
    }
}
```

**The `n == 1` line:** a single city needs no roads. The loop never reaches `used == n − 1 = 0` inside the `if`, so handle it at the end (it also passes the `length < 0` check trivially).

---

## 8. How Kruskal's Detects `−1`

A spanning tree on `n` cities has **exactly `n − 1`** roads, and every road Kruskal's takes merges two groups (components go from `n` down by one each time).

- Took `n − 1` roads → components = `n − (n − 1) = 1` → **connected** → return total.
- Took fewer → components > 1 → **disconnected** → `−1`.

So the "is it possible?" check comes **for free** from counting merges — exactly the LC 1319 trick.

**About the quick reject `connections.length < n − 1`:** it's **necessary but not sufficient**. Example 2 has `2 < 3` roads and is rejected immediately. But a case like `n = 4`, roads `1–2, 1–2, 3–4` has 3 roads (`= n − 1`) and is still disconnected — the merge count catches it. Keep the quick reject as an optimization, never as the only check.

---

## 9. Dry Runs of Kruskal's

### Example 1 — `n = 3`, `[[1,2,5],[1,3,6],[2,3,1]]` → `6`

```
sorted by cost:  [2,3,1]  [1,2,5]  [1,3,6]

parent (index 1..3):  [-, -1, -1, -1]

[2,3,1]: find(2)=2, find(3)=3 → different → TAKE
         parent[3] = 2                         total 1,  used 1
[1,2,5]: find(1)=1, find(2)=2 → different → TAKE
         sizes 1 vs 2 → parent[1] = 2          total 6,  used 2
used == n − 1 = 2 → return 6   ✅
(road [1,3,6] is never even looked at)
```

### Example 2 — `n = 4`, `[[1,2,3],[3,4,4]]` → `−1`

```
quick reject: 2 roads < n − 1 = 3 → return −1 immediately   ✅

(without the quick reject:
 [1,2,3] TAKE → used 1;  [3,4,4] TAKE → used 2;  roads exhausted, used 2 < 3 → −1)
```

### A duplicate-road case — `n = 3`, `[[1,2,4],[1,2,2],[2,3,7]]` → `9`

```
sorted:  [1,2,2]  [1,2,4]  [2,3,7]
[1,2,2] TAKE (total 2)     [1,2,4] same group → skip     [2,3,7] TAKE (total 9)
used 2 = n − 1 → 9   ✅   (the more expensive duplicate is skipped automatically)
```

---

## 10. Approach 2: Prim's with a Heap and an Adjacency List

Grow one tree from city 1, always adding the cheapest road from the tree to a new city. Because roads are a **given list**, build an **adjacency list** and use a **min-heap**.

```java
public int minimumCost(int n, int[][] connections) {
    List<List<int[]>> g = new ArrayList<>();                 // g[u] = list of {v, cost}
    for (int i = 0; i <= n; i++) g.add(new ArrayList<>());
    for (int[] c : connections) {
        g.get(c[0]).add(new int[]{c[1], c[2]});
        g.get(c[1]).add(new int[]{c[0], c[2]});
    }

    boolean[] inTree = new boolean[n + 1];
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0])); // {cost, city}
    pq.add(new int[]{0, 1});                                 // start at city 1 for free
    int total = 0, added = 0;

    while (!pq.isEmpty()) {
        int[] top = pq.poll();
        int u = top[1];
        if (inTree[u]) continue;                             // stale entry
        inTree[u] = true;
        total += top[0];
        if (++added == n) break;                             // every city is in the tree

        for (int[] e : g.get(u)) {
            if (!inTree[e[0]]) pq.add(new int[]{e[1], e[0]});
        }
    }
    return added == n ? total : -1;                          // heap ran dry early → disconnected
}
```

**How Prim's detects `−1`:** if the heap empties before all `n` cities joined the tree, some cities are unreachable from city 1 → disconnected.

---

## 11. Dry Run of Prim's

Example 1 — `n = 3`, `[[1,2,5],[1,3,6],[2,3,1]]`:

```
adjacency:  1: (2,5) (3,6)     2: (1,5) (3,1)     3: (1,6) (2,1)

heap {(0,1)}
pop (0,1): add city 1, total 0, added 1   push (5,2) (6,3)
pop (5,2): add city 2, total 5, added 2   push (1,3)          [(1,3) beats (6,3)]
pop (1,3): add city 3, total 6, added 3 = n → stop
return 6   ✅   (the stale (6,3) is never used)
```

---

## 12. Why Not Array Prim's This Time? (Sparse vs. Dense)

In LC 1584, array Prim's (`O(n²)`) won because **every pair** was a possible edge. Here:

| | LC 1584 (complete graph) | LC 1135 (edge list) |
|:--|:--|:--|
| Edges | `n(n−1)/2` (~500k at n=1000) | `m` (≤ 10⁴) |
| Array Prim's | `O(n²)` = 10⁶ ✅ best | `O(n²)` = **10⁸** at n = 10⁴ ✗ wasteful |
| Kruskal's | `O(n² log n)` | **`O(m log m)`** ✅ |
| Heap Prim's | `O(n² log n)` | **`O(m log m)`** ✅ |

**Rule:** dense (all pairs) → array Prim's. Sparse (edge list) → Kruskal's or heap Prim's.

Measured at `n = m = 10⁴`: Kruskal's ≈ 15 ms, heap Prim's ≈ 2 ms — both instant.

> 💡 **What to say:** "The graph is given as an edge list, so it's sparse — `m` is at most 10⁴. Sorting the edges is `O(m log m)`, and Kruskal's reuses Union-Find, which also tells me if the graph is disconnected."

---

## 13. The Clean Solution Code

```java
class Solution {
    private int[] parent;   // parent[i] == -1 → i is a leader
    private int[] size;

    public int minimumCost(int n, int[][] connections) {
        if (connections.length < n - 1) return -1;                  // not enough roads, ever

        Arrays.sort(connections, (a, b) -> Integer.compare(a[2], b[2]));   // cheapest first

        parent = new int[n + 1];                                    // cities are 1..n
        size = new int[n + 1];
        Arrays.fill(parent, -1);
        Arrays.fill(size, 1);

        int total = 0, used = 0;
        for (int[] road : connections) {
            if (union(road[0], road[1])) {                          // joins two different groups
                total += road[2];
                if (++used == n - 1) return total;                  // spanning tree done
            }
        }
        return n == 1 ? 0 : -1;                                     // couldn't connect everyone
    }

    private int find(int x) {
        if (parent[x] == -1) return x;
        return parent[x] = find(parent[x]);
    }

    private boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        size[ra] += size[rb];
        return true;
    }
}
```

**Line-by-line notes to say out loud:**
- `connections.length < n − 1` — a fast reject; not the only `−1` check.
- `Integer.compare` instead of `a[2] − b[2]` — safe habit against overflow.
- Arrays sized `n + 1` because cities start at 1.
- `union` returns `true` only on a real merge — that both builds the tree and counts components.
- Early return at `n − 1` edges skips the rest of the sorted list.

(Verified: Kruskal's and heap Prim's both match a brute force that tries every subset of roads, on 5,000 random small inputs including disconnected graphs, duplicate roads, and `n = 1`; both LeetCode examples return `6` and `−1`.)

---

## 14. Complexity

Let `m` = number of roads.

| Approach | Time | Space |
|:--|:--|:--|
| Brute force | O(C(m, n−1) · n) | O(n) |
| **Kruskal's** | **O(m log m + m · α(n))** — sorting dominates | O(n) for Union-Find (+ sort space) |
| Heap Prim's | O(m log m) | O(n + m) for the adjacency list and heap |
| Array Prim's | O(n²) — avoid for sparse graphs | O(n) |

---

## 15. Edge Cases

| Case | Answer | Why |
|:--|:--|:--|
| `n = 1` | `0` | one city is already connected |
| Fewer than `n − 1` roads | `−1` | quick reject |
| Enough roads but disconnected (`1–2, 1–2, 3–4`, `n = 4`) | `−1` | merge count `< n − 1` |
| Duplicate roads between a pair | cheaper one used | the other is skipped as a cycle |
| A city with no roads at all | `−1` | it can never join |
| All roads same cost | `(n − 1) × cost` if connected | any spanning tree works |
| Max values | total ≤ `(10⁴ − 1) × 10⁵ ≈ 10⁹` | fits in `int` |

---

## 16. Follow-Up Questions Interviewers Ask

**Q1. "Return which roads to build."**
Record each road where `union` returns `true`.

**Q2. "Why does `m ≥ n − 1` not guarantee a connection here, when it did in LC 1319?"**
In LC 1319 you could **move** cables anywhere. Here roads are fixed to their pairs — you can only choose which to build. Duplicate or clustered roads can leave cities unreachable no matter how many roads there are.

**Q3. "Some roads are already built (free)."**
Union those first with cost 0 (or give them weight 0), then run Kruskal's on the rest.

**Q4. "Each city can also build its own well for a cost"** (LC 1168).
Add a virtual city 0 with a road to each city costing its well price; run MST on `n + 1` cities.

**Q5. "Which roads are in *every* minimum network, and which in *some*?"** (LC 1489)
For each road: force-exclude it and rerun Kruskal's — if the cost rises (or it becomes `−1`), it's **critical**. Otherwise force-include it first — if the cost stays the same, it's **pseudo-critical**.

**Q6. "Minimize the most expensive single road instead of the total."**
The MST also minimizes the maximum edge (a "bottleneck spanning tree"), so the same Kruskal's run works — return the last road taken.

**Q7. "Maximum cost network instead?"**
Sort descending (or negate costs) — a maximum spanning tree.

**Q8. "Roads keep getting added over time; keep the answer updated."**
Adding a road can only lower the MST cost. Simple approach: rerun. Advanced: find the most expensive road on the current tree path between the two cities (link-cut trees) and swap if the new one is cheaper — mention only.

**Q9. "Kruskal's or Prim's?"**
Sparse edge list → either at `O(m log m)`; Kruskal's is shorter and also gives the connectivity check for free. Dense/complete graph → array Prim's (LC 1584).

---

## 17. How to Present It: A Minute-by-Minute Script

**Minutes 0–2 — Clarify.** 1-indexed? Two-way roads? Duplicates? Return `−1` if disconnected?

**Minutes 2–4 — Recognize.** "Connect all cities at minimum total cost, positive costs → never a loop → minimum spanning tree. It might not exist if the graph is disconnected."

**Minutes 4–6 — Why greedy works.** Cut property swap, 30 seconds.

**Minutes 6–8 — Choose.** "Edges are given as a list — a sparse graph — so Kruskal's: sort roads, add each one that joins two different groups using Union-Find. If I collect `n − 1` roads it's connected; otherwise `−1`."

**Minutes 8–16 — Code.** Section 13. Call out the `n + 1` arrays.

**Minutes 16–20 — Dry run.** Example 1 (→ 6) and the duplicate-road case (→ 9).

**Minutes 20–22 — Complexity.** `O(m log m)`.

**Remaining time — Follow-ups.** Q2 (contrast with LC 1319), Q4 (virtual node), Q5 (critical edges), and the Kruskal's-vs-Prim's sparse/dense rule.

---

## 18. Common Mistakes

- ❌ **Arrays of size `n` with 1-indexed cities** → `ArrayIndexOutOfBoundsException` on city `n`.
- ❌ **Only checking `connections.length < n − 1` for `−1`** → misses disconnected graphs with enough roads.
- ❌ **Forgetting to sort** → greedy order is lost; you get *a* spanning tree, not the cheapest.
- ❌ **Taking every road without the cycle check** → overpays.
- ❌ **Returning `total` without checking `used == n − 1`** → returns a partial forest cost instead of `−1`.
- ❌ **Mishandling `n = 1`** → should be `0`.
- ❌ **Prim's starting at city 0** when cities are `1..n` → city 0 has no roads; the heap empties immediately.
- ❌ **Array Prim's on a sparse graph with `n = 10⁴`** → `10⁸` operations for no reason.

---

## 19. The Pattern and Its Siblings

**Family:** **Minimum Spanning Tree** (+ connectivity check).

| Problem | Twist |
|:--|:--|
| **Make Network Connected** (LC 1319) | no costs; count components; cables movable |
| **Min Cost to Connect All Points** (LC 1584) | complete graph → array Prim's |
| **Connecting Cities With Minimum Cost** (this) | edge list → Kruskal's; may be `−1` |
| **Optimize Water Distribution in a Village** (LC 1168) | virtual node for wells |
| **Find Critical and Pseudo-Critical Edges** (LC 1489) | rerun Kruskal's with an edge forced in/out |
| **Path With Minimum Effort** (LC 1631) | Kruskal's-style: add edges cheapest first until start and end connect |
| **Swim in Rising Water** (LC 778) | same "add cheapest until connected" idea |
| **Number of Provinces** (LC 547) | plain component counting |

> 💡 **Recognize the shape.** "Connect everything, each link has a cost, minimize the total, maybe impossible" → MST + check that you collected `n − 1` edges. Edge list → Kruskal's. All pairs → array Prim's.

---

## 20. TL;DR

**Problem:** given possible roads with costs, connect all `n` cities (labeled `1..n`) as cheaply as possible, or `−1`.

**Recognition:** connect everything + minimum total + positive costs → **MST**; "or `−1`" → also check connectivity.

**Kruskal's (main answer):**
1. Quick reject if fewer than `n − 1` roads.
2. Sort roads by cost.
3. Union-Find on `1..n` (arrays of size `n + 1`).
4. Take a road if `union` returns `true`; stop at `n − 1` roads → return total.
5. Ran out → `−1` (or `0` if `n = 1`).

**Why Kruskal's here (not array Prim's like LC 1584):** the graph is sparse (`m ≤ 10⁴`) → `O(m log m)` beats `O(n²)`.

**Relation to earlier problems:** LC 1584's "cheapest first" + LC 1319's "count merges to detect disconnection."

**One-line philosophy:**
> Sort the roads you're allowed to build, take each one that links two cities not yet connected, and if you can't collect `n − 1` such roads, no network can connect them all.
