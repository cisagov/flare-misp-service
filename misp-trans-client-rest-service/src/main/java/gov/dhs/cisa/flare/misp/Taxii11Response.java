package gov.dhs.cisa.flare.misp;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class Taxii11Response {
    private static Logger log = LoggerFactory.getLogger(Taxii11Response.class);
    public static final String PREFIX_NS = "taxii_11:";

    @Autowired
    MispTransHelper mispTransHelper;

    public static String removeXmlNs(String tag) {
        if (tag.indexOf(PREFIX_NS) == 0) {
            return tag.replace(PREFIX_NS, "");
        }
        return tag;
    }

    public Document processPollResponse(ResponseEntity<String> response) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            InputSource is = new InputSource(new StringReader(response.getBody()));
            Document respDoc = builder.parse(is);
            log.debug("\nDOM Document: {}", respDoc.getNodeName());
            return respDoc;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Node getPollRequest(Document doc) {
        NodeList nodes = doc.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node curNode = nodes.item(i);
            String tag = removeXmlNs(curNode.getNodeName());
            if (tag.contains("Poll_Response")) {
                log.info("Found Poll_Response.");
                return curNode;
            }
        }
        log.info("Poll_Response is not found");
        return null;
    }

    public List<Node> getContentBlocks(Node pollResponse) {
        List<Node> contentBlocks = new ArrayList<Node>();

        NodeList nodes;
        if (pollResponse != null && pollResponse.hasChildNodes()) {
            nodes = pollResponse.getChildNodes();

            for (int i = 0; i < nodes.getLength(); i++) {
                Node curNode = nodes.item(i);
                if (curNode.getNodeType() == Node.TEXT_NODE) {
                    continue;
                }
                String tag = removeXmlNs(curNode.getNodeName());
                if (tag.contains("Content_Block")) {
                    contentBlocks.add(curNode);
                } else {
                    log.debug("Found {}", tag);
                }
            }
        } else {
            log.info("No data from response.");
        }

        log.info("Found [{}] Content_Blocks", contentBlocks.size());
        return contentBlocks;
    }

    public List<Node> getStixPackages(List<Node> contentBlocks) {
        List<Node> stixPackages = new ArrayList<Node>();
        contentBlocks.stream().forEach(contentBlock -> {
            List<Node> packages = getStixPackage(contentBlock);
            packages.stream().forEach(stixPackage -> {
                stixPackages.add(stixPackage);
            });
        });
        log.info("Found [{}] STIX_Packages", stixPackages.size());
        return stixPackages;
    }

    private List<Node> getStixPackage(Node contentBlock) {
        List<Node> stixPackages = new ArrayList<Node>();
        NodeList nodes = contentBlock.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node curNode = nodes.item(i);
            if (curNode.getNodeType() == Node.TEXT_NODE) {
                continue;
            }
            String tag = removeXmlNs(curNode.getNodeName());
            if (tag.contains("Content")) {
                NodeList packageNodes = curNode.getChildNodes();
                for (int j = 0; j < packageNodes.getLength(); j++) {
                    Node stixPackage = packageNodes.item(j);
                    if (stixPackage.getNodeName().contains("STIX_Package")) {
                        stixPackages.add(stixPackage);
                    }
                }
            } else {
                log.debug("Found {}", tag);
            }
        }

        return stixPackages;
    }

    public void processPollResponse(List<Node> stixPackages, String processType) {
        log.info("Taxii11Response:processPollResponse:processType: {}", processType);
        switch (processType) {
            case "stixToMisp":
                storeStixPackagesToMisp(stixPackages);
                break;
            case "xmlOutput":
                storeStixPackagesToFile(stixPackages);
                break;
            default:
                break;
        }
    }

    private void storeStixPackagesToMisp(List<Node> stixPackages) {
        stixPackages.stream().forEach(stixPackage -> {
            String stixId = stixPackage.getAttributes().getNamedItem("id").getNodeValue();
            try {
                sendToMisp(stixId, Taxii11Request.toXml(stixPackage));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
    }

    private HttpHeaders makeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set("Content-type", "application/xml");
        headers.set("Accept", "application/xml");
        headers.set("Authorization", Config.getProperty("misp.authorization.key"));
        return headers;
    }

    private void sendToMisp(String stixId, String stixPackageXml) throws IOException {
        Tracker tracker = new Tracker();
        tracker.setStixId(stixId);

        if (mispTransHelper.exists(tracker)) {
            log.debug("STIX Package exists: skip sending to MISP >>>>>>>>>>>>>>>>>> {}", stixId);
            return;
        }

        log.info("Storing to MISP Server StixId: {}", stixId);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(stixPackageXml, makeHeaders());
        ResponseEntity<String> response;

        String url = Config.getProperty("misptransclient.post.baseurl");
        log.debug("Pacakge stixId : {} \nstix-body:\n{} \nSend To MISP Server URL:{}", stixId, stixPackageXml, url);

        try {
            response = restTemplate.exchange(new URI(url), HttpMethod.POST, entity, String.class);
            tracker.setStatusCode(response.getStatusCodeValue());
            log.debug("Response Body :\n{}", response.getBody());
            log.debug("Response Status Code: {}", response.getStatusCodeValue());
            tracker.setEventId(getMispMispEventId(response.getBody()));
            log.info("StixId: {} ------ Status Code: {} ", tracker.getStixId(), tracker.status_code);
        } catch (Exception e) {
            log.debug(">>> FIELD TO STORE TO MISP:{}, Status: {}", stixId, tracker.getStatusCode());
        }

        mispTransHelper.serializeUser(tracker);
    }

    private String getMispMispEventId(String body) {
        JSONObject json = new JSONObject(body);
        JSONObject event = json.getJSONObject("Event");
        return String.valueOf(event.getInt("id"));
    }

    private void storeStixPackagesToFile(List<Node> stixPackages) {
        stixPackages.stream().forEach(stixPackage -> {
            String stixId = stixPackage.getAttributes().getNamedItem("id").getNodeValue();
            if (stixId.contains(":")) {
                stixId = stixId.replaceAll(":", "-");
            }

            log.info("storing to file {}", stixId);

            try {
                storeToFile(stixId, Taxii11Request.toXml(stixPackage));
            } catch (TransformerException | IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    private void storeToFile(String stixId, String stixPackageXml) throws IOException {
        log.debug("storing {} to file {}", stixPackageXml, stixId);
        String fileName = Config.getOutputFilePath(stixId);
        log.info("storing into file {}", fileName);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)))) {
            bw.write(stixPackageXml);
        }
    }
}
