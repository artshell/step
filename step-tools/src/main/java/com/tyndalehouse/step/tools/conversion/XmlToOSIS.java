package com.tyndalehouse.step.tools.conversion;

import com.tyndalehouse.step.core.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.jasypt.util.text.BasicTextEncryptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.*;

/**
 * Joins all the Biblica XML files together
 *
 * @author chrisburrell
 */
public class XmlToOSIS {
    private final String otPath;
    private final String ntPath;
    private final String outputPath;
    private String pathToOsis2Mod;
    private String conversionType;
    private String encryptKeyForNIV;
    private String stepObfuscationKey;

    public XmlToOSIS(final String otPath, final String ntPath, final String outputPath,
                     final String pathToOsis2Mod,
                     final String conversionType,
                     final String encryptKeyForNIV, final String stepObfuscationKey) {
        this.otPath = otPath;
        this.ntPath = ntPath;
        this.outputPath = outputPath;
        this.pathToOsis2Mod = pathToOsis2Mod;
        this.conversionType = conversionType;
        this.encryptKeyForNIV = encryptKeyForNIV;
        this.stepObfuscationKey = stepObfuscationKey;
    }

    private void parse() throws Exception {
        long timeNow = System.currentTimeMillis();
        final List<File> fileList = readFiles();
        System.out.println(String.format("Read files... [%dms]", System.currentTimeMillis() - timeNow));
        timeNow = System.currentTimeMillis();

        //now need to read in all files
        File[] files = fileList.toArray(new File[0]);
//        File f = File.createTempFile("cjb-", "xml");
        File f = new File("c:\\temp\\usx.xml");

        System.out.println("Merging...");
        timeNow = System.currentTimeMillis();
        final Document input = merge(getRootName(), files);
        System.out.println(String.format("Merged... [%dms]", System.currentTimeMillis() - timeNow));

        System.out.println("Transforming...");
        timeNow = System.currentTimeMillis();
        applyXslt(input, f);
        System.out.println(String.format("Transforming... [%dms]", System.currentTimeMillis() - timeNow));
        timeNow = System.currentTimeMillis();
        System.out.println(f.getAbsolutePath());

        //now call osis2mod to make the module
        timeNow = System.currentTimeMillis();
        System.out.println(String.format("Moving nodes..."));
        timeNow = System.currentTimeMillis();
        System.out.println(String.format("Converting..."));
//        convertToSwordModule(f);
        System.out.println(String.format("Converted... [%dms]", System.currentTimeMillis() - timeNow));
    }

    private String getRootName() {
        switch (this.conversionType) {
            case "biblica":
                return "/biblicaDocument/scripture";
            case "usx":
                return "/usx";
        }
        throw new ConversionException("Unable to identify type of conversion required");
    }

    private void convertToSwordModule(final File f) throws IOException, InterruptedException {
        System.out.println("Converting to sword module...");

        BasicTextEncryptor bte = new BasicTextEncryptor();
        bte.setPassword(this.stepObfuscationKey);
        System.out.println("Key for conf file: " + bte.encrypt(this.encryptKeyForNIV));

        Process p = Runtime.getRuntime().exec(String.format("%s %s %s -z -c %s", this.pathToOsis2Mod, this.outputPath, f.getAbsolutePath(), encryptKeyForNIV));
        String line;
        BufferedReader inputReader =
                new BufferedReader
                        (new InputStreamReader(p.getInputStream()));
        while ((line = inputReader.readLine()) != null) {
            System.out.println(line);
        }
        p.waitFor();
        inputReader.close();
    }

    private void applyXslt(final Document input, File output) throws Exception {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer(
                new StreamSource(getClass().getResourceAsStream(String.format("/transform-%s.xsl", this.conversionType))));
        final DOMResult outputTarget = new DOMResult();
        transformer.transform(new DOMSource(input), outputTarget);


        Document n = (Document) outputTarget.getNode();
        moveNodes(n);
        Transformer fileTransformer = tFactory.newTransformer();
        fileTransformer.transform(new DOMSource(n), new StreamResult(new FileOutputStream(output)));
    }

    private static List<Node> nodes = new ArrayList<>();

    private static void moveNodes(Node node) {
        // do something with the current node instead of System.out
//        System.out.println(node.getNodeName());

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                //record nodes that are marked as to be moved
                if ("pre-verse".equals(((Element) currentNode).getAttribute("step"))) {
                    nodes.add(currentNode);
                }

                if ("verse".equals(currentNode.getNodeName())) {
                    Node parent = currentNode.getParentNode();
                    final Iterator<Node> iterator = nodes.iterator();
                    while (iterator.hasNext()) {
                        final Node next = iterator.next();
                        parent.insertBefore(next, currentNode);
                        ((Element) next).removeAttribute("step");
                        iterator.remove();

                    }
                }
                //calls this method for all the children which is Element
                moveNodes(currentNode);
            }
        }
    }

    private Document merge(String expression,
                           File... files) throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression compiledExpression = xpath
                .compile(expression);
        return merge(compiledExpression, files);
    }

    private Document merge(XPathExpression expression,
                           File... files) throws Exception {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        docBuilderFactory
                .setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = docBuilderFactory
                .newDocumentBuilder();
        Document base = docBuilder.parse(files[0]);

        Node results = (Node) expression.evaluate(base,
                XPathConstants.NODE);
        if (results == null) {
            throw new IOException(files[0]
                    + ": expression does not evaluate to node");
        }

        for (int i = 1; i < files.length; i++) {
            Document merge = docBuilder.parse(files[i]);
            Node nextResults = (Node) expression.evaluate(merge,
                    XPathConstants.NODE);
            while (nextResults.hasChildNodes()) {
                Node kid = nextResults.getFirstChild();
                nextResults.removeChild(kid);
                kid = base.importNode(kid, true);
                results.appendChild(kid);
            }
        }

        return base;
    }

    private void print(OutputStream os, Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory
                .newTransformer();
        DOMSource source = new DOMSource(doc);
        Result result = new StreamResult(os);
        transformer.transform(source, result);
    }


    private List<File> readFiles() {
        List<File> files = new ArrayList<>();
        readFiles(files, otPath);
        //nt path is optional
        if (StringUtils.isNotBlank(this.ntPath)) {
            readFiles(files, ntPath);
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(final File o1, final File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return files;
    }

    private void readFiles(final List<File> files, final String path) {
        files.addAll(FileUtils.listFiles(new File(path), new String[]{getExtension()}, false));
//        for (Iterator<File> iterator = files.iterator(); iterator.hasNext(); ) {
//            final File f = iterator.next();
//            if (!f.getName().contains("PSA")) {
//                iterator.remove();
//            }
//        }
    }

    private String getExtension() {
        switch (this.conversionType) {
            case "biblica":
                return "xml";
            case "usx":
                return "usx";
        }
        throw new ConversionException("Conversion type not supported: " + this.conversionType);
    }

    public static void main(String[] args) throws Exception {
        String otPath = "C:\\temp\\usx";
        String ntPath = null;
        String outputPath = "C:\\Users\\Chris\\AppData\\Roaming\\Sword\\modules\\texts\\ztext\\rom";
        String osis2mod = "C:\\dev\\personal\\sword-utilities-1.7.0-1\\osis2mod";
        String type = "usx";
        String key = "aaaaaaaa";
        String obfuscation = "bbbbbbbb";

        if (args.length == 6) {
            otPath = args[0];
            ntPath = args[1];
            outputPath = args[2];
            osis2mod = args[3];
            type = args[4];
            key = args[5];
            obfuscation = args[6];
        } else {
            System.out.println("!!!!!! Ignoring parameters on command line !!!!!!");
        }

        new XmlToOSIS(otPath, ntPath, outputPath, osis2mod, type, key, obfuscation).parse();
    }
}
