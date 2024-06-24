pipeline {
    agent any

    environment {
        MARKDOWN_FILE = 'min-markdown.md'
        PAYLOAD_TEMPLATE = '''
        {
            "type": "page",
            "title": "Jenkins MD {{date}}",
            "space": {
                "key": "EAI"
            },
            "ancestors": [
                {
                    "id": "2359303"
                }
            ],
            "body": {
                "storage": {
                    "value": "{{content}}",
                    "representation": "wiki"
                }
            }
        }
        '''
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    if (!fileExists(env.MARKDOWN_FILE)) {
                        error "File ${env.MARKDOWN_FILE} was not found!"
                    }
                }
            }
        }

        stage('Send MD to Confluence') {
            steps {
                script {
                    def markdownContent = readFile(env.MARKDOWN_FILE).replaceAll('\n', '\\\\n')

                    def payload = env.PAYLOAD_TEMPLATE
                    payload = payload.replace('{{date}}', new Date().format("yyyyMMddHHmmss"))
                    payload = payload.replace('{{content}}', markdownContent)

                    echo "Print payload:\n${payload}"

                    def headers = [[name: 'Authorization', value: 'Bearer NDcyNzIxODQ3ODg4OnZ2mWxAuTG0M2fjvz7zihRShmaQ']]

                    def response = httpRequest(
                        url: 'http://confluence:8090/rest/api/content',
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        requestBody: payload,
                        customHeaders: headers,
                        validResponseCodes: '200:201',
                        consoleLogResponseBody: true
                    )

                    echo "Response: ${response.status}"
                    echo "Response Body: ${response.content}"
                }
            }
        }

        stage('Export Confluence PDF') {
            steps {
                script {
                    def headers = [[name: 'Authorization', value: 'Bearer NDcyNzIxODQ3ODg4OnZ2mWxAuTG0M2fjvz7zihRShmaQ']]

                    def response = httpRequest(
                        url: 'http://confluence:8090/wiki/rest/api/content/2359303/export/pdf',
                        httpMode: 'GET',
                        customHeaders: headers,
                        acceptType: 'APPLICATION_OCTETSTREAM',
                        quiet: true,
                        validResponseCodes: '200'
                    )

                    writeFile file: "output.pdf", text: response.content

                    echo "PDF saved as output.pdf"
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
            archiveArtifacts artifacts: 'output.pdf', allowEmptyArchive: true
        }
    }
}
