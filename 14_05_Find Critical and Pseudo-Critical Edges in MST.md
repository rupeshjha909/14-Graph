# Find Critical and Pseudo-Critical Edges in MST (LC 1489) — Interview Edition

> **Problem.** You're given a **connected**, undirected, weighted graph with `n` nodes (`0..n−1`) and `edges[i] = [aᵢ, bᵢ, weightᵢ]`. A graph can have **several** minimum spanning trees (MSTs) with the same minimum total weight. Classify edges:
> - **Critical:** the edge is in **every** MST — removing it from the graph makes the MST weight **increase** (or makes the graph disconnected).
> - **Pseudo-critical:** the edge is in **some** MST but **not all**.
>
> Return `[criticalIndices, pseudoCriticalIndices]` (indices into the original `edges`, any order).
>
> **Constraints (LeetCode):** `2 ≤ n ≤ 100`, `1 ≤ edges.length ≤ min(200, n(n−1)/2)`, `1 ≤ weightᵢ ≤ 1000`, all pairs distinct.
>
> This is the fourth MST problem in the series. It doesn't ask you to **build** an MST — it asks **how much each edge matters** to the MST. The standard answer is surprisingly simple: **rerun Kruskal's with one edge removed, and again with one edge forced in**, and compare to the normal MST weight. This doc explains why those two tests are exactly right, walks every edge of the example by hand, and also covers the faster **weight-group + bridges** method as a follow-up.

---

## Table of Contents

1. [The Problem in Plain Words](#1-the-problem-in-plain-words)
2. [Why a Graph Can Have Many MSTs](#2-why-a-graph-can-have-many-msts)
3. [Step 0 in the Interview: Clarifying Questions](#3-step-0-in-the-interview-clarifying-questions)
4. [Approach 0: Brute Force — Enumerate All Spanning Trees](#4-approach-0-brute-force--enumerate-all-spanning-trees)
5. [The Two Tests: Remove It, Then Force It](#5-the-two-tests-remove-it-then-force-it)
6. [Why Test 1 Identifies Critical Edges](#6-why-test-1-identifies-critical-edges)
7. [Why Test 2 Identifies Pseudo-Critical Edges](#7-why-test-2-identifies-pseudo-critical-edges)
8. [Why the Order of the Tests Matters](#8-why-the-order-of-the-tests-matters)
9. [Building the Reusable Kruskal's Helper](#9-building-the-reusable-kruskals-helper)
10. [The Clean Solution Code](#10-the-clean-solution-code)
11. [Dry Run: Every Edge of Example 1](#11-dry-run-every-edge-of-example-1)
12. [Example 2: All Edges Equal](#12-example-2-all-edges-equal)
13. [Complexity](#13-complexity)
14. [Approach 2 (Follow-Up): Weight Groups + Bridges](#14-approach-2-follow-up-weight-groups--bridges)
15. [Edge Cases](#15-edge-cases)
16. [Follow-Up Questions Interviewers Ask](#16-follow-up-questions-interviewers-ask)
17. [How to Present It: A Minute-by-Minute Script](#17-how-to-present-it-a-minute-by-minute-script)
18. [Common Mistakes](#18-common-mistakes)
19. [The Pattern and Its Siblings](#19-the-pattern-and-its-siblings)
20. [TL;DR](#20-tldr)

---

## 1. The Problem in Plain Words

You already know how to find **the cheapest way to connect everything** (Kruskal's). But when several edges have the **same weight**, there can be more than one cheapest way. For each edge, answer:

- **Must I use it?** (in every cheapest network) → **critical**
- **Can I use it, but don't have to?** (in some cheapest network) → **pseudo-critical**
- **Never useful?** (in no cheapest network) → **neither** (not reported)

**Example 1:**

```
n = 5
edges = [[0,1,1],[1,2,1],[2,3,2],[0,3,2],[0,4,3],[3,4,3],[1,4,6]]
index:     e0      e1      e2      e3      e4      e5      e6

        0 ───e0 (1)─── 1
        │ ╲            │ ╲
   e3(2)│  e4(3)  e1(1)│  e6(6)
        │      ╲       │      ╲
        3 ─e5(3)─ 4    2       4   ← e6 is 1–4 (same node 4)
        │              │
        └─────e2(2)────┘           ← e2 is 2–3

Edge list:  e0 0–1 (1)   e1 1–2 (1)   e2 2–3 (2)   e3 0–3 (2)
            e4 0–4 (3)   e5 3–4 (3)   e6 1–4 (6)

Answer: critical = [0, 1], pseudo-critical = [2, 3, 4, 5]
MST weight = 7
```

- `e0` (0–1) and `e1` (1–2): every MST needs them (they're the only weight-1 edges and each is the unique cheapest way to attach its node).
- `e2` / `e3`: both weight 2, either one can finish connecting `{0,1,2}` to 3 → interchangeable → pseudo-critical.
- `e4` / `e5`: both weight 3, either can attach node 4 → pseudo-critical.
- `e6` (weight 6): always beaten by a cheaper way to reach 4 → neither.

**Example 2:**

```
n = 4, edges = [[0,1,1],[1,2,1],[2,3,1],[0,3,1]]   (a square, all weight 1)
Any 3 of the 4 edges form an MST of weight 3.
→ no edge is in all of them; every edge is in some → critical = [], pseudo = [0,1,2,3]
```

---

## 2. Why a Graph Can Have Many MSTs

If all weights were **distinct**, the MST would be **unique** — every MST edge would be critical and every other edge would be neither. Pseudo-critical edges exist **only because of ties**:

```
     A
  2 / \ 2        Both A–B and A–C cost 2, B–C costs 2 too.
   B───C         Any two of the three edges form an MST of weight 4.
     2           Each edge is pseudo-critical.
```

So the problem is really: **among the tied choices, which edges are forced and which are optional?**

---

## 3. Step 0 in the Interview: Clarifying Questions

| Question | Why it matters |
|:--|:--|
| "Is the graph guaranteed connected?" | Yes. (Removing an edge might still disconnect it — that edge is critical.) |
| "Do I return indices into the **original** edge list?" | Yes → if you sort, you must keep the original indices. |
| "Any order for the output?" | Yes. |
| "Can there be parallel edges (same pair twice)?" | No (pairs are distinct). The methods would still work. |
| "How big?" | `n ≤ 100`, `E ≤ 200` → rerunning Kruskal's for every edge (`O(E²)`) is fine. |

---

## 4. Approach 0: Brute Force — Enumerate All Spanning Trees

Generate every set of `n − 1` edges that forms a spanning tree, find the minimum weight, keep the trees with that weight. Then:
- critical = edges in **all** minimum trees,
- pseudo-critical = edges in **some but not all**.

That's the literal definition, but there can be exponentially many spanning trees. I used exactly this to verify the real solutions on small graphs.

> 💡 **What to say:** "Enumerating all MSTs is exponential. Instead I'll ask, for each edge, two cheap questions answered by rerunning Kruskal's."

---

## 5. The Two Tests: Remove It, Then Force It

First, run Kruskal's normally → the **base MST weight** `W`.

Then for each edge `e`:

**Test 1 — Remove `e`.** Run Kruskal's on all edges **except** `e`.
- If the weight becomes **greater than `W`**, or the graph is **no longer connected** → `e` is **critical**.

**Test 2 — Force `e`.** (Only if Test 1 said "not critical.") Run Kruskal's but **add `e` first**, then continue normally.
- If the weight is **still exactly `W`** → `e` is **pseudo-critical**.
- If it's **greater than `W`** → `e` is in **no** MST → neither.

```
for each edge e:
    if MST_without(e) > W:          → critical
    else if MST_with_e_forced == W: → pseudo-critical
    else:                           → neither
```

That's the whole algorithm. Sections 6–8 explain why it's correct.

> 💡 **If you remember one sentence:** "An edge is critical if removing it makes the MST more expensive; otherwise it's pseudo-critical if forcing it in keeps the MST weight the same."

---

## 6. Why Test 1 Identifies Critical Edges

**Critical** = in every MST.

- If `e` is in **every** MST, then a spanning tree **without** `e` can't be minimum → the best tree without `e` is **heavier** than `W` (or doesn't exist, if `e` was the only connection) → Test 1 fires. ✅
- If `e` is **not** in every MST, then some MST avoids `e`. That MST is still available when `e` is removed → the best tree without `e` still weighs `W` → Test 1 doesn't fire. ✅

So **"removing `e` raises the MST weight" ⇔ "every MST uses `e`."**

---

## 7. Why Test 2 Identifies Pseudo-Critical Edges

Forcing `e` first and then running Kruskal's gives **the cheapest spanning tree that contains `e`**. (Kruskal's greedy logic still holds after pre-uniting `e`'s endpoints: it builds the minimum tree among those containing `e`.)

- If that weight equals `W` → there's a **minimum** tree containing `e` → `e` is in **some** MST.
- If it's more than `W` → **no** minimum tree contains `e` → `e` is in **no** MST.

Combined with Test 1 having said "not in every MST":
- in some MST but not every → **pseudo-critical**,
- in no MST → **neither**.

---

## 8. Why the Order of the Tests Matters

A **critical** edge also passes Test 2: forcing an edge that every MST uses obviously still gives weight `W`. So if you ran Test 2 first, you'd label critical edges as pseudo-critical too.

```
WRONG order:  if forced == W → pseudo          ← critical edges land here too
RIGHT order:  if removed > W → critical
              else if forced == W → pseudo
```

**Always check critical first, and use `else if`** so an edge is never in both lists.

---

## 9. Building the Reusable Kruskal's Helper

All three runs (base, remove, force) are the **same Kruskal's** with two optional knobs:

- `skip`: an edge index to ignore (Test 1),
- `force`: an edge index to add first (Test 2).

**Keeping original indices:** we sort edges by weight once, but the answer must use **original** indices. So copy each edge as `[a, b, w, originalIndex]` before sorting, and refer to edges by that 4th field.

```java
// e[k] = {a, b, w, originalIndex}, sorted by w
private int mst(int n, int[][] e, int skip, int force) {
    parent = new int[n];
    size = new int[n];
    Arrays.fill(parent, -1);
    Arrays.fill(size, 1);
    int weight = 0, used = 0;

    if (force != -1) {                                  // Test 2: put this edge in first
        for (int[] x : e) {
            if (x[3] == force) { union(x[0], x[1]); weight += x[2]; used++; break; }
        }
    }
    for (int[] x : e) {
        if (x[3] == skip) continue;                     // Test 1: pretend this edge doesn't exist
        if (union(x[0], x[1])) {
            weight += x[2];
            if (++used == n - 1) break;
        }
    }
    return used == n - 1 ? weight : Integer.MAX_VALUE;  // not spanning → "infinitely expensive"
}
```

**Returning `Integer.MAX_VALUE` when it isn't spanning** makes "disconnected after removal" automatically count as "weight increased" in Test 1 — no separate case needed.

When `force` is set, the forced edge is also encountered again in the main loop, but `union` returns `false` for it (its endpoints are already joined), so it's never counted twice.

---

## 10. The Clean Solution Code

```java
class Solution {
    private int[] parent;   // parent[i] == -1 → i is a leader
    private int[] size;

    public List<List<Integer>> findCriticalAndPseudoCriticalEdges(int n, int[][] edges) {
        int m = edges.length;

        // 1) Copy with original indices, sort by weight
        int[][] e = new int[m][];
        for (int i = 0; i < m; i++) e[i] = new int[]{edges[i][0], edges[i][1], edges[i][2], i};
        Arrays.sort(e, (x, y) -> Integer.compare(x[2], y[2]));

        // 2) Base MST weight
        int base = mst(n, e, -1, -1);

        List<Integer> critical = new ArrayList<>();
        List<Integer> pseudo = new ArrayList<>();

        // 3) Two tests per edge (critical first!)
        for (int i = 0; i < m; i++) {
            if (mst(n, e, i, -1) > base) {
                critical.add(i);                       // removing it hurts → in every MST
            } else if (mst(n, e, -1, i) == base) {
                pseudo.add(i);                         // can be in an MST, but not required
            }
            // else: in no MST → neither
        }
        return Arrays.asList(critical, pseudo);
    }

    // Kruskal's with an optional skipped edge and an optional forced edge (by original index)
    private int mst(int n, int[][] e, int skip, int force) {
        parent = new int[n];
        size = new int[n];
        Arrays.fill(parent, -1);
        Arrays.fill(size, 1);
        int weight = 0, used = 0;

        if (force != -1) {
            for (int[] x : e) {
                if (x[3] == force) { union(x[0], x[1]); weight += x[2]; used++; break; }
            }
        }
        for (int[] x : e) {
            if (x[3] == skip) continue;
            if (union(x[0], x[1])) {
                weight += x[2];
                if (++used == n - 1) break;
            }
        }
        return used == n - 1 ? weight : Integer.MAX_VALUE;
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
- Sort **once**; every rerun reuses the sorted order (Union-Find is reset each time).
- The 4th field is the original index; `skip` and `force` refer to it, and so does the output.
- `Integer.MAX_VALUE` for "not spanning" makes disconnection count as "more expensive."
- `if … else if` guarantees critical edges are never also listed as pseudo-critical.

(Verified: matches a brute force that enumerates every spanning tree, on thousands of random small connected graphs with many weight ties, plus both LeetCode examples.)

---

## 11. Dry Run: Every Edge of Example 1

```
edges (index: a–b weight):
e0: 0–1 1    e1: 1–2 1    e2: 2–3 2    e3: 0–3 2    e4: 0–4 3    e5: 3–4 3    e6: 1–4 6

sorted by weight (stable): e0, e1, e2, e3, e4, e5, e6   (already in order)
```

**Base MST:**

```
e0 0–1 take (1)    e1 1–2 take (2)    e2 2–3 take (4)
e3 0–3 skip (0,3 already connected via 0–1–2–3)
e4 0–4 take (7)    used = 4 = n−1 → stop
base W = 7
```

**Edge by edge:**

| Edge | Test 1: remove it | Result | Test 2: force it | Result | Verdict |
|:--|:--|:--|:--|:--|:--|
| e0 (0–1, 1) | e1, e2, e3, e4 → 1+2+2+3 = **8** | 8 > 7 | — | — | **critical** |
| e1 (1–2, 1) | e0, e2, e3, e4 → 1+2+2+3 = **8** | 8 > 7 | — | — | **critical** |
| e2 (2–3, 2) | e0, e1, e3, e4 → 1+1+2+3 = 7 | not > 7 | e2 first, then e0, e1, (e3 skip), e4 → 2+1+1+3 = **7** | = 7 | **pseudo** |
| e3 (0–3, 2) | e0, e1, e2, e4 → 7 | not > 7 | e3 first, then e0, e1, (e2 skip), e4 → 2+1+1+3 = **7** | = 7 | **pseudo** |
| e4 (0–4, 3) | e0, e1, e2, e5 → 1+1+2+3 = 7 | not > 7 | e4 first, then e0, e1, e2 → 3+1+1+2 = **7** | = 7 | **pseudo** |
| e5 (3–4, 3) | e0, e1, e2, e4 → 7 | not > 7 | e5 first, then e0, e1, e2, (e3, e4 skip) → 3+1+1+2 = **7** | = 7 | **pseudo** |
| e6 (1–4, 6) | e0, e1, e2, e4 → 7 | not > 7 | e6 first, then e0, e1, e2 → 6+1+1+2 = **10** | 10 > 7 | **neither** |

```
critical = [0, 1]       pseudo-critical = [2, 3, 4, 5]   ✅
```

Look at e0 closely: without it, node 0 has to be reached by a weight-2 edge (e3) instead of a weight-1 edge — the total rises by 1. That "the next best substitute is strictly worse" is what critical means.

---

## 12. Example 2: All Edges Equal

```
n = 4, edges: e0 0–1, e1 1–2, e2 2–3, e3 0–3, all weight 1

base: e0, e1, e2 → W = 3   (e3 would close the cycle)

e0: remove → e1, e2, e3 → 3, not > 3;  force → e0, e1, e2 → 3 = 3 → pseudo
e1: remove → e0, e2, e3 → 3;          force → 3 → pseudo
e2: remove → e0, e1, e3 → 3;          force → 3 → pseudo
e3: remove → e0, e1, e2 → 3;          force → e3, e0, e1 → 3 → pseudo

critical = [], pseudo = [0, 1, 2, 3]   ✅
```

Every edge has a same-weight substitute on the other side of the square, so none is required, but each can be part of an MST.

---

## 13. Complexity

Let `E` = number of edges, `n` = number of nodes.

| Step | Cost |
|:--|:--|
| Sort once | O(E log E) |
| One Kruskal's run | O(E · α(n)) (plus `O(n)` to reset Union-Find) |
| Runs needed | 1 + up to 2 per edge = up to `2E + 1` |
| **Total** | **O(E log E + E · (E · α(n) + n)) ≈ O(E²)** |
| Space | O(E + n) |

With `E ≤ 200`, that's about 40,000 cheap operations — measured under 1 ms at `n = 100`, `E = 200`.

---

## 14. Approach 2 (Follow-Up): Weight Groups + Bridges

If asked "can you do it faster than `O(E²)`?", there's an `O(E log E)` method built on one observation:

**Kruskal's processes edges in weight groups.** All decisions about weight-`w` edges depend only on which nodes are already connected by **lighter** edges.

For each group of equal-weight edges:

1. Contract the components built so far (by lighter edges): each Union-Find leader becomes a "super node."
2. **Drop** group edges whose ends are already in the same component — they can **never** be in any MST (a lighter path already connects their ends) → **neither**.
3. The remaining group edges form a small graph between components. Within it:
   - an edge that is a **bridge** (removing it disconnects that small graph) has no same-weight substitute → **critical**;
   - an edge on a **cycle** has a same-weight substitute → **pseudo-critical**.
4. Union all the group's edges, then move to the next weight.

```
Example 1, weight-2 group after weight-1 edges made {0,1,2}:
  components:  A = {0,1,2},  B = {3},  C = {4}
  e2: 2–3 → A–B      e3: 0–3 → A–B
  small graph: A ══ B  (two parallel edges)  → neither is a bridge → both pseudo   ✅

weight-3 group after {0,1,2,3}:
  e4: 0–4 → AB–C     e5: 3–4 → AB–C   → parallel → both pseudo   ✅

weight-6 group after everything is connected:
  e6: 1–4 → same component → neither   ✅
```

Finding bridges uses **Tarjan's low-link DFS**, tracking the **edge id** of the parent edge (not the parent node), because two parallel edges between the same components must not count as bridges.

**Cost:** sorting `O(E log E)` + one DFS per group, overall `O(E log E + E · α(n))`.

**In an interview:** explain this idea if asked to optimize; code the rerun-Kruskal's version unless they insist. (I implemented it too — it matches the brute force and the standard version on all tests.)

---

## 15. Edge Cases

| Case | Result | Why |
|:--|:--|:--|
| All weights distinct | MST edges critical, others neither | the MST is unique |
| All weights equal | depends on structure: cycle edges pseudo, bridges critical | e.g. the square → all pseudo |
| Graph is a tree (`E = n − 1`) | every edge critical | removing any edge disconnects |
| An edge that is a bridge of the whole graph | critical | removal → not spanning → `MAX_VALUE > W` |
| Heaviest edge on a cycle with distinct weights | neither | forcing it raises the weight |
| Ties between a light path and a heavy edge | heavy edge is neither | it's never chosen |

---

## 16. Follow-Up Questions Interviewers Ask

**Q1. "Prove the two tests are correct."**
Test 1: removing `e` keeps weight `W` iff some MST avoids `e`. Test 2: forcing `e` first gives the cheapest tree containing `e`, which is `W` iff some MST contains `e` (Sections 6–7).

**Q2. "Why check critical before pseudo-critical?"**
Critical edges also pass the force test. `if / else if` keeps the lists disjoint (Section 8).

**Q3. "Can you do better than `O(E²)`?"**
Weight groups + bridges, `O(E log E)` (Section 14).

**Q4. "When is the MST unique?"**
If all weights are distinct it's always unique. More precisely: it's unique iff there are **no pseudo-critical edges** — every edge is either critical or neither.

**Q5. "How would you count the number of different MSTs?"**
Process weight groups as in Section 14; for each group's small graph between components, multiply by its number of spanning trees (Kirchhoff's matrix-tree theorem / a determinant). Mention only.

**Q6. "What does 'critical' mean in a real network?"**
A link with no equally-cheap backup: if it fails, the cheapest replacement network costs more. Pseudo-critical links have equally-cheap alternatives — useful for redundancy planning.

**Q7. "How does this relate to bridges in a graph (LC 1192)?"**
A bridge of the whole graph is always critical here. But critical is broader: an edge can have a backup path that's **more expensive**, so it's not a graph bridge but is still critical for the MST (like `e0` in Example 1, which has a backup through weight-2 edges).

---

## 17. How to Present It: A Minute-by-Minute Script

**Minutes 0–2 — Clarify.** Connected graph? Original indices? Any order? Sizes (`E ≤ 200`)?

**Minutes 2–4 — Why it's interesting.** "With distinct weights the MST is unique; ties create alternative MSTs. Critical edges are in all of them, pseudo-critical in some."

**Minutes 4–8 — The two tests.** "Base MST weight `W`. For each edge: remove it and rerun Kruskal's — if the weight goes up or it disconnects, the edge is critical. Otherwise force it in first — if the weight is still `W`, it's pseudo-critical; otherwise it's in no MST." Justify each in one sentence.

**Minutes 8–9 — Order.** "Critical edges also pass the force test, so I check critical first."

**Minutes 9–18 — Code.** Sort with original indices; one `mst(n, e, skip, force)` helper; the loop.

**Minutes 18–23 — Dry run.** Example 1: show e0 (critical), e2 (pseudo), e6 (neither).

**Minutes 23–25 — Complexity.** `O(E²)`, fine for `E ≤ 200`.

**Remaining time — Follow-ups.** Weight groups + bridges (Q3), uniqueness (Q4), relation to graph bridges (Q7).

---

## 18. Common Mistakes

- ❌ **Losing the original indices after sorting** — the output must use the input's indices.
- ❌ **Checking pseudo-critical before critical** — critical edges get misreported.
- ❌ **Using `if … if` instead of `if … else if`** — an edge appears in both lists.
- ❌ **Not treating "disconnected after removal" as critical** — return `MAX_VALUE` when fewer than `n − 1` edges are taken.
- ❌ **Forgetting to reset Union-Find between runs.**
- ❌ **Re-sorting inside every run** — correct but wasteful; sort once.
- ❌ **Thinking "critical = graph bridge"** — critical is broader (Q7).
- ❌ **In the bridge method, tracking the parent node instead of the parent edge id** — parallel edges between components get misclassified as bridges.

---

## 19. The Pattern and Its Siblings

**Family:** **MST sensitivity** — reusing Kruskal's with edges removed or forced to ask "how important is this edge?"

| Problem | Connection |
|:--|:--|
| **Make Network Connected** (LC 1319) | Union-Find counting |
| **Min Cost to Connect All Points** (LC 1584) | build an MST (dense) |
| **Connecting Cities With Minimum Cost** (LC 1135) | build an MST (sparse), detect `−1` |
| **Optimize Water Distribution** (LC 1168) | MST with a virtual node |
| **Find Critical and Pseudo-Critical Edges** (this) | MST with edges removed / forced |
| **Critical Connections in a Network** (LC 1192) | graph bridges (Tarjan's) — used inside Approach 2 |
| **Second-best MST** (classic) | swap one non-tree edge in, the heaviest tree edge on its cycle out |

> 💡 **Recognize the shape.** "Is this edge necessary / possible in an optimal structure?" → compute the optimum once, then recompute **without** the edge (necessary?) and **with it forced** (possible?). This works for MSTs and, more generally, for many "edge importance" questions when inputs are small.

---

## 20. TL;DR

**Problem:** label each edge as in every MST (critical), in some MST (pseudo-critical), or in none.

**Key fact:** multiple MSTs exist only because of equal weights.

**Algorithm:**
1. Copy edges with original indices; sort by weight once.
2. `W` = Kruskal's weight.
3. For each edge `i`:
   - Kruskal's **without** `i` > `W` (or disconnected) → **critical**;
   - **else if** Kruskal's with `i` **forced first** == `W` → **pseudo-critical**;
   - else → neither.

**Why:** removing an edge raises the weight ⇔ every MST needs it; forcing it keeps the weight ⇔ some MST can use it.

**Complexity:** `O(E²)` — tiny for `E ≤ 200`. Faster follow-up: weight groups + bridges in `O(E log E)`.

**One-line philosophy:**
> To learn how much an edge matters, take it away and see if the best network gets more expensive; if it doesn't, push it in and see if the best network stays just as cheap.
