max_stale = "10m"
retry     = "5s"
log_level = "warn"

vault {
    renew_token = true
    ssl {
        enabled = true
        verify = false
    }
    retry {
        enabled = true
        attempts = 0
    }
}

template {
    source = "/var/jenkins_home/jenkins_config/credentials.ctmpl"
    destination = "/var/jenkins_home/jenkins_config/credentials.txt"
    perms = 0600
    backup = true
}

template {
    source = "/var/jenkins_home/jenkins_config/kubernetes.ctmpl"
    destination = "/var/jenkins_home/jenkins_config/kubernetes.txt"
    perms = 0600
    backup = true
}