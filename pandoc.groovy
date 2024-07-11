import groovy.json.JsonSlurper

pipeline {
    agent any

    environment {
        TESTE='teste'
    }

    stages {
        stage('Docker') {
            steps {
                script {
                    sh """
                        docker ps -a
                        ls $(pwd)
                    """
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
            archiveArtifacts artifacts: '**/*.pdf', allowEmptyArchive: true
        }
    }
}