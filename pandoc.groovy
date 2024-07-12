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
        stage('Convert MD') {
            steps {
                script {
                    def outputTypesList = outputTypes.split(',')
                    def outputFileName = "out-${inputFile.split("\\.")[0]}"

                    for (outputType in outputTypesList) {
                        if (!fileExists(inputFile)) {
                            error "File ${inputFile} was not found!"
                        }

                        sh """
                            pandoc ${inputFile} -o ${outputFileName}.${outputType.trim()}
                        """

                        echo "Converted ${inputFile} to ${outputFileName}.${outputType.trim()}"
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
