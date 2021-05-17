# SPE-GUI
Project files for DATX05 MPCSN, a master thesis project spring 2021.

A GUI for application design and performance reporting of data streaming applications.

Currently it supports the [Liebre](https://github.com/vincenzo-gulisano/Liebre) and [Apache Flink](https://github.com/apache/flink) SPEs.

## How to run
#### IntelliJ
Open as a Maven project in IntelliJ and run `Main.java`

#### JAR
Run the `GUI.jar` file located at `/out/artifacts/GUI_jar/`.
Note that the two smartgraph files need to be in the same directory as the JAR file.

## Screenshots
Designing a streaming application (& code generation):

![Designing a streaming application](/images/gui-2.png)

Visualising the code of a streaming application:

![Visualising the code of a streaming application](/images/gui-4.png)

Live statistics for Liebre:

![Live statistics for Liebre](/images/gui-3-2.png)

Metrics from Graphite:

![Metrics from Graphite](/images/gui-graphite.png)
