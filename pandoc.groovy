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
        stage('Verify Files in Docker') {
            steps {
                script {
                    sh """
                        docker run --rm --volume "\$(pwd):/data" alpine ls -la /data
                    """
                }
            }
        }
        stage('Docker') {
            steps {
                script {
                    sh """
                        docker run --rm --volume "\$(pwd):/data" --user \$(id -u):\$(id -g) pandoc/core /data/markdown-sample.md -o /data/markdown-sample.html
                    """
                }
            }
        }
    }
}
