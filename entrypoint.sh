#!/usr/bin/env bash

git clone ssh://git@your_scm_here/jenkins_config_as_code.git ${JENKINS_HOME}/jenkins_config
mv ${JENKINS_HOME}/jenkins_config/*.groovy ${JENKINS_HOME}/init.groovy.d/

consul-template \
  -consul-addr "$CONSUL_ADDR" \
  -vault-addr "$VAULT_ADDR" \
  -config "jenkins_config.hcl" \
  -once

