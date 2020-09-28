package de.fraunhofer.camunda.javaserver.reasoning;

import java.io.*;
import java.util.Map;
import lombok.Getter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

/**
 * @author Florian Patzer florian.patzer@iosb.fraunhofer.de
 * Class implementing the basics for SWRL reasoning using Drools engine
 */
@Getter
public class SWRLReasoner implements FraunhoferReasoner<String, String> {

    private SWRLRuleEngine ruleEngine;
    private OWLOntologyManager ontologyManager;
    private OWLOntology ontology;

    public SWRLReasoner(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public SWRLReasoner(InputStream in) {
        ontologyManager = OWLManager.createOWLOntologyManager();
        try {
            loadOntology(in);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        ruleEngine = createRuleEngine(this.ontology);
    }

    @Override
    public void loadOntology(File file) throws OWLOntologyCreationException {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(file);
    }

    public void loadOntology(InputStream in) throws OWLOntologyCreationException {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(in);
    }

    private SWRLRuleEngine createRuleEngine(OWLOntology ontology) {
        return SWRLAPIFactory.createSWRLRuleEngine(ontology);
    }

    @Override
    public void addRules(Map<String, String> rules) {
        if (ruleEngine == null) {
            return;
        }
        rules.forEach((String ruleName, String rule) -> {
            try {
                ruleEngine.createSWRLRule(ruleName, rule);
            } catch (SWRLParseException | SWRLBuiltInException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void infer() {
        ruleEngine.infer();
    }

    @Override
    public void saveOntology() {
        try {
            ontologyManager.saveOntology(ontology);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    public void saveOntology(OutputStream outputStream) {
        try {
            ontologyManager.saveOntology(ontology, outputStream);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    public void saveOntology(File file) throws FileNotFoundException {
        saveOntology(new FileOutputStream(file));
    }

    @Override
    public void inferAndSaveFromRules(Map<String, String> rules) {
        addRules(rules);
        infer();
        saveOntology();
    }
}
