pipeline {
    agent any
    stages {
        stage('Verify Files in Jenkins') {
            steps {
                script {
                    sh 'ls -la $(pwd)'
                }
            }
        }
        stage('Test if pandoc is available') {
            steps {
                script {
                    sh """
                        pandoc -v
                    """
                }
            }
        }
        stage('Convert input file') {
            parallel {
                stage('Convert MD to HTML') {
                    steps {
                        script {
                            sh """
                                pandoc ${inputFile} -o out-markdown-sample.html
                            """
                        }
                    }
                }
                stage('Convert MD to PDF') {
                    steps {
                        script {
                            sh """
                                pandoc ${inputFile} -o out-markdown-sample.pdf
                            """
                        }
                    }
                }
                stage('Convert MD to DOCX') {
                    steps {
                        script {
                            sh """
                                pandoc ${inputFile} -o out-markdown-sample.docx
                            """
                        }
                    }
                }
                stage('Custom convert') {
                    steps {
                        script {
                            if (!fileExists(inputFile)) {
                                error "File ${inputFile} was not found!"
                            }
                            sh """
                                pandoc ${inputFile} -o out-markdown-sample.${outputType}
                            """
                        }
                    }
                }
            }
        }
    }
    post {
        failure {
            echo 'The pipeline failed.'
        }
        success {
            echo 'Pipeline concluded successfully!'
            archiveArtifacts artifacts: '**/*.*', allowEmptyArchive: true
        }
    }
}
