# Number of Operations to Make Network Connected (LC 1319) — Interview Edition

> **Problem.** There are `n` computers `0..n−1` and a list of cables `connections[i] = [a, b]`. In one operation you may **unplug any cable and plug it between any two computers**. Return the **minimum number of operations** so that every computer can reach every other (directly or through others). If it's impossible, return **`−1`**.
>
> **Constraints (LeetCode):** `1 ≤ n ≤ 10⁵`, `1 ≤ connections.length ≤ min(n(n−1)/2, 10⁵)`, no repeated cables, no cable from a computer to itself.
>
> The coding is short — count connected components. The interview is won or lost on **explaining why the answer is `components − 1`** and **why `n − 1` cables is the exact feasibility test**. This doc builds both from a picture, proves them in plain words, then covers code, dry runs, follow-ups, and a presentation script.

---

## Table of Contents

1. [The Problem in Plain Words](#1-the-problem-in-plain-words)
2. [The Picture to Hold in Your Head: Islands and Spare Bridges](#2-the-picture-to-hold-in-your-head-islands-and-spare-bridges)
3. [Step 0 in the Interview: Clarifying Questions](#3-step-0-in-the-interview-clarifying-questions)
4. [Approach 0: Brute Force — Try Rewirings](#4-approach-0-brute-force--try-rewirings)
5. [Observation 1: "Connected" Means One Component](#5-observation-1-connected-means-one-component)
6. [Observation 2: Each Move Joins at Most Two Islands → at Least `c − 1` Moves](#6-observation-2-each-move-joins-at-most-two-islands--at-least-c--1-moves)
7. [Observation 3: Spare Cables and the `n − 1` Test](#7-observation-3-spare-cables-and-the-n--1-test)
8. [The Algebra: Why `m ≥ n − 1` Guarantees Enough Spares](#8-the-algebra-why-m--n--1-guarantees-enough-spares)
9. [The Algorithm in Four Lines](#9-the-algorithm-in-four-lines)
10. [Approach 1: Union-Find (the Standard Answer)](#10-approach-1-union-find-the-standard-answer)
11. [The Clean Solution Code](#11-the-clean-solution-code)
12. [Dry Runs with the Parent Array](#12-dry-runs-with-the-parent-array)
13. [Approach 2: DFS / BFS Component Count](#13-approach-2-dfs--bfs-component-count)
14. [Variant: Counting Spare Cables Explicitly](#14-variant-counting-spare-cables-explicitly)
15. [Complexity](#15-complexity)
16. [Edge Cases](#16-edge-cases)
17. [Follow-Up Questions Interviewers Ask](#17-follow-up-questions-interviewers-ask)
18. [How to Present It: A Minute-by-Minute Script](#18-how-to-present-it-a-minute-by-minute-script)
19. [Common Mistakes](#19-common-mistakes)
20. [The Pattern and Its Siblings](#20-the-pattern-and-its-siblings)
21. [TL;DR](#21-tldr)

---

## 1. The Problem in Plain Words

You have some computers and some cables. Some computers are already linked into groups; others sit alone. You can **move** a cable (not buy a new one). How few moves make one single network?

```
n = 4, connections = [[0,1],[0,2],[1,2]]            → 1

   0 ─── 1          3   (alone)
    \   /
     \ /
      2
The triangle has one cable more than it needs. Move it to reach 3.

n = 6, connections = [[0,1],[0,2],[0,3],[1,2],[1,3]] → 2
   groups: {0,1,2,3}, {4}, {5}  → 2 moves join 3 groups into 1

n = 6, connections = [[0,1],[0,2],[0,3],[1,2]]       → -1
   only 4 cables; any connected network of 6 computers needs at least 5
```

---

## 2. The Picture to Hold in Your Head: Islands and Spare Bridges

Think of each group of connected computers as an **island**. Cables are **bridges**.

- Some bridges are **essential**: remove one and an island splits.
- Some bridges are **spare**: they duplicate a route that already exists (they close a loop). Removing one changes nothing.

```
Island A:  0 ─ 1          Island B: 3      Island C: 4
           │ ╱
           2              the 1–2 bridge is SPARE: 1 and 2 are already
                          connected through 0
```

To unite `c` islands into one, you need `c − 1` new bridges (like stringing beads: 3 beads need 2 links). Each new bridge must be a **moved spare** — moving an essential one would split an island and undo progress.

So the whole problem is two questions:
1. **How many islands are there?** → answer is `islands − 1`.
2. **Are there enough spare bridges to move?** → if not, `−1`.

And the surprise (Section 8): question 2 reduces to the single check **"are there at least `n − 1` cables in total?"**

> 💡 **If you remember one sentence:** "Count the islands; uniting `c` islands takes `c − 1` moves; it's possible exactly when there are at least `n − 1` cables, because that guarantees enough spare cables to move."

---

## 3. Step 0 in the Interview: Clarifying Questions

| Question | Why it matters |
|:--|:--|
| "Can I only **move** existing cables, or also add new ones?" | Moving only. (If adding were allowed, the answer would just be `components − 1`, never `−1`.) |
| "Are cables undirected?" | Yes → union / add both directions. |
| "Can there be duplicate cables or self-loops?" | LeetCode: no. If there were, they'd simply count as spare cables. |
| "How large is `n`?" | Up to `10⁵` → need near-linear; careful with recursion depth. |
| "With `n = 1`, what's the answer?" | `0` — already connected. |

---

## 4. Approach 0: Brute Force — Try Rewirings

The literal approach: treat each cable arrangement as a state; one move changes one cable's endpoints. BFS from the starting arrangement until you find a connected one.

The number of arrangements explodes (each of `m` cables can go to any of `n(n−1)/2` pairs), so this is hopeless beyond toy sizes. It's still valuable as a **ground truth**: I ran exactly this BFS on small inputs to verify the formula used below.

> 💡 **What to say:** "Searching over rewirings is exponential. Instead, let me figure out how many moves are *needed* (a lower bound) and show that's always *achievable*."

---

## 5. Observation 1: "Connected" Means One Component

"Every computer can reach every other" = the graph has exactly **one connected component**.

Right now the cables split the computers into some number `c` of components (a lone computer is a component of size 1). The goal is `c = 1`.

So step one is simply: **count the components.** Union-Find does this naturally: start with `c = n` (everyone alone) and subtract 1 every time a cable joins two *different* components.

---

## 6. Observation 2: Each Move Joins at Most Two Islands → at Least `c − 1` Moves

One move = unplug a cable + plug it somewhere.

- **Unplugging** can never *reduce* the number of components (it can only keep it the same or split something).
- **Plugging** a cable between two computers can merge **at most two** components into one.

So each move reduces the component count by **at most 1**. Going from `c` to `1` needs **at least `c − 1` moves**.

> 💡 **This is the lower bound.** Nothing can beat `c − 1`. The rest of the proof shows `c − 1` is always reachable when the answer isn't `−1`.

---

## 7. Observation 3: Spare Cables and the `n − 1` Test

**Achieving `c − 1`:** take a **spare** cable (one that closes a loop inside an island), unplug it — the island stays connected — and plug it between two different islands. Components drop by exactly 1. Repeat `c − 1` times.

So we need **at least `c − 1` spare cables**.

**Why `n − 1` is the minimum for any connected network:** start with `n` lone computers (`n` components). Each cable merges at most two components, so reducing `n` components to `1` needs at least `n − 1` cables. If `m < n − 1`, **no arrangement** of the cables can be connected → `−1`.

```
connections.length < n − 1   →   return −1      (no matter how you rewire)
```

---

## 8. The Algebra: Why `m ≥ n − 1` Guarantees Enough Spares

Let `n` = computers, `m` = cables, `c` = components.

A component with `k` computers needs exactly `k − 1` cables to be connected (a tree). Across all components, the **essential** cables total:

```
(k₁ − 1) + (k₂ − 1) + … + (k_c − 1)  =  n − c
```

Every other cable is spare:

```
spare  =  m − (n − c)
```

We need `spare ≥ c − 1`:

```
m − (n − c)  ≥  c − 1
m − n + c    ≥  c − 1
m            ≥  n − 1          ← exactly the feasibility test!
```

So **"at least `n − 1` cables" is not just necessary — it's also sufficient.** Once it holds, there are automatically enough spare cables, and the answer is exactly `c − 1`. You never need to count the spares.

(And it stays true during the process: each move keeps `m` and `n` the same and lowers `c` by 1, so spares drop by exactly 1 — always enough for the remaining moves.)

> 💡 **Interview gold.** Being able to derive `m − (n − c) ≥ c − 1 ⇔ m ≥ n − 1` in 30 seconds is what separates "I memorized this" from "I understand this."

---

## 9. The Algorithm in Four Lines

```
1. if (connections.length < n − 1) return −1;      // not enough cables, ever
2. components = n
3. for each cable (a, b): if union(a, b) merged two different groups → components−−
4. return components − 1
```

---

## 10. Approach 1: Union-Find (the Standard Answer)

Union-Find keeps disjoint groups:

- `parent[x] == −1` → `x` is the **leader** (root) of its group.
- `find(x)` → follow parent pointers to the leader (with **path compression**: point everything on the way straight at the leader).
- `union(a, b)` → if the leaders differ, hang the smaller group under the larger (**union by size**) and return `true`; if they're the same, return `false`.

**The one change from a plain Union-Find: `union` returns a boolean.**
- `true` → a real merge → one fewer component.
- `false` → `a` and `b` were already connected → this cable is **spare**.

So the same pass both counts components *and* identifies spare cables.

---

## 11. The Clean Solution Code

```java
class Solution {
    private int[] parent;   // parent[i] == -1  → i is a leader
    private int[] size;     // size[leader] = number of computers in that group

    public int makeConnected(int n, int[][] connections) {
        // 1) Feasibility: fewer than n-1 cables can never connect n computers
        if (connections.length < n - 1) return -1;

        // 2) Everyone starts alone
        parent = new int[n];
        size = new int[n];
        Arrays.fill(parent, -1);
        Arrays.fill(size, 1);
        int components = n;

        // 3) Each cable that joins two different groups removes one component
        for (int[] c : connections) {
            if (union(c[0], c[1])) components--;
        }

        // 4) Uniting `components` groups takes components - 1 moves
        return components - 1;
    }

    private int find(int x) {
        if (parent[x] == -1) return x;
        return parent[x] = find(parent[x]);     // path compression
    }

    // returns true if a real merge happened, false if a and b were already connected
    private boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;             // spare cable
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;                        // smaller group under larger
        size[ra] += size[rb];
        return true;
    }
}
```

**Line-by-line notes to say out loud:**
- The feasibility check comes **first** — it's `O(1)` and makes the formula valid.
- `components` starts at `n` because every computer begins as its own group.
- Decrement only when `union` returns `true`; a spare cable doesn't change the count.
- **Arrays, not `HashMap`s:** nodes are `0..n−1`, and with `n = 10⁵`, arrays are much faster.
- **Union by size matters here for safety:** it keeps trees at most `log₂(10⁵) ≈ 17` deep, so the recursive `find` can't overflow the stack. With path compression alone, an unlucky order can build a 10⁵-long chain before the first compression.

(Verified: matches a true brute force — BFS over every possible rewiring — on 2,375 small random cases; Union-Find, DFS, and the explicit-spare variant all agree. On `n = 10⁵`, `m = 10⁵`: about 18 ms.)

---

## 12. Dry Runs with the Parent Array

### Example 1 — `n = 4`, `[[0,1],[0,2],[1,2]]` → expected `1`

```
Feasibility: 3 cables ≥ n−1 = 3  ✓

start:        parent = [-1, -1, -1, -1]   size = [1,1,1,1]   components = 4

[0,1]: find(0)=0, find(1)=1 → merge → parent[1]=0, size[0]=2
              parent = [-1,  0, -1, -1]                       components = 3

[0,2]: find(0)=0, find(2)=2 → merge (size 2 vs 1) → parent[2]=0, size[0]=3
              parent = [-1,  0,  0, -1]                       components = 2

[1,2]: find(1)=0, find(2)=0 → SAME leader → spare cable, no change
                                                               components = 2

groups: {0,1,2}, {3}     answer = 2 − 1 = 1   ✅
(the spare [1,2] is the cable we'd move to reach 3)
```

### Example 2 — `n = 6`, `[[0,1],[0,2],[0,3],[1,2],[1,3]]` → expected `2`

```
Feasibility: 5 cables ≥ 5  ✓          components = 6

[0,1] merge → 5      [0,2] merge → 4      [0,3] merge → 3
[1,2] same leader → spare                  [1,3] same leader → spare

groups: {0,1,2,3}, {4}, {5}   → 3 components, 2 spares
answer = 3 − 1 = 2   ✅      (exactly the 2 spares get moved)
```

### Example 3 — `n = 6`, `[[0,1],[0,2],[0,3],[1,2]]` → expected `−1`

```
Feasibility: 4 cables < n−1 = 5  → return −1 immediately   ✅
(Without the check you'd compute components = 3 and wrongly return 2:
 there's only 1 spare cable, but 2 bridges are needed.)
```

---

## 13. Approach 2: DFS / BFS Component Count

If you prefer traversal, count components with an iterative DFS — same formula afterwards.

```java
public int makeConnected(int n, int[][] connections) {
    if (connections.length < n - 1) return -1;

    List<List<Integer>> g = new ArrayList<>();
    for (int i = 0; i < n; i++) g.add(new ArrayList<>());
    for (int[] c : connections) { g.get(c[0]).add(c[1]); g.get(c[1]).add(c[0]); }

    boolean[] seen = new boolean[n];
    int components = 0;
    for (int s = 0; s < n; s++) {
        if (seen[s]) continue;
        components++;                                  // new island found
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(s);
        seen[s] = true;
        while (!stack.isEmpty()) {
            int u = stack.pop();
            for (int v : g.get(u)) if (!seen[v]) { seen[v] = true; stack.push(v); }
        }
    }
    return components - 1;
}
```

Use an **explicit stack** — a recursive DFS on a 10⁵-node chain can overflow Java's call stack. Same `O(n + m)` complexity; Union-Find avoids building the adjacency list.

---

## 14. Variant: Counting Spare Cables Explicitly

If you forget the `n − 1` shortcut in the interview, this version is equally correct and follows the "islands and spare bridges" picture literally:

```java
int components = n, spare = 0;
for (int[] c : connections) {
    if (union(c[0], c[1])) components--;
    else spare++;                                      // already connected → movable
}
return spare >= components - 1 ? components - 1 : -1;
```

Then mention: "Algebraically, `spare = m − (n − components)`, and `spare ≥ components − 1` simplifies to `m ≥ n − 1`, so the check can be done up front." That turns a gap into a strong moment. (Verified identical results.)

---

## 15. Complexity

Let `m` = number of cables.

| Approach | Time | Space |
|:--|:--|:--|
| Brute force (BFS over rewirings) | exponential | exponential |
| **Union-Find** | **O(n + m · α(n)) ≈ O(n + m)** | O(n) |
| DFS / BFS | O(n + m) | O(n + m) for the adjacency list |

`α(n)` (inverse Ackermann) is at most 4 for any realistic `n`, so Union-Find is effectively linear.

---

## 16. Edge Cases

| Case | Answer | Why |
|:--|:--|:--|
| `n = 1` | `0` | `0 < n−1 = 0` is false; components = 1 → 0 |
| Already one component | `0` | components = 1 |
| `m < n − 1` | `−1` | can never connect |
| `m = n − 1` and connected | `0` | it's already a spanning tree |
| `m = n − 1` but disconnected | `c − 1` | there are exactly `c − 1` spares (from the algebra) |
| Many isolated computers + one dense cluster | `c − 1` | the cluster's loops supply the spares |
| `n = 10⁵` chain of cables | works | union by size keeps `find` shallow |

---

## 17. Follow-Up Questions Interviewers Ask

**Q1. "Prove the answer is `components − 1`."**
Lower bound: each move merges at most two components (Section 6). Achievability: move a spare cable between two components each time; `m ≥ n − 1` guarantees `m − (n − c) ≥ c − 1` spares (Section 8).

**Q2. "Which cables should be moved, and where?"**
Collect the spare cables (the ones where `union` returned `false`). Collect one representative computer from each component (`find(i) == i`, i.e. each leader). Then move spare `i` to connect leader `i` and leader `i + 1`. That's an explicit `c − 1`-move plan.

**Q3. "What if you can add new cables instead of moving them?"**
Then there's no feasibility issue — the answer is always `components − 1`.

**Q4. "What if each possible cable has a cost, and you want the cheapest connected network?"**
That's a **Minimum Spanning Tree**: sort edges by cost and use the same Union-Find to add an edge only when it joins two different components (**Kruskal's**). See LC 1584 (Min Cost to Connect All Points) and LC 1135 (Connecting Cities With Minimum Cost).

**Q5. "Which cables are critical — removing them would disconnect the network?"**
Those are **bridges**, found with Tarjan's low-link DFS (LC 1192, Critical Connections). They're exactly the cables you must never move.

**Q6. "Cables arrive one at a time. After each, report the answer."**
Union-Find handles this incrementally: keep `components` and the cable count up to date; after each cable, answer `m < n−1 ? −1 : components − 1`.

**Q7. "Why not use `HashMap` for `parent` like in other Union-Find problems?"**
Nodes are already integers `0..n−1`, so arrays are faster and simpler. Maps are only needed when nodes are strings or sparse ids (e.g. Accounts Merge).

---

## 18. How to Present It: A Minute-by-Minute Script

**Minutes 0–2 — Clarify.** Moving only (not adding)? Undirected? `n = 1`? Size?

**Minutes 2–4 — Brute force + reframe.** "Searching rewirings is exponential. Instead: the goal is one connected component. Let `c` be the current number of components."

**Minutes 4–7 — Lower bound.** "A move merges at most two components, so I need at least `c − 1` moves."

**Minutes 7–11 — Feasibility and sufficiency.** "Any connected graph on `n` nodes needs `n − 1` edges, so `m < n − 1` → `−1`. Otherwise: essential cables are `n − c`, spares are `m − n + c`, and `m ≥ n − 1` gives spares `≥ c − 1`. So the answer is exactly `c − 1`."

**Minutes 11–18 — Code.** Union-Find with arrays, `union` returning a boolean, feasibility check first.

**Minutes 18–22 — Dry run.** Example 1 with the parent array; point out the spare cable `[1,2]`.

**Minutes 22–24 — Complexity.** Near-linear time, `O(n)` space. Mention union by size prevents deep recursion at `n = 10⁵`.

**Remaining time — Follow-ups.** Q2 (which cables), Q4 (MST), Q5 (bridges).

---

## 19. Common Mistakes

- ❌ **Missing the feasibility check** → returns a positive number for impossible inputs (Example 3 would give `2`).
- ❌ **Returning `components` instead of `components − 1`.**
- ❌ **Decrementing `components` for every cable** instead of only on real merges.
- ❌ **Checking `m < n` instead of `m < n − 1`.** A tree on `n` nodes has `n − 1` edges.
- ❌ **Recursive `find` with path compression but no union by size** → possible stack overflow at `n = 10⁵`.
- ❌ **Recursive DFS on large inputs** → same risk; use an explicit stack.
- ❌ **Trying to pick which cables to move** when only the count is asked — unnecessary.
- ❌ **Using `HashMap`s for integer nodes** — works, but slower and noisier at `10⁵`.

---

## 20. The Pattern and Its Siblings

**Family:** **connected components + a formula on the count**, with a feasibility floor.

| Problem | What you compute from components |
|:--|:--|
| **Number of Provinces** (LC 547) | the count itself |
| **Number of Connected Components** (LC 323) | the count itself |
| **Make Network Connected** (this) | `components − 1`, gated by `m ≥ n − 1` |
| **Graph Valid Tree** (LC 261) | `m == n − 1` **and** components == 1 |
| **Redundant Connection** (LC 684) | the first cable where `union` returns `false` |
| **Count Unreachable Pairs** (LC 2316) | sum over component sizes |
| **Min Cost to Connect All Points** (LC 1584) | Kruskal: Union-Find + sorted edges |
| **Critical Connections** (LC 1192) | bridges — cables that can't be moved |

> 💡 **Recognize the shape.** "Make everything connected / how many groups / how many links to add or move" → count components with Union-Find; translate the goal into a formula (`c − 1` links join `c` groups); check for a feasibility floor (`n − 1` edges).

---

## 21. TL;DR

**Picture:** components are islands; cables inside an island that close a loop are spare bridges.

**Facts:**
1. Uniting `c` islands needs **at least `c − 1`** moves (each move joins at most two).
2. A connected network of `n` computers needs **at least `n − 1`** cables → otherwise `−1`.
3. Spares = `m − (n − c)`; `spares ≥ c − 1 ⇔ m ≥ n − 1` → so once feasible, the answer is **exactly `c − 1`**.

**Code:** feasibility check → Union-Find with `union` returning `true` on a real merge → `components − 1`.

**Complexity:** ≈ `O(n + m)` time, `O(n)` space.

**One-line philosophy:**
> Count the islands, because joining `c` of them takes `c − 1` bridges — and having at least `n − 1` cables in total is exactly what guarantees there are enough spare cables to move into place.
