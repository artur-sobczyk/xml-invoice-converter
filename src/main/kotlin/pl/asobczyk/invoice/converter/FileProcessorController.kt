package pl.asobczyk.invoice.converter

import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpSession


@Controller
class FileProcessorController(
        private val fileProcessorService: FileProcessorService
) {

    companion object {
        const val FILE_ATTRIBUTE = "file"
        const val ERROR_ATTRIBUTE = "error"
        const val WRONG_PRODUCTS_ATTRIBUTE = "wrong"
    }

    val log = LoggerFactory.getLogger(FileProcessorController::class.java)

    @PostMapping("/upload")
    fun handleFileUpload(@RequestParam("file") file: MultipartFile,
                         session: HttpSession): String {
        session.removeAttribute(ERROR_ATTRIBUTE)
        try {
            val result = fileProcessorService.processFile(toString(file))
            session.setAttribute(FILE_ATTRIBUTE, result.csv)
            if (result.incorrectEntries?.isNotEmpty() == true) {
                session.setAttribute(WRONG_PRODUCTS_ATTRIBUTE, result.incorrectEntries)
            }
        } catch (e: Exception) {
            log.error("buuu... something wrong...", e)
            log.error(toString(file))
            session.setAttribute(ERROR_ATTRIBUTE, "Coś poszło nie tak, sprawdź czy wgrywany plik jest poprawny, jeśli tak to zaostaje ci się tylko skotaktować z autorem :(")
        }

        return "redirect:/"
    }

    @GetMapping("/file")
    @ResponseBody
    fun downloadConvertedFile(session: HttpSession): ResponseEntity<String> {

        if (session.getAttribute(FILE_ATTRIBUTE) == null) {
            return ResponseEntity.noContent().build()
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + getFileName() + "\"")
                .body(session.getAttribute(FILE_ATTRIBUTE).toString())
    }

    fun toString(file: MultipartFile): String {
        var stream: InputStream = ByteArrayInputStream(file.bytes)
        return IOUtils.toString(stream, "UTF-8")
    }

    fun getFileName(): String {
        return "result" + LocalDateTime.now(ZoneId.of("CET"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")) + ".csv"
    }

}
