package pl.asobczyk.invoice.converter

import com.sun.org.apache.xerces.internal.dom.ElementImpl
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class DetailLine(
        var SuppliersArticleNo: String = "",
        var SuppliersDescription: String = "",
        var UnitQtyDelivered: String = "",
        var UnitNetPrice: String = ""
)

fun toCSVString(line: DetailLine): String {
    var code = line.SuppliersArticleNo.split("-")[0] +
            String.format("KOL%04d", Integer.parseInt(line.SuppliersArticleNo.split("-")[1]))

    if (code.startsWith("772607")) {
        code = code.replace("772607", "736325", true)
    }

    if (code.startsWith("715328")) {
        code = code.replace("715328", "700835", true)
    }

    return code + ";" + line.UnitQtyDelivered + ";" + line.UnitNetPrice
}


fun readXml(): Document {
    val xmlFile = File("GUETERMANN_INVOICE.xml")

    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(xmlFile.readText()))
    val doc = dBuilder.parse(xmlInput)
    doc.normalizeDocument()

    val list = doc.getElementsByTagName("DetailLine")

    for (i in 0 until list.length) {
        val elem = list.item(i) as ElementImpl
        val line = DetailLine()
        line.SuppliersArticleNo = elem.getElementsByTagName("SuppliersArticleNo").item(0).textContent;
        line.SuppliersDescription = elem.getElementsByTagName("SuppliersDescription").item(0).textContent;
        line.UnitQtyDelivered = elem.getElementsByTagName("UnitQtyDelivered").item(0).textContent;
        line.UnitNetPrice = elem.getElementsByTagName("UnitNetPrice").item(0).textContent;
        println(toCSVString(line))

    }

    return doc
}

fun processXml(content: String): String {
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(content))
    val doc = dBuilder.parse(xmlInput)
    doc.normalizeDocument()

    val list = doc.getElementsByTagName("DetailLine")
    val sb = StringBuilder()

    for (i in 0 until list.length) {
        val elem = list.item(i) as ElementImpl
        val line = DetailLine()
        line.SuppliersArticleNo = elem.getElementsByTagName("SuppliersArticleNo").item(0).textContent;
        line.SuppliersDescription = elem.getElementsByTagName("SuppliersDescription").item(0).textContent;
        line.UnitQtyDelivered = elem.getElementsByTagName("UnitQtyDelivered").item(0).textContent;
        line.UnitNetPrice = elem.getElementsByTagName("UnitNetPrice").item(0).textContent;
        sb.append(toCSVString(line) + "\n")
    }

    return sb.toString()
}