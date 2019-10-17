import java.io.File
import java.io.InputStream
import java.lang.Runtime.getRuntime
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.test.assertEquals

val optimizations =
    ("tti tbaa scoped-noalias assumption-cache-tracker targetlibinfo verify ee-instrument simplifycfg domtree sroa early-cse lower-expect targetlibinfo tti tbaa scoped-noalias assumption-cache-tracker profile-summary-info forceattrs inferattrs callsite-splitting ipsccp called-value-propagation globalopt domtree mem2reg deadargelim domtree basicaa aa loops lazy-branch-prob lazy-block-freq opt-remark-emitter instcombine simplifycfg basiccg globals-aa prune-eh inline functionattrs argpromotion domtree sroa basicaa aa memoryssa early-cse-memssa speculative-execution domtree basicaa aa lazy-value-info jump-threading lazy-value-info correlated-propagation simplifycfg domtree basicaa aa loops lazy-branch-prob lazy-block-freq opt-remark-emitter instcombine libcalls-shrinkwrap loops branch-prob block-freq lazy-branch-prob lazy-block-freq opt-remark-emitter pgo-memop-opt domtree basicaa aa loops lazy-branch-prob lazy-block-freq opt-remark-emitter tailcallelim simplifycfg reassociate domtree loops loop-simplify lcssa-verification lcssa basicaa aa scalar-evolution loop-rotate licm loop-unswitch simplifycfg domtree basicaa aa loops lazy-branch-prob lazy-block-freq opt-remark-emitter instcombine loop-simplify lcssa-verification lcssa scalar-evolution indvars loop-idiom loop-deletion loop-unroll mldst-motion aa memdep lazy-branch-prob lazy-block-freq opt-remark-emitter gvn basicaa aa memdep memcpyopt sccp domtree demanded-bits bdce basicaa aa loops lazy-branch-prob lazy-block-freq opt-remark-emitter instcombine lazy-value-info jump-threading lazy-value-info correlated-propagation domtree basicaa aa memdep dse loops loop-simplify lcssa-verification lcssa aa scalar-evolution licm postdomtree adce simplifycfg domtree basicaa aa loops lazy-branch-prob lazy-block-freq opt-remark-emitter instcombine barrier elim-avail-extern basiccg rpo-functionattrs globalopt globaldce basiccg globals-aa float2int domtree loops loop-simplify lcssa-verification lcssa basicaa aa scalar-evolution loop-rotate loop-accesses lazy-branch-prob lazy-block-freq opt-remark-emitter loop-distribute branch-prob block-freq scalar-evolution basicaa aa loop-accesses demanded-bits lazy-branch-prob lazy-block-freq opt-remark-emitter loop-vectorize loop-simplify scalar-evolution aa loop-accesses loop-load-elim basicaa aa lazy-branch-prob lazy-block-freq opt-remark-emitter instcombine simplifycfg domtree loops scalar-evolution basicaa aa demanded-bits lazy-branch-prob lazy-block-freq opt-remark-emitter slp-vectorizer opt-remark-emitter instcombine loop-simplify lcssa-verification lcssa scalar-evolution loop-unroll lazy-branch-prob lazy-block-freq opt-remark-emitter instcombine loop-simplify lcssa-verification lcssa scalar-evolution licm alignment-from-assumptions strip-dead-prototypes globaldce constmerge domtree loops branch-prob block-freq loop-simplify lcssa-verification lcssa basicaa aa scalar-evolution branch-prob block-freq loop-sink lazy-branch-prob lazy-block-freq opt-remark-emitter instsimplify div-rem-pairs simplifycfg verify").split(
        " "
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

    when {
        params.containsKey("-t") -> runTests()
        params.containsKey("-h") -> showHelp()
        else -> execute(
            params.getOrDefault("-p", emptyList()),
            params.getOrDefault("-k", listOf("0"))[0].toInt(),
            params.getOrDefault("-tf", listOf("./"))[0],
            params.getOrDefault("-bp", listOf("./"))[0],
            params.getOrDefault("-bt", listOf("./"))[0]
        )
    }

}

fun runTests() {
    val currentOptimizations = optimizations.toList()

    assertEquals(
        currentOptimizations.size + 1,
        shakeAdd(currentOptimizations).size
    )

    assertEquals(
        currentOptimizations.size - 1,
        shakeRemove(
            currentOptimizations,
            Random.nextInt(1, optimizations.size - 1),
            isFirst = false,
            isLast = false
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
            isFirst = false,
            isLast = false
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
    println("Optimization Selector")
    println("-p\tList of programs (benchmarks) that should be in bench_path and will be copied to bench_run_path then runned and its results evaluated, one by one. In case they are not on root of bench_path, it could have a prefix of dir")
    println("-k\tTimes to try optimizing its neighbourhood before quitting and assuming a optimistic collection of optimizations")
    println("-tf\tPath to test framework")
    println("-bp\tPath to benchmarks (bench_path)")
    println("-bt\tPath to the directory tf will get the benchmarks to run (bench_run_path)")
    println("-t\tRun the tests for checking if the shaking algorithm is working fine")
    println("-h\tShow this help message")
    println("\n\nExample: optimizations_selector.jar -p firstProgram path/to/second/secondProgram -k 10 -tf /home/username/tf -bp /home/username/tf/Benchs/ -bt /home/username/tf/Benchs/MyBenchs/")
}

fun execute(
    programsNames: List<String>,
    repeatTimes: Int,
    tfPath: String,
    benchPath: String,
    benchRunPath: String
) {
    val resultFile = File("Result.csv")
    resultFile.writeText("Program\tBestResult\tOptimizations\n")

    programsNames.forEach {

        println("Starting optimization selection for $it")

        var k = 1
        var tryCount = 1
        var bestResult = playOnce(
            optimizations,
            tfPath,
            it,
            tryCount,
            benchPath,
            benchRunPath
        )

        while (k++ <= repeatTimes) {
            val currentResult =
                playOnce(
                    shake(bestResult.optimizations),
                    tfPath,
                    it,
                    tryCount,
                    benchPath,
                    benchRunPath
                )
            if (currentResult.time < bestResult.time) {
                bestResult = currentResult
                println("New best on try $k")
                k = 1
            }
            tryCount++
        }

        resultFile.appendText("$it\t${bestResult.time}\t${join(bestResult.optimizations, " ")}\n")
    }
}

fun shake(currentOptimizations: List<String>): List<String> {

    val shouldAdd = Random.nextBoolean()
    if (shouldAdd) {
        return shakeAdd(currentOptimizations)
    }

    val index = Random.nextInt(currentOptimizations.size)
    val shouldRemove = Random.nextBoolean()

    val isFirst = index == 0
    val isLast = index == currentOptimizations.size - 1
    if (shouldRemove) {
        return shakeRemove(currentOptimizations, index, isFirst, isLast)
    }
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

fun join(list: List<String>, separator: String): String {
    var fullString = ""

    if (list.isEmpty()) return fullString

    for (i in 0 until list.size - 1) {
        fullString += list[i] + separator
    }

    return fullString + list.last()
}

fun String.runCommand(workingDir: File, environmentVariables: Array<String>, print: Boolean = false): Int {
    val exec = getRuntime().exec(this, environmentVariables, workingDir)

    if (print)
        exec.inputStream.bufferedReader().lines().forEach { println(it) }
//    exec.errorStream.bufferedReader().lines().forEach{println(it)}

    while (exec.isAlive);
    return exec.exitValue()
}

fun playOnce(
    optimizations: List<String>,
    tfPath: String,
    programFullName: String,
    tryCount: Int,
    benchsPath: String,
    benchsRunPath: String
): Result {

    val programSplitted = programFullName.split("/")
    var programPath = benchsPath

    if (programSplitted.size > 1) {
        programPath += programSplitted.slice(0 until programSplitted.size - 1)
            .map { "$it/" }
            .reduce { acc, it -> "$acc$it" }
    }

    val programName = programSplitted.last()

    File(benchsRunPath).deleteRecursively()
    val benchmarkProgram = File("$programPath$programName")

    if (!benchmarkProgram.isDirectory) {
        println("Directory ${benchmarkProgram.absolutePath} not found")
        exitProcess(0)
    }

    benchmarkProgram.copyRecursively(File("$benchsRunPath$programName"))

    return getMediumResult(optimizations, tfPath, programName, tryCount)
}

private fun getMediumResult(
    optimizations: List<String>,
    tfPath: String,
    programName: String,
    tryCount: Int
): Result {
    val tfDirectory = File(tfPath)
    var results = emptyList<Float>()
    val optValue = join(optimizations.map { "-$it" }, " ")

    for (i in 1..3) {
        "./run.sh".runCommand(
            tfDirectory,
            arrayOf("OPT=$optValue", "COMP=1", "EXEC=0")
        )
        File("${tfPath}run.log").delete()
        "./run.sh".runCommand(
            tfDirectory,
            arrayOf("OPT=$optValue", "COMP=0", "EXEC=1")
        )
        results = results.plus(getResult(tfPath, programName, tryCount, i))
    }

    println("Results: ${results.map { "$it " }}")

    return Result((results.average() * 1000), optimizations)
}

private fun getResult(
    tfPath: String,
    programName: String,
    tryCount: Int,
    repetitionCount: Int
): Float {
    val file = File(tfPath + "run.log")
    val scanner = Scanner(
        file,
        "UTF-8"
    ).useDelimiter("\n")

    val header = scanner.next().split("\t")
    val content = scanner.next().split("\t")

    val fileData = header.zip(content).toMap()

    val newFile = File("$tfPath/results/$programName/${tryCount}_$repetitionCount.csv")
    if (file.renameTo(newFile))
        println("$file successfully renamed to $newFile")
    else
        println("Failed on renaming $file to $newFile")

    return fileData.getOrDefault(
        "JobRuntime",
        "-1"
    ).toFloat()
}
