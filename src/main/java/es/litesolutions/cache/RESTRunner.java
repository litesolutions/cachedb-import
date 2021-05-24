package es.litesolutions.cache;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RESTRunner extends Runner {

    final String url;

    final String[] SYSTEMDB = {"CACHELIB", "IRISLIB", "CACHESYS", "IRISSYS"};

    final static CookieManager cm = new CookieManager();;

    final Map<String, String> arguments;

    public RESTRunner(String url, final Map<String, String> arguments) throws IOException
    {
        super(null);
        this.url = url;
        this.arguments = arguments;
    }

    @Override
    public void importStream(Path path) throws IOException
    {
        System.out.println("importStream: " + path.toString());

    }

    @Override
    public Set<String> listItems(boolean includeSys) throws IOException
    {
        JSONObject result = this.get("/docnames/*/cls,int,mac,inc?generated=0");
        final Set<String> set = new HashSet<>();

        JSONArray content = result.getJSONObject("result").getJSONArray("content");
        for (Object item : content) {
            String name = ((JSONObject) item).getString("name");
            String fromdb = ((JSONObject) item).getString("db");
            if (!includeSys && (name.startsWith("%") || Arrays.stream(SYSTEMDB).anyMatch(fromdb::equals))) {
                continue;
            }
            set.add(name);
        }

        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<String> importFile(Path path, boolean includeSys) throws IOException
    {
        byte[] encoded = Files.readAllBytes(path);
        final Set<String> set = new HashSet<>();
        HashMap<String, String> files = new HashMap<>();
        if (FileSystems.getDefault().getPathMatcher("glob:*.xml").matches(path.getFileName())) {
            files = extractItemsFromXMLFile(path);
        } else if (FileSystems.getDefault().getPathMatcher("glob:*.ro").matches(path.getFileName())) {
            files = extractItemsFromROFile(path);
        }

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String name = entry.getKey();
            JSONObject request = new JSONObject();
            request.put("enc", false);
            JSONArray contentLines = new JSONArray(entry.getValue().split("\n"));
            request.put("content", contentLines);

            System.out.printf("Import '%s' from '%s'\n", name, path);
            JSONObject response = this.put("/doc/" + name + "?ignoreConflict=1", request);
            set.add(name);
        }

        return Collections.unmodifiableSet(set);
    }

    private HashMap<String, String> extractItemsFromXMLFile(Path path)
    {
        HashMap<String, String> files = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(path.toString())) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setValidating(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(fis);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/Export/*";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(
                    doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String name = node.getAttributes().getNamedItem("name").getNodeValue();
                String nodeType = node.getNodeName();
                if ("Class".equals(nodeType)) {
                    name += ".cls";
                } else if ("Routine".equals(nodeType)) {
                    name += "." + node.getAttributes().getNamedItem("type").getNodeValue().toLowerCase();
                } else {
                    continue;
                }
                Document newDoc = builder.newDocument();
                Node exportNode = newDoc.createElement("Export");
                Node newNode = node.cloneNode(true);
                newDoc.adoptNode(newNode);
                exportNode.appendChild(newNode);
                newDoc.appendChild(exportNode);

                NamedNodeMap attrs = node.getParentNode().getAttributes();
                for (int j = 0; j < attrs.getLength(); j++) {
                    Node attr = attrs.item(j);
                    ((Element) exportNode).setAttribute(attr.getNodeName(), attr.getNodeValue());
                }
                files.put(name, docToString(newDoc));
            }
        } catch (Exception e) {
            System.err.print(e);
        }

        return files;
    }

    private HashMap<String, String> extractItemsFromROFile(Path path)
    {
        HashMap<String, String> files = new HashMap<>();
        // Routine^INT^1^65496,84164^0
        final Pattern routinePattern = Pattern.compile("^([^\\^]+)\\^([^\\^]+)\\^\\d+\\^\\d+,\\d+\\^\\d\\n(.*)", Pattern.DOTALL);
        // line with only one dot convert to empty line
        final Pattern codeEmptyLines = Pattern.compile("^.$", Pattern.MULTILINE);

        try (Scanner scanner = new Scanner(path.toFile())) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            String generator = "Cache";
            String roHeader = scanner.findWithinHorizon("%RO", 0) + scanner.nextLine();

            scanner.useDelimiter(Pattern.compile("\\n\\n(?=.+\\^)"));
            while (scanner.hasNext()) {
                String line = scanner.next();
                Matcher m = routinePattern.matcher(line);
                if (m.matches()) {
                    String name = m.group(1) + "." + m.group(2).toLowerCase();
                    Document newDoc = builder.newDocument();
                    Element exportNode = newDoc.createElement("Export");
                    exportNode.setAttribute("generator", generator);
                    exportNode.setAttribute("version", "25");
                    Element routineNode = newDoc.createElement("Routine");
                    routineNode.setAttribute("name", m.group(1));
                    routineNode.setAttribute("type", m.group(2));
                    newDoc.appendChild(exportNode);
                    exportNode.appendChild(routineNode);
                    String routineContent = codeEmptyLines.matcher(m.group(3)).replaceAll("");
                    routineNode.appendChild(newDoc.createCDATASection(routineContent));
                    files.put(name, docToString(newDoc));
                }
            }


        } catch (Exception e) {
            System.err.print(e);
        }

        return files;
    }

    private static String docToString(Document doc)
    {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    @Override
    public void writeClassContent(String itemName, Path path) throws IOException
    {
        System.out.printf("Export '%s' to '%s'\n", itemName, path);
        JSONObject response = this.get("/doc/" + itemName + "?format=udl");
        JSONArray content = response.getJSONObject("result").getJSONArray("content");
        BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), true));
        for (Object item : content) {
            writer.append(item + "\r\n");
        }
        writer.close();
    }

    private JSONObject put(String requestURL, JSONObject data) throws IOException
    {
        return request("PUT", requestURL, data);
    }

    private JSONObject post(String requestURL, JSONObject data) throws IOException
    {
        return request("POST", requestURL, data);
    }

    private JSONObject get(String requestURL) throws IOException
    {
        return request("GET", requestURL, null);
    }

    private JSONObject request(String method, String requestURL, JSONObject data) throws IOException
    {
        URL url = new URL(this.url + requestURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        StringBuilder cookieBuilder = new StringBuilder();
        List<HttpCookie> cookies = this.cm.getCookieStore().getCookies();
        if (cookies.size() > 0) {
            for (int i = 0; i < cookies.size(); i++) {
                cookieBuilder.append("; ").append(cookies.get(i).toString());
            }
            connection.setRequestProperty("Cookie", cookieBuilder.toString());
        } else {
            String username = this.arguments.get("user");
            String password = this.arguments.get("password");
            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encoded);
        }

        if (data != null) {
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(data.toString());
            out.close();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        List<String> cookiesHeader = connection.getHeaderFields().get("SET-COOKIE");
        if (cookiesHeader != null) {
            for (String cookie : cookiesHeader) {
                this.cm.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
            }
        }

        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        JSONObject resultObject = new JSONObject(content.toString());

        return resultObject;
    }

}
