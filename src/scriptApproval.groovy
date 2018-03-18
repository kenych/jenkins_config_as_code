import org.jenkinsci.plugins.scriptsecurity.scripts.*

ScriptApproval script = ScriptApproval.get()

ConfigObject conf = new ConfigSlurper().parse(new File(System.getenv("JENKINS_HOME") + '/jenkins_config/scriptApproval.txt').text)

conf.scriptApproval.approvedSignatures.each{ approvedSignature ->
  println("checking for new signature ${approvedSignature}")

  def found = script.approvedSignatures.find { it == approvedSignature }

  if (!found){
    println("Approving signature ${approvedSignature}")
    script.approveSignature(approvedSignature)
  }

}


