# Fraunhofer Ontology Management System

[![Compatible with Camunda Modeler version 3.3](https://img.shields.io/badge/Camunda%20Modeler-3.3+-blue.svg)](https://github.com/camunda/camunda-modeler)
[![license](https://img.shields.io/badge/license-MIT-green.svg)](https://opensource.org/licenses/MIT)


This research implementation of the [Camunda Platform](https://docs.camunda.org) aims to serve as a host for rule engines and their execution. Executed rules and workflow can be monitored during runtime by accessing the `Camunda Cockpit`.
New Processes may directly be deployed to the Engine from within the modeler, and run by starting them from `Camunda Tasklist`

## Eclipse
To be able to run this project in Eclipse jre 12 or higher is requiered. Moreover, no module-info.java is supposed to be set and the build path needs to be adapted to not contain the jre system library within the module path but within the class path.
## Running the Server

To  run this servlet inside an Apache Tomcat Server on your local development machine, you first need to clone https://gitlab-ext.iosb.fraunhofer.de/betnik/tomcat-camunda. The Tomcat Project on Gitlab provides all required Camunda 
dependencies and initializes all basic Camunda Modules such as Cockpit or Tasklist.

After this you have to move the `/target/app.war` file, which you get from running `mvn install` to `%CATALINA_HOME%/webapps/app.war`. `%CATALINA_HOME%` describes the root directory of your Tomcat Server folder. 

If you want to be fancy and setup the server in your development environment make sure to add the `engine-rest.war` and the `ROOT.war` in the webapps directory.They provide the Admin, Tasklist and Cockpit features for your deployment.

### REST

By default the REST endpoint which can be set in the Camunda Modeler should be set to `http://localhost:8080/engine-rest`. 

## General Usage and Best Practices

This section covers general usage of most introduced interfaces and classes of this Ontology Management System. We will start with some simple examples and dissect them to give you a starting point for your own implementation. 

First of all you should have already checked out the `Running the Server` section. Try to get your simple setup working and try out the included BPMN definitions in the resources, to make sure everything works before developing your own workers.
After you have verified that your local deployment works as intended, we can now continue with our example that should get you started.

The following diagram showcases most of our public interfaces and classes that are meant to be extended if the user intends to add custom features. `ExternalWorker` is our base class
that handles adapting to the Camunda Backend in case some of their interface definitions change on update. The External Worker also serves as a proxy for our custom `ExternalExecutor`. The purpose of `External Executor` is to 
have an execution context where variables like `externalTask` and its dependent externalTaskService are available to methods without explicitly having to declare method parameters each time we want to use them.
As can be seen there are multiple child classes inheriting from `ExternalWorker`. `ExternalReasoningWorker` is a special subclass since it redefines the base classes execute semantic. 
Instead of receiving a `ExternalExecutor` the subclass now receives a `ExternalReasoningStrategy` which provides a well defined interface for ontology manipulation. 

![Class Diagram of most basic Workers](docs/classDiagram.png)

With this new knowledge let us define some custom Strategies to use with our `ExternalReasoningWorker`.

```java
public class ExternalStrategies {

    public static ExternalReasoningStrategy jenaStrategy() {
        return (inputStreamOntology, localVars, defaultPathToOutFile) -> {
            try {
                JenaReasoner jenaReasoner = new JenaReasoner(inputStreamOntology, defaultPathToOutFile);
                List<String> rules = new ArrayList<>(localVars.values());
                jenaReasoner.inferAndSaveFromRules(rules);

                return defaultPathToOutFile;
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
```
A strategy simply executes its ontology manipulation logic after being provided the previous ontology in the workflow via an `InputStream`. Additionally 
the strategy also receives the current local variables of the task in a map. Local variables come from the `BPMN` process definition as a `java.util.Map`. 
The user also may write his output ontology to a custom filepath that has to be returned from the method. If he does not want to use a custom path he also may use `defaultPathToOutFile` instead.

With this new strategy, creating a new worker is as simple as calling the ExternalWorkerBuilder's build method like this. Every call to `build()` creates its own thread for the worker to run in.
Note that strategies may not be reused without proper synchronization.

```java
ExternalReasoningWorkerBuilder.getInstance()
                              .setOutputDirectory(outPath)
                              .setWorkerId(String.valueOf(atomicInteger.getAndIncrement()))
                              .setReasoningStrategy(ExternalStrategies.jenaStrategy())
                              .setTopic("jena")
                              .build()
```

## Add Ontology Manipulation Code
* Create new strategy in ExternalStrategies
* Register the strategy in ReasoningManagementApplication


## Before you Publish

* [ ] Clearly state which Camunda Modeler version your plug-in is compatible with
* [ ] Always leave the checked in code cleaner than the code that is checked out


## Additional Resources

### General Camunda Modeler Resources 

* [List of existing plug-ins](https://github.com/camunda/camunda-modeler-plugins)
* [Plug-ins documentation](https://github.com/camunda/camunda-modeler/tree/master/docs/plugins)

### BPMN Lint Resources 

* [BPMN Lint](https://github.com/bpmn-io/bpmnlint)
* [Camunda Modeler Custom Linter](https://github.com/camunda/camunda-modeler-linter-plugin)
* [Camunda Modeler Custom Linter Plugin Rules](https://github.com/camunda/camunda-modeler-custom-linter-rules-plugin)
* [List of available bpmn lint rules](https://github.com/bpmn-io/bpmnlint/tree/master/docs/rules)


## Licence

MIT

