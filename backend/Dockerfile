FROM tomcat:9.0-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY build/libs/backend-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
COPY src/main/resources/google-credentials.json /opt/google-credentials.json

ENV GOOGLE_APPLICATION_CREDENTIALS=/opt/google-credentials.json
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
ENV LANG=ko_KR.UTF-8

EXPOSE 8080

CMD ["catalina.sh", "run"]
