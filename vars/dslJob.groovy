#!/usr/bin/groovy
/*
Configures jenkins to execute all files called *_jobdsl.groovy.
These *_jobdsl.groovy files will refer to the "builder" class JobConfigurator contained in this library so
we copy the library file into the source code tree of the target project where it may be accessed.
 */

def call() {
    // addition classpath doesn't work when security enabled so I have to take the alternative action
    // of copying the utility script utils.JobConfigurator into the source tree of the job

    steps.node {

        steps.echo "!!!!! Configuring Jobs !!!!!! "
        steps.checkout scm

        prepareLibraryFile()

        steps.jobDsl targets: ['src/jobs/**/*jobdsl.groovy'].join('\n'),
                removedJobAction: 'DELETE',
                removedViewAction: 'DELETE',
                lookupStrategy: 'SEED_JOB',
                sandbox: true
                //additionalClasspath: [libraryLocation].join('\n')
                //additionalParameters: [message: 'Hello from pipeline', credentials: 'SECRET']
    }
}

String prepareLibraryFile() {

    String librarySrcFile = "utils/JobConfigurator.groovy"
    String libraryTargFile = "src/jobs/$librarySrcFile"

    steps.echo "Preparing $libraryTargFile"

    def request = libraryResource librarySrcFile
    steps.writeFile file: libraryTargFile , text: request

    steps.echo "Installed $libraryTargFile"
}
