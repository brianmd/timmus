FROM clojure:latest

RUN apt-get update && \
    apt-get install -y sudo && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN adduser --disabled-password --gecos '' clojure && \
    adduser clojure sudo && \
    echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

ENV MAVEN_VERSION="3.3.9" \
    M2_HOME=/usr/lib/mvn

RUN cd /tmp && \
  wget "http://ftp.unicamp.br/pub/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" && \
  tar -zxvf "apache-maven-$MAVEN_VERSION-bin.tar.gz" && \
  mv "apache-maven-$MAVEN_VERSION" "$M2_HOME" && \
  ln -s "$M2_HOME/bin/mvn" /usr/bin/mvn

RUN echo "cleaning up" && \
  # apk del wget && \
  rm -f /tmp/* /var/cache/apk/*

RUN mkdir -p /root/docker
WORKDIR /root
ENV HOME /root

# 7897=nrepl, 7002=cljs-nrepl, 3449=http
# EXPOSE 7897 7002 3449
EXPOSE 7000 3007 7002 3449

# WORKDIR /home/clojure
# ENV HOME /home/clojure
# VOLUME ["/home/clojure"]

# USER clojure

