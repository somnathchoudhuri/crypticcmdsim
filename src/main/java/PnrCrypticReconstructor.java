import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.namespace.NamespaceContext; // NEW IMPORT
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator; // NEW IMPORT
import java.util.List;

/**
 * PnrCrypticReconstructor: A utility to simulate the conversion of structured
 * PNR XML content (e.g., from GetReservationRS) back into the sequence of
 * cryptic Host commands used to create the PNR.
 */
class PnrNamespaceContext implements NamespaceContext {
    private static final String PNR_NAMESPACE_URI = "http://example.com/pnr";
    private static final String PNR_PREFIX = "pnr";

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix.equals(PNR_PREFIX)) {
            return PNR_NAMESPACE_URI;
        }
        return null;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        if (namespaceURI.equals(PNR_NAMESPACE_URI)) {
            return PNR_PREFIX;
        }
        return null;
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        // Not strictly required for this simple application
        return null;
    }
}

public class PnrCrypticReconstructor {

    // --- Simulated PNR XML Payload ---
    private static final String SIMULATED_PNR_XML =
        "<Reservation xmlns=\"http://example.com/pnr\">" +
        "    <PNR_Summary Locator=\"ABCDEF\"/>" +
        "    <Travelers>" +
        "        <Passenger id=\"1\" LastName=\"JONES\" FirstName=\"ALAN\" PaxType=\"ADT\"/>" +
        "        <Passenger id=\"2\" LastName=\"JONES\" FirstName=\"DORIS\" PaxType=\"ADT\"/>" +
        "    </Travelers>" +
        "    <Itinerary>" +
        "        <AirSegment id=\"S1\" Carrier=\"DL\" FlightNumber=\"742\" Class=\"Y\" " +
        "                    DepartureDate=\"2025-10-15\" Origin=\"ATL\" Destination=\"BOS\" " +
        "                    Status=\"HK\" Seats=\"2\"/>" +
        "    </Itinerary>" +
        "    <Contacts>" +
        "        <Phone Number=\"5551234\" AreaCode=\"214\" Type=\"B\"/>" +
        "        <Email Address=\"alan.jones@corp.com\" Type=\"B\"/>" +
        "    </Contacts>" +
        "    <AdminData>" +
        "        <ReceivedFrom Name=\"LINDA\"/>" +
        "        <Ticketing Deadline=\"2025-09-17\" Type=\"TAU\"/>" +
        "        <FormOfPayment Type=\"CC\" Code=\"AX\" Number=\"1234567890123456\" Expiration=\"05/26\"/>" +
        "    </AdminData>" +
        "</Reservation>";

    private final XPath xpath;
    private final Document pnrDocument;

    public PnrCrypticReconstructor(String pnrXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Crucial: Must be namespace aware to process the 'xmlns' declaration
        factory.setNamespaceAware(true); 
        DocumentBuilder builder = factory.newDocumentBuilder();
        this.pnrDocument = builder.parse(new InputSource(new StringReader(pnrXml)));
        
        // FIX: Configure XPath engine with the custom namespace context
        XPathFactory xpathFactory = XPathFactory.newInstance();
        this.xpath = xpathFactory.newXPath();
        this.xpath.setNamespaceContext(new PnrNamespaceContext());
    }

    /**
     * Reconstructs the entire sequence of cryptic commands required to create the PNR.
     */
    public List<String> reconstructCommands() throws Exception {
        List<String> commands = new ArrayList<>();

        System.out.println("--- Starting Cryptic Command Reconstruction ---");
        System.out.println("NOTE: This simulates the original inputs based on retrieved PNR content.\n");

        // 1. Mandatory Name Entry (Prefix 1) [1]
        commands.addAll(reconstructNames());

        // 2. Air Segment Sell (Prefix 0) [2]
        commands.addAll(reconstructAirSegments());

        // 3. Mandatory Phone Entry (Prefix 9 or P) [1]
        commands.addAll(reconstructPhone());

        // 4. Mandatory Received From (Prefix R or 6) [1]
        commands.addAll(reconstructReceivedFrom());

        // 5. Mandatory Ticketing Field (Prefix T or 7) [1]
        commands.addAll(reconstructTicketing());

        // 6. Form of Payment / Remarks (Prefix 5-*) [2]
        commands.addAll(reconstructFormOfPayment());
        
        System.out.println("\n--- PNR Committed with End Transaction (ET) Command ---");
        commands.add("ET"); // Final command to commit the record [3, 4]

        return commands;
    }

    private List<String> reconstructNames() throws Exception {
        List<String> names = new ArrayList<>();
        // FIX: Using namespace prefix 'pnr'
        NodeList passengerNodes = (NodeList) xpath.evaluate("//pnr:Passenger", pnrDocument, XPathConstants.NODESET);
        int totalPassengers = passengerNodes.getLength();

        if (totalPassengers > 0) {
            StringBuilder nameCommand = new StringBuilder();
            Node firstPassenger = passengerNodes.item(0);
            String lastName = firstPassenger.getAttributes().getNamedItem("LastName").getNodeValue();

            // Structure: 1[Count].[LastName]/[FirstName1]/[FirstName2]/... [1]
            nameCommand.append("1.").append(totalPassengers).append(lastName).append("/");
            for (int i = 0; i < totalPassengers; i++) {
                Node pNode = passengerNodes.item(i);
                nameCommand.append(pNode.getAttributes().getNamedItem("FirstName").getNodeValue());
                if (i < totalPassengers - 1) {
                    nameCommand.append("/");
                }
            }
            names.add(nameCommand.toString());
            System.out.println("Names (1): " + nameCommand.toString());
        }
        return names;
    }

    private List<String> reconstructAirSegments() throws Exception {
        List<String> segments = new ArrayList<>();
        // FIX: Using namespace prefix 'pnr'
        NodeList segmentNodes = (NodeList) xpath.evaluate("//pnr:AirSegment", pnrDocument, XPathConstants.NODESET);

        for (int i = 0; i < segmentNodes.getLength(); i++) {
            Node segment = segmentNodes.item(i);
            String carrier = segment.getAttributes().getNamedItem("Carrier").getNodeValue();
            String flightNum = segment.getAttributes().getNamedItem("FlightNumber").getNodeValue();
            String bookingClass = segment.getAttributes().getNamedItem("Class").getNodeValue();
            String depDateStr = segment.getAttributes().getNamedItem("DepartureDate").getNodeValue();
            String origin = segment.getAttributes().getNamedItem("Origin").getNodeValue();
            String destination = segment.getAttributes().getNamedItem("Destination").getNodeValue();
            String seats = segment.getAttributes().getNamedItem("Seats").getNodeValue();
            
            // Convert YYYY-MM-DD to GDS format DDMON
            LocalDate depDate = LocalDate.parse(depDateStr);
            String gdsDate = depDate.format(DateTimeFormatter.ofPattern("ddMMM")).toUpperCase();

            // Long Sell Format: 0[Carrier][Flight#][Class][CityPair]NN
            // Status (HK/SS) replaced with request status (NN - Need) [2, 5]
            String command = String.format("0%s%s%s%s%s%sNN%s", 
                carrier, flightNum, bookingClass, gdsDate, origin, destination, seats);
            
            segments.add(command);
            System.out.println("Segment " + (i + 1) + " (0): " + command);
        }
        return segments;
    }

    private List<String> reconstructPhone() throws Exception {
        List<String> phones = new ArrayList<>();
        // FIX: Using namespace prefix 'pnr'
        NodeList phoneNodes = (NodeList) xpath.evaluate("//pnr:Phone", pnrDocument, XPathConstants.NODESET);

        for (int i = 0; i < phoneNodes.getLength(); i++) {
            Node phone = phoneNodes.item(i);
            String number = phone.getAttributes().getNamedItem("Number").getNodeValue();
            String areaCode = phone.getAttributes().getNamedItem("AreaCode").getNodeValue();
            String type = phone.getAttributes().getNamedItem("Type").getNodeValue(); // B, H, A (Business, Home, Agency)

            // Format: 9[AreaCode]-[Number]-[1]
            String command = String.format("9%s-%s-%s", areaCode, number, type);
            phones.add(command);
            System.out.println("Phone (9): " + command);
        }
        return phones;
    }

    private List<String> reconstructReceivedFrom() throws Exception {
        List<String> received = new ArrayList<>();
        // FIX: Using namespace prefix 'pnr'
        Node receivedFromNode = (Node) xpath.evaluate("//pnr:ReceivedFrom", pnrDocument, XPathConstants.NODE);

        if (receivedFromNode!= null) {
            String name = receivedFromNode.getAttributes().getNamedItem("Name").getNodeValue();
            // Format: R.[Name] or 6[Name][1]
            String command = "R." + name;
            received.add(command);
            System.out.println("Received From (R): " + command);
        }
        return received;
    }

    private List<String> reconstructTicketing() throws Exception {
        List<String> ticketing = new ArrayList<>();
        // FIX: Using namespace prefix 'pnr'
        Node ticketingNode = (Node) xpath.evaluate("//pnr:Ticketing", pnrDocument, XPathConstants.NODE);

        if (ticketingNode!= null) {
            String dateStr = ticketingNode.getAttributes().getNamedItem("Deadline").getNodeValue();
            
            // Convert YYYY-MM-DD to GDS format DDMON
            LocalDate deadlineDate = LocalDate.parse(dateStr);
            String gdsDate = deadlineDate.format(DateTimeFormatter.ofPattern("ddMMM")).toUpperCase();

            // Ticketing Format: T.TAU/[1]
            String command = "T.TAU/" + gdsDate;
            ticketing.add(command);
            System.out.println("Ticketing (T): " + command);
        }
        return ticketing;
    }

    private List<String> reconstructFormOfPayment() throws Exception {
        List<String> fop = new ArrayList<>();
        // FIX: Using namespace prefix 'pnr'
        Node fopNode = (Node) xpath.evaluate("//pnr:FormOfPayment", pnrDocument, XPathConstants.NODE);

        if (fopNode!= null) {
            String type = fopNode.getAttributes().getNamedItem("Type").getNodeValue();
            String code = fopNode.getAttributes().getNamedItem("Code").getNodeValue();

            if ("CC".equals(type)) {
                String number = fopNode.getAttributes().getNamedItem("Number").getNodeValue();
                String expiration = fopNode.getAttributes().getNamedItem("Expiration").getNodeValue();

                // Credit Card FOP: 5-*[Code][Number]‡[Expiration]
                String separator = "‡"; 
                String command = String.format("5-*%s%s%s%s", code, number, separator, expiration); // The asterisk (*) is mandatory for validation [1, 2]
                fop.add(command);
                System.out.println("FOP/Remark (5-*): " + command);
            } else if ("CASH".equals(type)) {
                 // Simple FOP: 5-CASH [2]
                fop.add("5-CASH");
                System.out.println("FOP/Remark (5-): 5-CASH");
            }
        }
        return fop;
    }

    public static void main(String args[]) { 
        try {
            PnrCrypticReconstructor reconstructor = new PnrCrypticReconstructor(SIMULATED_PNR_XML);
            List<String> crypticCommands = reconstructor.reconstructCommands();

            System.out.println("\n=======================================================");
            System.out.println("FINAL RECONSTRUCTED CRYPTIC WORKFLOW");
            System.out.println("=======================================================");
            for (String command : crypticCommands) {
                System.out.println(command);
            }
            System.out.println("=======================================================");

        } catch (Exception e) {
            System.err.println("An error occurred during PNR reconstruction:");
            e.printStackTrace();
        }
    }
}