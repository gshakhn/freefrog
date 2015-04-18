FROM dockerfile/java:oracle-java8
MAINTAINER Courage Labs, LLC <tech@couragelabs.com>

RUN sudo apt-get update

ADD target/freefrog-0.0.1-SNAPSHOT-standalone.jar /srv/freefrog.jar

EXPOSE 3000

CMD ["java", "-server", "-jar", "/srv/freefrog.jar"]
