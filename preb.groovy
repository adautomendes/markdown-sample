import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

def outputFileName = "preb_${getcurrentDateHour('yyyyMMdd_HHmmss')}.pdf"
def currentDateHour = getcurrentDateHour('MM/dd/yyyy HH:mm:ss')
def currentDate = getcurrentDateHour('MM/dd/yyyy')
def listForAcceptance = ''
def listForApproval = ''

pipeline {
    agent any

    environment {
        JIRA_TOKEN = 'Basic YWRhdXRvOmFkYXV0bw=='
    }

    stages {
        stage('Export PReB to PDF') {
            steps {
                script {
                    def url = "http://confluence:8090/spaces/flyingpdf/pdfpageexport.action?pageId=${PReB_PAGE_ID}"

                    sh """
                        curl -v -L -u "adauto:adauto" -H "X-Atlassian-Token: no-check" "${url}" -o "${outputFileName}"
                    """

                    echo "PDF saved as ${outputFileName}"
                }
            }
        }

        stage('Upload PReB PDF to Confluence') {
            steps {
                script {
                    def url = "http://confluence:8090/rest/api/content/${PReB_PAGE_ID}/child/attachment"

                    sh """
                        curl -u "adauto:adauto" -X POST -H "X-Atlassian-Token: nocheck" -F "file=@${outputFileName}" -F "comment=File attached at ${currentDateHour}" "${url}" 2>/dev/null
                    """

                    echo 'PDF uploaded to http://localhost:8090/display/EAI/PReB'
                }
            }
        }

        stage('Get Initiatives for Acceptance') {
            steps {
                script {
                    def headers = [[name: 'Authorization', value: env.JIRA_TOKEN]]
                    def responseForAcceptance = httpRequest(
                        url: 'http://jira:8080/rest/api/2/search?jql=filter=10400&fields=key,summary',
                        httpMode: 'GET',
                        contentType: 'APPLICATION_JSON',
                        customHeaders: headers,
                        validResponseCodes: '200',
                        consoleLogResponseBody: true
                    )

                    def jsonSlurper = new JsonSlurper()
                    def jsonResponseForAcceptance = jsonSlurper.parseText(responseForAcceptance.content)

                    jsonResponseForAcceptance.issues.each { issue ->
                        listForAcceptance += "<li><a href=\"http://localhost:8080/browse/${issue.key}\">${issue.key}</a> - ${issue.fields.summary}"
                    }

                    println "List for acceptance: ${listForAcceptance}"
                }
            }
        }

        stage('Get Initiatives for Approval') {
            steps {
                script {
                    def headers = [[name: 'Authorization', value: env.JIRA_TOKEN]]
                    def responseForApproval = httpRequest(
                        url: 'http://jira:8080/rest/api/2/search?jql=filter=10500&fields=key,summary',
                        httpMode: 'GET',
                        contentType: 'APPLICATION_JSON',
                        customHeaders: headers,
                        validResponseCodes: '200',
                        consoleLogResponseBody: true
                    )

                    def jsonSlurper = new JsonSlurper()
                    def jsonResponseForApproval = jsonSlurper.parseText(responseForApproval.content)

                    jsonResponseForApproval.issues.each { issue ->
                        listForApproval += "<li><a href=\"http://localhost:8080/browse/${issue.key}\">${issue.key}</a> - ${issue.fields.summary}"
                    }

                    println "List for approval: ${listForApproval}"
                }
            }
        }

        stage('Send email for PReB') {
            steps {
                script {
                    emailext(
                        body: """
                            <html>
                                <body>
                                    <h2>BUCs for PReB at ${currentDate}</h2>
                                    <p>For acceptance:</p>
                                    <ul>${listForAcceptance}</ul>
                                    <p>For approval:</p>
                                    <ul>${listForApproval}</ul>
                                </body>
                            </html>
                        """,
                        subject: "PReB ${currentDate}",
                        to: 'adauto.junior@live.com'
                    )
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

def getcurrentDateHour(format) {
    def sdf = new SimpleDateFormat(format)
    return sdf.format(new Date())
}
