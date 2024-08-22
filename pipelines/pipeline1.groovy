pipeline {
    agent any
    stages {
        stage('Stage 1') {
            steps {
                script {
                    println "Running ${name}"
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
        }
    }
}
