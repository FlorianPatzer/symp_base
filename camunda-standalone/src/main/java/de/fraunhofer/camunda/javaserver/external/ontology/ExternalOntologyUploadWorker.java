package de.fraunhofer.camunda.javaserver.external.ontology;

import de.fraunhofer.camunda.javaserver.external.ExternalExecutor;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExternalOntologyUploadWorker extends ExternalOntologyWorker implements ExternalExecutor {

    public ExternalOntologyUploadWorker(String id, File outputDir) {
        super(id, outputDir, ExternalWorkerTopics.ONTUPLOAD.toString());
        setExternalExecutor(this);
    }

    @Override
    public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log("All Variables in Task {} : {}", externalTask.getActivityId(), getAllVariables());
        log("Task local Variables in Task {} : {}", externalTask.getActivityId(), this.getLocalStringMap());

        List<String> ontologyURIList;

        if (this.getBpmnXmlParser()
                .parentTaskTopic(this.currentExternalTask.getActivityId())
                .equals(ExternalWorkerTopics.ONTUPLOAD)) {
            ontologyURIList = fetchOntologyListFromParentUpload(externalTask);
            log("upload task fetched ontology list from parent upload task {}", ontologyURIList);
        } else {
            ontologyURIList = new ArrayList<>();
            log("upload task creates new ontology uri list because no upload task preceded this task");
        }

        ontologyURIList.addAll(expandDirs(getLocalStrings()));

        log("Completed upload task");
        externalTaskService.complete(externalTask, getResultMapFromObject(externalTask.getActivityId(), ontologyURIList));
    }

    public static List<String> expandDirs(List<String> filePaths) {
        return filePaths.stream()
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
    }

}
