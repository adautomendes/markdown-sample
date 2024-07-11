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
        stage('Convert markdown to HTML') {
            steps {
                script {
                    sh """
                        pandoc markdown-sample.md -o markdown-sample.html
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
            archiveArtifacts artifacts: '**/*.html', allowEmptyArchive: true
        }
    }
}
