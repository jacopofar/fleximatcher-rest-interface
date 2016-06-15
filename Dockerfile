FROM java:8-jdk
RUN apt-get update && apt-get install -y maven
RUN update-java-alternatives -s java-1.8.0-openjdk-amd64
RUN cd /opt && git clone https://github.com/jacopofar/fleximatcher.git
RUN cd /opt/fleximatcher && mvn clean install
ADD . /opt/fleximatcher-web-interface
RUN cd /opt/fleximatcher-web-interface && mvn install
WORKDIR /opt/fleximatcher-web-interface
CMD ["mvn","exec:java"]
