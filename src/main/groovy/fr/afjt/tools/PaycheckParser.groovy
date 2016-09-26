package fr.afjt.tools

import com.itextpdf.awt.geom.Rectangle
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.*
import groovy.json.JsonSlurper

class PaycheckParser {

    static void main(String... args) {
        println "Paycheck Parser for A.F.J.T."

        def configFile = new File("./config.json")
        def jsonConfig = new JsonSlurper().parseText(configFile.text)
        jsonConfig.data.each {
            println it.name
        }

        def cli = new CliBuilder(usage: 'run.sh <pdfFiles>')
        cli.paycheck(args: 1, argName: 'file', 'use given file for paycheck input')

        def options = cli.parse(args)

        if (options.paycheck) {
            def parser = new PaycheckParser()
            println "Paycheck file : " + options.paycheck
            parser.parse(options.paycheck, jsonConfig)
        }
    }

    def parse(String filename, jsonConfig) {
        PdfReader reader = new PdfReader(filename);

        jsonConfig.data.each {

            Rectangle rect = new Rectangle(it.location.x, it.location.y, it.location.width, it.location.height)

            RenderFilter[] filter = new RegionTextRenderFilter(rect);
            TextExtractionStrategy strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
            println "Text parsed for '${it.name}':" + PdfTextExtractor.getTextFromPage(reader, it.location.page, strategy)

        }

        if (jsonConfig.scan) {
            for (int y = 0; y < 600; y += 15) {
                for (int x = 0; x < 800; x += 50) {
                    Rectangle rect = new Rectangle(x, y, 50, 15)
                    RenderFilter[] filter = new RegionTextRenderFilter(rect);
                    TextExtractionStrategy strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
                    String text = PdfTextExtractor.getTextFromPage(reader, 1, strategy)
                    if (text != '') {
                        println "${x},${y}:"+text
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