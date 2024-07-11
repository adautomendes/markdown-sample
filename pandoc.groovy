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
                        docker run --rm --volume "\$(pwd):/data" --user \$(id -u):\$(id -g) pandoc/core markdown-sample.md -o markdown-sample.html
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