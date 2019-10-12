import kotlin.random.Random
import kotlin.streams.toList
import kotlin.test.assertEquals

val optimizations = listOf(
    "tti",
    "tbaa",
    "scoped-noalias",
    "assumption-cache-tracker",
    "targetlibinfo",
    "verify",
    "ee-instrument",
    "simplifycfg",
    "domtree",
    "sroa",
    "early-cse",
    "lower-expect",
    "targetlibinfo",
    "tti",
    "tbaa",
    "scoped-noalias",
    "assumption-cache-tracker",
    "profile-summary-info",
    "forceattrs",
    "inferattrs",
    "callsite-splitting",
    "ipsccp",
    "called-value-propagation",
    "globalopt",
    "domtree",
    "mem2reg",
    "deadargelim",
    "domtree",
    "basicaa",
    "aa",
    "loops",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instcombine",
    "simplifycfg",
    "basiccg",
    "globals-aa",
    "prune-eh",
    "inline",
    "functionattrs",
    "argpromotion",
    "domtree",
    "sroa",
    "basicaa",
    "aa",
    "memoryssa",
    "early-cse-memssa",
    "speculative-execution",
    "domtree",
    "basicaa",
    "aa",
    "lazy-value-info",
    "jump-threading",
    "lazy-value-info",
    "correlated-propagation",
    "simplifycfg",
    "domtree",
    "basicaa",
    "aa",
    "loops",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instcombine",
    "libcalls-shrinkwrap",
    "loops",
    "branch-prob",
    "block-freq",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "pgo-memop-opt",
    "domtree",
    "basicaa",
    "aa",
    "loops",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "tailcallelim",
    "simplifycfg",
    "reassociate",
    "domtree",
    "loops",
    "loop-simplify",
    "lcssa-verification",
    "lcssa",
    "basicaa",
    "aa",
    "scalar-evolution",
    "loop-rotate",
    "licm",
    "loop-unswitch",
    "simplifycfg",
    "domtree",
    "basicaa",
    "aa",
    "loops",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instcombine",
    "loop-simplify",
    "lcssa-verification",
    "lcssa",
    "scalar-evolution",
    "indvars",
    "loop-idiom",
    "loop-deletion",
    "loop-unroll",
    "mldst-motion",
    "aa",
    "memdep",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "gvn",
    "basicaa",
    "aa",
    "memdep",
    "memcpyopt",
    "sccp",
    "domtree",
    "demanded-bits",
    "bdce",
    "basicaa",
    "aa",
    "loops",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instcombine",
    "lazy-value-info",
    "jump-threading",
    "lazy-value-info",
    "correlated-propagation",
    "domtree",
    "basicaa",
    "aa",
    "memdep",
    "dse",
    "loops",
    "loop-simplify",
    "lcssa-verification",
    "lcssa",
    "aa",
    "scalar-evolution",
    "licm",
    "postdomtree",
    "adce",
    "simplifycfg",
    "domtree",
    "basicaa",
    "aa",
    "loops",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instcombine",
    "barrier",
    "elim-avail-extern",
    "basiccg",
    "rpo-functionattrs",
    "globalopt",
    "globaldce",
    "basiccg",
    "globals-aa",
    "float2int",
    "domtree",
    "loops",
    "loop-simplify",
    "lcssa-verification",
    "lcssa",
    "basicaa",
    "aa",
    "scalar-evolution",
    "loop-rotate",
    "loop-accesses",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "loop-distribute",
    "branch-prob",
    "block-freq",
    "scalar-evolution",
    "basicaa",
    "aa",
    "loop-accesses",
    "demanded-bits",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "loop-vectorize",
    "loop-simplify",
    "scalar-evolution",
    "aa",
    "loop-accesses",
    "loop-load-elim",
    "basicaa",
    "aa",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instcombine",
    "simplifycfg",
    "domtree",
    "loops",
    "scalar-evolution",
    "basicaa",
    "aa",
    "demanded-bits",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "slp-vectorizer",
    "opt-remark-emitter",
    "instcombine",
    "loop-simplify",
    "lcssa-verification",
    "lcssa",
    "scalar-evolution",
    "loop-unroll",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instcombine",
    "loop-simplify",
    "lcssa-verification",
    "lcssa",
    "scalar-evolution",
    "licm",
    "alignment-from-assumptions",
    "strip-dead-prototypes",
    "globaldce",
    "constmerge",
    "domtree",
    "loops",
    "branch-prob",
    "block-freq",
    "loop-simplify",
    "lcssa-verification",
    "lcssa",
    "basicaa",
    "aa",
    "scalar-evolution",
    "branch-prob",
    "block-freq",
    "loop-sink",
    "lazy-branch-prob",
    "lazy-block-freq",
    "opt-remark-emitter",
    "instsimplify",
    "div-rem-pairs",
    "simplifycfg",
    "verify",
    "domtree",
    "domtree"
)

fun main(args: Array<String>) {
    var params: Map<String, List<String>> = emptyMap()
    var last = ""

    args.forEach {
        if (it.startsWith('-')) {
            last = it
            params = params.plus(it to emptyList())
        } else {
            params = params.plus(
                last to params.getOrDefault(last, emptyList()).plus(it)
            )
        }
    }

    if (params.containsKey("-t")) {
        runTests()
    } else if (params.containsKey("-h")) {
        showHelp()
    } else {
        execute(
            params.getOrDefault("-p", emptyList()),
            params.getOrDefault("-k", listOf("0"))[0].toInt()
        )
    }

}

fun runTests() {
    val currentOptimizations = optimizations.stream().toList()

    assertEquals(
        currentOptimizations.size + 1,
        shakeAdd(currentOptimizations).size
    )

    assertEquals(
        currentOptimizations.size - 1,
        shakeRemove(
            currentOptimizations,
            Random.nextInt(1, optimizations.size - 1),
            false,
            false
        ).size
    )
    assertEquals(
        currentOptimizations.size - 1, shakeRemove(
            currentOptimizations, 0, true,
            isLast = false
        ).size
    )
    assertEquals(
        currentOptimizations.size - 1, shakeRemove(
            currentOptimizations, optimizations.size - 1,
            isFirst = false,
            isLast = true
        ).size
    )

    assertEquals(
        currentOptimizations.size,
        shakeModify(
            currentOptimizations,
            Random.nextInt(1, optimizations.size - 1),
            false,
            false
        ).size
    )
    assertEquals(
        currentOptimizations.size, shakeModify(
            currentOptimizations, 0, true,
            isLast = false
        ).size
    )
    assertEquals(
        currentOptimizations.size, shakeModify(
            currentOptimizations, optimizations.size - 1,
            isFirst = false,
            isLast = true
        ).size
    )

    println("All tests completed: it's all working fine")
}

fun showHelp() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun execute(programsNames: List<String>, repeatTimes: Int) {
    programsNames.forEach {

        var bestResult = playOnce(optimizations)
        var k = 1

        while (k <= repeatTimes) {
            val currentResult = playOnce(shake(bestResult.optimizations))
            if (currentResult.time < bestResult.time) {
                bestResult = currentResult
                k = 1
            }
        }
    }
}

fun shake(currentOptimizations: List<String>): List<String> {

    val shouldAdd = Random.nextBoolean()
    if (shouldAdd) {
        println("Will add")
        return shakeAdd(currentOptimizations)
    }

    val index = Random.nextInt(currentOptimizations.size)
    val shouldRemove = Random.nextBoolean()

    val isFirst = index == 0
    val isLast = index == currentOptimizations.size - 1
    if (shouldRemove) {
        println("Will remove $isFirst $isLast")
        return shakeRemove(currentOptimizations, index, isFirst, isLast)
    }
    println("Will modify, $index")
    return shakeModify(currentOptimizations, index, isFirst, isLast)
}

private fun shakeAdd(currentOptimizations: List<String>) =
    currentOptimizations.plus(optimizations.random())

private fun shakeModify(
    currentOptimizations: List<String>,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean
): List<String> {
    val newOptimization = optimizations.random()

    if (isFirst) {
        return listOf(newOptimization).plus(currentOptimizations.slice(1 until currentOptimizations.size))
    }

    val newOptimizations =
        currentOptimizations.subList(0, index).plus(newOptimization)
    if (isLast) {
        return newOptimizations
    }

    return newOptimizations.plus(
        currentOptimizations.subList(
            index + 1,
            currentOptimizations.size
        )
    )
}

private fun shakeRemove(
    currentOptimizations: List<String>,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean
): List<String> {
    if (isFirst) {
        return currentOptimizations.slice(index + 1 until currentOptimizations.size)
    } else {
        if (isLast) {
            return currentOptimizations.slice(0 until index)
        }
    }

    return currentOptimizations.slice(0 until index).plus(
        currentOptimizations.slice(
            index + 1 until currentOptimizations.size
        )
    )
}

fun playOnce(optimizations: List<String>): Result {
    println("Size: ${optimizations.size}")
    return Result(Random.nextInt(), optimizations)
}
