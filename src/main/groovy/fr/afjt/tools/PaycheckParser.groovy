package fr.afjt.tools

import com.itextpdf.awt.geom.Rectangle
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.*
import groovy.json.JsonSlurper

class PaycheckParser {

    static void main(String... args) {
        println "Paycheck Parser for A.F.J.T."

        // Load the json config file
        def configFile = new File("./config.json")
        def jsonConfig = new JsonSlurper().parseText(configFile.text)

        // Parse the command line arguments
        def cli = new CliBuilder(
                usage: 'run.sh [options] [pdf files or folders]',
                header: '\nAvailable options (use -h for help):\n')
        cli.with {
            h(longOpt: 'help', 'print this message')
            vo(longOpt: "verticalOffset", args: 1, argName: 'offset', 'positive offset for vertical coordinates')
            nvo(longOpt: "negativeVerticalOffset", args: 1, argName: 'offset', 'negative offset for vertical coordinates')
            q('quiet mode, print only the short file name and the fields value separated by tabs')
            convertTime('convert <hour>h<minute>min time to fractional hours')
            H(longOpt: 'hightlight', 'Hightlight the configured zones in a copy of the pdf files')
        }
        def options = cli.parse(args)

        if (options.h) {
            cli.usage()
            return
        }

        // Override quiet mode from command line
        if (options.q) {
            jsonConfig.quiet = new Boolean(options.q)
        }

        // Override verticalOffset from command line
        // Because CliBuilder returns "15" when parsing a negative value "-15",
        // and no other work-around could be found, we have to use 2 parameters
        if (options.vo) {
            jsonConfig.verticalOffset = options.vo
        }else if (options.nvo){
            jsonConfig.verticalOffset = "-"+options.nvo
        }

        // Override convertTime from command line
        if (options.convertTime) {
            jsonConfig.convertTime = options.convertTime
        }

        // Override hightlight mode from command line
        if (options.H) {
            jsonConfig.highlight = new Boolean(options.H)
        }

        // Each argument represent either a file name or a folder name
        if (options.arguments()) {
            def parser = new PaycheckParser()
            def files = new ArrayList<File>()
            options.arguments().each {
                files << new File(it)
            }

            files.each { f ->
                if (f.isDirectory()) {
                    println "-- ${f.name} --"

                    f.eachFile {
                        print "${it.name - '.pdf'} :"
                        parser.parse(it.canonicalPath, jsonConfig)
                    }
                } else {
                    println "Paycheck file : ${f}"
                    parser.parse(f.canonicalPath, jsonConfig)
                }
            }
        } else {
            cli.usage()
        }
    }

    def parse(String filename, jsonConfig) {
        PdfReader reader = new PdfReader(filename);
        PdfStamper pdfStamper = new PdfStamper(reader,
                new FileOutputStream(filename - ".pdf" + "highlighted.pdf"));

        jsonConfig.data.each {
            float y = it.location.y + (it.fixedPosition ? 0 : jsonConfig.verticalOffset.toFloat())
            Rectangle rect = new Rectangle(it.location.x, it.location.y + (it.fixedPosition ? 0 : jsonConfig.verticalOffset.toFloat()), it.location.width, it.location.height)
            RenderFilter[] filter = new RegionTextRenderFilter(rect);
            TextExtractionStrategy strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
            String value = PdfTextExtractor.getTextFromPage(reader, it.location.page, strategy)

            if (jsonConfig.highlight) {
                PdfContentByte cb = pdfStamper.getUnderContent(it.location.page);
                cb.setCMYKColorStroke(255, 255, 0, 0)
                cb.setCMYKColorFill(0, 0, 255, 0)
                cb.setLineWidth(2f);
                cb.rectangle(it.location.x.toFloat(), it.location.y.toFloat() + (it.fixedPosition ? 0 : jsonConfig.verticalOffset.toFloat()), it.location.width.toFloat(), it.location.height.toFloat())
                cb.fill()
            }
            // convert timestamp from format <hour>h<minute>min to fractional hours
            if (it.type == "time" && jsonConfig.convertTime) {
                // do the conversion
                def pattern = /([^h]+)h([^m]+)min/
                def group = (value =~ /$pattern/)
                assert group.hasGroup()
                assert 1 == group.size()

                def hours = (group[0][1]).toFloat();
                def minutes = (group[0][2]).toFloat();
                def fractionalHours = hours + (minutes / 60.0)

                value = sprintf('%.2f', fractionalHours)
            }

            if (jsonConfig.quiet) {
                print value + '\t'
            } else {
                println "Text parsed for '${it.name}':" + value
            }

        }
        if (jsonConfig.highlight) {
            pdfStamper.close()
        }
        if (jsonConfig.scan) {
            for (int y = 0; y < 600; y += 15) {
                for (int x = 0; x < 800; x += 50) {
                    Rectangle rect = new Rectangle(x, y, 50, 15)
                    RenderFilter[] filter = new RegionTextRenderFilter(rect);
                    TextExtractionStrategy strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
                    String text = PdfTextExtractor.getTextFromPage(reader, 1, strategy)
                    if (text != '') {
                        println "${x},${y}:" + text
                    }
                }
            }
        }
    }
}