package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.external.ExternalExecutor;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.utils.OwlUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.jetbrains.annotations.NotNull;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ExternalOntologyRemergeWorker extends ExternalOntologyWorker implements ExternalExecutor {

    public ExternalOntologyRemergeWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTREMERGE.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask,
                                     ExternalTaskService externalTaskService) {

        TypedValue result;

        List<DeviceConfig> inputs = this.getBpmnXmlParser()
                                           .getJoinableOnts(this.currentExternalTask.getActivityId())
                                           .stream()
                                           .map(activityId -> {
                                      return new DeviceConfig(getOntFilePath(activityId), "");
                                  }).collect(Collectors.toList());
        
        result = mergeOntologies(inputs);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(externalTask.getActivityId(), result);
        externalTaskService.complete(externalTask, resultMap);
    }
    

	private TypedValue mergeOntologies(Collection<DeviceConfig> inputOntologies) {
		Iterator<DeviceConfig> it = inputOntologies.iterator();
		
		DeviceConfig firstConf = it.next();
		File ontInFile1 = new File(firstConf.getPath());
		final OWLOntologyManager m1 = OWLManager.createOWLOntologyManager();
		OWLOntology ont1 = null;
		
		try {
			ont1 = m1.loadOntologyFromOntologyDocument(ontInFile1);
		} catch (OWLOntologyCreationException e) {
			log.error("Unable to load ontology!", e);
			throw new RuntimeException();
		}
		
		while(it.hasNext()) {
			DeviceConfig c = it.next();
			File tmpOntFile = new File(c.getPath());
			
			final OWLOntologyManager tmpM = OWLManager.createOWLOntologyManager();
			OWLOntology tmpOnt = null;
			
			try {
				tmpOnt = tmpM.loadOntologyFromOntologyDocument(tmpOntFile);
			} catch (OWLOntologyCreationException e) {
				log.error("Unable to load ontology!", e);
				throw new RuntimeException();
			}
			
			final Set<OWLAxiom> axioms = tmpOnt.getABoxAxioms(Imports.INCLUDED);
			for(OWLAxiom a : axioms) {
				if(!ont1.containsAxiom(a)) {
					m1.addAxiom(ont1, a);
				}
			}
		}

		String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
		
		OwlUtils.owlApiSaveOntology(m1, ont1, combinedOntFileName);
		
		FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                .file(new File(combinedOntFileName))
                .mimeType("application/rdf+xml")
                .encoding("UTF-8")
                .create();
		
		return typedFileValue;
    }

    public static class DeviceConfig {

        private String inputPath;
        private String iriSuffix;

        public DeviceConfig(String inputPath, String iriSuffix) {
            this.inputPath = inputPath;
            this.iriSuffix = iriSuffix;
        }

        public String getIriSuffix() {
            return iriSuffix;
        }

        public String getPath() {
            return this.inputPath;
        }
    }
}
