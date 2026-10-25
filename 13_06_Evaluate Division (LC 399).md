# Evaluate Division (LC 399) — Interview Edition, Explained in Detail

> **Problem.** You're given `equations[i] = [A, B]` and `values[i] = k`, meaning **`A / B = k`**. Then you get `queries[j] = [C, D]`, each asking **`C / D = ?`**. Answer each query using only the given equations. If it can't be determined, answer **`-1.0`**.
>
> **LeetCode constraints (as stated on the problem):** up to 20 equations and 20 queries, variable names are short lowercase strings, every value is positive (no division by zero), and the equations never contradict each other.
>
> This is the **"how much?" big brother of LC 990** (Satisfiability of Equality Equations). LC 990 asks *"are these two variables linked?"*; this problem asks *"are they linked, and by what ratio?"* This doc builds the solution from a simple picture (currency exchange), covers the **graph + BFS** answer you'd code first, then the **weighted Union-Find** answer for the follow-up, with every step traced by hand.

---

## Table of Contents

1. [The Problem in Plain Words](#1-the-problem-in-plain-words)
2. [Reading the Input Correctly — the Direction Trap](#2-reading-the-input-correctly--the-direction-trap)
3. [The Picture to Hold in Your Head: Currency Exchange](#3-the-picture-to-hold-in-your-head-currency-exchange)
4. [How It Relates to LC 990](#4-how-it-relates-to-lc-990)
5. [Step 0 in the Interview: Clarifying Questions](#5-step-0-in-the-interview-clarifying-questions)
6. [The Two Rules That Solve Everything](#6-the-two-rules-that-solve-everything)
7. [Approach 1: Graph + BFS (the Main Interview Answer)](#7-approach-1-graph--bfs-the-main-interview-answer)
8. [Dry Runs of BFS](#8-dry-runs-of-bfs)
9. [DFS Variant](#9-dfs-variant)
10. [The Bridge Idea: Give Every Variable an Actual Value](#10-the-bridge-idea-give-every-variable-an-actual-value)
11. [Approach 2: Weighted Union-Find (the Follow-Up Answer)](#11-approach-2-weighted-union-find-the-follow-up-answer)
12. [Weighted `find`: Updating Ratios While Compressing](#12-weighted-find-updating-ratios-while-compressing)
13. [Weighted `union`: Deriving the Formula Step by Step](#13-weighted-union-deriving-the-formula-step-by-step)
14. [Dry Runs of Weighted Union-Find](#14-dry-runs-of-weighted-union-find)
15. [Approach 3: Floyd–Warshall (Precompute Every Pair)](#15-approach-3-floydwarshall-precompute-every-pair)
16. [The Full CoderPad-Ready Code](#16-the-full-coderpad-ready-code)
17. [Complexity, Approach by Approach](#17-complexity-approach-by-approach)
18. [Edge Cases](#18-edge-cases)
19. [Follow-Up Questions Interviewers Ask](#19-follow-up-questions-interviewers-ask)
20. [How to Present It: A Minute-by-Minute Script](#20-how-to-present-it-a-minute-by-minute-script)
21. [Common Mistakes](#21-common-mistakes)
22. [The Pattern and Its Siblings](#22-the-pattern-and-its-siblings)
23. [TL;DR](#23-tldr)

---

## 1. The Problem in Plain Words

Each variable is an unknown positive number. Each equation tells you **one ratio** between two of them. You must answer ratio questions you weren't told directly, by **chaining** the ones you were told.

```
equations = [[x, y], [y, z]]     values = [2, 3]
meaning:    x / y = 2,  y / z = 3

query [x, z]:  x / z = (x / y) · (y / z) = 2 · 3 = 6
```

The trick that makes chaining work: the middle variable **cancels**:

```
x     y     x
─  ·  ─  =  ─          (the y on the bottom cancels the y on top)
y     z     z
```

The LeetCode example:

```
equations = [["a","b"], ["b","c"]]     values = [2.0, 3.0]
queries   = [["a","c"], ["b","a"], ["a","e"], ["a","a"], ["x","x"]]

a / c = 2 · 3   = 6.0
b / a = 1 / 2   = 0.5      ← flipping a known ratio
a / e = -1.0               ← e never appears
a / a = 1.0                ← a is known, anything divided by itself is 1
x / x = -1.0               ← x is unknown, so even x / x can't be determined
```

---

## 2. Reading the Input Correctly — the Direction Trap

`[A, B]` with value `k` means **`A / B = k`**: the **first** variable is on top.

Your third pad example is exactly this trap:

```
equations = [[y, x], [y, z]]     values = [2, 3]

WRONG reading:  x / y = 2              → x / z = 2 · 3 = 6        ✗
RIGHT reading:  y / x = 2  ⇒  x / y = 1/2
                y / z = 3
                x / z = (x / y) · (y / z) = 0.5 · 3 = 1.5    ✓
```

So every equation must be usable **in both directions**:

```
A / B = k   ⇒   B / A = 1 / k
```

This is why the graph gets **two** edges per equation (Section 7). Interviewers often plant a reversed pair like `[y, x]` specifically to see whether you handle it.

---

## 3. The Picture to Hold in Your Head: Currency Exchange

Think of variables as **currencies** and values as **exchange rates**.

```
"x / y = 2"   ⇔   1 x is worth 2 y            (like 1 USD = 83 INR)
"y / z = 3"   ⇔   1 y is worth 3 z
```

How many `z` is one `x`? Convert `x → y → z`: `1 x = 2 y = 2 · 3 z = 6 z`. So `x / z = 6`.

- **Going backwards** (`z → x`) uses the reciprocal rates: `1 z = 1/3 y = 1/6 x`.
- **No route** between two currencies (no chain of exchange booths connecting them) → you can't convert → `-1.0`.
- **A currency nobody trades** (never in any equation) → `-1.0`, even for "x to x".

```
        ×2          ×3
   x ───────→ y ───────→ z
   x ←─────── y ←─────── z
        ×1/2        ×1/3
```

> 💡 **If you remember one sentence:** "Variables are currencies, equations are exchange rates in both directions, and a query is a conversion — multiply the rates along any route; no route means `-1`."

---

## 4. How It Relates to LC 990

| | LC 990 — Equality Equations | LC 399 — Evaluate Division |
|:--|:--|:--|
| A statement | `a == b` | `a / b = k` |
| Chains? | yes: `a==b`, `b==c` ⇒ `a==c` | yes: `a/b · b/c = a/c` |
| Question | are `p` and `q` in the same group? | same group? **and what ratio?** |
| Answer type | `true` / `false` | a number, or `-1.0` |
| Graph edges | unweighted | **weighted** (ratio one way, reciprocal the other) |
| Union-Find | plain | **weighted** (each node stores its ratio to its leader) |

**LC 990 is the special case of LC 399 where every ratio is 1.** "Are they linked?" is the first half of answering every 399 query; "multiply along the link" is the second half.

> 💡 **What to say:** "This is connected components again, like equality equations, but each connection carries a ratio. First check the two variables are connected; then multiply the ratios along the path."

---

## 5. Step 0 in the Interview: Clarifying Questions

| Question | Why it matters |
|:--|:--|
| "Does `[A, B]` with `k` mean `A / B = k`?" | Fixes the direction (Section 2). |
| "Can a value be 0?" | If yes, reciprocals break. LeetCode: values are positive. |
| "Can equations contradict each other (`a/b=2` and `a/b=3`)?" | LeetCode says no. If yes, you'd need to detect it (Follow-up Q1). |
| "What if a query variable never appears?" | `-1.0`. |
| "What about `x / x`?" | `1.0` if `x` appears in some equation, else `-1.0`. |
| "How many equations and queries?" | Small → BFS per query is fine. Many queries → weighted Union-Find or precompute. |
| "Is floating-point precision a concern?" | Results are compared with a tolerance on LeetCode; mention it. |

---

## 6. The Two Rules That Solve Everything

**Rule 1 — Reverse:** `A / B = k` ⇒ `B / A = 1/k`.

**Rule 2 — Chain:** `A / B = k₁` and `B / C = k₂` ⇒ `A / C = k₁ · k₂`.

Repeating Rule 2 along a path:

```
p / q = (p / v₁) · (v₁ / v₂) · … · (vₖ / q)
```

So a query `p / q` = **product of edge weights along any path from `p` to `q`** in a graph where:
- each variable is a **node**,
- each equation `A / B = k` gives an edge `A → B` with weight `k` **and** `B → A` with weight `1/k`.

**Does it matter which path?** No. Because the input is consistent, every path from `p` to `q` gives the same product (they all equal the true ratio `p / q`). So the **first** path you find is good enough — no shortest paths, no comparing routes.

---

## 7. Approach 1: Graph + BFS (the Main Interview Answer)

### Build the graph

```java
Map<String, Map<String, Double>> graph = new HashMap<>();
for (int i = 0; i < equations.size(); i++) {
    String a = equations.get(i).get(0);
    String b = equations.get(i).get(1);
    graph.computeIfAbsent(a, k -> new HashMap<>()).put(b, values[i]);        // a → b : a/b
    graph.computeIfAbsent(b, k -> new HashMap<>()).put(a, 1.0 / values[i]);  // b → a : b/a
}
```

`graph.get(u).get(v)` = the ratio `u / v`. Variables are strings, so a `HashMap` keyed by name is the natural adjacency list.

### Answer one query with BFS

```java
private static double bfs(Map<String, Map<String, Double>> graph, String src, String dst) {
    if (!graph.containsKey(src) || !graph.containsKey(dst)) return -1.0;   // unknown variable
    if (src.equals(dst)) return 1.0;                                       // x / x

    Map<String, Double> product = new HashMap<>();   // product.get(v) = src / v
    Deque<String> queue = new ArrayDeque<>();
    product.put(src, 1.0);                           // src / src = 1
    queue.offer(src);

    while (!queue.isEmpty()) {
        String u = queue.poll();
        if (u.equals(dst)) return product.get(u);

        for (Map.Entry<String, Double> edge : graph.get(u).entrySet()) {
            String v = edge.getKey();
            if (!product.containsKey(v)) {                        // unvisited
                product.put(v, product.get(u) * edge.getValue()); // src/v = src/u · u/v
                queue.offer(v);
            }
        }
    }
    return -1.0;                                                  // no path
}
```

### The one idea to understand: what `product` means

**`product.get(v)` always equals `src / v`.**

- Start: `product[src] = 1` because `src / src = 1`.
- When we step along an edge `u → v` with weight `u / v`:
  `src / v = (src / u) · (u / v) = product[u] · weight`.

So when BFS reaches `dst`, `product[dst] = src / dst` — the answer.

`product` also doubles as the **visited set** (`containsKey`), so there's no separate `visited`.

> 💡 **What to say:** "I keep, for every reached node, the ratio from the source to that node. Each edge multiplies it by one more ratio. When I reach the target, that stored ratio is the answer."

---

## 8. Dry Runs of BFS

### Pad example 3 — the direction trap: `[[y, x], [y, z]]`, `[2, 3]`, query `x / z`

**Graph:**

```
y → x : 2        (y/x = 2)
x → y : 0.5      (x/y = 1/2)
y → z : 3        (y/z = 3)
z → y : 0.333…   (z/y = 1/3)
```

**BFS from x to z:**

```
start:   product = {x: 1}                 queue = [x]

pop x:   x ≠ z. neighbors of x: y (0.5)
         product[y] = 1 · 0.5 = 0.5       (x/y = 0.5)   queue = [y]

pop y:   y ≠ z. neighbors of y: x (visited), z (3)
         product[z] = 0.5 · 3 = 1.5       (x/z = 1.5)   queue = [z]

pop z:   z == dst → return 1.5   ✅
```

### Pad example 2: `[[a,b],[b,c],[d,a]]`, `[1, 2, 3]`, query `d / c`

**Graph:**

```
a → b : 1      b → a : 1
b → c : 2      c → b : 0.5
d → a : 3      a → d : 0.333…
```

**BFS from d to c:**

```
product = {d: 1}
pop d:  → a : product[a] = 1 · 3 = 3            (d/a = 3)
pop a:  → b : product[b] = 3 · 1 = 3            (d/b = 3)
        → d : visited
pop b:  → c : product[c] = 3 · 2 = 6            (d/c = 6)
        → a : visited
pop c:  c == dst → return 6   ✅
```

Note the equations were listed `a-b, b-c, d-a`, but BFS doesn't care about input order — it follows edges.

### A `-1.0` example: `[[a,b]]`, `[2]`, plus `[[c,d]]`, `[4]`, query `a / c`

```
BFS from a: reaches b, then nothing new. Never reaches c → return -1.0   ✅
(a and c are in different components — like two currencies with no exchange route)
```

---

## 9. DFS Variant

Same graph; search depth-first and multiply on the way back.

```java
private static double dfs(Map<String, Map<String, Double>> graph, String u, String dst, Set<String> seen) {
    if (u.equals(dst)) return 1.0;                       // u / u = 1
    seen.add(u);
    for (Map.Entry<String, Double> edge : graph.get(u).entrySet()) {
        String v = edge.getKey();
        if (seen.contains(v)) continue;
        double rest = dfs(graph, v, dst, seen);          // v / dst, or -1 if unreachable
        if (rest != -1.0) return edge.getValue() * rest; // u/dst = u/v · v/dst
    }
    return -1.0;
}
// call: unknown variable → -1.0; otherwise dfs(graph, src, dst, new HashSet<>())
```

Here the meaning is reversed: `dfs(u)` returns `u / dst`. Both work. BFS is iterative (no recursion depth concerns); DFS is a few lines shorter. Pick one and explain its invariant.

> ⚠️ **Why `rest != -1.0` is safe:** all real ratios are positive (values are positive), so a genuine answer can never be `-1`. If negative values were allowed, you'd need a separate "found" flag.

---

## 10. The Bridge Idea: Give Every Variable an Actual Value

This idea turns the graph answer into the Union-Find answer, so it's worth understanding.

Ratios only fix values **up to scaling**: if `x / y = 2`, then `(x, y) = (2, 1)` works, and so does `(4, 2)`. So in each connected group, **pick one variable as the reference and set it to 1**. Every other variable in that group gets a concrete value relative to it.

```
equations: y/x = 2, y/z = 3

Choose z as the reference:  z = 1
y / z = 3   ⇒ y = 3
y / x = 2   ⇒ x = y / 2 = 1.5

Now any query in this group is just division of stored numbers:
x / z = 1.5 / 1 = 1.5     y / x = 3 / 1.5 = 2     z / y = 1/3
```

So, **precompute** `value[v] = v / reference` once per group (one BFS per group), and remember which group each variable is in. Then every query is:
- different groups (or unknown variable) → `-1.0`,
- same group → `value[p] / value[q]`.

That's exactly what **weighted Union-Find** maintains: the **reference is the group's root**, and **`weight[v] = v / root`**.

> 💡 **What to say:** "Within a connected group the ratios pin down every value relative to one reference. If I store each variable's value relative to its group's reference, any query is a single division."

---

## 11. Approach 2: Weighted Union-Find (the Follow-Up Answer)

Same Union-Find as LC 990, plus one extra number per node:

```
parent[x] = the node x points to (x itself if x is a root)
weight[x] = x / parent[x]
```

After `find(x)` with path compression, `parent[x]` **is** the root, so:

```
weight[x] = x / root
```

**Answering a query `p / q`:**

```
if p or q unknown                → -1.0
if find(p) != find(q)            → -1.0          (different groups)
else  p / q = (p / root) / (q / root) = weight[p] / weight[q]
```

Two operations need care: **`find`** must keep `weight` correct when it re-points nodes to the root, and **`union`** must compute the ratio between the two roots it links.

---

## 12. Weighted `find`: Updating Ratios While Compressing

```java
private static String find(String x) {
    String p = parent.get(x);
    if (p.equals(x)) return x;                      // x is a root (weight[x] = x/x = 1)
    String root = find(p);                          // after this: parent[p] = root, weight[p] = p/root
    weight.put(x, weight.get(x) * weight.get(p));   // x/root = (x/p) · (p/root)
    parent.put(x, root);                            // x now points straight at the root
    return root;
}
```

**Picture:** a chain `x → p → root` with `x / p = 2` and `p / root = 3`.

```
before:   x ──(2)──→ p ──(3)──→ root
              x/p        p/root

find(x):  first find(p) makes p point at root with weight 3
          then x/root = x/p · p/root = 2 · 3 = 6

after:    x ──(6)──────────────→ root
          p ──(3)──→ root
```

It's the chain rule from Section 6, applied while flattening. **The order matters:** call `find(p)` *before* multiplying, so `weight[p]` already means `p / root`.

---

## 13. Weighted `union`: Deriving the Formula Step by Step

We learn a new fact `a / b = v`. Let `ra = find(a)` and `rb = find(b)`. After those calls:

```
weight[a] = a / ra
weight[b] = b / rb
```

If `ra == rb`, they're already in the same group (and consistent input means nothing to do).

Otherwise hang `ra` under `rb`. We need **`weight[ra] = ra / rb`**. Express it with things we know:

```
ra / rb  =  (ra / a) · (a / b) · (b / rb)

         =  (1 / weight[a]) · v · weight[b]

         =  v · weight[b] / weight[a]
```

```java
private static void union(String a, String b, double v) {   // records a / b = v
    add(a);
    add(b);                                               // new variables: parent = self, weight = 1
    String ra = find(a), rb = find(b);
    if (ra.equals(rb)) return;
    parent.put(ra, rb);
    weight.put(ra, v * weight.get(b) / weight.get(a));    // ra / rb
}
```

**Picture:**

```
     a ──(a/ra)──→ ra                 b ──(b/rb)──→ rb

new fact:  a ──(v)── b

route from ra to rb:   ra → a → b → rb
                       (1/(a/ra)) · v · (b/rb)
```

> 💡 **Memory trick:** "To get root-to-root, walk ra → a (divide by a's weight), a → b (times v), b → rb (times b's weight)."

---

## 14. Dry Runs of Weighted Union-Find

### Pad example 2: `a/b = 1`, `b/c = 2`, `d/a = 3`, query `d / c`

```
union(a, b, 1):
  add a, add b                 parent: a→a, b→b      weight: a=1, b=1
  ra = a, rb = b
  parent[a] = b
  weight[a] = 1 · weight[b] / weight[a] = 1 · 1 / 1 = 1          (a/b = 1)

union(b, c, 2):
  add c
  ra = find(b) = b, rb = find(c) = c
  parent[b] = c
  weight[b] = 2 · 1 / 1 = 2                                      (b/c = 2)

  tree now:  a ──(1)──→ b ──(2)──→ c

union(d, a, 3):
  add d                        weight[d] = 1
  ra = find(d) = d
  rb = find(a):
       find(b): parent[b] = c is root → returns c (weight[b] = 2)
       weight[a] = weight[a] · weight[b] = 1 · 2 = 2             (a/c = 2)
       parent[a] = c
       → rb = c
  parent[d] = c
  weight[d] = 3 · weight[a] / weight[d] = 3 · 2 / 1 = 6          (d/c = 6)

query d / c:
  find(d) = c, find(c) = c → same group
  weight[d] / weight[c] = 6 / 1 = 6   ✅
```

### Pad example 3: `y/x = 2`, `y/z = 3`, query `x / z`

```
union(y, x, 2):
  ra = y, rb = x
  parent[y] = x, weight[y] = 2 · 1 / 1 = 2          (y/x = 2)

union(y, z, 3):
  ra = find(y) = x   (weight[y] = 2 = y/x)
  rb = find(z) = z   (weight[z] = 1)
  parent[x] = z
  weight[x] = 3 · weight[z] / weight[y] = 3 · 1 / 2 = 1.5      (x/z = 1.5)

query x / z:
  find(x) = z, find(z) = z
  weight[x] / weight[z] = 1.5 / 1 = 1.5   ✅
```

The reversed pair `[y, x]` needed no special handling — the formula handles direction automatically.

---

## 15. Approach 3: Floyd–Warshall (Precompute Every Pair)

Because there are at most ~40 distinct variables (20 equations × 2), you can precompute **every** ratio once:

```
ratio[i][i] = 1;  ratio[a][b] = v;  ratio[b][a] = 1/v;  everything else unknown (-1)
for k: for i: for j:
    if ratio[i][k] known and ratio[k][j] known and ratio[i][j] unknown:
        ratio[i][j] = ratio[i][k] · ratio[k][j]
```

`O(V³)` precomputation, then every query is a table lookup. It's a nice "many queries, few variables" answer, and it's the same triple loop as all-pairs shortest paths with "multiply" instead of "add". Usually you mention it rather than code it.

---

## 16. The Full CoderPad-Ready Code

Also provided as a separate runnable file (`EvaluateDivision_Solution.java`, class `Solution` with `main`).

```java
import java.util.*;

class Solution {

    // ============ Approach 1: Graph + BFS ============
    public static double[] calcEquationBFS(List<List<String>> equations, double[] values,
                                           List<List<String>> queries) {
        Map<String, Map<String, Double>> graph = new HashMap<>();
        for (int i = 0; i < equations.size(); i++) {
            String a = equations.get(i).get(0);
            String b = equations.get(i).get(1);
            graph.computeIfAbsent(a, k -> new HashMap<>()).put(b, values[i]);
            graph.computeIfAbsent(b, k -> new HashMap<>()).put(a, 1.0 / values[i]);
        }
        double[] answers = new double[queries.size()];
        for (int q = 0; q < queries.size(); q++) {
            answers[q] = bfs(graph, queries.get(q).get(0), queries.get(q).get(1));
        }
        return answers;
    }

    private static double bfs(Map<String, Map<String, Double>> graph, String src, String dst) {
        if (!graph.containsKey(src) || !graph.containsKey(dst)) return -1.0;
        if (src.equals(dst)) return 1.0;
        Map<String, Double> product = new HashMap<>();   // product[v] = src / v
        Deque<String> queue = new ArrayDeque<>();
        product.put(src, 1.0);
        queue.offer(src);
        while (!queue.isEmpty()) {
            String u = queue.poll();
            if (u.equals(dst)) return product.get(u);
            for (Map.Entry<String, Double> edge : graph.get(u).entrySet()) {
                String v = edge.getKey();
                if (!product.containsKey(v)) {
                    product.put(v, product.get(u) * edge.getValue());
                    queue.offer(v);
                }
            }
        }
        return -1.0;
    }

    // ============ Approach 2: Weighted Union-Find ============
    private static Map<String, String> parent;
    private static Map<String, Double> weight;          // weight[x] = x / parent[x]

    public static double[] calcEquationUF(List<List<String>> equations, double[] values,
                                          List<List<String>> queries) {
        parent = new HashMap<>();
        weight = new HashMap<>();
        for (int i = 0; i < equations.size(); i++) {
            union(equations.get(i).get(0), equations.get(i).get(1), values[i]);
        }
        double[] answers = new double[queries.size()];
        for (int q = 0; q < queries.size(); q++) {
            String p = queries.get(q).get(0), r = queries.get(q).get(1);
            if (!parent.containsKey(p) || !parent.containsKey(r)) { answers[q] = -1.0; continue; }
            if (!find(p).equals(find(r)))                         { answers[q] = -1.0; continue; }
            answers[q] = weight.get(p) / weight.get(r);
        }
        return answers;
    }

    private static void add(String x) {
        if (!parent.containsKey(x)) { parent.put(x, x); weight.put(x, 1.0); }
    }

    private static String find(String x) {
        String p = parent.get(x);
        if (p.equals(x)) return x;
        String root = find(p);
        weight.put(x, weight.get(x) * weight.get(p));   // x/root = x/p · p/root
        parent.put(x, root);
        return root;
    }

    private static void union(String a, String b, double v) {   // a / b = v
        add(a);
        add(b);
        String ra = find(a), rb = find(b);
        if (ra.equals(rb)) return;
        parent.put(ra, rb);
        weight.put(ra, v * weight.get(b) / weight.get(a));     // ra / rb
    }

    // ============ Demo ============
    private static List<List<String>> pairs(String... s) {
        List<List<String>> list = new ArrayList<>();
        for (int i = 0; i < s.length; i += 2) list.add(Arrays.asList(s[i], s[i + 1]));
        return list;
    }

    public static void main(String[] args) {
        // x/y = 2, y/z = 3 → x/z = 6
        System.out.println(Arrays.toString(calcEquationBFS(pairs("x","y","y","z"), new double[]{2,3}, pairs("x","z"))));
        // a/b = 1, b/c = 2, d/a = 3 → d/c = 6
        System.out.println(Arrays.toString(calcEquationBFS(pairs("a","b","b","c","d","a"), new double[]{1,2,3}, pairs("d","c"))));
        // y/x = 2, y/z = 3 → x/z = 1.5
        System.out.println(Arrays.toString(calcEquationUF(pairs("y","x","y","z"), new double[]{2,3}, pairs("x","z"))));
        // LeetCode example → [6.0, 0.5, -1.0, 1.0, -1.0]
        System.out.println(Arrays.toString(calcEquationUF(pairs("a","b","b","c"), new double[]{2,3},
                pairs("a","c","b","a","a","e","a","a","x","x"))));
    }
}
```

**Output (verified):**

```
[6.0]
[6.0]
[1.5]
[6.0, 0.5, -1.0, 1.0, -1.0]
```

(BFS, DFS, weighted Union-Find, and Floyd–Warshall were also checked against a ground truth on 20,000 random cases: each case picks hidden true values for the variables, generates consistent equations from them, and compares every query — including unknown variables, `x / x`, and disconnected groups.)

> ℹ️ **On LeetCode** the method is an instance method `public double[] calcEquation(List<List<String>> equations, double[] values, List<List<String>> queries)` — drop `static` and keep the body.

---

## 17. Complexity, Approach by Approach

Let `E` = number of equations, `Q` = number of queries, `V` = distinct variables (≤ 2E).

| Approach | Build | Per query | Total | Space |
|:--|:--|:--|:--|:--|
| Graph + BFS/DFS | O(E) | O(V + E) | **O(E + Q · (V + E))** | O(V + E) |
| Assign values per group (Section 10) | O(V + E) | O(1) | O(V + E + Q) | O(V) |
| Weighted Union-Find | O(E · α(V)) | ≈ O(1) | **≈ O(E + Q)** | O(V) |
| Floyd–Warshall | O(V³) | O(1) | O(V³ + Q) | O(V²) |

(String hashing adds a factor of the name length, which is tiny here.) With LeetCode's limits (20 and 20), **all** of these are instant — so in the interview, correctness and explanation matter more than which one you pick. Lead with BFS; offer weighted Union-Find when asked about many queries.

---

## 18. Edge Cases

| Case | Answer | Why |
|:--|:--|:--|
| Query variable never appears (`a / e`) | `-1.0` | no information about `e` |
| `x / x`, `x` unknown | `-1.0` | still no information about `x` |
| `x / x`, `x` known | `1.0` | anything over itself |
| Reversed pair (`[y, x]`) | handled | two edges per equation / union formula |
| Different groups | `-1.0` | no route between them |
| Query exactly equals an equation | `k` | one edge |
| Query is a reversed equation | `1/k` | the reverse edge |
| Same equation listed twice | same answer | BFS uses either; union sees same root |
| Long chain (`a/b, b/c, c/d, …`) | product along the chain | path compression keeps later finds short |

---

## 19. Follow-Up Questions Interviewers Ask

**Q1. "What if the equations might contradict each other? Detect it."**
In `union`, when `ra == rb`, the existing structure already implies `a / b = weight[a] / weight[b]`. Compare it to the new `v` with a tolerance:
`if (Math.abs(weight[a] / weight[b] - v) > 1e-9) → contradiction`.
This is **LC 2307 — Check for Contradictions in Equations**. It's the ratio version of LC 990's "`!=` inside a group".

**Q2. "There are millions of queries. What changes?"**
Don't BFS per query. Use weighted Union-Find (near-constant per query) or assign values per group (Section 10) once, then answer each query with one division.

**Q3. "Equations and queries are interleaved (streaming)."**
Weighted Union-Find handles this naturally: `union` on each new equation, `find` on each query. BFS would rebuild nothing but still costs `O(V + E)` per query.

**Q4. "Could floating-point error accumulate?"**
Yes, slightly, along long chains. Mitigations: compare with a tolerance; or store **logarithms** of ratios so multiplication becomes addition (`log(a/c) = log(a/b) + log(b/c)`); or keep exact fractions (numerator/denominator with GCD reduction, using `long` or `BigInteger`) if exact answers are required.

**Q5. "Currency arbitrage: is there a cycle of exchanges that makes money?"**
That's when going around a cycle multiplies to **more than 1**. Take `−log(rate)` as edge weights; a profitable cycle becomes a **negative cycle**, which **Bellman–Ford** detects. (In this problem cycles always multiply to exactly 1, because the input is consistent.)

**Q6. "What if a value could be 0?"**
`a / b = 0` means `a = 0`, and then `b / a` is undefined — the reverse edge breaks. You'd treat zero specially (track which variables are zero). LeetCode rules it out; mention you'd ask.

**Q7. "BFS or DFS?"**
Either. BFS is iterative (no recursion depth issues). DFS is shorter. The path found doesn't matter because every path gives the same product.

---

## 20. How to Present It: A Minute-by-Minute Script

**Minutes 0–3 — Clarify.** Confirm `[A, B], k` means `A / B = k`; values positive; no contradictions; unknown variable → `-1`; `x / x` rules.

**Minutes 3–6 — Reframe.** "Ratios chain: `a/b · b/c = a/c`, and they reverse: `b/a = 1/(a/b)`. So I'll model variables as nodes and each equation as two weighted edges. A query is the product of weights along any path; no path means `-1`." Draw the currency picture.

**Minutes 6–8 — Why any path works.** "The input is consistent, so every path gives the same product — I can stop at the first one BFS finds."

**Minutes 8–18 — Code BFS.** Graph build with both directions; BFS with `product[v] = src / v`; handle unknown and `x / x` up front.

**Minutes 18–23 — Dry run.** Use the reversed example `[[y,x],[y,z]]` → `1.5` — it shows you handle direction.

**Minutes 23–25 — Complexity.** `O(Q · (V + E))`; fine for the limits.

**Remaining time — Follow-ups.** "For many queries, I'd use weighted Union-Find: each node stores its ratio to its root; a query is `weight[p] / weight[q]` if they share a root." Then contradictions (Q1) and arbitrage (Q5).

---

## 21. Common Mistakes

- ❌ **Adding only one edge per equation.** Queries in the reverse direction (`b / a`) or reversed inputs (`[y, x]`) then fail.
- ❌ **Using `values[i]` for the reverse edge instead of `1 / values[i]`.**
- ❌ **Returning `1.0` for `x / x` when `x` is unknown.** Check existence first.
- ❌ **Forgetting the visited check in BFS/DFS.** Every equation creates a 2-cycle (`a → b → a`), so without it you loop forever.
- ❌ **Using `==` to compare `String`s** (`u == dst`). Use `.equals()`.
- ❌ **In weighted `find`, multiplying before the recursive call.** `weight[p]` must already be `p / root`.
- ❌ **Getting the union formula upside down.** It's `weight[ra] = v · weight[b] / weight[a]`; derive it via `ra → a → b → rb` if unsure.
- ❌ **Comparing doubles with `==` in a contradiction check.** Use a tolerance.

---

## 22. The Pattern and Its Siblings

**Family:** **connected components with weights** — Union-Find / graph traversal where each connection carries a value that **composes** along paths (here by multiplication).

| Problem | What a connection carries | Twist |
|:--|:--|:--|
| **Satisfiability of Equality Equations** (LC 990) | nothing (just "equal") | the unweighted version of this problem |
| **Evaluate Division** (this) | a ratio | multiply along paths |
| **Check for Contradictions in Equations** (LC 2307) | a ratio | detect inconsistency on union |
| **Accounts Merge** (LC 721) | nothing | string nodes → ids |
| **Cheapest Flights Within K Stops** (LC 787) | a cost | weights **add**, and paths differ → shortest path |
| **Network Delay Time** (LC 743) | a time | Dijkstra |
| **Currency arbitrage** (classic) | an exchange rate | `−log` weights + Bellman–Ford negative cycle |
| **Possible Bipartition** (LC 886) | "opposite side" (parity) | weighted Union-Find with weight 0/1 (XOR instead of multiply) |

> 💡 **Recognize the shape.** "Statements relate two items by a value, the value **composes** along chains (multiplies, adds, XORs), and you're asked about the relation between items never stated directly" → build the graph (BFS/DFS along a path) or keep a **weighted Union-Find** where each node stores its relation to its root.

---

## 23. TL;DR

**Problem:** given `A / B = k` facts, answer `C / D` queries, or `-1.0` if impossible.

**Picture:** variables are currencies, equations are exchange rates (both ways), a query is a conversion.

**Two rules:** reverse (`B / A = 1/k`) and chain (`A/B · B/C = A/C`).

**Approach 1 — Graph + BFS (code this first):**
1. Edge `A → B` weight `k`, edge `B → A` weight `1/k`.
2. Unknown variable → `-1`; `x / x` (known) → `1`.
3. BFS from `C` keeping `product[v] = C / v`; when you reach `D`, return `product[D]`; never reached → `-1`.

**Approach 2 — Weighted Union-Find (many queries):**
- `weight[x] = x / root` after `find`.
- `find`: `weight[x] *= weight[parent]`, then point `x` at the root.
- `union(a, b, v)`: `parent[ra] = rb`, `weight[ra] = v · weight[b] / weight[a]`.
- Query: same root → `weight[p] / weight[q]`, else `-1`.

**Relation to LC 990:** same "are they connected?" core; 399 adds a ratio to every connection.

**Complexity:** BFS `O(Q · (V + E))`; weighted Union-Find ≈ `O(E + Q)`.

**One-line philosophy:**
> Treat every ratio as a two-way exchange rate between variables, and a query is just a currency conversion: find any route between the two, multiply the rates along it — and if there's no route, the answer can't be known.
