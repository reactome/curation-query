package org.reactome.diagrams;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;

import java.util.*;
import java.util.stream.Collectors;

public class IconFinder {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * Takes a list of db ids specified in an input file and for each entity
	 * represented by the db id, outputs a tab delimited row containing the entity's
	 * db id, display name, schema class, and a pipe delimited list of the first
	 * represented pathway in each pathway diagram instance in which the entity has
	 * its own diagram icon (this does not include icons of complexes, sets, etc.
	 * which contain the icon, but only icons which represents the entity itself)
	 * @param args Command line arguments for the IconFinder program
	 * @throws SQLException Thrown if unable to connect to the configured Reactome database
	 */
	public static void main(String[] args) throws SQLException {
		logger.info("Running IconFinder");

		String pathToResources = args.length > 0 ? args[0] : "src/main/resources/config.properties";

		Properties props = new Properties();
		try {
			props.load(new FileInputStream(pathToResources));
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<Long> inputDBIDs = getDBIDsFromInputFile(props.getProperty("input_file"));
		MySQLAdaptor dba = getDBA(props);
		Map<Long, Set<Long>> iconIdToRepresentedPathwayIDs = getIconIdToDiagramRepresentedPathwayIDs(dba);

		System.out.println(
			String.join("\t", "Entity DB ID", "Entity Name", "Entity Class", "Represented Pathway IDs")
		);
		for (long inputDBID : inputDBIDs) {
			String inputName = "N/A";
			String inputClass = "N/A";
			try {
				GKInstance inputInstance = dba.fetchInstance(inputDBID);
				if (inputInstance != null) {
					inputName = inputInstance.getDisplayName();
					inputClass = inputInstance.getSchemClass().getName();
				} else {
					logger.warn("DB ID {} has no instance in {} at {}",
						inputDBID, dba.getDBName(), dba.getDBHost());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println(String.join("\t", Long.toString(inputDBID), inputName, inputClass,
				getRepresentedPathwayIDsAsString(inputDBID, iconIdToRepresentedPathwayIDs)));
		}
		logger.info("IconFinder has completed");
	}

	/**
	 * Parses provided input file and returns the list of db ids in the file
	 * @param inputFile Path to the input file
	 * @return List of db ids in input file
	 */
	private static List<Long> getDBIDsFromInputFile(String inputFile) {
		try {
			return Files.readAllLines(new File(inputFile).toPath(), StandardCharsets.UTF_8)
				.stream()
				.map(Long::parseLong)
				.collect(Collectors.toList());
		} catch (IOException e) {
			logger.fatal("Unable to read input file {}", inputFile, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a mapping of db id for entities in pathway diagrams to the db ids of the
	 * first pathway each of those diagrams represent (i.e. the first value in the represented
	 * pathway attribute of each pathway diagram instance in which the entity was found)
	 * @param dba Database adaptor for target Reactome database (e.g. gk_central)
	 * @return Map of entity db id to represented pathway ids
	 */
	private static Map<Long, Set<Long>> getIconIdToDiagramRepresentedPathwayIDs(MySQLAdaptor dba) {
		Map<Long, Set<Long>> iconIdToDiagramRepresentedPathways = new HashMap<>();

		for (GKInstance pathwayDiagramInstance : fetchPathwayDiagramInstances(dba)) {
			String pathwayDiagramXMLString = getPathwayDiagramXML(pathwayDiagramInstance);

			Document pathwayDiagramXMLDocument;
			try {
				pathwayDiagramXMLDocument = convertStringToXMLDocument(pathwayDiagramXMLString);
			} catch (ParserConfigurationException | IOException | SAXException e) {
				logger.error("Unable to parse XML for pathway diagram {}",
					pathwayDiagramInstance.getExtendedDisplayName(), e);
				continue;
			}

			NodeList childNodes = pathwayDiagramXMLDocument.getElementsByTagName("Nodes").item(0).getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node currentNode = childNodes.item(i);

				if (!isTextNode(currentNode) && !isNoteNode(currentNode)) {
					Node reactomeIdAttributeNode = currentNode.getAttributes().getNamedItem("reactomeId");
					long reactomeId = Long.parseLong(reactomeIdAttributeNode.getNodeValue());

					iconIdToDiagramRepresentedPathways.computeIfAbsent(reactomeId, key -> new HashSet<>());

					try {
						iconIdToDiagramRepresentedPathways.get(reactomeId)
							.add(getRepresentedPathwayID(pathwayDiagramInstance));
					} catch (Exception e) {
						logger.error("{} in pathway diagram {} has no represented pathway",
							reactomeId, pathwayDiagramInstance.getExtendedDisplayName(), e);
					}
				}
			}
		}

		return iconIdToDiagramRepresentedPathways;
	}

	/**
	 * Creates database adaptor for target Reactome database specified in the properties object
	 * @param props The properties specified in the configuration file for connecting to the target Reactome database
	 * @return MySQLAdaptor to target Reactome database
	 * @throws SQLException Thrown if unable to connect to the target Reactome database
	 */
	private static MySQLAdaptor getDBA(Properties props) throws SQLException {
		String host = props.getProperty("host");
		String database = props.getProperty("db");
		String user = props.getProperty("user");
		String password = props.getProperty("pass");
		int port = Integer.parseInt(props.getProperty("port"));

		return new MySQLAdaptor(host, database, user, password, port);
	}

	/**
	 * Retrieves all pathway diagram instances in target Reactome database
	 * @param dba Database adaptor to target Reactome database
	 * @return List of pathway diagram instances in target Reactome database
	 */
	@SuppressWarnings("unchecked")
	private static List<GKInstance> fetchPathwayDiagramInstances(MySQLAdaptor dba) {
		try {
			return new ArrayList<>(dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram));
		} catch (Exception e) {
			logger.fatal("Unable to fetch pathway diagram instances from {}", dba.toString(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the XML of the pathway diagram instance given as a parameter
	 * @param pathwayDiagramInstance Pathway Diagram instance from target Reactome database
	 * @return XML as a String describing the pathway diagram's rendering or an empty String if unavailable
	 */
	private static String getPathwayDiagramXML(GKInstance pathwayDiagramInstance) {
		try {
			return (String) pathwayDiagramInstance.getAttributeValue(ReactomeJavaConstants.storedATXML);
		} catch (Exception e) {
			logger.error("Unable to get pathway diagram XML for {}",
				pathwayDiagramInstance.getExtendedDisplayName(), e);
			return "";
		}
	}

	/**
	 * Parses XML string and returns an org.w3c.dom.Document representing the XML structure
	 * @param xmlString Raw XML String
	 * @return Document object representing the XML hierarchy
	 * @throws ParserConfigurationException Thrown if unable to create a new DocumentBuilder object
	 * @throws IOException Thrown if there is an IO error in accessing the InputSource object
	 * @throws SAXException Thrown if there is an error parsing the XML
	 */
	// Adapted from https://howtodoinjava.com/xml/parse-string-to-xml-dom/
	private static Document convertStringToXMLDocument(String xmlString)
		throws ParserConfigurationException, IOException, SAXException {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(xmlString)));
	}

	/**
	 * Tests if a node is of type "text"
	 * @param node XML Node Object representing part of a pathway diagram
	 * @return <code>true</code> if node's name is "#text", <code>false</code> otherwise
	 */
	private static boolean isTextNode(Node node) {
		return node.getNodeName().equals("#text");
	}


	/**
	 * Tests if a node is of type "Note"
	 * @param node XML Node Object representing part of a pathway diagram
	 * @return <code>true</code> if node's name is "org.gk.render.Note", <code>false</code> otherwise
	 */
	private static boolean isNoteNode(Node node) {
		return node.getNodeName().equals("org.gk.render.Note");
	}

	/**
	 * Retrieves the db id of the first represented pathway of the pathway diagram instance
	 * @param pathwayDiagramInstance Pathway Diagram Instance from target Reactome database
	 * @return db id of the first represented pathway of the pathway diagram instance
	 * @throws Exception Thrown if there is an error retrieving the represented pathway attribute values
	 * 					 from the pathway diagram instance
	 */
	private static long getRepresentedPathwayID(GKInstance pathwayDiagramInstance) throws Exception {
		@SuppressWarnings("unchecked")
		List<GKInstance> representedPathways =
			pathwayDiagramInstance.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
		return representedPathways.get(0).getDBID();
	}

	/**
	 * String representation of the represented pathway ids in which an entity has an icon
	 * @param iconId db id of entity
	 * @param iconIdToRepresentedPathwayIDs map of entity db id to represented pathway db ids in which it participates
	 * @return Pipe delimited list of represented pathway db ids or "N/A" if no represented pathways exist
	 */
	private static String getRepresentedPathwayIDsAsString(long iconId,
		Map<Long, Set<Long>> iconIdToRepresentedPathwayIDs) {
			Set<Long> representedPathwayIDs = iconIdToRepresentedPathwayIDs.get(iconId);
			if (representedPathwayIDs != null && !representedPathwayIDs.isEmpty()) {
				return representedPathwayIDs.stream().map(id -> Long.toString(id)).collect(Collectors.joining("|"));
			} else {
				return "N/A";
			}
	}
}
