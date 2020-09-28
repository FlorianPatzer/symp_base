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
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ExternalOntologyMergeWorker extends ExternalOntologyWorker implements ExternalExecutor {
    private final String DELIMITER = "#";
	
	private final String BASE_ONTOLOGY_PATH = System.getProperty("catalina.base") + "/webapps/reasoning_bpm_war_exploded/tboxes/system_base_ontology.owl";

    public ExternalOntologyMergeWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTMERGE.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask,
                                     ExternalTaskService externalTaskService) {

        TypedValue result;

        if (this.getBpmnXmlParser()
                .parentTaskTopic(this.currentExternalTask.getActivityId())
                .equals(ExternalWorkerTopics.ONTUPLOAD)) {
            Collection<String> ontologyURIList = fetchOntologyListFromParentUpload(externalTask);
            log("Ontology List: {}", ontologyURIList);
            try {
                result = mergeOntologiesFromPaths(ontologyURIList);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            List<DeviceConfig> inputs = this.getBpmnXmlParser()
                                           .getJoinableOnts(this.currentExternalTask.getActivityId())
                                           .stream()
                                           .map(activityId -> {
                                      FileValue val = currentExternalTask.getVariableTyped(activityId);
                                      return new DeviceConfig(val.getFilename(), extractIriSuffix(val.getFilename()));
                                  })
                                           .collect(Collectors.toList());
            result = mergeOntologiesFromConfig(inputs);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(externalTask.getActivityId(), result);
        externalTaskService.complete(externalTask, resultMap);
    }
    
    /**
     * Extracts the IRI suffix following a naming convention where the file always ends with -irisuffix.owl where irisuffix is the suffix.
     * Note that the irisuffix can neither contain a dash nor a dot.
     * @param filename Filename to extract the suffix from
     * @return The suffix as String
     */
    private String extractIriSuffix(String filename) {
    	log("Received Filename " + filename);
		int dotIndex = filename.lastIndexOf(".");
		int dashIndex = filename.lastIndexOf("-");
		return filename.substring(dashIndex, dotIndex);
	}

	private TypedValue mergeOntologiesFromConfig(Collection<DeviceConfig> inputOntologies) {
    	String baseOntFileName = BASE_ONTOLOGY_PATH;
    	File baseOntInFile = new File(baseOntFileName);
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLOntology baseOnt = null;
		
		try {
			baseOnt = m.loadOntologyFromOntologyDocument(baseOntInFile);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		final String baseIri = baseOnt.getOntologyID().getOntologyIRI().get().toString();
		
		for (DeviceConfig conf : inputOntologies) {
			OWLOntology tmpOnt = null;
			HashMap<OWLEntity, IRI> entityToIri = new HashMap<>();
			final OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			try {
				tmpOnt = man.loadOntologyFromOntologyDocument(new File(conf.getPath()));
			} catch (OWLOntologyCreationException e) {
				log.error("Unable to load ontology with path" + conf.getPath());
				e.printStackTrace();
				return null;
			}
			final OWLEntityRenamer renamer = new OWLEntityRenamer(man, Collections.singleton(tmpOnt));
			
			tmpOnt.getIndividualsInSignature().forEach(indiv -> {
				entityToIri.put(indiv, IRI.create(indiv.getIRI().toString().replace(baseIri, baseIri + "/" + conf.getIriSuffix())));
			});
			
			man.applyChanges(renamer.changeIRI(entityToIri));
			/*Set<OWLNamedIndividual> ar = tmpOnt.getIndividualsInSignature();
			Iterator<OWLNamedIndividual> it = ar.iterator();
			int stop = 10;
			while(it.hasNext()) {
				if(stop == 0) {
					break;
				}
				System.out.println(it.next().getIRI());
				stop--;
			}*/
			Set<OWLAxiom> tmpAxioms = tmpOnt.getABoxAxioms(null);
			m.addAxioms(baseOnt, tmpAxioms);
		}

		String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
		
		OwlUtils.owlApiSaveOntology(m, baseOnt, combinedOntFileName);
		
		FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                .file(new File(combinedOntFileName))
                .mimeType("application/rdf+xml")
                .encoding("UTF-8")
                .create();
		
		return typedFileValue;
    }

    /*private TypedValue mergeOntologiesFromConfig(Collection<DeviceConfig> inputOntologies) {
        String combinedOntFileName = getOntFilePath(this.currentExternalTask.getActivityId());
        OntModel union = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        for (DeviceConfig conf : inputOntologies) {
            union = extendOntModel(union, conf);
        }

        File outfile = new File(combinedOntFileName);
        outfile.getParentFile()
               .mkdirs();
        try (FileOutputStream out = new FileOutputStream(outfile)) {
            union.write(out, "RDF/XML");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        FileValue typedFileValue = Variables.fileValue("combined_ont.owl")
                                            .file(new File(combinedOntFileName))
                                            .mimeType("application/rdf+xml")
                                            .encoding("UTF-8")
                                            .create();

        log("Output ontology written to {}", combinedOntFileName);
        return typedFileValue;
    }*/

    private TypedValue mergeOntologiesFromPaths(Collection<String> paths) throws IllegalAccessException {
        List<DeviceConfig> inputOntologies = paths.stream()
                                                  .map(s -> {
                                                      return new DeviceConfig(s, FilenameUtils.getBaseName(s));
                                                  })
                                                  .collect(Collectors.toList());
       return mergeOntologiesFromConfig(inputOntologies);
    }

    @NotNull
    private OntModel extendOntModel(OntModel union, DeviceConfig conf) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.read(conf.getPath(), "RDF/XML");

        // Get Iterator for all "type"-properties with range/type "NamedIndividual"
        Property a = model.getProperty(model.getNsPrefixURI("rdf") + "type");
        Resource namedIndividual = model.getResource(model.getNsPrefixURI("owl") + "NamedIndividual");
        ResIterator rit = model.listResourcesWithProperty(a, namedIndividual);

        // Go through all (named) individuals of the current model
        ArrayList<String> newNamespaces = new ArrayList<>();
        while (rit.hasNext()) {
            Resource individual = rit.next();

            int dIndex = individual.getNameSpace()
                                   .indexOf(DELIMITER);
            String newNamespace =
                    individual.getNameSpace()
                              .substring(0, dIndex) + "/" + conf.getIriSuffix() + DELIMITER;

            if (!newNamespaces.contains(newNamespace)) {
                newNamespaces.add(newNamespace);
                // Add prefix for new namespace and add namespace itself
                String nsUriPrefix = model.getNsURIPrefix(individual.getNameSpace());
                nsUriPrefix = nsUriPrefix == null ? "" : nsUriPrefix;
                model.setNsPrefix(nsUriPrefix + conf.getIriSuffix(), newNamespace);
            }
            ResourceUtils.renameResource(individual, newNamespace + individual.getLocalName());
        }

        return ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, union.union(model));
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
