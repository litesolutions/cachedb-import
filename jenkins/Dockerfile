FROM jenkins/jenkins:lts

USER root

COPY --chown=jenkins:jenkins ./build/libs/cachedb-import-*.jar /opt/cachedb/cachedb-import.jar

RUN --mount=type=bind,src=.,dst=/tmp/cachedb-import \
    cp /tmp/cachedb-import/jenkins/casc.yaml /usr/share/jenkins/ref/ && \
    cp /tmp/cachedb-import/jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt && \
    jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt && \
    cp -R /tmp/cachedb-import/jenkins/jobs /usr/share/jenkins/ref/jobs/

ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/casc.yaml

# COPY Cache-MDX2JSON2.xml /usr/share/jenkins/ref/jobs/Cache-MDX2JSON2/config.xml
# COPY Airplane.xml /usr/share/jenkins/ref/jobs/Airplane/config.xml