package tree

import java.util.*

class TreeNode {


}

fun main() {
    val n: Queue<Pair<Int, String>> = PriorityQueue<Pair<Int, String>>(
            kotlin.Comparator<Pair<Int, String>> { a: Pair<Int, String>, b: Pair<Int, String> ->
                a.first - b.first
            }
    )
    n.add(8 to "op0")
    n.add(100 to "]l0")
    n.add(10 to "Kl0")
    n.add(-10 to "Kl0")
    n.first { it.first == 100 }.also { println(it) }
}
