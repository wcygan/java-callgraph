package gr.gousiosg.javacg.stat.support.coverage;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JacocoCoverage {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacocoCoverage.class);
    public static final String METHOD_TYPE = "METHOD";
    public static final String LINE_TYPE = "LINE";
    public static final String BRANCH_TYPE = "BRANCH";

    private boolean hasCoverage = false;

    private Map<String, Report.Package.Class.Method> methodCoverage = new HashMap<>();

    public JacocoCoverage(Optional<String > maybeFilepath) throws IOException, ParserConfigurationException, JAXBException, SAXException {

        if (maybeFilepath.isPresent()) {
            /* Convert the jacoco.xml file into a Report object */
            Report report = JacocoCoverageParser.getReport(maybeFilepath.get());

            /* Iterate over all packages in report */
            for (Report.Package pkg : report.getPackage()) {

                /* Iterate over all classes in a package */
                for (Report.Package.Class clazz : pkg.getClazz()) {

                    /* Find all methods in a class */
                    List<Report.Package.Class.Method> methods = clazz.getContent().stream()
                            .filter(s -> s instanceof JAXBElement)
                            .map(s -> (JAXBElement<?>) s)
                            .filter(je -> je.getValue() instanceof Report.Package.Class.Method)
                            .map(je -> (Report.Package.Class.Method) je.getValue())
                            .collect(Collectors.toList());

                    /* Store "covered" methods in methodCoverage */
                    methods.forEach(method -> {
                        String qualifiedName = qualifiedName(
                                clazz.getName(),
                                method.getName(),
                                method.getDesc()
                        );
                        methodCoverage.putIfAbsent(qualifiedName, method);
                    });
                }
            }

            /* Indicate that coverage has been applied */
            hasCoverage = true;
        }
    }

    public void applyCoverage(Graph<ColoredNode, DefaultEdge> graph) {
        LOGGER.info("Applying coverage!");
        Map<String, ColoredNode> nodeMap = nodeMap(graph.vertexSet());
        nodeMap.keySet().forEach(node -> {
            if (methodCoverage.containsKey(node)) {
                Report.Package.Class.Method m = methodCoverage.get(node);
                nodeMap.get(node).mark(m);
            } else {
                nodeMap.get(node).markMissing();
            }
        });

    }

    private static Map<String, ColoredNode> nodeMap(Set<ColoredNode> nodes) {
        return nodes.stream()
                .collect(Collectors.toMap(ColoredNode::getLabel, node -> node));
    }

    private static String qualifiedName(String className, String methodName, String argDescriptors) {
        String argTypes = Arrays.stream(Type.getArgumentTypes(argDescriptors))
                .map(Type::getClassName)
                .collect(Collectors.joining(","));
        return "\"" + className.replace("/", ".") + ":" + methodName + "(" + argTypes + ")\"";
    }

    public boolean hasCoverage() {
        return hasCoverage;
    }
}
