import java.nio.file.Files 
import java.nio.file.Path 
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.system.exitProcess
import java.io.IOException

fun main(vararg args: String) { 
    val dir = try { 
        Path.of(args[0]) 
    } catch (_: Exception) { 
        println("The target directory must be specified as the only argument of the application") 
        exitProcess(1) 
    } 
 
    check(Files.exists(dir)) 
 
    val result = solution(dir) 
    for ((path, size) in result.fileSizes.entries.sortedBy { it.key }) { 
        println("$path: $size") 
    } 
    println("Total: ${result.totalSize}") 
} 
 
class SolutionResult( 
    val fileSizes: Map<Path, Long>, 
    val totalSize: Long, 
) 

//I used walkFileTree() Method and FileVisitor
//Files.walkFileTree() is a method designed to recursively visit all files and subdirectories within a given directory

private fun solution(dir: Path): SolutionResult { 

    val calculator = DirectorySizeCalculator()
    Files.walkFileTree(dir, calculator)
    
    return (SolutionResult(calculator.fileSizes, calculator.totalSize))
}

class DirectorySizeCalculator : SimpleFileVisitor<Path>() {
    val finalResult = SolutionResult(emptyMap<Path,Long>(),0)
    val fileSizes = mutableMapOf<Path, Long>()
    var totalSize: Long = 0 

    //hashset used to keep all encountered fileKeys to avoid double counting
    val visitedFileKeys = HashSet<Any?>()

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        //Since you cannot create hardlinks to a directory there is no need to check visitedFileKeys
        //By default option NOFOLLOW_LINKS so any encounterd softlink will not be followed, no need to check visitedFileKeys  

        //While solving the task I tried to replicate du's behaviour so that's why I didn't include the size of the directory entry lists
        //addFileSize(dir, attrs)

        return FileVisitResult.CONTINUE
    }

    //I commented the error messages so it does not mess up the script but i left the catch statements in just to show that they are handled
    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        try {
            if (attrs.isRegularFile || attrs.isSymbolicLink) { //check for hardlinks, symlinks can also have hardlinks
                val fileKey = attrs.fileKey()

                if (visitedFileKeys.add(fileKey)) { //Returns true if the key was added meaning the file was not encountered before
                    addFileSize(file, attrs)
                }
            }
            //when encountering special files ignore them (attrs.isOther) or files of unkown type
        }
        catch (e: SecurityException) {
            //System.err.println("Permission denied accessing attributes: $file (${e.message}). Ignoring file.")
        } 
        catch (e: IOException) {
            //System.err.println("I/O error accessing attributes: $file (${e.message}). Ignoring file.")
        } 
        catch (t: Throwable) {
           //System.err.println("Unexpected error: $file (${t.message}). Ignoring file.")
        }

        return FileVisitResult.CONTINUE
    }

    //Helper funcion to add file sizes to solution
    private fun addFileSize(file: Path, attrs: BasicFileAttributes) {
        try{
            val fileSize = attrs.size()
            totalSize += fileSize
            fileSizes.put(file, fileSize)
        } 
        catch (e: IOException) {
            //System.err.println("Could not get size for: $file (${e.message}). Ignoring.")
        } 
        catch (e: SecurityException) {
            //System.err.println("Permission denied getting size for: $file. Ignoring.")
        }
        
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        //System.err.println("Failed to access: $file (${exc.message}). Ignoring.")
        return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        /*if (exc != null) {
            System.err.println("Error occurred while processing contents of directory: $dir (${exc.message}).")
        }*/
        return FileVisitResult.CONTINUE
    }
}