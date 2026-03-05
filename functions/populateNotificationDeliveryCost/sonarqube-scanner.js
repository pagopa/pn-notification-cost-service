let options = {
    "sonar.organization": "pagopa",
    "sonar.projectKey": "pagopa_pn-notification-cost-service-populateNotificationDeliveryCost"
}

if (process.env.PR_NUM) {
    options["sonar.pullrequest.base"] = process.env.BRANCH_TARGET;
    options["sonar.pullrequest.branch"] = process.env.BRANCH_NAME;
    options["sonar.pullrequest.key"] = process.env.PR_NUM;
}

process.env.SONAR_SCANNER_OPTS = "-Dsonar.scanner.skipJreProvisioning=true";

const scanner = require("sonarqube-scanner");

scanner(
  {
    serverUrl: "https://sonarcloud.io",
    options: options
  },
  () => process.exit()
);