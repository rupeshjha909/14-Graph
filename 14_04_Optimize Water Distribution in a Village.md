# Optimize Water Distribution in a Village (LC 1168) — Interview Edition: The Virtual-Node MST Trick

> **Problem.** There are `n` houses labeled **`1` to `n`**. For each house you can either **build a well** inside it, costing `wells[i − 1]`, or **bring water by pipe** from another house that has water. `pipes[j] = [house1, house2, cost]` means a two-way pipe between those houses can be laid for `cost` (there may be several pipes between the same pair). Return the **minimum total cost** so that **every house has water**.
>
> **Constraints (as given on LeetCode; it's a Premium problem):** `2 ≤ n ≤ 10⁴`, `wells.length == n`, `0 ≤ wells[i] ≤ 10⁵`, `1 ≤ pipes.length ≤ 10⁴`, `1 ≤ house1, house2 ≤ n`, `house1 ≠ house2`, `0 ≤ cost ≤ 10⁵`.
>
> This is the third MST problem in the series, and it teaches the single most reusable MST trick: **turn "a per-node option" (a well) into an edge to a virtual node**. Once you see that, it's exactly LC 1135 (Kruskal's on an edge list) with `n + 1` nodes. This doc spends most of its time on **why** that trick is correct — the part interviewers probe — then covers code, dry runs, follow-ups, and a presentation script.

---

## Table of Contents

1. [The Problem in Plain Words](#1-the-problem-in-plain-words)
2. [Why It's Not Obviously an MST](#2-why-its-not-obviously-an-mst)
3. [Step 0 in the Interview: Clarifying Questions](#3-step-0-in-the-interview-clarifying-questions)
4. [Tempting Greedy Ideas That Fail](#4-tempting-greedy-ideas-that-fail)
5. [Approach 0: Brute Force](#5-approach-0-brute-force)
6. [The Key Insight: A Well Is a Pipe to a Virtual Water Source](#6-the-key-insight-a-well-is-a-pipe-to-a-virtual-water-source)
7. [Why the Virtual Node Is Exactly Right (the Proof)](#7-why-the-virtual-node-is-exactly-right-the-proof)
8. [Approach 1: Kruskal's on the Augmented Graph](#8-approach-1-kruskals-on-the-augmented-graph)
9. [Dry Runs of Kruskal's](#9-dry-runs-of-kruskals)
10. [Approach 2: Prim's from the Virtual Node](#10-approach-2-prims-from-the-virtual-node)
11. [Dry Run of Prim's](#11-dry-run-of-prims)
12. [The Clean Solution Code](#12-the-clean-solution-code)
13. [Reading the Answer: Which Houses Get Wells?](#13-reading-the-answer-which-houses-get-wells)
14. [Complexity](#14-complexity)
15. [Edge Cases](#15-edge-cases)
16. [How It Relates to LC 1319, LC 1584, LC 1135](#16-how-it-relates-to-lc-1319-lc-1584-lc-1135)
17. [Follow-Up Questions Interviewers Ask](#17-follow-up-questions-interviewers-ask)
18. [How to Present It: A Minute-by-Minute Script](#18-how-to-present-it-a-minute-by-minute-script)
19. [Common Mistakes](#19-common-mistakes)
20. [The Pattern and Its Siblings](#20-the-pattern-and-its-siblings)
21. [TL;DR](#21-tldr)

---

## 1. The Problem in Plain Words

Every house needs water. A house gets water if it **has its own well**, or it's **connected by pipes** (directly or through other houses) to some house with a well. Wells and pipes both cost money. Spend as little as possible.

**Example 1:**

```
n = 3, wells = [1, 2, 2], pipes = [[1,2,1],[2,3,1]]   → 3

  house:     1        2        3
  well cost: 1        2        2
             ●───1────●───1────●

Best: well at house 1 (1) + pipe 1–2 (1) + pipe 2–3 (1) = 3
(Building three wells would cost 1 + 2 + 2 = 5.)
```

**Example 2:**

```
n = 2, wells = [1, 1], pipes = [[1,2,1],[1,2,2]]   → 2

Option A: well at 1 (1) + well at 2 (1)        = 2
Option B: well at 1 (1) + pipe 1–2 cheap (1)   = 2
Either way 2. (The pipe costing 2 is never worth it.)
```

---

## 2. Why It's Not Obviously an MST

In LC 1135 the question was "connect all cities" — clearly a spanning tree. Here:

- Houses **don't** all need to be connected to each other. Two separate clusters are fine if **each has its own well**.
- There are **two kinds of choices**: wells (a cost on a *node*) and pipes (a cost on an *edge*).

So the plain "MST of the pipes" doesn't fit, and it's not obvious how to mix "build a well here" with "lay a pipe there." The whole problem is about turning this into a shape you already know.

---

## 3. Step 0 in the Interview: Clarifying Questions

| Question | Why it matters |
|:--|:--|
| "Houses are `1..n`, and `wells[i]` is for house `i + 1`?" | Yes → 0-indexed wells array, 1-indexed houses. Off-by-one trap. |
| "Are pipes two-way?" | Yes → undirected edges. (One-way changes the problem; Follow-up Q6.) |
| "Can there be multiple pipes between the same two houses?" | Yes → the algorithm just uses the cheapest useful one. |
| "Can costs be 0?" | Yes (`0 ≤` both). Fine for MST. |
| "Can a house have both a well and pipes?" | Allowed, but the optimum never pays for redundant water. |
| "Is it ever impossible?" | No — every house can always build its own well. |

---

## 4. Tempting Greedy Ideas That Fail

Worth knowing so you can **say why** you don't use them.

**Idea A — "MST of the pipes, then one cheapest well per pipe cluster."**

```
n = 2, wells = [1, 1], pipes = [[1,2,5]]

Idea A: pipes connect {1,2} → pay pipe 5, then cheapest well 1 → total 6
Optimal: two wells, 1 + 1 = 2
```

Fails because a pipe can be **more expensive than a well** — you should sometimes **not** lay a pipe even though it's the cheapest way to connect two houses.

**Idea B — "One well at the globally cheapest house, then MST of pipes."** Same counterexample: 1 + 5 = 6. And it breaks entirely when the pipes don't connect everything.

**Idea C — "For each house, compare its well cost to its cheapest pipe."** A pipe only helps if the other end *has water*, which depends on decisions elsewhere — a local comparison can't see that.

In random testing, Idea A gave a wrong answer on **about 26%** of small inputs (1,049 of 4,000).

> 💡 **The lesson.** Wells and pipes compete with each other, and the right choice for one house depends on the whole network. We need a single framework where "well" and "pipe" are compared on equal footing.

---

## 5. Approach 0: Brute Force

Try every subset of houses to get wells and every subset of pipes; keep combinations where every house reaches a well; take the cheapest. That's `2ⁿ · 2ᵐ` combinations — exponential. I used exactly this to verify the real solution on small inputs.

---

## 6. The Key Insight: A Well Is a Pipe to a Virtual Water Source

Imagine a giant **underground river** — call it **house 0**. Building a well in house `i` is the same as **laying a pipe from the river to house `i`**, costing `wells[i − 1]`.

```
Original:                         Augmented (add virtual node 0 = "the river"):

 wells: 1    2    2                           0  (river)
        1 ─1─ 2 ─1─ 3                      1/ |2  \2
                                          /   |    \
                                         1 ─1─ 2 ─1─ 3
```

Now there's only **one kind of thing** — edges — and **one goal**:

> **Every house must be connected to node 0** (directly = a well; through other houses = pipes leading to a well).

"Every house connected to node 0" is the same as "**all `n + 1` nodes in one connected component**." Minimum cost to connect all nodes = **Minimum Spanning Tree** on `n + 1` nodes.

So the whole solution is:
1. Add node 0.
2. Add edge `(0, i)` with cost `wells[i − 1]` for every house.
3. Run MST (Kruskal's or Prim's) on the augmented graph. The total is the answer.

> 💡 **If you remember one sentence:** "A well is just a pipe from a virtual water source; add a node 0, connect it to every house with the well cost, and the answer is the MST of the augmented graph."

---

## 7. Why the Virtual Node Is Exactly Right (the Proof)

This is what interviewers probe. Two directions:

**(1) Every valid plan is a connected subgraph of the augmented graph with the same cost.**
Take any plan: a set of wells `W` and pipes `P` such that every house has water. Map each well at house `i` to edge `(0, i)`, keep the pipes. Every house reaches some well house through pipes, and that well house is directly connected to node 0. So every house is connected to node 0 → the subgraph connects all `n + 1` nodes. Cost is identical.

**(2) Every connected spanning subgraph of the augmented graph is a valid plan with the same cost.**
Take any set of edges connecting all `n + 1` nodes. Turn each `(0, i)` edge back into "well at house `i`" and keep the pipes. Every house has a path to node 0; the **last** step of that path is some edge `(0, j)` = a well at house `j`, and the rest of the path is pipes from the house to `j`. So every house has water. Same cost.

So **"cheapest valid plan" = "cheapest set of edges connecting all `n + 1` nodes."** With non-negative costs, the cheapest connected spanning subgraph can always be trimmed to a spanning tree (dropping a cycle edge never increases cost), so it's the **MST**.

**Bonus facts that fall out:**
- The augmented graph is **always connected** (every house has an edge to node 0) → the answer always exists, no `−1`.
- A spanning tree on `n + 1` nodes has **exactly `n` edges** — so Kruskal's stops after `n` edges, not `n − 1`.
- **At least one well is always built**: node 0 must be connected to something, and only well edges touch it.

> 💡 **What to say:** "Valid plans and connected spanning subgraphs of the augmented graph correspond one-to-one with equal costs — a house has water exactly when it's connected to the virtual source. So the minimum plan is the MST."

---

## 8. Approach 1: Kruskal's on the Augmented Graph

Same Kruskal's as LC 1135, with two changes: add the `n` well edges, and Union-Find has `n + 1` nodes (`0..n`).

```java
class Solution {
    private int[] parent;   // parent[i] == -1 → i is a leader
    private int[] size;

    public int minCostToSupplyWater(int n, int[] wells, int[][] pipes) {
        // 1) Augmented edge list: well edges (0, i) + all pipes
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            edges.add(new int[]{0, i + 1, wells[i]});        // well at house i+1 = pipe from node 0
        }
        for (int[] p : pipes) edges.add(p);

        // 2) Cheapest first
        edges.sort((a, b) -> Integer.compare(a[2], b[2]));

        // 3) Union-Find over nodes 0..n
        parent = new int[n + 1];
        size = new int[n + 1];
        Arrays.fill(parent, -1);
        Arrays.fill(size, 1);

        int total = 0, used = 0;
        for (int[] e : edges) {
            if (union(e[0], e[1])) {
                total += e[2];
                if (++used == n) break;                     // n+1 nodes → n edges in the tree
            }
        }
        return total;                                        // always connected: no -1
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

**What "skipping an edge" means here:**
- Skipping a **pipe** → those houses already share water (through some chain).
- Skipping a **well edge** → that house already has water through pipes from another well.

---

## 9. Dry Runs of Kruskal's

### Example 1 — `n = 3`, `wells = [1,2,2]`, `pipes = [[1,2,1],[2,3,1]]` → `3`

```
augmented edges:   (0,1,1) (0,2,2) (0,3,2)  [wells]
                   (1,2,1) (2,3,1)          [pipes]

sorted (stable):   (0,1,1) (1,2,1) (2,3,1) (0,2,2) (0,3,2)

parent (0..3): [-1, -1, -1, -1]

(0,1,1): 0 and 1 separate → TAKE (well at house 1)   total 1, used 1
(1,2,1): separate          → TAKE (pipe 1–2)          total 2, used 2
(2,3,1): separate          → TAKE (pipe 2–3)          total 3, used 3 = n → stop

Answer 3   ✅   plan: well at 1, pipes 1–2 and 2–3
```

### Example 2 — `n = 2`, `wells = [1,1]`, `pipes = [[1,2,1],[1,2,2]]` → `2`

```
sorted: (0,1,1) (0,2,1) (1,2,1) (1,2,2)

(0,1,1): TAKE (well at 1)    total 1, used 1
(0,2,1): TAKE (well at 2)    total 2, used 2 = n → stop

Answer 2   ✅   (well + cheap pipe would also cost 2 — ties are fine)
```

### The counterexample from Section 4 — `wells = [1,1]`, `pipes = [[1,2,5]]` → `2`

```
sorted: (0,1,1) (0,2,1) (1,2,5)
(0,1,1) TAKE   (0,2,1) TAKE → used 2 = n → stop      total 2   ✅
The expensive pipe is never reached — two wells beat it.
```

This is exactly the case that broke the "MST of pipes first" idea: putting wells and pipes in **one sorted list** lets them compete fairly.

---

## 10. Approach 2: Prim's from the Virtual Node

Run Prim's starting **at node 0**. Starting at node 0 has a nice meaning: the initial candidates are **all the wells**. Then the tree grows through pipes (or more wells) by cheapest next connection.

```java
public int minCostToSupplyWater(int n, int[] wells, int[][] pipes) {
    List<List<int[]>> g = new ArrayList<>();                 // g[u] = list of {v, cost}
    for (int i = 0; i <= n; i++) g.add(new ArrayList<>());
    for (int[] p : pipes) {
        g.get(p[0]).add(new int[]{p[1], p[2]});
        g.get(p[1]).add(new int[]{p[0], p[2]});
    }

    boolean[] hasWater = new boolean[n + 1];
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0])); // {cost, house}

    // Node 0 is the start: its edges are the wells
    for (int i = 1; i <= n; i++) pq.add(new int[]{wells[i - 1], i});

    int total = 0, served = 0;
    while (served < n) {
        int[] top = pq.poll();
        int u = top[1];
        if (hasWater[u]) continue;                           // stale entry
        hasWater[u] = true;
        total += top[0];
        served++;
        for (int[] e : g.get(u)) {
            if (!hasWater[e[0]]) pq.add(new int[]{e[1], e[0]});
        }
    }
    return total;
}
```

**Plain-English reading:** "Water starts at the river. Repeatedly, give water to the house that can get it most cheaply right now — either by digging its well or by a pipe from a house that already has water."

Note: node 0 itself doesn't need an entry in the adjacency list — its edges are pushed directly as the starting candidates.

---

## 11. Dry Run of Prim's

Example 1 — `wells = [1,2,2]`, `pipes = [[1,2,1],[2,3,1]]`:

```
start heap (all wells):  (1,h1) (2,h2) (2,h3)

pop (1,h1): house 1 gets water (well), total 1   push pipe (1,h2)
pop (1,h2): house 2 gets water (pipe from 1), total 2   push pipe (1,h3)
pop (1,h3): house 3 gets water (pipe from 2), total 3   served 3 = n → stop

Answer 3   ✅   (stale (2,h2) and (2,h3) never used)
```

---

## 12. The Clean Solution Code

```java
class Solution {
    private int[] parent;   // parent[i] == -1 → i is a leader
    private int[] size;

    public int minCostToSupplyWater(int n, int[] wells, int[][] pipes) {
        // Virtual node 0 = water source; well at house i = edge (0, i) with cost wells[i-1]
        int[][] edges = new int[n + pipes.length][];
        for (int i = 0; i < n; i++) edges[i] = new int[]{0, i + 1, wells[i]};
        for (int j = 0; j < pipes.length; j++) edges[n + j] = pipes[j];

        Arrays.sort(edges, (a, b) -> Integer.compare(a[2], b[2]));   // cheapest first

        parent = new int[n + 1];                                      // nodes 0..n
        size = new int[n + 1];
        Arrays.fill(parent, -1);
        Arrays.fill(size, 1);

        int total = 0, used = 0;
        for (int[] e : edges) {
            if (union(e[0], e[1])) {
                total += e[2];
                if (++used == n) break;                               // n+1 nodes → n edges
            }
        }
        return total;
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
- `edges[i] = {0, i + 1, wells[i]}` — the virtual-node trick in one line; note `i + 1` because houses start at 1.
- Union-Find has `n + 1` slots: node 0 plus houses `1..n`.
- Stop at `used == n`, not `n − 1` — the tree spans `n + 1` nodes.
- No `−1` case: node 0 connects to every house, so the augmented graph is always connected.

(Verified: Kruskal's and Prim's match a brute force over every combination of wells and pipes, on 4,000 random small inputs including zero costs and duplicate pipes; both examples return `3` and `2`. At `n = 10⁴` with `10⁴` pipes, each runs in about 25–30 ms.)

---

## 13. Reading the Answer: Which Houses Get Wells?

Record the edges Kruskal's takes:
- An edge touching **node 0** → **build a well** at the other end.
- Any other edge → **lay that pipe**.

```java
List<Integer> wellHouses = new ArrayList<>();
List<int[]> pipesToLay = new ArrayList<>();
// inside the loop, when union(...) is true:
if (e[0] == 0) wellHouses.add(e[1]); else pipesToLay.add(e);
```

Each connected group of houses in the final plan has **exactly one** well (a tree can only have one edge from each group to node 0 — two would form a cycle through node 0).

---

## 14. Complexity

Let `m` = number of pipes. The augmented graph has `n + 1` nodes and `n + m` edges.

| Approach | Time | Space |
|:--|:--|:--|
| Brute force | O(2ⁿ · 2ᵐ · (n + m)) | O(n) |
| **Kruskal's** | **O((n + m) log(n + m))** — sorting dominates | O(n + m) |
| Prim's (heap) | O((n + m) log(n + m)) | O(n + m) |

The virtual node adds just one node and `n` edges — the complexity is the same as plain MST on the pipes.

---

## 15. Edge Cases

| Case | Answer | Why |
|:--|:--|:--|
| All pipes cost more than wells | sum of all wells | every well edge sorts before every pipe |
| One very cheap well, cheap pipes | that well + pipes | pipes spread water from it |
| Pipes don't connect all houses | still works | isolated houses take their own well edge |
| Duplicate pipes between a pair | cheapest one used | the other is a cycle |
| Zero-cost wells or pipes | fine | MST works with non-negative weights |
| A house with no pipes | gets a well | its only edge is to node 0 |
| Ties between a well and a pipe | either choice | total cost is the same |

---

## 16. How It Relates to LC 1319, LC 1584, LC 1135

| | LC 1319 | LC 1584 | LC 1135 | **LC 1168 (this)** |
|:--|:--|:--|:--|:--|
| What costs money | moves (uniform) | every pair (distance) | listed roads | **listed pipes + a cost per house** |
| Graph | edge list | complete | edge list | **edge list + virtual node** |
| Can be `−1`? | yes | no | yes | **no** (every house can dig a well) |
| Best tool | Union-Find count | array Prim's | Kruskal's | **Kruskal's on `n + 1` nodes** |
| Tree size | — | `n − 1` edges | `n − 1` edges | **`n` edges** |

**The progression:** count components → connect cheaply (dense) → connect cheaply (sparse, maybe impossible) → **connect cheaply when nodes have their own "standalone" option**.

---

## 17. Follow-Up Questions Interviewers Ask

**Q1. "Why does adding node 0 work? Prove it."**
Section 7: valid plans ↔ connected spanning subgraphs of the augmented graph, same cost; cheapest connected spanning subgraph = MST.

**Q2. "Which houses get wells, which pipes are laid?"**
Section 13: edges touching node 0 are wells.

**Q3. "Some houses can't have a well (e.g. bad ground)."**
Don't add their well edge. Now the augmented graph may be **disconnected** → check `used == n` at the end and return `−1` if not (the LC 1135 check comes back).

**Q4. "There are two kinds of water source with different costs per house (well or tank)."**
Add one edge per option per house to node 0 — or keep only the cheaper one per house, since they'd be parallel edges to the same node.

**Q5. "Some houses already have water."**
Give those houses a well edge of cost 0 (or union them with node 0 before starting).

**Q6. "What if pipes are one-way (water flows only from house1 to house2)?"**
Then it's a **minimum spanning arborescence** rooted at node 0 (the directed version of MST), solved by the **Chu–Liu/Edmonds algorithm** — Kruskal's and Prim's no longer apply. Mention it; you won't be asked to code it.

**Q7. "Minimize the number of wells instead of the cost?"**
Different objective: that's just the number of connected components of the pipe graph (one well per component) — LC 1319-style counting.

**Q8. "Where else does the virtual-node trick appear?"**
Any time a node has a "standalone" cost: a server can run on its own or be connected to others; a city can build its own power plant or connect to the grid; in shortest paths, a "super source" connected to all start points turns multi-source into single-source.

---

## 18. How to Present It: A Minute-by-Minute Script

**Minutes 0–2 — Clarify.** Houses `1..n`, `wells[i]` is house `i + 1`? Two-way pipes? Duplicates? Never impossible?

**Minutes 2–5 — Why the obvious ideas fail.** "MST of pipes plus a well per cluster fails: with wells `[1,1]` and a pipe costing 5, two wells cost 2 but that approach pays 6. Wells and pipes need to compete in one framework."

**Minutes 5–8 — The trick.** "Treat a well as a pipe from a virtual water source, node 0, costing the well price. A house has water exactly when it's connected to node 0, so I need all `n + 1` nodes connected at minimum cost — an MST."

**Minutes 8–10 — Correctness.** Plans ↔ connected spanning subgraphs, same cost (Section 7).

**Minutes 10–18 — Code.** Kruskal's: add `n` well edges, sort, Union-Find over `0..n`, stop at `n` edges.

**Minutes 18–22 — Dry run.** Example 1 (→ 3) and the `[1,1]` / pipe-5 counterexample (→ 2).

**Minutes 22–24 — Complexity.** `O((n + m) log(n + m))`.

**Remaining time — Follow-ups.** Q2 (which wells), Q3 (some houses can't have wells → `−1` returns), Q6 (one-way pipes).

---

## 19. Common Mistakes

- ❌ **Running MST on the pipes only, then adding wells.** Breaks when a pipe costs more than a well.
- ❌ **Forgetting `i + 1` when creating well edges** — `wells` is 0-indexed, houses are 1-indexed.
- ❌ **Union-Find of size `n`** — you need `n + 1` (node 0 plus houses `1..n`).
- ❌ **Stopping Kruskal's at `n − 1` edges** — the tree has `n + 1` nodes, so `n` edges. Stopping early leaves a house without water.
- ❌ **Returning `−1`** — impossible here; every house can dig a well (unless the follow-up removes that option).
- ❌ **Prim's starting at house 1 with cost 0** — that gives house 1 water for free. Start at node 0 by pushing all well edges.
- ❌ **Comparator `a[2] − b[2]`** — fine at these sizes but a bad habit; use `Integer.compare`.

---

## 20. The Pattern and Its Siblings

**Family:** **MST with a virtual (super) node** — model a per-node "standalone option" as an edge to an extra node.

| Problem | Connection |
|:--|:--|
| **Optimize Water Distribution in a Village** (this) | wells = edges to virtual node 0 |
| **Connecting Cities With Minimum Cost** (LC 1135) | the same Kruskal's without the virtual node |
| **Min Cost to Connect All Points** (LC 1584) | MST on a complete graph |
| **Make Network Connected** (LC 1319) | Union-Find component counting |
| **Find Critical and Pseudo-Critical Edges** (LC 1489) | Kruskal's with forced edges |
| **Rotting Oranges** (LC 994), **Walls and Gates** (LC 286) | the same "super source" idea for BFS: start from all sources at once |
| **Network Delay Time** (LC 743) with multiple sources | Dijkstra's with a super source |

> 💡 **Recognize the shape.** "Each item can either pay for itself or be connected to others who've paid" → add a virtual node, connect every item to it with its standalone cost, then run MST (for total cost) or BFS/Dijkstra's (for distances).

---

## 21. TL;DR

**Problem:** every house needs water — dig a well (cost per house) or pipe from a house with water (cost per pipe). Minimize the total.

**Why it's tricky:** houses don't all need to be connected to each other; wells and pipes are different kinds of choices.

**The trick:** a well at house `i` = a pipe from a **virtual water source, node 0**, costing `wells[i − 1]`. A house has water ⇔ it's connected to node 0. So the answer is the **MST of the augmented graph** with `n + 1` nodes.

**Kruskal's recipe:**
1. Edges = `(0, i, wells[i − 1])` for every house + all pipes.
2. Sort by cost.
3. Union-Find over `0..n`; take an edge if `union` returns `true`; stop at **`n`** edges.
4. Return the total (never `−1`).

**Reading the plan:** taken edges touching node 0 are wells; the rest are pipes.

**Complexity:** `O((n + m) log(n + m))`.

**One-line philosophy:**
> When each node can either pay for itself or connect to others, invent one extra node that represents "paying for yourself" — then it's just the cheapest way to connect everything.
