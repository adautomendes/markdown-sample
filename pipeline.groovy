pipeline {
    agent any

    environment {
        HEADERS = [[name: 'Authorization', value: 'Bearer NDcyNzIxODQ3ODg4OnZ2mWxAuTG0M2fjvz7zihRShmaQ']]
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
                    "id": "360451"
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

                    def response = httpRequest(
                        url: 'http://confluence:8090/rest/api/content',
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        requestBody: payload,
                        customHeaders: env.HEADERS,
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
                    def response = httpRequest(
                        url: 'http://confluence:8090/rest/export/pdf/content/1933321',
                        httpMode: 'GET',
                        acceptType: 'APPLICATION_PDF',
                        customHeaders: env.HEADERS,
                        validResponseCodes: '200',
                        consoleLogResponseBody: true,
                        outputFile: 'page.pdf'
                    )

                    echo "PDF exportado com sucesso: ${response.status}"
                    echo "PDF salvo como: page.pdf"
                }
            }
        }
    }

    post {
        failure {
            echo 'The pipeline failed.'
        }
        success {
            echo 'Pipeline concluded successfully.'
        }
    }
}
