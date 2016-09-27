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
        def cli = new CliBuilder(usage: 'run.sh <pdf files or folders>')
        cli.verticalOffset(args: 1, argName: 'offset', 'adjust vertical coordinates for grabbing text')
        cli.q('quiet mode: print only the short file name and the fields value separated by tabs')
        cli.convertTime('convert <hour>h<minute>min time to fractional hours')
        def options = cli.parse(args)

        // Override quiet mode from command line
        if (options.q) {
            jsonConfig.quiet = new Boolean(options.q)
        }

        // Override verticalOffset from command line
        if (options.verticalOffset) {
            jsonConfig.verticalOffset = options.verticalOffset
        }

        // Override convertTime from command line
        if (options.convertTime) {
            jsonConfig.convertTime = options.convertTime
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
            println cli.usage
        }
    }

    def parse(String filename, jsonConfig) {
        PdfReader reader = new PdfReader(filename);

        jsonConfig.data.each {

            Rectangle rect = new Rectangle(it.location.x, it.location.y + jsonConfig.verticalOffset.toFloat(), it.location.width, it.location.height)

            RenderFilter[] filter = new RegionTextRenderFilter(rect);
            TextExtractionStrategy strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
            String value = PdfTextExtractor.getTextFromPage(reader, it.location.page, strategy)

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
        println ''
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
        /*
             for (int page = 1; page <= 2; page++) {
                 PdfDictionary pd = reader.getPageN(page);
                 Iterator keys = pd.getKeys().iterator();
                 while (keys.hasNext())
                     println 'Debug :'+keys.next()

                 PdfObject object = pd.getDirectObject(PdfName.CONTENTS)
                 println object.class.name

                 if (object instanceof PRStream){
                     PRStream stream = (PRStream) object
                     byte[] b;
                     try {
                         b = PdfReader.getStreamBytes(stream);
                     }
                     catch(UnsupportedPdfException e) {
                         b = PdfReader.getStreamBytesRaw(stream);
                     }

                 }

                 println "Page ${page} contents below:"
                 println object.toString()

                 keys = object.getKeys().iterator()
                 while (keys.hasNext())
                     println '  '+keys.next()

                 println PdfTextExtractor.getTextFromPage(reader, page);
             }
             */
    }


    def test(String filename) {
        PdfReader reader = new PdfReader(filename);
        PdfDictionary pd = reader.getPageN(3);
        Iterator keys = pd.getKeys().iterator();
        while (keys.hasNext())
            System.out.println(keys.next());
        PdfObject object = pd.get(PdfName.CONTENTS);
        System.out.println(object.getClass().getName());
        if (object instanceof PRIndirectReference) {
            PRIndirectReference array = (PRIndirectReference) object;
            PdfReader rrr = array.getReader();
            rrr.getNumberOfPages();
            System.out.println("stop");
        }
    }
}