// Plugins needed: Job DSL
pipelineJob('pipeline-1') {
    description('Pipeline 1')
    logRotator {
        numToKeep(10)
    }
    properties {
        pipelineTriggers {
            triggers {
                cron {
                    spec('*/2 * * * *')
                }
            }
        }
    }
    parameters {
        stringParam('name', 'Pipeline 1', 'Name of pipeline')
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/adautomendes/markdown-sample')
                        credentials('Github - adautomendes')
                    }
                    branches('main')
                    scriptPath('pipelines/pipeline1.groovy')
                }
            }
        }
    }
}

pipelineJob('pipeline-2') {
    description('Pipeline 2')
    logRotator {
        numToKeep(10)
    }
    properties {
        pipelineTriggers {
            triggers {
                cron {
                    spec('*/2 * * * *')
                }
            }
        }
    }
    parameters {
        stringParam('name', 'Pipeline 2', 'Name of pipeline')
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/adautomendes/markdown-sample')
                        credentials('Github - adautomendes')
                    }
                    branches('main')
                    scriptPath('pipelines/pipeline2.groovy')
                }
            }
        }
    }
}