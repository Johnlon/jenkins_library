package utils

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.jobs.MultibranchWorkflowJob
import javaposse.jobdsl.dsl.jobs.WorkflowJob

public class JobConfigurator {
    final DslFactory dsl
    final PrintStream out
    final Map cfg

    JobConfigurator(DslFactory dsl, PrintStream out, Map cfg = [:]) {
        this.dsl = dsl
        this.out = out
        this.cfg = cfg
    }

    JobConfigurator with(Map overrideCfg) {
        Map curr = cfg.clone()
        curr << overrideCfg
        return new JobConfigurator(dsl, out, curr)
    }

    MultibranchWorkflowJob multibranchPipeline(Map overrideCfg = [:]) {
        def c = this.with(overrideCfg)

        MultibranchWorkflowJobConfigurator.configure(dsl, c, out)
    }

    WorkflowJob pipeline(Map overrideCfg = [:]) {
        def c = this.with(overrideCfg)

        WorkflowJobConfigurator.configure(dsl, c, out)
    }

    Object get(String name) {
        return cfg.get(name)
    }

    Object get(String name, Object deflt) {
        return cfg.get(name, deflt)
    }
}

class Services {
    static final String gitHttpServer = "http://localhost:9000/"
    static final String gitHttpServerVersion = "11.0.2"
    static final String gitScp = "git@localhost:"
}

import static utils.Services.*

class MultibranchWorkflowJobConfigurator {

    static def configure(DslFactory dsl, JobConfigurator c, PrintStream out) {

        def jobName = c.get("jobName")
        def gitRepo = c.get("gitRepo") // eg infra/projectA
        def branches = c.get("branches", "")
        def numBuildsToKeep = c.get("numBuildsToKeep", "20") .toInteger()
        def jenkinsFile = c.get("scriptPath", "Jenkinsfile")

        out.println("CONFIGURING : $jobName")
        out.println("  gitRepo   : $gitRepo")
        out.println("  branches  : $branches")
        out.println("  scriptPath: $jenkinsFile")

        dsl.with {
            multibranchPipelineJob(jobName) {
                branchSources {
                    branchSource {
                        source {
                            git {
                                id("git")

                                remote("${gitScp}${gitRepo}.git")

                                browser {
                                    //bitbucketWeb
                                    gitLab {
                                        repoUrl("${gitHttpServer}${gitRepo}")
                                        version(gitHttpServerVersion)
                                    }
                                }

                                traits {
                                    if (branches != "") {
                                        headRegexFilter {
                                            regex("master|develop")
                                        }
                                    }

                                    // can't use this approach YET !! Need to use a config block as further down
                                    // https://github.com/jenkinsci/git-plugin/pull/595
                                    //gitBranchDiscovery {}
                                }

                                //credentialsId('github-ci')
                                //includes('JENKINS-*')

                                // http://localhost:7000/plugin/job-dsl/api-viewer/index.html#path/multibranchPipelineJob-branchSources-branchSource-source-git-traits-headRegexFilter

                                // possibly add to avoid extraneous triggers from libs ? http://localhost:7000/plugin/job-dsl/api-viewer/index.html#path/multibranchPipelineJob-branchSources-branchSource-strategy-defaultBranchPropertyStrategy-props-noTriggerBranchProperty
                            }
                        }
                        buildStrategies {

                        }
                    }
                }

                // discover Branches (workaround due to JENKINS-46202)
                // possible fix .. https://github.com/jenkinsci/git-plugin/pull/595
                // also .. https://issues.jenkins-ci.org/browse/JENKINS-45504
                // and https://issues.jenkins-ci.org/browse/JENKINS-45860
                configure {
//        def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
//        traits << 'com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait' {
//            strategyId(3) // detect all branches
//        }

                    def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
                    traits << 'jenkins.plugins.git.traits.BranchDiscoveryTrait' {
                        //strategyId(3) // detect all branches
                    }
                }

                // check every minute for scm changes as well as new / deleted branches
                triggers {
                    periodic(1)
                }

                orphanedItemStrategy {
                    discardOldItems {
                        numToKeep(numBuildsToKeep)
                    }
                }

                factory {
                    workflowBranchProjectFactory {
                        scriptPath(jenkinsFile)
                    }
                }
            }
        }
    }
}
class WorkflowJobConfigurator {

    static def configure(DslFactory dsl, JobConfigurator c, PrintStream out) {

        def jobName = c.get("jobName")
        def gitRepo = c.get("gitRepo") // eg infra/projectA
        def branches = c.get("branches", "")
        def numBuildsToKeep = c.get("numBuildsToKeep", "20") .toInteger()
        def jenkinsFile = c.get("scriptPath", "Jenkinsfile")

        out.println("CONFIGURING : $jobName")
        out.println("  gitRepo   : $gitRepo")
        out.println("  branches  : $branches")
        out.println("  scriptPath: $jenkinsFile")

        dsl.with {
            pipelineJob(jobName) {
                branchSources {
                    branchSource {
                        source {
                            git {
                                id("git")

                                remote("${gitScp}${gitRepo}.git")

                                browser {
                                    //bitbucketWeb
                                    gitLab {
                                        repoUrl("${gitHttpServer}${gitRepo}")
                                        version(gitHttpServerVersion)
                                    }
                                }

                                traits {
                                    if (branches != "") {
                                        headRegexFilter {
                                            regex("master|develop")
                                        }
                                    }

                                    // can't use this approach YET !! Need to use a config block as further down
                                    // https://github.com/jenkinsci/git-plugin/pull/595
                                    //gitBranchDiscovery {}
                                }

                                //credentialsId('github-ci')
                                //includes('JENKINS-*')

                                // http://localhost:7000/plugin/job-dsl/api-viewer/index.html#path/multibranchPipelineJob-branchSources-branchSource-source-git-traits-headRegexFilter

                                // possibly add to avoid extraneous triggers from libs ? http://localhost:7000/plugin/job-dsl/api-viewer/index.html#path/multibranchPipelineJob-branchSources-branchSource-strategy-defaultBranchPropertyStrategy-props-noTriggerBranchProperty
                            }
                        }
                        buildStrategies {

                        }
                    }
                }

                // discover Branches (workaround due to JENKINS-46202)
                // possible fix .. https://github.com/jenkinsci/git-plugin/pull/595
                // also .. https://issues.jenkins-ci.org/browse/JENKINS-45504
                // and https://issues.jenkins-ci.org/browse/JENKINS-45860
                configure {
//        def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
//        traits << 'com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait' {
//            strategyId(3) // detect all branches
//        }

                    def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
                    traits << 'jenkins.plugins.git.traits.BranchDiscoveryTrait' {
                        //strategyId(3) // detect all branches
                    }
                }

                // check every minute for scm changes as well as new / deleted branches
                triggers {
                    periodic(1)
                }

                orphanedItemStrategy {
                    discardOldItems {
                        numToKeep(numBuildsToKeep)
                    }
                }

                factory {
                    workflowBranchProjectFactory {
                        scriptPath(jenkinsFile)
                    }
                }
            }
        }
    }
}