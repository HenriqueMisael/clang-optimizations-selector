import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.lang.Runtime.getRuntime
import java.util.*
import kotlin.random.Random
import kotlin.streams.toList
import kotlin.system.exitProcess
import kotlin.test.assertEquals

val optimizations =
    ("tti tbaa scopednoalias assumptioncachetracker targetlibinfo verify eeinstrument simplifycfg domtree sroa earlycse lowerexpect targetlibinfo tti tbaa scopednoalias assumptioncachetracker profilesummaryinfo forceattrs inferattrs callsitesplitting ipsccp calledvaluepropagation globalopt domtree mem2reg deadargelim domtree basicaa aa loops lazybranchprob lazyblockfreq optremarkemitter instcombine simplifycfg basiccg globalsaa pruneeh inline functionattrs argpromotion domtree sroa basicaa aa memoryssa earlycsememssa speculativeexecution domtree basicaa aa lazyvalueinfo jumpthreading lazyvalueinfo correlatedpropagation simplifycfg domtree basicaa aa loops lazybranchprob lazyblockfreq optremarkemitter instcombine libcallsshrinkwrap loops branchprob blockfreq lazybranchprob lazyblockfreq optremarkemitter pgomemopopt domtree basicaa aa loops lazybranchprob lazyblockfreq optremarkemitter tailcallelim simplifycfg reassociate domtree loops loopsimplify lcssaverification lcssa basicaa aa scalarevolution looprotate licm loopunswitch simplifycfg domtree basicaa aa loops lazybranchprob lazyblockfreq optremarkemitter instcombine loopsimplify lcssaverification lcssa scalarevolution indvars loopidiom loopdeletion loopunroll mldstmotion aa memdep lazybranchprob lazyblockfreq optremarkemitter gvn basicaa aa memdep memcpyopt sccp domtree demandedbits bdce basicaa aa loops lazybranchprob lazyblockfreq optremarkemitter instcombine lazyvalueinfo jumpthreading lazyvalueinfo correlatedpropagation domtree basicaa aa memdep dse loops loopsimplify lcssaverification lcssa aa scalarevolution licm postdomtree adce simplifycfg domtree basicaa aa loops lazybranchprob lazyblockfreq optremarkemitter instcombine barrier elimavailextern basiccg rpofunctionattrs globalopt globaldce basiccg globalsaa float2int domtree loops loopsimplify lcssaverification lcssa basicaa aa scalarevolution looprotate loopaccesses lazybranchprob lazyblockfreq optremarkemitter loopdistribute branchprob blockfreq scalarevolution basicaa aa loopaccesses demandedbits lazybranchprob lazyblockfreq optremarkemitter loopvectorize loopsimplify scalarevolution aa loopaccesses looploadelim basicaa aa lazybranchprob lazyblockfreq optremarkemitter instcombine simplifycfg domtree loops scalarevolution basicaa aa demandedbits lazybranchprob lazyblockfreq optremarkemitter slpvectorizer optremarkemitter instcombine loopsimplify lcssaverification lcssa scalarevolution loopunroll lazybranchprob lazyblockfreq optremarkemitter instcombine loopsimplify lcssaverification lcssa scalarevolution licm alignmentfromassumptions stripdeadprototypes globaldce constmerge domtree loops branchprob blockfreq loopsimplify lcssaverification lcssa basicaa aa scalarevolution branchprob blockfreq loopsink lazybranchprob lazyblockfreq optremarkemitter instsimplify divrempairs simplifycfg verify").split(
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
    println("\n\nExample: optimizations_selector.jar -p firstProgram path/to/second/secondProgram -k 10 -tf ~/tf -bp ~/tf/Benchs/ -bt ~/tf/Benchs/MyBenchs/")
}

fun execute(
    programsNames: List<String>,
    repeatTimes: Int,
    tfPath: String,
    benchPath: String,
    benchRunPath: String
) {
    val resultFile = PrintStream("Result.csv")
    resultFile.println("Program\tBestResult\tOptimizations")

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

        resultFile.println("$it\t${bestResult.time}\t${join(bestResult.optimizations, " ")}")
    }

    resultFile.close()
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

    for (i in 0 until list.size - 1) {
        fullString += list[i] + separator
    }

    return fullString + list.last()
}

fun String.runCommand(
    workingDir: File,
    environmentVariables: Array<String>
): InputStream {
    val exec = getRuntime().exec(this, environmentVariables, workingDir)
    return exec.inputStream!!
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
    val environmentVariables =
        arrayOf("OPT=\"${join(optimizations.map { "-$it" }, " ")}\"")
    val tfDirectory = File(tfPath)

    var results = emptyList<Float>()

    for (i in 1..3) {
        "./run.sh".runCommand(
            tfDirectory,
            environmentVariables
        ).bufferedReader().lines().forEach { println(it) }
        results = results.plus(getResult(tfPath, programName, tryCount, i))
    }

    println("Results===========")
    results.forEach{print(it)}
    println("==================")

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

    val newFile = file.copyTo(
        File("$tfPath${programName}_${tryCount}_$repetitionCount.csv"),
        true
    )
    println("$file successfully renamed to $newFile")

    return fileData.getOrDefault(
        "JobRuntime",
        "0"
    ).toFloat()
}
