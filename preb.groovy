import java.text.SimpleDateFormat

def outputFileName = "preb_${getCurrentDate("yyyyMMdd_HHmmss")}.pdf"
def currentDate = getCurrentDate("MM/dd/yyyy HH:mm:ss")

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
                        curl -u "adauto:adauto" -X POST -H "X-Atlassian-Token: nocheck" -F "file=@${outputFileName}" -F "comment=File attached at ${currentDate}" "${url}" 2>/dev/null
                    """

                    echo "PDF uploaded to http://localhost:8090/display/EAI/PReB"
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

def getCurrentDate(format) {
    def sdf = new SimpleDateFormat(format)
    return sdf.format(new Date())
}
