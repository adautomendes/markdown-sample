import groovy.json.JsonSlurper

def createdPageId = ''

pipeline {
    agent any

    environment {
        CONFLUENCE_TOKEN = 'Bearer NDcyNzIxODQ3ODg4OnZ2mWxAuTG0M2fjvz7zihRShmaQ'
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
                    "value": "{{markdownContent}}",
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
                    payload = payload.replace('{{markdownContent}}', markdownContent)

                    echo "Request payload:\n${payload}"

                    def headers = [[name: 'Authorization', value: env.CONFLUENCE_TOKEN]]

                    def response = httpRequest(
                        url: 'http://confluence:8090/rest/api/content',
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        requestBody: payload,
                        customHeaders: headers,
                        validResponseCodes: '200:201',
                        consoleLogResponseBody: true
                    )
                    
                    def jsonSlurper = new JsonSlurper()
                    def jsonResponse = jsonSlurper.parseText(response.content)

                    echo "Response status: ${response.status}"
                    echo "Response payload:\n${response.content}"
                    echo "Generated page id: ${jsonResponse.id}"

                    createdPageId = jsonResponse.id
                }
            }
        }

        stage('Export Created Page to PDF') {
            steps {
                script {
                    def url = "http://confluence:8090/spaces/flyingpdf/pdfpageexport.action?pageId=${createdPageId}"

                    sh """
                        curl -v -L -u "adauto:adauto" -H "X-Atlassian-Token: no-check" "${url}" -o "jenkins.pdf"
                    """

                    echo "PDF saved as jenkins.pdf"
                }
            }
        }

        stage('Export PRI to PDF') {
            steps {
                script {
                    def url = "http://confluence:8090/spaces/flyingpdf/pdfpageexport.action?pageId=1015815"

                    sh """
                        curl -v -L -u "adauto:adauto" -H "X-Atlassian-Token: no-check" "${url}" -o "pri.pdf"
                    """

                    echo "PDF saved as pri.pdf"
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
