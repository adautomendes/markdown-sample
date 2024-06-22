pipeline {
    agent any

    environment {
        CONFLUENCE_URL = 'http://confluence:8090/rest/api/content'
        PAYLOAD_TEMPLATE = '''
        {
            "type": "page",
            "title": "Markdown Test {{date}}",
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
        MARKDOWN_FILE = 'min-markdown.md'
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    // Verifica se o arquivo de markdown existe
                    if (!fileExists(env.MARKDOWN_FILE)) {
                        error "Arquivo de markdown não encontrado: ${env.MARKDOWN_FILE}"
                    }
                }
            }
        }

        stage('Send REST Request') {
            steps {
                script {
                    // Lê o conteúdo do arquivo de markdown
                    def markdownContent = readFile(env.MARKDOWN_FILE).replaceAll('\n', '\\n')

                    // Substitui o placeholder no payload template
                    def payload = env.PAYLOAD_TEMPLATE
                    payload = payload.replace('{{date}}', new Date().format("yyyyMMddHHmmss"))
                    payload = payload.replace('{{content}}', markdownContent)

                    // Envia a requisição POST
                    def response = httpRequest(
                        url: env.CONFLUENCE_URL,
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        requestBody: payload,
                        validResponseCodes: '200:201',
                        consoleLogResponseBody: true
                    )

                    // Processa a resposta
                    echo "Response: ${response.status}"
                    echo "Response Body: ${response.content}"
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
