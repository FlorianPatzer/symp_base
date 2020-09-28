package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.external.ExternalExecutor;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import de.fraunhofer.camunda.javaserver.external.ontology.ExternalOntologyRemergeWorker.DeviceConfig;
import de.fraunhofer.camunda.javaserver.utils.OwlUtils;
import lombok.extern.slf4j.Slf4j;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ExternalOntologyMappingWorker extends ExternalOntologyWorker implements ExternalExecutor {

    public ExternalOntologyMappingWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTMAPPING.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask,
                                     ExternalTaskService externalTaskService) {

        TypedValue result;
        
        Collection<String> inputOntologyList = null;
        
        if (this.getBpmnXmlParser()
                .parentTaskTopic(this.currentExternalTask.getActivityId())
                .equals(ExternalWorkerTopics.ONTUPLOAD)) {
            inputOntologyList = fetchOntologyListFromParentUpload(externalTask);
            log("Ontology List: {}", inputOntologyList);
        } else {
        	throw new RuntimeException("Ontology-Mapping is not allowed to follow any other task than ONTUPLOAD");
        }
        
        List<String> filePaths = getLocalStrings();
        
        filePaths = filePaths.stream()
                .flatMap(s -> {
                    File f = new File(s);
                    if (f.isDirectory()) {
                        return Arrays.stream(f.listFiles())
                                     .map(File::getAbsolutePath);
                    } else {
                        return Stream.of(new File(s).getAbsolutePath());
                    }
                })
                .collect(Collectors.toList());
        
        //Currently only one mapping ontology is supported. Thus, only the first file is selected.
        result = mergeOntologies(inputOntologyList, filePaths.get(0));

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(externalTask.getActivityId(), result);
        externalTaskService.complete(externalTask, resultMap);
    }
    

	/*private TypedValue mergeOntologies(Collection<String> inputOntologyList, String mappingOntologyPath) {
		Iterator<String> inputIterator = inputOntologyList.iterator();
		
		File mappingOntInFile = new File(mappingOntologyPath);
		final OWLOntologyManager mappingOntManager = OWLManager.createOWLOntologyManager();
		OWLOntology mappingOnt = null;
		AutoIRIMapper mapper = new AutoIRIMapper(new File(System.getProperty("catalina.base") + "/webapps/reasoning_bpm_war_exploded/tboxes"), false);
		mapper.update();
		for(IRI iri :mapper.getOntologyIRIs()) {
			System.out.println(iri.toString() + ": "+mapper.getDocumentIRI(iri));
		}
		mappingOntManager.getIRIMappers().add(mapper);
		
		try {
			mappingOnt = mappingOntManager.loadOntologyFromOntologyDocument(mappingOntInFile);
		} catch (OWLOntologyCreationException e) {
			log.error("Unable to load ontology!", e);
			throw new RuntimeException();
		}
		
		while(inputIterator.hasNext()) {
			String inputOntPath = inputIterator.next();
			File tmpOntFile = new File(inputOntPath);
			
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
				if(!mappingOnt.containsAxiom(a)) {
					mappingOntManager.addAxiom(mappingOnt, a);
				}
			}
		}

		String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
		
		OwlUtils.owlApiSaveOntology(mappingOntManager, mappingOnt, combinedOntFileName);
		
		FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                .file(new File(combinedOntFileName))
                .mimeType("application/rdf+xml")
                .encoding("UTF-8")
                .create();
		
		return typedFileValue;
    }*/
    
    private TypedValue mergeOntologies(Collection<String> inputOntologyList, String mappingOntologyPath) {
		Iterator<String> inputIterator = inputOntologyList.iterator();
		
		File mappingOntInFile = new File(mappingOntologyPath);
		final OWLOntologyManager mappingOntManager = OWLManager.createOWLOntologyManager();
		OWLOntology mappingOnt = null;
		AutoIRIMapper mapper = new AutoIRIMapper(new File(System.getProperty("catalina.base") + "/webapps/reasoning_bpm_war_exploded/tboxes"), false);
		mapper.update();
		
		//-------------Load input ontology, to get its IRI---
		final OWLOntologyManager tmpM = OWLManager.createOWLOntologyManager();
		File tmpOntInFile = new File(inputIterator.next());
		OWLOntology tmpOnt = null;
		
		try {
			tmpOnt = tmpM.loadOntologyFromOntologyDocument(tmpOntInFile);
		} catch (OWLOntologyCreationException e) {
			log.error("Unable to load ontology!", e);
			throw new RuntimeException();
		}
		
		final Set<OWLAxiom> axioms = tmpOnt.getAxioms(Imports.INCLUDED);
		
		/*IRI inputOntIRI = tmpOnt.getOntologyID().getOntologyIRI().get();
		
		Collection<OWLOntologyIRIMapper> newMappers = new ArrayList<>(); 
		
		//	Replace the documentIRI for that IRI
		for(IRI iri : mapper.getOntologyIRIs()) {
			IRI documentIri = null;
			if(!iri.toURI().toString().equals(inputOntIRI.toURI().toString())) {
				documentIri = mapper.getDocumentIRI(iri);
			} else {
				documentIri = IRI.create(tmpOntInFile);
			}
			newMappers.add(new SimpleIRIMapper(iri, documentIri));
			mappingOntManager.getIRIMappers().add(new SimpleIRIMapper(iri, documentIri));
		}
		mappingOntManager.getIRIMappers().add(newMappers);
		//----
		*/
		
		mappingOntManager.getIRIMappers().add(mapper);
		
		try {
			mappingOnt = mappingOntManager.loadOntologyFromOntologyDocument(mappingOntInFile);
		} catch (OWLOntologyCreationException e) {
			log.error("Unable to load ontology!", e);
			throw new RuntimeException();
		}
		mappingOntManager.addAxioms(mappingOnt, axioms);
		
		
    
    /*
		final Set<OWLAxiom> axioms = tmpOnt.getABoxAxioms(Imports.INCLUDED);
	    Set<OWLImportsDeclaration> imports = tmpOnt.getImportsDeclarations();
	    mappingOntManager.addAxioms(mappingOnt, axioms);
	    for(OWLImportsDeclaration decl : imports){
	    	mappingOntManager.applyChange(new AddImport(mappingOnt, decl));
	    }
	    */
		/*for(OWLAxiom a : axioms) {
			if(!mappingOnt.containsAxiom(a)) {
				mappingOntManager.addAxiom(mappingOnt, a);
			}
		}*/

		String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
		
		OwlUtils.owlApiSaveOntology(mappingOntManager, mappingOnt, combinedOntFileName);
		
		FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                .file(new File(combinedOntFileName))
                .mimeType("application/rdf+xml")
                .encoding("UTF-8")
                .create();
		
		return typedFileValue;
    }
}
