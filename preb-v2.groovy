import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

def currentDateHour = getcurrentDateHour('MM/dd/yyyy HH:mm:ss')
def currentDate = getcurrentDateHour('MM/dd/yyyy')
def wikiTableForAcceptance = ''
def htmlTableForAcceptance = ''
def wikiTableForApproval = ''
def htmlTableForApproval = ''
def confluencePageTemplate = """
h1. PReB ${currentDateHour}

h2. BUCs for Acceptance
{{wikiTableForAcceptance}}

h2. BUCs for Approval
{{wikiTableForApproval}}

h2. Meeting notes
"""
def confluencePayload = """
{
    "type": "page",
    "title": "PReB ${currentDateHour}",
    "space": {
        "key": "EAI"
    },
    "ancestors": [
        {
            "id": "{{parentPage}}"
        }
    ],
    "body": {
        "wiki": {
            "value": "{{template}}",
            "representation": "wiki"
        }
    }
}
"""

pipeline {
    agent any

    environment {
        JIRA_TOKEN = 'Basic YWRhdXRvOmFkYXV0bw=='
        CONFLUENCE_TOKEN = 'Bearer NDcyNzIxODQ3ODg4OnZ2mWxAuTG0M2fjvz7zihRShmaQ'
    }

    stages {
        stage('Get Initiatives for Acceptance') {
            steps {
                script {
                    def responseForAcceptance = makeJiraRequest("10400", "key,summary,reporter,assignee")

                    def jsonSlurper = new JsonSlurper()
                    def jsonResponseForAcceptance = jsonSlurper.parseText(responseForAcceptance.content)

                    wikiTableForAcceptance = getWikiTableList(jsonResponseForAcceptance)
                    htmlTableForAcceptance = getHtmlTableList(jsonResponseForAcceptance)

                    println "List for acceptance: ${wikiTableForAcceptance}"
                }
            }
        }

        stage('Get Initiatives for Approval') {
            steps {
                script {
                    def responseForApproval = makeJiraRequest("10500", "key,summary,reporter,assignee")

                    def jsonSlurper = new JsonSlurper()
                    def jsonResponseForApproval = jsonSlurper.parseText(responseForApproval.content)

                    wikiTableForApproval = getWikiTableList(jsonResponseForApproval)
                    htmlTableForApproval = getHtmlTableList(jsonResponseForApproval)

                    println "List for approval: ${wikiTableForApproval}"
                }
            }
        }

        stage('Post Confluence page under PReB v2') {
            steps {
                script {
                    confluencePageTemplate = confluencePageTemplate.replace('{{wikiTableForAcceptance}}', wikiTableForAcceptance)
                    confluencePageTemplate = confluencePageTemplate.replace('{{wikiTableForApproval}}', wikiTableForApproval)
                    confluencePageTemplate = confluencePageTemplate.replace("\n", "\\n")

                    confluencePayload = confluencePayload.replace('{{template}}', confluencePageTemplate)
                    confluencePayload = confluencePayload.replace('{{parentPage}}', "${PReB_PAGE_ID}")

                    echo "Request payload:\n${confluencePayload}"

                    def headers = [[name: 'Authorization', value: env.CONFLUENCE_TOKEN]]

                    def response = httpRequest(
                        url: 'http://confluence:8090/rest/api/content',
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        requestBody: confluencePayload,
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

        stage('Send email for PReB') {
            steps {
                script {
                    emailext(
                        body: """
                            <html>
                                <head>
                                    <style type="text/css" data-hse-inline-css="true">
                                        table, th, td {
                                            border: 1px solid black;
                                            border-collapse: collapse;
                                            padding: 5px;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <h2>BUCs for PReB at ${currentDate}</h2>
                                    <h3>For acceptance:</h3>
                                    ${htmlTableForAcceptance}
                                    <h3>For approval:</h3>
                                    ${htmlTableForApproval}
                                </body>
                            </html>
                        """,
                        subject: "PReB ${currentDate}",
                        to: 'adauto.mendes@ericsson.com'
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
        }
    }
}

def getcurrentDateHour(format) {
    def sdf = new SimpleDateFormat(format)
    return sdf.format(new Date())
}

def makeJiraRequest(filter, fields) {
    def headers = [[name: 'Authorization', value: env.JIRA_TOKEN]]
    def jiraResponse = httpRequest(
        url: "http://jira:8080/rest/api/2/search?jql=filter=${filter}&fields=${fields}",
        httpMode: 'GET',
        contentType: 'APPLICATION_JSON',
        customHeaders: headers,
        validResponseCodes: '200',
        consoleLogResponseBody: true
    )

    return jiraResponse
}

def getWikiTableList(issueList) {
    def issueTable = ''

    issueTable += "||Key||Summary||Reporter||Assignee||Outcome||\n"
    issueList.issues.each { issue ->
        def reporter = issue.fields.reporter != null ? "[~${issue.fields.reporter.name}]" : "N/A"
        def assignee = issue.fields.assignee != null ? "[~${issue.fields.assignee.name}]" : "N/A"

        issueTable += "|[${issue.key}|http://localhost:8080/browse/${issue.key}]|${issue.fields.summary}|${reporter}|${assignee}| |\n"
    }

    return issueTable
}

def getHtmlTableList(issueList) {
    def htmlList = ''

    htmlList += "<table><tr><th>Key</th><th>Summary</th><th>Reporter</th><th>Assignee</th></tr>"
    issueList.issues.each { issue ->
        def reporter = issue.fields.reporter != null ? issue.fields.reporter.displayName : "N/A"
        def assignee = issue.fields.assignee != null ? issue.fields.assignee.displayName : "N/A"

        htmlList += "<tr><td><a href=\"http://localhost:8080/browse/${issue.key}\">${issue.key}</a></td><td>${issue.fields.summary}</td><td>${reporter}</td><td>${assignee}</td></tr>"
    }
    htmlList += "</table>"

    return htmlList
}
