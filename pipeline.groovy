import groovy.json.JsonSlurper

def markdownPageId = ''
def wikiPageId = ''

pipeline {
    agent any

    environment {
        CONFLUENCE_TOKEN = 'Bearer NDcyNzIxODQ3ODg4OnZ2mWxAuTG0M2fjvz7zihRShmaQ'
        MARKDOWN_FILE = 'min-markdown.md'
        WIKI_FILE = 'min-markdown.wiki'
        PAYLOAD_TEMPLATE = '''
        {
            "type": "page",
            "title": "Jenkins {{type}} {{date}}",
            "space": {
                "key": "EAI"
            },
            "ancestors": [
                {
                    "id": "3047425"
                }
            ],
            "body": {
                "wiki": {
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
                    if (!fileExists(env.WIKI_FILE)) {
                        error "File ${env.WIKI_FILE} was not found!"
                    }
                }
            }
        }
        
        stage('Send MARKDOWN to Confluence') {
            steps {
                script {
                    def markdownContent = readFile(env.MARKDOWN_FILE).replaceAll('\n', '\\\\n')

                    def payload = env.PAYLOAD_TEMPLATE
                    payload = payload.replace('{{date}}', new Date().format("yyyyMMddHHmmss"))
                    payload = payload.replace('{{markdownContent}}', markdownContent)
                    payload = payload.replace('{{type}}', 'MARKDOWN')

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

                    markdownPageId = jsonResponse.id
                }
            }
        }

        stage('Send WIKI to Confluence') {
            steps {
                script {
                    def markdownContent = readFile(env.WIKI_FILE).replaceAll('\n', '\\\\n')

                    def payload = env.PAYLOAD_TEMPLATE
                    payload = payload.replace('{{date}}', new Date().format("yyyyMMddHHmmss"))
                    payload = payload.replace('{{markdownContent}}', markdownContent)
                    payload = payload.replace('{{type}}', 'WIKI')

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

                    wikiPageId = jsonResponse.id
                }
            }
        }

        stage('Export Created Pages to PDF') {
            steps {
                script {
                    def url = "http://confluence:8090/spaces/flyingpdf/pdfpageexport.action?pageId=${markdownPageId}"

                    sh """
                        curl -v -L -u "adauto:adauto" -H "X-Atlassian-Token: no-check" "${url}" -o "markdown.pdf"
                    """

                    echo "PDF saved as markdown.pdf"
                }
                script {
                    def url = "http://confluence:8090/spaces/flyingpdf/pdfpageexport.action?pageId=${wikiPageId}"

                    sh """
                        curl -v -L -u "adauto:adauto" -H "X-Atlassian-Token: no-check" "${url}" -o "wiki.pdf"
                    """

                    echo "PDF saved as wiki.pdf"
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
