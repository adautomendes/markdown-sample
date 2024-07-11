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
        stage('Convert markdown to PDF') {
            steps {
                script {
                    sh """
                        pandoc markdown-sample.md -o markdown-sample.pdf
                    """
                }
            }
        }
        stage('Convert markdown to DOCX') {
            steps {
                script {
                    sh """
                        pandoc markdown-sample.md -o markdown-sample.docx
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
            archiveArtifacts artifacts: '**/*.html,**/*.pdf,**/*.docx', allowEmptyArchive: true
        }
    }
}
