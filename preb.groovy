import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

def outputFileName = "preb_${getcurrentDateHour("yyyyMMdd_HHmmss")}.pdf"
def currentDateHour = getcurrentDateHour("MM/dd/yyyy HH:mm:ss")
def currentDate = getcurrentDateHour("MM/dd/yyyy")
def mailBody = ''

pipeline {
    agent any

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

                    echo "PDF uploaded to http://localhost:8090/display/EAI/PReB"
                }
            }
        }

        stage('Get Jira issues') {
            steps {
                script {
                    def headers = [[name: 'Authorization', value: "Basic YWRhdXRvOmFkYXV0bw=="]]
                    def response = httpRequest(
                        url: 'http://jira:8080/rest/api/2/search?jql=filter%3D10400&fields=key%2Csummary',
                        httpMode: 'GET',
                        contentType: 'APPLICATION_JSON',
                        customHeaders: headers,
                        validResponseCodes: '200',
                        consoleLogResponseBody: true
                    )

                    def jsonSlurper = new JsonSlurper()
                    def jsonResponse = jsonSlurper.parseText(response.content)

                    jsonResponse.issues.each { issue -> 
                        mailBody += "${issue.key} - ${issue.fields.summary} - Link: http://localhost:8080/browse/${issue.key}\n"
                    }

                    println "Body: ${mailBody}"
                }
            }
        }

        stage('Send email for PReB') {
            steps {
                script {
                    mail(
                        from: 'adauto.junior@live.com',
                        to: 'adauto.junior@live.com',
                        subject: "PReB ${currentDate}",
                        body: "BUCs for PReB at ${currentDate}:\n\n${mailBody}"
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
