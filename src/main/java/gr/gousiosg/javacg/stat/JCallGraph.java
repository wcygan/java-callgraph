/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.stat.support.Arguments;
import gr.gousiosg.javacg.stat.support.JacocoXMLParser;
import gr.gousiosg.javacg.stat.support.coloring.ColoredNode;
import gr.gousiosg.javacg.stat.support.coloring.GraphColoring;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Set;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
public class JCallGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(JCallGraph.class);
    private static final String REACHABILITY = "reachability";
    private static final String ANCESTRY = "ancestry";
    private static final String DELIMITER = "-";
    private static final String DOT_SUFFIX = ".dot";

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting java-cg!");

            /* Setup arguments */
            Arguments arguments = new Arguments(args);

            /* Create callgraph */
            Graph<String, DefaultEdge> graph = GraphUtils.staticCallgraph(arguments.getJars());

            /* Should we store the graph in a file? */
            if (arguments.maybeOutput().isPresent()) {
                GraphUtils.writeGraph(graph, GraphUtils.defaultExporter(), asDot(arguments.maybeOutput().get()));
            }

            /* Should we compute reachability from the entry point? */
            if (arguments.maybeEntryPoint().isPresent()) {
                try {
                    Set<String> coverage = arguments.maybeCoverage().isEmpty() ? new HashSet<>() :
                            JacocoXMLParser.parseCoverage(arguments.maybeCoverage().get());

                    /* Fetch reachability */
                    Graph<ColoredNode, DefaultEdge> reachability = GraphUtils.reachability(graph, arguments.maybeEntryPoint().get(), arguments.maybeDepth());

                    GraphColoring.applyCoverage(reachability, coverage);
                    /* Should we store the reachability reachability in a file? */
                    if (arguments.maybeOutput().isPresent()) {
                        String subgraphOutputName = arguments.maybeOutput().get() + DELIMITER + REACHABILITY;

                        /* Does this reachability have a depth? */
                        if (arguments.maybeDepth().isPresent()) {
                            subgraphOutputName = subgraphOutputName + DELIMITER + arguments.maybeDepth().get();
                        }

                        GraphUtils.writeGraph(reachability, GraphUtils.coloredExporter(), asDot(subgraphOutputName));
                    }

                    /* Should we fetch ancestry? */
                    if (arguments.maybeAncestry().isPresent()) {
                        Graph<ColoredNode, DefaultEdge> ancestry = GraphUtils.ancestry(graph, arguments.maybeEntryPoint().get(), arguments.maybeAncestry().get());
                        GraphColoring.applyCoverage(ancestry, coverage);

                        /* Should we store the ancestry in a file? */
                        if (arguments.maybeOutput().isPresent()) {
                            String subgraphOutputName = arguments.maybeOutput().get() + DELIMITER + ANCESTRY + DELIMITER + arguments.maybeAncestry().get();
                            GraphUtils.writeGraph(ancestry, GraphUtils.coloredExporter(), asDot(subgraphOutputName));
                        }
                    }

                } catch (IOException e) {
                    LOGGER.error("Error parsing coverage: " + e.getMessage());
                    System.exit(1);
                }
            }

        } catch (InputMismatchException e) {
            LOGGER.error("Unable to load callgraph: " + e.getMessage());
            System.exit(1);
        }

        LOGGER.info("java-cg is finished! Enjoy!");
    }

    private static String asDot(String name) {
        return name.endsWith(DOT_SUFFIX) ? name : (name + DOT_SUFFIX);
    }

}
