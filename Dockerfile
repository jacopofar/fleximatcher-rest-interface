FROM java:8-jdk
RUN apt-get update && apt-get install -y maven
RUN update-java-alternatives -s java-1.8.0-openjdk-amd64
RUN git clone https://github.com/jacopofar/fleximatcher.git
RUN cd fleximatcher && mvn clean install
ADD . /fleximatcher-web-interface
RUN cd fleximatcher-web-interface && mvn install
WORKDIR /fleximatcher-web-interface
CMD ["mvn","exec:java"]
