package pl.asobczyk.invoice.converter

import com.sun.org.apache.xerces.internal.dom.ElementImpl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

@Service
class FileProcessorService {

    val log = LoggerFactory.getLogger(FileProcessorService::class.java)!!

    data class DetailLine(
            var SuppliersArticleNo: String = "",
            var ProcessedSuppliersArticleNo: String = "",
            var SuppliersDescription: String = "",
            var UnitQtyDelivered: String = "",
            var UnitNetPrice: String = "") {

        fun isOK(): Boolean {
            return SuppliersArticleNo?.contains("-")
        }

        fun toCsvEntry(): String {
            return "$ProcessedSuppliersArticleNo;$UnitQtyDelivered;$UnitNetPrice\n"
        }
    }

    data class ProcessResult(var csv: String? = null, var incorrectEntries: String? = null)

    fun processFile(xmlContent: String): ProcessResult {
        val rawEntriesList = findEntries(xmlContent)
        val detailLineList = processEntries(rawEntriesList)
        val result = ProcessResult()
        result.csv = detailLineList.filter { it.isOK() }.joinToString(separator = "") { it.toCsvEntry() }
        result.incorrectEntries = detailLineList.filter { !it.isOK() }.joinToString { "${it.SuppliersArticleNo}\n" }
        return result
    }

    fun findEntries(xmlContent: String): NodeList {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(StringReader(xmlContent))
        val doc = dBuilder.parse(xmlInput)
        doc.normalizeDocument()

        return doc.getElementsByTagName("DetailLine")
    }

    fun processEntries(rawEntriesList: NodeList): List<DetailLine> {
        val detailLineList = arrayListOf<DetailLine>()
        for (i in 0 until rawEntriesList.length) {
            parseEntry(rawEntriesList.item(i) as ElementImpl?).also {
                if (it == null) {
                    log.warn("Something is wrong with item $i, ", rawEntriesList.item(i).textContent)
                } else {
                    processSuppliersArticleNo(it)
                    detailLineList.add(it)
                }
            }
        }

        return detailLineList
    }

    fun parseEntry(item: ElementImpl?): DetailLine? {

        if (item == null) {
            return null
        }

        val line = DetailLine()
        line.SuppliersArticleNo = item.getElementsByTagName("SuppliersArticleNo").item(0).textContent
        line.SuppliersDescription = item.getElementsByTagName("SuppliersDescription").item(0).textContent
        line.UnitQtyDelivered = item.getElementsByTagName("UnitQtyDelivered").item(0).textContent
        line.UnitNetPrice = item.getElementsByTagName("UnitNetPrice").item(0)?.textContent ?: "0"

        return line
    }

    fun processSuppliersArticleNo(detailLine: DetailLine) {
        if (detailLine.isOK()) {
            var code = detailLine.SuppliersArticleNo.split("-")[0] +
                    String.format("KOL%04d", Integer.parseInt(detailLine.SuppliersArticleNo.split("-")[1]))

            if (code.startsWith("772607")) {
                code = code.replace("772607", "736325", true)
            }

            if (code.startsWith("715328")) {
                code = code.replace("715328", "700835", true)
            }

            detailLine.ProcessedSuppliersArticleNo = code
        }
    }
}