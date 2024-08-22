pipelineJob('pipeline-1') {
    description('Pipeline 1')
    logRotator {
        numToKeep(10)
    }
    parameters {
        stringParam('name', 'Pipeline 1', 'Name of the pipeline')
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
    parameters {
        stringParam('name', 'Pipeline 2', 'Name of the pipeline')
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