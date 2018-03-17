import hudson.model.*
import jenkins.model.*
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl

def domain = Domain.global()
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

def instance = System.getenv("JENKINS_INSTANCE_NAME").replaceAll('-','_')

ConfigObject conf = new ConfigSlurper().parse(new File(System.getenv("JENKINS_HOME")+'/jenkins_config/credentials.txt').text)

conf.common_credentials.each { key, credentials ->
    println("Adding common credential ${key}")
    store.addCredentials(domain, new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, key, credentials.description, credentials.username, credentials.password))
}


conf."${instance}_credentials".each { key, credentials ->
    println("Adding ${instance} credential ${key}")
    store.addCredentials(domain, new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, key, credentials.description, credentials.username, credentials.password))
}

println("Successfully configured credentials")