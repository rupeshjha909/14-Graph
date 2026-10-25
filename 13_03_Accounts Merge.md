# Accounts Merge (LC 721) — Interview Edition: Recognition, Approaches, Code, Follow-Ups

> **Problem.** `accounts[i] = [name, email1, email2, ...]`. Two accounts belong to the **same person** if they share **at least one email**. People can share a name, so the name alone never proves anything. Merge accounts of the same person and return each as `[name, ...emails in sorted order]`. The order of the accounts in the answer doesn't matter.
>
> **Constraints (LeetCode):** `1 ≤ accounts.length ≤ 1000`, `2 ≤ accounts[i].length ≤ 10`, emails are valid and up to 30 characters.
>
> This problem is **hard to recognize and easy to code**. So this doc spends most of its effort on the **thought process** — how you get from "accounts and names" to "connected components" — then walks through four solutions (brute force, DFS, Union-Find on emails, Union-Find on accounts), explains **why emails are mapped to integer ids**, and finishes with dry runs, follow-ups, and a script for presenting it.

---

## Table of Contents

1. [Understanding the Problem](#1-understanding-the-problem)
2. [Step 0 in the Interview: Clarifying Questions](#2-step-0-in-the-interview-clarifying-questions)
3. [Why the Pattern Is Hard to See](#3-why-the-pattern-is-hard-to-see)
4. [The Recognition Process: Five Questions](#4-the-recognition-process-five-questions)
5. [Approach 0: Brute Force — Merge Until Nothing Changes](#5-approach-0-brute-force--merge-until-nothing-changes)
6. [The Key Design Choice: What Are the Nodes?](#6-the-key-design-choice-what-are-the-nodes)
7. [Approach 1: Build the Email Graph, DFS Each Component](#7-approach-1-build-the-email-graph-dfs-each-component)
8. [Approach 2: Union-Find on Emails — the 4-Step Plan](#8-approach-2-union-find-on-emails--the-4-step-plan)
9. [Why Map Each Email to an Integer Id?](#9-why-map-each-email-to-an-integer-id)
10. [The Union-Find Engine](#10-the-union-find-engine)
11. [The Clean Solution Code](#11-the-clean-solution-code)
12. [Dry Run](#12-dry-run)
13. [Variant A: Union-Find Directly on Strings](#13-variant-a-union-find-directly-on-strings)
14. [Variant B: Union-Find on Accounts](#14-variant-b-union-find-on-accounts)
15. [The Tricky Bits](#15-the-tricky-bits)
16. [Complexity, Approach by Approach](#16-complexity-approach-by-approach)
17. [Edge Cases](#17-edge-cases)
18. [Follow-Up Questions Interviewers Ask](#18-follow-up-questions-interviewers-ask)
19. [How to Present It: A Minute-by-Minute Script](#19-how-to-present-it-a-minute-by-minute-script)
20. [Common Mistakes](#20-common-mistakes)
21. [The Pattern and Its Siblings](#21-the-pattern-and-its-siblings)
22. [TL;DR](#22-tldr)

---

## 1. Understanding the Problem

```
accounts = [["John", "johnsmith@mail.com", "john_newyork@mail.com"],
            ["John", "johnsmith@mail.com", "john00@mail.com"],
            ["Mary", "mary@mail.com"],
            ["John", "johnnybravo@mail.com"]]

Output (any account order):
  ["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"]
  ["Mary", "mary@mail.com"]
  ["John", "johnnybravo@mail.com"]
```

- Accounts 1 and 2 share `johnsmith@mail.com` → same person → merged (three unique emails).
- Account 4 is also "John" but shares **no** email → a **different** John → stays separate.
- Mary shares nothing → separate.

**Sorting note:** emails are sorted by plain string order (character codes). That's why `john00` (`'0'` = 48) comes before `john_newyork` (`'_'` = 95), which comes before `johnsmith` (`'s'` = 115). `Collections.sort` on strings does exactly this.

> 💡 **The mental model.** Think of emails as people's phone numbers written on sticky notes. Each account is a note listing some numbers that you *know* belong to one person. If two notes share any number, staple them together. Keep stapling until no two piles share a number. Each final pile is one person.

---

## 2. Step 0 in the Interview: Clarifying Questions

| Question | Why it matters |
|:--|:--|
| "If two accounts share an email, do they always have the same name?" | Yes (LC guarantees it). That's why taking the name from any email in a group is safe. |
| "Can two different people have the same name?" | Yes — so **never** group by name. |
| "Can an email repeat inside one account?" | Treat as possible; the output must have **unique** emails. |
| "Does output order matter?" | Account order: no. Emails inside an account: **sorted**. |
| "Can an account have just one email?" | Yes (`length ≥ 2` means name + at least one email). It still appears in the output. |
| "How big is the input?" | ≤ 1000 accounts × ≤ 9 emails → ≤ 9000 emails. Brute force is borderline; Union-Find is comfortable. |

---

## 3. Why the Pattern Is Hard to See

The problem uses real-world words — *accounts, names, merge* — and none of the graph words (*node, edge, connected, component*). Two more things hide it:

- **The name is a decoy.** It looks like the grouping key, but it isn't.
- **"Same person" spreads in chains.** Account A shares an email with B, and B shares a *different* email with C. Then A and C are the same person even though they share nothing directly.

```
A: [x, y]      B: [y, z]      C: [z, w]
     └── y ──┘      └── z ──┘
A, B, C are one person — A and C share nothing, but the chain connects them.
```

That chaining property is called **transitivity**: if A~B and B~C, then A~C. **Transitive grouping is the definition of connected components.** The tools for connected components are **DFS/BFS** and **Union-Find**.

> 💡 **The trigger phrase.** Whenever you read "X and Y are in the same group if they share/overlap/connect", ask: *does it chain?* If yes, it's connected components — no matter how the problem is dressed.

---

## 4. The Recognition Process: Five Questions

This is the reusable part. Ask these in order:

**Q1. Is the grouping transitive?**
Yes — "same person" chains through shared emails. → **Connected components.** → DFS or Union-Find.

**Q2. What are the nodes?**
The crux. Two options: **emails** or **accounts**. Pick the one where the edges are **already sitting in the input**. Emails in the same account are obviously linked — no searching needed. → **Nodes = emails** (accounts also work — see Section 6 and Variant B).

**Q3. What's an edge?**
Two emails appearing in the **same account**.

**Q4. What does one component mean?**
All emails reachable from each other = **one person's complete email set** = one merged account.

**Q5. What post-processing does the output need?**
Attach the **name** (from any email in the component), **sort** the emails, and output `[name, emails...]`.

```
"same person if they share an email"
     │
     ├─ Q1: chains? yes ──────────────→ connected components
     ├─ Q2: nodes? emails ────────────→ the input already lists their links
     ├─ Q3: edge? same account ───────→ union all emails of an account
     ├─ Q4: component? one person
     └─ Q5: output? name + sorted emails
```

> 💡 **What to say:** "Ownership spreads through shared emails, so this is grouping by transitive connection — connected components. I'll make each email a node, connect emails that appear in the same account, and each component becomes one merged account."

---

## 5. Approach 0: Brute Force — Merge Until Nothing Changes

The literal version of the sticky-note picture: keep each account as a set of emails; repeatedly find **any two** sets that overlap and merge them; stop when no pair overlaps.

```java
List<String> names = new ArrayList<>();
List<Set<String>> sets = new ArrayList<>();
for (List<String> acc : accounts) {
    names.add(acc.get(0));
    sets.add(new HashSet<>(acc.subList(1, acc.size())));
}

boolean changed = true;
while (changed) {
    changed = false;
    search:
    for (int i = 0; i < sets.size(); i++)
        for (int j = i + 1; j < sets.size(); j++)
            if (!Collections.disjoint(sets.get(i), sets.get(j))) {
                sets.get(i).addAll(sets.get(j));     // merge j into i
                sets.remove(j);
                names.remove(j);
                changed = true;
                break search;                         // restart the scan
            }
}
// then: sort each set, prepend its name
```

**Correct, but slow.** Each pass compares up to `A²` pairs of sets, each comparison costs up to `K` (emails per set, growing as sets merge), and there can be up to `A` passes → roughly **`O(A³ · K)`**. For `A = 1000`, that's far too much in the worst case (a long chain where each pass only merges one pair).

> 💡 **The waste.** We rediscover the same overlaps over and over, and compare accounts that could never be related. A graph traversal or Union-Find visits each email a constant number of times instead.

---

## 6. The Key Design Choice: What Are the Nodes?

| | Nodes = **emails** | Nodes = **accounts** |
|:--|:--|:--|
| Edges | emails in the same account (directly in the input) | accounts sharing an email (need an `email → account` map to discover) |
| Number of nodes | total unique emails (≤ 9000) | number of accounts (≤ 1000) |
| Collecting the output | each component is already a set of emails | each component is a set of accounts; gather their emails |
| Which to pick? | **default** — the edges are free | also clean with an "owner" map (Variant B) |

Both are correct. Emails-as-nodes is the standard explanation because **a shared email is literally the bridge node** that connects two accounts:

```
account 1:  johnsmith — john_newyork
account 2:  johnsmith — john00
               ↑
   the shared node; both accounts' emails end up in one component
```

> 💡 **The transferable skill.** In "connected components in disguise" problems, the creative step is choosing the nodes so that the edges are already in the input. Pick wrong and you need extra indexing; pick right and the input *is* the edge list.

---

## 7. Approach 1: Build the Email Graph, DFS Each Component

Make the graph explicit, then collect each component with a traversal.

**Building edges cheaply.** An account with `k` emails doesn't need all `k(k−1)/2` pairs as edges — connecting every email to the **first** email of the account (a "star") keeps them connected with only `k − 1` edges.

```java
public List<List<String>> accountsMerge(List<List<String>> accounts) {
    Map<String, List<String>> adj = new HashMap<>();
    Map<String, String> emailName = new HashMap<>();

    for (List<String> acc : accounts) {
        String first = acc.get(1);
        for (int k = 1; k < acc.size(); k++) {
            String email = acc.get(k);
            adj.putIfAbsent(email, new ArrayList<>());
            emailName.put(email, acc.get(0));
            if (k > 1) {                                   // star edges: first <-> email
                adj.get(first).add(email);
                adj.get(email).add(first);
            }
        }
    }

    Set<String> seen = new HashSet<>();
    List<List<String>> result = new ArrayList<>();
    for (String start : adj.keySet()) {
        if (!seen.add(start)) continue;                    // already in some component

        List<String> component = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();          // iterative DFS: no stack overflow
        stack.push(start);
        while (!stack.isEmpty()) {
            String u = stack.pop();
            component.add(u);
            for (String v : adj.get(u)) if (seen.add(v)) stack.push(v);
        }

        Collections.sort(component);
        List<String> row = new ArrayList<>();
        row.add(emailName.get(start));
        row.addAll(component);
        result.add(row);
    }
    return result;
}
```

`seen.add(v)` returns `false` if `v` was already there — a compact "mark visited and check" in one call.

This is a perfectly good interview answer. Union-Find (next) is usually preferred for "merge groups" problems because it doesn't need an explicit adjacency list and the code is the same template every time.

---

## 8. Approach 2: Union-Find on Emails — the 4-Step Plan

```
STEP 1  INDEX   give every unique email an integer id; remember each email's name
STEP 2  UNION   in each account, union the first email with every other email
STEP 3  GROUP   for each email, find its root; bucket emails by root
STEP 4  BUILD   for each bucket: sort the emails, put the name in front
```

- **Step 2 — why union with the first email only?** Union is transitive: after `union(e1, e2)` and `union(e1, e3)`, all three are in one set. That's `k − 1` unions per account, the same "star" trick as the DFS edges.
- **Cross-account merging happens automatically.** You never compare two accounts. If account 2 contains an email already unioned from account 1, the sets join through that shared email.

---

## 9. Why Map Each Email to an Integer Id?

**Short answer: you don't *have* to.** Union-Find works directly on strings (Variant A, Section 13). The id is an engineering choice. It gives you four things:

**1. You can use plain arrays.**

```java
int[] parent = new int[n];        // instead of HashMap<String, String>
int[] size   = new int[n];
```

With string keys, **every step** of `find` hashes the email (cost proportional to its length) and does a map lookup. With ids, each step is a single array access. Each email is hashed **once** — when its id is assigned — and never again during the unions and finds.

**2. You reuse the Union-Find template you already know.** Number of Provinces, Redundant Connection, Connected Components — all are written on integer nodes `0..n−1`. Mapping emails to ids lets you paste that engine unchanged. In an interview, reusing well-practised code is less risky than adapting it.

**3. Comparisons are simple and safe.** `if (ra == rb)` on `int` is exactly right. On `String`, `==` compares **object references**, not text. For Strings you must write `ra.equals(rb)`.

> **A careful correction to an earlier claim:** in this particular problem, I tested the string version with `==` / `!=` on 20,000 random inputs with deliberately duplicated `String` objects, and it still produced correct results — because a `HashMap` keeps returning the same stored object, so roots usually end up being the same reference. It did **not** crash. But it's correct only by accident. The same mistake gives wrong answers in a connectivity check like `if (find(a) == find(b)) return true;`, where two equal-but-different objects would be reported as "not connected". An interviewer will flag it either way. Using ids removes the whole question.

**4. Ids deduplicate naturally.** `if (!emailId.containsKey(email))` guarantees each unique email is one node, even if it appears in several accounts or twice in one. (The string version gets the same effect with `parent.putIfAbsent(email, email)`, so this one is a tie.)

| | Ids + `int[]` | Strings + `HashMap` |
|:--|:--|:--|
| Each `find` step | array access | hash the string + map lookup |
| Root comparison | `==` (safe) | must use `.equals()` |
| Template reuse | standard integer UF | needs adapting |
| Extra map needed | `emailId` | none |
| Big-O | same | same |

> 💡 **What to say:** "The nodes are emails. I could run Union-Find on strings with a `HashMap<String, String>`, but I'll map each email to an integer id once. Then I can use my standard array-based Union-Find, each find step is an array access instead of a string hash, and I avoid `==` vs `.equals()` pitfalls."

---

## 10. The Union-Find Engine

The same engine as your other Union-Find docs: **`-1` marks a root**, `size[root]` is the set size, **path compression** in `find`, **union by size**.

```java
private int[] parent;   // parent[i] == -1  → i is a root
private int[] size;     // size[root] = number of elements in the set

private int find(int x) {
    if (parent[x] == -1) return x;          // root
    return parent[x] = find(parent[x]);     // path compression: point straight at the root
}

private void union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra == rb) return;                   // already in the same set
    if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }   // ra = the larger set
    parent[rb] = ra;                        // hang the smaller set under the larger
    size[ra] += size[rb];
}
```

**Why both optimizations?**
- **Union by size** keeps trees shallow (height ≤ log n), so recursion in `find` is never deep.
- **Path compression** flattens trees as you query them.
- Together, each operation costs `α(n)` (inverse Ackermann) — effectively constant.

This is identical to the map-based version (`Map<Integer, Integer> parent` with `-1` for roots); arrays are just faster and shorter once nodes are `0..n−1`.

---

## 11. The Clean Solution Code

```java
class Solution {
    private int[] parent;   // parent[i] == -1  → i is a root
    private int[] size;

    public List<List<String>> accountsMerge(List<List<String>> accounts) {
        // STEP 1: INDEX — email → id (first time seen), email → name
        Map<String, Integer> emailId = new HashMap<>();
        Map<String, String> emailName = new HashMap<>();
        for (List<String> acc : accounts) {
            String name = acc.get(0);
            for (int k = 1; k < acc.size(); k++) {
                String email = acc.get(k);
                if (!emailId.containsKey(email)) emailId.put(email, emailId.size());
                emailName.put(email, name);
            }
        }

        int n = emailId.size();
        parent = new int[n];
        size = new int[n];
        Arrays.fill(parent, -1);
        Arrays.fill(size, 1);

        // STEP 2: UNION — connect every email of an account to its first email
        for (List<String> acc : accounts) {
            int first = emailId.get(acc.get(1));
            for (int k = 2; k < acc.size(); k++) {
                union(first, emailId.get(acc.get(k)));
            }
        }

        // STEP 3: GROUP — bucket emails by their root
        Map<Integer, List<String>> groups = new HashMap<>();
        for (Map.Entry<String, Integer> e : emailId.entrySet()) {
            int root = find(e.getValue());
            groups.computeIfAbsent(root, r -> new ArrayList<>()).add(e.getKey());
        }

        // STEP 4: BUILD — [name, sorted emails...]
        List<List<String>> result = new ArrayList<>();
        for (List<String> emails : groups.values()) {
            Collections.sort(emails);
            List<String> row = new ArrayList<>();
            row.add(emailName.get(emails.get(0)));     // every email in the group has the same name
            row.addAll(emails);
            result.add(row);
        }
        return result;
    }

    private int find(int x) {
        if (parent[x] == -1) return x;
        return parent[x] = find(parent[x]);
    }

    private void union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return;
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        size[ra] += size[rb];
    }
}
```

**Line-by-line notes you can say out loud:**
- `emailId.put(email, emailId.size())` — the next id is "how many ids exist so far", so ids are `0, 1, 2, …` with no counter variable.
- `emailName.put(email, name)` can overwrite, but it's always the same name for the same email (guaranteed), so that's harmless.
- Step 2 starts at `k = 2` because `acc.get(0)` is the name and `acc.get(1)` is `first`.
- Step 3 iterates `emailId` (unique emails), not the accounts, so duplicates never appear in the output.

(Verified: this solution, the DFS version, and both Union-Find variants all match the brute force on 20,000 random inputs, including duplicate emails inside an account, same-name-different-person accounts, and long merge chains. The sample produces the expected output.)

---

## 12. Dry Run

Abbreviated emails: `js` = johnsmith, `jny` = john_newyork, `j00` = john00, `mary`, `jb` = johnnybravo.

```
accounts = [["John", js, jny], ["John", js, j00], ["Mary", mary], ["John", jb]]
```

**STEP 1 — INDEX**

```
emailId   = { js:0, jny:1, j00:2, mary:3, jb:4 }
emailName = { js:John, jny:John, j00:John, mary:Mary, jb:John }
parent    = [-1, -1, -1, -1, -1]       size = [1, 1, 1, 1, 1]
```

**STEP 2 — UNION**

```
account 1  [John, js, jny]:  union(0, 1)
           roots 0, 1 → equal sizes → parent[1] = 0, size[0] = 2
           parent = [-1, 0, -1, -1, -1]

account 2  [John, js, j00]:  union(0, 2)
           roots 0, 2 → size 2 vs 1 → parent[2] = 0, size[0] = 3
           parent = [-1, 0, 0, -1, -1]
           ↑ j00 joined js's set because js (id 0) was the shared bridge

account 3  [Mary, mary]:     only one email → nothing to union
account 4  [John, jb]:       only one email → nothing to union
```

**STEP 3 — GROUP**

```
find(0)=0  find(1)=0  find(2)=0   → group 0: [js, jny, j00]
find(3)=3                          → group 3: [mary]
find(4)=4                          → group 4: [jb]
```

**STEP 4 — BUILD**

```
group 0 sorted: [john00, john_newyork, johnsmith] → ["John", john00@…, john_newyork@…, johnsmith@…]
group 3:                                           → ["Mary", mary@…]
group 4:                                           → ["John", johnnybravo@…]
```

Matches the expected output ✅. Note the two "John" results: same name, different components, because no email ever linked them.

**A chain example** (to show transitivity without any direct overlap):

```
[["A", x, y], ["A", y, z], ["A", z, w]]

union(x,y)   → {x,y}
union(y,z)   → {x,y,z}        (y bridges accounts 1 and 2)
union(z,w)   → {x,y,z,w}      (z bridges accounts 2 and 3)

Result: ["A", w, x, y, z]   — accounts 1 and 3 never shared an email directly
```

---

## 13. Variant A: Union-Find Directly on Strings

No ids — the email itself is the node. A root is an email whose parent is itself.

```java
private Map<String, String> parent = new HashMap<>();

private String find(String x) {
    String p = parent.get(x);
    if (p.equals(x)) return x;                 // root — use equals, not ==
    String root = find(p);
    parent.put(x, root);                       // path compression
    return root;
}

public List<List<String>> accountsMerge(List<List<String>> accounts) {
    Map<String, String> emailName = new HashMap<>();
    for (List<String> acc : accounts)
        for (int k = 1; k < acc.size(); k++) {
            parent.putIfAbsent(acc.get(k), acc.get(k));   // each email starts as its own root
            emailName.put(acc.get(k), acc.get(0));
        }

    for (List<String> acc : accounts)
        for (int k = 2; k < acc.size(); k++) {
            String a = find(acc.get(1)), b = find(acc.get(k));
            if (!a.equals(b)) parent.put(b, a);           // no union by size here
        }

    Map<String, List<String>> groups = new HashMap<>();
    for (String email : parent.keySet())
        groups.computeIfAbsent(find(email), r -> new ArrayList<>()).add(email);
    // ...sort each group and prepend emailName.get(...) as before
}
```

Shorter (no id map), but each `find` step hashes a string, comparisons need `.equals()`, and it skips union by size (you could add a `Map<String, Integer> size`). Mention it as a valid alternative; code the id version.

---

## 14. Variant B: Union-Find on Accounts

Nodes = **account indices** `0..A−1`. Keep an **owner map** `email → first account that contained it`. When an email shows up again in account `i`, union `i` with its owner.

```java
// parent/size arrays sized A (number of accounts), same find/union as Section 10
Map<String, Integer> owner = new HashMap<>();
for (int i = 0; i < accounts.size(); i++) {
    List<String> acc = accounts.get(i);
    for (int k = 1; k < acc.size(); k++) {
        String email = acc.get(k);
        Integer o = owner.get(email);
        if (o == null) owner.put(email, i);     // first time: account i owns it
        else union(o, i);                        // seen before: same person
    }
}

Map<Integer, TreeSet<String>> groups = new HashMap<>();   // TreeSet = sorted + unique
for (Map.Entry<String, Integer> e : owner.entrySet())
    groups.computeIfAbsent(find(e.getValue()), r -> new TreeSet<>()).add(e.getKey());

// output: for each group, name = accounts.get(root).get(0), then the TreeSet's emails
```

**Why it's nice:** fewer nodes (≤ 1000 instead of ≤ 9000), the account index naturally gives the name, and the `owner` map plays the role of the `emailId` map. Verified identical results. This is a great answer to the follow-up *"can you make the accounts the nodes instead?"*

---

## 15. The Tricky Bits

- **Same name ≠ same person.** Two Johns with no common email stay in different components because nothing unions them. Never key on the name.
- **Different names never mix inside a component.** Every component is built from accounts chained by shared emails, which belong to one person, so any email's name is the right one.
- **Sort only the emails.** Build the sorted email list first, then put the name in front.
- **Duplicates.** Iterating unique emails (`emailId` keys, `owner` keys, or a `TreeSet`) removes duplicates for free.
- **You never compare accounts with each other.** Shared emails do the bridging.

---

## 16. Complexity, Approach by Approach

Let `A` = number of accounts, `N` = total emails across all accounts, `L` = max email length.

| Approach | Time | Space | Notes |
|:--|:--|:--|:--|
| Brute force (merge overlapping sets) | ~O(A³ · K) worst case | O(N) | rescans all pairs after each merge |
| Email graph + DFS | O(N · L + N log N) | O(N) | building the graph hashes each email; sorting dominates |
| **Union-Find on emails (ids)** | **O(N · L + N · α(N) + N log N)** | O(N) | standard answer |
| Union-Find on strings | O(N · L · α(N) + N log N) | O(N) | every find step re-hashes |
| Union-Find on accounts | O(N · L + N · α(A) + N log N) | O(N) | fewer UF nodes |

In plain words: **hashing each email once, near-constant Union-Find operations, and the sorting of emails (`N log N` comparisons) dominates.** With `N ≤ 9000`, every non-brute approach runs in milliseconds.

---

## 17. Edge Cases

| Case | Handling |
|:--|:--|
| Account with one email | No unions; it becomes its own group of one. |
| Same email repeated inside one account | Gets one id; appears once in the output. |
| Same name, no shared email | Separate components → separate outputs. |
| Long chain (each account overlaps only the next) | Union is transitive → one big component. |
| All accounts share one email | Everything collapses into one account. |
| Every account disjoint | Output = input with each account's emails sorted and deduplicated. |

---

## 18. Follow-Up Questions Interviewers Ask

**Q1. "Why Union-Find over DFS?"**
Both are `O(N log N)` here because of sorting. Union-Find needs no explicit adjacency list, the code is a reusable template, and it handles **incremental** merges naturally (Q4). DFS is just as correct.

**Q2. "Why map emails to ids?"**
Section 9: arrays instead of hash maps, the standard template, safe `==` comparisons. Strings also work.

**Q3. "Can you use accounts as the nodes?"**
Yes — Variant B with an `email → owner account` map.

**Q4. "What if accounts arrive one at a time (streaming) and you must answer queries in between?"**
Union-Find shines here: assign ids to new emails as they arrive, union within each new account, and answer "are these two emails the same person?" with `find(a) == find(b)`. DFS would need a re-traversal per query.

**Q5. "What if two accounts with the same email had different names?"**
The problem guarantees it can't happen. If it could, you'd need a rule (e.g., first name seen, or the most frequent) — ask the interviewer.

**Q6. "Can the recursion in `find` overflow the stack?"**
Not with union by size: tree height is at most `log₂ N` (≈ 14 for 9000 emails). Without union by size, an adversarial order could build a long chain, so either keep union by size or write `find` iteratively.

**Q7. "How would this work at scale — millions of users across machines?"**
Same idea as a distributed connected-components job: partition by email hash, emit "email ↔ account" edges, and run iterative label propagation (each node adopts the smallest label among its neighbors until nothing changes). Mention it only if asked; it shows you can connect the algorithm to real systems such as identity resolution.

---

## 19. How to Present It: A Minute-by-Minute Script

**Minutes 0–2 — Clarify.** Same email ⇒ same name? Same name ⇒ maybe different people? Duplicates? Output ordering?

**Minutes 2–4 — Brute force.** "I could repeatedly merge any two accounts whose email sets overlap until nothing changes. That's correct but roughly cubic in the number of accounts."

**Minutes 4–7 — Recognize.** "Ownership spreads through shared emails — if A shares with B and B with C, all three are one person. That's transitive grouping: connected components."

**Minutes 7–9 — Choose nodes and engine.** "I'll make emails the nodes — emails in the same account are directly connected, so the edges are already in the input. I'll use Union-Find, and map each email to an integer id so I can use arrays."

**Minutes 9–20 — Code.** The four steps: index → union → group → build. Say why you union with the first email only.

**Minutes 20–25 — Dry run.** The sample: point out how `johnsmith` bridges accounts 1 and 2, and why the two "John" outputs stay separate.

**Minutes 25–28 — Complexity.** "Unions are near-constant with path compression and union by size; sorting the emails dominates: `O(N log N)`."

**Remaining time — Follow-ups.** Have Q2 (ids), Q3 (accounts as nodes), and Q4 (streaming) ready.

---

## 20. Common Mistakes

- ❌ **Grouping by name.** Different people can share names.
- ❌ **Comparing every pair of accounts.** That's the brute force; let shared emails bridge accounts.
- ❌ **Using `==` / `!=` on `String` roots.** Use `.equals()` — or use integer ids and avoid the issue.
- ❌ **Assigning an email a new id each time it appears.** Check `containsKey` first, or the same email becomes two nodes and accounts won't merge.
- ❌ **Forgetting accounts with a single email.** They must still appear in the output — they do, because Step 3 iterates every email.
- ❌ **Sorting the name together with the emails.** Sort emails, then prepend the name.
- ❌ **Starting the union loop at `k = 1`.** Index 0 is the name; `acc.get(1)` is the first email.
- ❌ **Recursive DFS on a huge component.** Use an explicit stack (as in Section 7).

---

## 21. The Pattern and Its Siblings

**Family:** **connected components in disguise** → Union-Find or DFS/BFS. The creative step is always choosing the nodes.

| Problem | What the nodes are | What an edge is |
|:--|:--|:--|
| **Number of Provinces** (LC 547) | cities | adjacency-matrix 1s |
| **Number of Connected Components** (LC 323) | nodes | edge list |
| **Redundant Connection** (LC 684) | nodes | the first edge joining two already-connected nodes |
| **Accounts Merge** (this) | **emails** (or accounts) | same account |
| **Most Stones Removed** (LC 947) | rows and columns | a stone links its row and column |
| **Similar String Groups** (LC 839) | strings | differ by one swap |
| **Satisfiability of Equality Equations** (LC 990) | letters | `a == b` |
| **Smallest String With Swaps** (LC 1202) | indices | swappable pairs |
| **Sentence Similarity II** (LC 737) | words | similar pairs (transitive) |

> 💡 **Recognize the shape.** "Things belong together if they share something, and that sharing chains" → connected components. Ask: *what are the nodes so the edges are already in my input?* Then pick Union-Find for merging/streaming, DFS for one-off traversal.

---

## 22. TL;DR

**Problem:** merge accounts that share any email; output `[name, sorted unique emails]` per person.

**Recognition:** "same person" chains through shared emails → **transitive grouping** → **connected components**.

**Nodes:** emails (edges are free: same account). Accounts also work with an `email → owner` map.

**Recipe (Union-Find):**
1. **Index** — email → integer id (first time seen), email → name.
2. **Union** — in each account, union the first email with every other email.
3. **Group** — bucket emails by `find(id)`.
4. **Build** — sort each bucket, prepend the name.

**Why ids:** array-based Union-Find, one hash per email instead of one per `find` step, safe `==` on roots, and your standard template. Strings also work — use `.equals()`.

**Complexity:** `O(N log N)` (sorting dominates), `O(N)` space.

**One-line philosophy:**
> When "belongs together" spreads through shared pieces, stop thinking about the objects and think about the pieces: make the emails the nodes, link the ones that appear together, and every connected group is one person.
